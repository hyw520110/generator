package org.hyw.tools.generator.metadata;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlPrimaryKey;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.Naming;
import org.hyw.tools.generator.exception.DatabaseException;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 SQL 文件的元数据读取器
 * <p>
 * 通过解析 DDL 文件（CREATE TABLE 语句）提取表结构元数据，作为 JDBC 元数据查询的替代方案。
 * 适用于以下场景：
 * 1. 无法连接目标数据库（如离线环境、生产库禁止直连）
 * 2. 需要基于 SQL 脚本预生成代码（如新项目初始化前快速生成脚手架）
 * 3. 需要 100% 还原 DDL 脚本中的注释、字段约束、默认值（避免 information_schema 掩盖）
 * <p>
 * 解析引擎使用 Druid 内置的 SQL AST 解析器，不能用正则表达式，因为正则会被换行、
 * 函数默认值、嵌套约束等特殊格式击穿。
 *
 * @author heyiwu
 * @version 2.0
 */
public class SqlFileMetadataReader implements MetadataReader {

    private static final Logger logger = LoggerFactory.getLogger(SqlFileMetadataReader.class);

    /**
     * SQL 方言固定为 mysql，当前项目 DDL 均使用 MySQL 方言
     */
    private static final String SQL_DIALECT = "mysql";

    /**
     * SQL 文件扩展名
     */
    private static final String SQL_FILE_SUFFIX = ".sql";

    private final DataSourceConf dataSource;
    private final GlobalConf global;

    public SqlFileMetadataReader(DataSourceConf dataSource, GlobalConf global) {
        if (dataSource == null) {
            throw new IllegalArgumentException("数据源配置不能为空");
        }
        this.dataSource = dataSource;
        this.global = global;
    }

    @Override
    public List<Table> readAllTables() {
        List<Table> tables = new ArrayList<>();
        String sqlPath = dataSource.getSqlPath();
        if (StringUtils.isBlank(sqlPath)) {
            logger.warn("SQL 文件路径未配置（dataSource.sqlPath 为空），无法读取表元数据");
            return tables;
        }

        File targetFile = resolveSqlFile(sqlPath);
        if (!targetFile.exists()) {
            logger.warn("SQL 文件或目录不存在: {}", sqlPath);
            return tables;
        }

        List<File> sqlFiles = findSqlFiles(targetFile);
        if (sqlFiles.isEmpty()) {
            logger.warn("未在指定路径下找到任何 .sql 文件: {}", sqlPath);
            return tables;
        }

        logger.info("开始解析 SQL 文件，共 {} 个文件，来源路径: {}", sqlFiles.size(), sqlPath);
        for (File sqlFile : sqlFiles) {
            try {
                String sqlContent = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);
                // Druid 1.2.23 解析 ADD COLUMN IF NOT EXISTS 可能会报错，所以预处理替换掉
                sqlContent = sqlContent.replaceAll("ADD COLUMN IF NOT EXISTS", "ADD COLUMN");

                List<SQLStatement> stmtList = SQLUtils.parseStatements(sqlContent, SQL_DIALECT);
                logger.debug("解析文件: {}，共 {} 条 SQL 语句", sqlFile.getName(), stmtList.size());

                for (SQLStatement stmt : stmtList) {
                    if (!(stmt instanceof MySqlCreateTableStatement)) {
                        continue;
                    }
                    Table table = parseCreateTable((MySqlCreateTableStatement) stmt);
                    if (table != null) {
                        tables.add(table);
                        logger.debug("成功解析表: {}（字段数: {}）", table.getName(), table.getFieldsSize());
                    }
                }
            } catch (Exception e) {
                logger.error("解析 SQL 文件失败: {}", sqlFile.getAbsolutePath(), e);
                throw new DatabaseException("解析 SQL 文件失败: " + sqlFile.getAbsolutePath(), e);
            }
        }
        logger.info("SQL 文件解析完成，共读取 {} 张表", tables.size());
        return tables;
    }

    /**
     * 智能解析 SQL 文件路径（支持 classpath:、~、以及多模块工程下的向上级回溯相对路径）
     */
    private File resolveSqlFile(String sqlPath) {
        if (sqlPath.startsWith("~")) {
            sqlPath = sqlPath.replaceFirst("^~", System.getProperty("user.home"));
        }
        File targetFile = new File(sqlPath);
        if (targetFile.exists()) {
            return targetFile;
        }
        if (sqlPath.startsWith("classpath:")) {
            String cpPath = sqlPath.substring("classpath:".length());
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(cpPath);
            if (url != null) {
                return new File(url.getFile());
            }
        } else if (!targetFile.isAbsolute()) {
             // 向上级目录追溯寻找（适配多模块工程在子模块启动时，读取根目录下的 sql 相对路径）
             File current = new File(System.getProperty("user.dir")).getAbsoluteFile();
             while (current != null) {
                 File candidate = new File(current, sqlPath);
                 if (candidate.exists()) {
                     return candidate;
                 }
                 // 若找到带有 .git 的根目录，则不再往上找
                 if (new File(current, ".git").exists()) {
                     break;
                 }
                 current = current.getParentFile();
             }
        }
        return targetFile;
    }

    /**
     * 从 CREATE TABLE 语句解析单张表
     */
    private Table parseCreateTable(MySqlCreateTableStatement createStmt) {
        // 表名去掉反引号包装
        String tableName = stripBackticks(createStmt.getTableName());
        if (StringUtils.isBlank(tableName)) {
            logger.warn("CREATE TABLE 语句缺少表名，跳过");
            return null;
        }

        // 表注释
        String tableComment = "";
        if (createStmt.getComment() != null) {
            tableComment = stripQuotes(createStmt.getComment().toString());
        }

        Table table = new Table(tableName, tableComment);
        table.setBeanName(StringUtils.capitalFirst(processName(tableName)));

        // 先收集表级独立主键定义（如 PRIMARY KEY (`id`)），用于回填列级未标记的字段
        List<String> tableLevelPrimaryKeys = collectTableLevelPrimaryKeys(createStmt);

        // 解析列定义
        for (SQLColumnDefinition column : createStmt.getColumnDefinitions()) {
            parseColumn(column, table, tableLevelPrimaryKeys);
        }

        return table;
    }

    /**
     * 解析列定义并添加到表中
     */
    private void parseColumn(SQLColumnDefinition column, Table table, List<String> tableLevelPrimaryKeys) {
        String colName = stripBackticks(column.getName().getSimpleName());
        if (StringUtils.isBlank(colName)) {
            return;
        }

        // 列类型，如 VARCHAR / INT / BIGINT / DATETIME
        String colType = column.getDataType() != null ? column.getDataType().getName() : "";
        String colComment = column.getComment() != null ? stripQuotes(column.getComment().toString()) : "";

        TabField field = new TabField(colName, colType);
        field.setComment(colComment);

        // 主键：列级 PRIMARY KEY 或 表级 PRIMARY KEY (col) 命中
        boolean isPrimaryKey = column.isPrimaryKey() || tableLevelPrimaryKeys.contains(colName);
        field.setPrimarykey(isPrimaryKey);

        // 自增标识（对应 JDBC 模式从 extra='auto_increment' 读取）
        field.setIdentity(column.isAutoIncrement());

        // 可空性：DDL 中未显式声明 NOT NULL 时，MySQL 默认允许 NULL
        // containsNotNullConstaint() 返回 true 表示有 NOT NULL 约束
        field.setNullAble(!column.containsNotNullConstaint());

        // 类型转换：SQL_FILE 模式下 JDBC URL 可能为空，TypeConvertor 可能为 null
        TypeConvertor convertor = dataSource.getTypeConvertor();
        if (convertor != null) {
            KeyPair<String, FieldType> pair = convertor.convert(colType);
            if (pair != null) {
                field.setJdbcType(pair.getKey());
                field.setFieldType(pair.getValue());
            }
        } else {
            logger.warn("TypeConvertor 未初始化（可能未配置 JDBC URL），字段 {} 的 Java 类型转换被跳过", colName);
        }

        field.setPropertyName(processName(colName));

        // 与 DatabaseMetadataReader 一致：若去前缀/转驼峰后与已存在字段重名，
        // 退化用原始列名+驼峰规则再次映射（contianField 按 name 比较，addField 会跳过同名行）
        table.addField(field);
        if (table.containField(field) && global != null) {
            char[] seps = global.getSeparators() != null ? global.getSeparators() : new char[]{'_', '-'};
            field.setPropertyName(StringUtils.toCamelCase(field.getName(), seps, false));
        }
    }

    /**
     * 收集表级独立主键定义（PRIMARY KEY (`col1`, `col2`)），返回去反引号的主键列名列表
     */
    private List<String> collectTableLevelPrimaryKeys(MySqlCreateTableStatement createStmt) {
        List<String> primaryKeys = new ArrayList<>();
        for (SQLTableElement element : createStmt.getTableElementList()) {
            if (element instanceof MySqlPrimaryKey) {
                MySqlPrimaryKey pk = (MySqlPrimaryKey) element;
                pk.getColumns().forEach(item -> {
                    String name = extractColumnName(item.getExpr());
                    if (name != null) {
                        primaryKeys.add(name);
                    }
                });
            }
        }
        return primaryKeys;
    }

    /**
     * 从 SQLExpr 提取列名（兼容 SQLIdentifierExpr 和带反引号的字符串）
     */
    private String extractColumnName(com.alibaba.druid.sql.ast.SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return stripBackticks(((SQLIdentifierExpr) expr).getSimpleName());
        }
        // 其他类型退化到 toString 后去反引号
        return stripBackticks(expr.toString());
    }

    /**
     * 去除反引号包装（`col_name` -> col_name）
     */
    private static String stripBackticks(String name) {
        if (name == null) {
            return null;
        }
        return name.replace("`", "");
    }

    /**
     * 去除字符串字面量的单引号包裹（'注释内容' -> 注释内容）
     */
    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "");
    }

     /**
     * 递归查找 SQL 文件
     */
    private List<File> findSqlFiles(File target) {
        List<File> sqlFiles = new ArrayList<>();
        if (target.isDirectory()) {
            File[] files = target.listFiles();
            if (files != null) {
                for (File file : files) {
                    sqlFiles.addAll(findSqlFiles(file));
                }
            }
        } else if (target.getName().endsWith(SQL_FILE_SUFFIX)) {
            sqlFiles.add(target);
        }
        return sqlFiles;
    }

    /**
     * 处理名称：根据 global 配置的命名策略转换表名/字段名
     */
    private String processName(String name) {
        if (global == null) {
            return name;
        }
        if (!global.isCapitalMode() && StringUtils.isCapitalMode(name)) {
            name = name.toLowerCase();
        }
        String[] tablePrefix = global.getTablePrefix();
        if (tablePrefix != null && tablePrefix.length >= 1) {
            name = StringUtils.removePrefix(name, tablePrefix);
        }
        if (global.getNaming() == Naming.NOCHANGE) {
            return name;
        }
        if (global.getNaming() == Naming.TOCAMEL) {
            return StringUtils.removePrefixAndCamel(name, tablePrefix, global.getSeparators());
        }
        if (global.getNaming() == Naming.TOPASCAL) {
            return StringUtils.removePrefixAndPascal(name, tablePrefix, global.getSeparators());
        }
        return name;
    }

    @Override
    public List<Table> readTables(TableFilter filter) {
        List<Table> allTables = readAllTables();
        List<Table> result = new ArrayList<>();
        for (Table table : allTables) {
            if (filter == null || filter.matches(table.getName())) {
                result.add(table);
            }
        }
        return result;
    }

    @Override
    public Table readTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return null;
        }
        for (Table table : readAllTables()) {
            if (table.getName().equalsIgnoreCase(tableName)) {
                return table;
            }
        }
        return null;
    }

    @Override
    public List<String> getAllTableNames() {
        List<String> names = new ArrayList<>();
        for (Table table : readAllTables()) {
            names.add(table.getName());
        }
        return names;
    }

    @Override
    public boolean tableExists(String tableName) {
        return readTable(tableName) != null;
    }

    @Override
    public String getSourceDescription() {
        return "SQL 文件: " + dataSource.getSqlPath();
    }
}
