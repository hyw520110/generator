package org.hyw.tools.generator.metadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.BooleanUtils;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.dao.QuerySQL;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.conf.db.TableRelation;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.Naming;
import org.hyw.tools.generator.enums.db.DBType;
import org.hyw.tools.generator.exception.GeneratorException;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 数据库元数据读取器
 * <p>
 * 主要改进:
 * 1. 增强异常处理和错误报告
 * 2. 支持失败后继续处理 (或快速失败)
 * 3. 添加读取统计
 * 4. 使用 try-with-resources 管理资源
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class DatabaseMetadataReader implements MetadataReader {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataReader.class);

    private final DataSourceConf dataSource;
    private final GlobalConf global;

    /**
     * 成功读取的表数量
     */
    private final AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 失败的表数量
     */
    private final AtomicInteger failedCount = new AtomicInteger(0);

    /**
     * 失败的表信息列表
     */
    private final List<FailedTable> failedTables = new ArrayList<>();

    /**
     * 是否快速失败模式
     */
    private boolean failFast = false;

    public DatabaseMetadataReader(DataSourceConf dataSource, GlobalConf global) {
        if (dataSource == null) {
            throw new IllegalArgumentException("数据源配置不能为空");
        }
        this.dataSource = dataSource;
        this.global = global;
    }

    @Override
    public List<Table> readAllTables() {
        return readTables(TableFilter.includeAll());
    }

    @Override
    public List<Table> readTables(TableFilter filter) {
        // 重置统计
        successCount.set(0);
        failedCount.set(0);
        failedTables.clear();

        List<Table> tables = new ArrayList<>();
        QuerySQL sql = dataSource.getQuerySQL();

        logger.debug("开始读取数据库元数据...");

        // 使用 try-with-resources 管理资源
        try (Connection con = dataSource.getCon();
             PreparedStatement pst = con.prepareStatement(sql.getTabComments())) {

            try (ResultSet results = pst.executeQuery()) {
                while (results.next()) {
                    String tabName = results.getString(sql.getTbName());
                    if (StringUtils.isEmpty(tabName)) {
                        logger.warn("数据库表查询为空！包含规则：{}",
                                filter != null ? Arrays.toString(filter.getInclude()) : "无");
                        break;
                    }

                    // 应用过滤器
                    if (filter != null && !filter.matches(tabName)) {
                        continue;
                    }

                    Table table = new Table(tabName, results.getString(sql.getTbComment()));
                    table.setBeanName(StringUtils.capitalFirst(processName(tabName)));
                    table.setCreateTime(results.getString(sql.getTbCreateTime()));

                    try (Statement st = con.createStatement()) {
                        tables.add(setTableFields(table, st));
                        successCount.incrementAndGet();
                        logger.debug("成功读取表：{}", tabName);

                    } catch (SQLException e) {
                        logger.error("读取表 {} 字段信息失败", tabName, e);
                        failedCount.incrementAndGet();
                        failedTables.add(new FailedTable(tabName, e.getMessage()));

                        // 记录错误但继续处理下一张表 (除非快速失败模式)
                        if (failFast) {
                            throw new GeneratorException(
                                GeneratorException.ErrorCode.DATABASE_METADATA_READ_FAILED,
                                e,
                                dataSource.getDbName() + "." + tabName
                            );
                        }
                    }
                }
            }

            // 输出读取报告
            logReadSummary();

        } catch (GeneratorException e) {
            throw e;
        } catch (Exception e) {
            logger.error("读取表元数据失败", e);
            throw new GeneratorException(
                GeneratorException.ErrorCode.DATABASE_METADATA_READ_FAILED,
                e,
                "未知错误：" + e.getMessage()
            );
        }

        return tables;
    }

    @Override
    public Table readTable(String tableName) {
        List<Table> tables = readTables(TableFilter.include(tableName));
        return tables.isEmpty() ? null : tables.get(0);
    }

    @Override
    public List<String> getAllTableNames() {
        List<String> names = new ArrayList<>();
        try {
            List<Table> tables = readAllTables();
            for (Table table : tables) {
                names.add(table.getName());
            }
        } catch (GeneratorException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取所有表名失败", e);
            throw new GeneratorException(
                GeneratorException.ErrorCode.DATABASE_QUERY_FAILED,
                e,
                e.getMessage()
            );
        }
        return names;
    }

    @Override
    public boolean tableExists(String tableName) {
        return readTable(tableName) != null;
    }

    @Override
    public String getSourceDescription() {
        return dataSource.getDBType() != null ? dataSource.getDBType().getName() + " 数据库" : "数据库";
    }

    /**
     * 设置表字段信息
     */
    private Table setTableFields(Table table, Statement st) throws SQLException {
        ResultSet results = null;
        try {
            QuerySQL sql = dataSource.getQuerySQL();
            results = st.executeQuery(String.format(sql.getTbFields(), table.getName()));

            while (results.next()) {
                TabField field = new TabField(results.getString(sql.getFieldName()),
                        results.getString(sql.getFieldType()));
                field.setComment(results.getString(sql.getFieldComment()));

                // 转换字段类型
                KeyPair<String, FieldType> pair = dataSource.getTypeConvertor().convert(field.getType());
                field.setJdbcType(pair.getKey());
                field.setFieldType(pair.getValue());

                // 处理字段名
                field.setPropertyName(processName(field.getName()));

                // 是否主键
                String key = results.getString(sql.getFieldKeyValue().getKey());
                field.setPrimarykey(StringUtils.equals(key, sql.getFieldKeyValue().getValue()));

                // 数据库特定的字段属性处理
                if (dataSource.getDBType().isMySQL()) {
                    field.setNullAble(BooleanUtils.toBoolean(results.getString(sql.getFieldNull())));
                    field.setIdentity(StringUtils.equals(results.getString(sql.getExtraKeyValue().getKey()),
                            sql.getExtraKeyValue().getValue()));
                }

                table.addField(field);

                // 字段名处理后是否重名
                if (table.containField(field)) {
                    field.setPropertyName(StringUtils.toCamelCase(field.getName(),
                            global != null ? global.getSeparators() : new char[]{'_', '-'}, false));
                }
            }
        } catch (SQLException e) {
            logger.error("读取表 {} 字段信息失败", table.getName(), e);
            throw e;
        }

        return table;
    }

    /**
     * 处理字段名称
     */
    private String processName(String name) {
        if (global == null) {
            return name;
        }

        // 处理大写命名
        if (!global.isCapitalMode() && StringUtils.isCapitalMode(name)) {
            name = name.toLowerCase();
        }

        // 删除表前缀
        String[] tablePrefix = global.getTablePrefix();
        if (tablePrefix != null && tablePrefix.length >= 1) {
            name = StringUtils.removePrefix(name, tablePrefix);
        }

        // 应用命名策略
        if (global.getNaming() == Naming.NOCHANGE) {
            return name;
        }

        if (global.getNaming() == Naming.TOCAMEL) {
            return StringUtils.removePrefixAndCamel(name, tablePrefix,
                    global.getSeparators());
        }

        if (global.getNaming() == Naming.TOPASCAL) {
            return StringUtils.removePrefixAndPascal(name, tablePrefix,
                    global.getSeparators());
        }

        return name;
    }

    /**
     * 输出读取报告
     */
    private void logReadSummary() {
        logger.info("========== 数据库元数据读取报告 ==========");
        logger.info("成功：{} 个表", successCount.get());
        logger.info("失败：{} 个表", failedCount.get());

        if (!failedTables.isEmpty()) {
            logger.warn("失败的表:");
            for (FailedTable failed : failedTables) {
                logger.warn("  - {}: {}", failed.tableName, failed.errorMessage);
            }
        }
        logger.info("========================================");
    }

    /**
     * 获取成功读取的表数量
     */
    public int getSuccessCount() {
        return successCount.get();
    }

    /**
     * 获取失败的表数量
     */
    public int getFailedCount() {
        return failedCount.get();
    }

    /**
     * 获取失败的表信息列表
     */
    public List<FailedTable> getFailedTables() {
        return new ArrayList<>(failedTables);
    }

    /**
     * 设置是否快速失败模式
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * 是否快速失败模式
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * 失败的表信息
     */
    @Data
    public static class FailedTable {
        public final String tableName;
        public final String errorMessage;
        
        public FailedTable(String tableName, String errorMessage) {
            this.tableName = tableName;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 获取表关系（外键关系）
     * 
     * @param tableNames 要查询关系的表名列表
     * @return 表关系列表
     */
    public List<TableRelation> getTableRelations(List<String> tableNames) {
        List<TableRelation> relations = new ArrayList<>();
        
        if (tableNames == null || tableNames.isEmpty()) {
            return relations;
        }
        
        Connection con = null;
        try {
            // 使用 DriverManager 直接获取连接，避免连接池已关闭的问题
            String url = dataSource.getDBType().buildUrl(dataSource.getIp(), dataSource.getPort(), dataSource.getDbName());
            logger.info("获取数据库连接 - IP: {}, 端口: {}, 数据库: {}, 用户: {}", 
                dataSource.getIp(), dataSource.getPort(), dataSource.getDbName(), dataSource.getUsername());
            con = java.sql.DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword());
            
            DatabaseMetaData metaData = con.getMetaData();
            String catalog = con.getCatalog();
            String schema = getSchema(con);
            
            // 获取表注释映射（使用同一个连接）
            Map<String, String> tableComments = getTableComments(tableNames, con);
            
            for (String tableName : tableNames) {
                try {
                    // 先获取该表所有列的可空性信息
                    Map<String, Boolean> columnNullable = getColumnNullableMap(metaData, catalog, schema, tableName);
                    
                    // 获取该表引用的其他表（外键关系）
                    try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
                        while (rs.next()) {
                            TableRelation relation = new TableRelation();
                            relation.setSourceTable(tableName);
                            relation.setSourceComment(tableComments.get(tableName));
                            relation.setTargetTable(rs.getString("PKTABLE_NAME"));
                            relation.setTargetComment(tableComments.get(rs.getString("PKTABLE_NAME")));
                            relation.setFkColumn(rs.getString("FKCOLUMN_NAME"));
                            relation.setPkColumn(rs.getString("PKCOLUMN_NAME"));
                            relation.setFkName(rs.getString("FK_NAME"));
                            
                            // 设置外键列是否可为空
                            String fkColumn = relation.getFkColumn();
                            relation.setNullable(columnNullable.getOrDefault(fkColumn, true));
                            
                            relations.add(relation);
                            logger.debug("发现表关系: {}", relation.getRelationDesc());
                        }
                    }
                } catch (SQLException e) {
                    logger.warn("获取表 {} 的外键关系失败: {}", tableName, e.getMessage());
                }
            }
            
            logger.info("共发现 {} 个表关系", relations.size());
            
        } catch (Exception e) {
            logger.error("获取表关系失败", e);
            throw new GeneratorException(
                GeneratorException.ErrorCode.DATABASE_QUERY_FAILED,
                e,
                "获取表关系失败：" + e.getMessage()
            );
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.warn("关闭数据库连接失败", e);
                }
            }
        }
        
        return relations;
    }
    
    /**
     * 获取表所有列的可空性映射
     */
    private Map<String, Boolean> getColumnNullableMap(DatabaseMetaData metaData, String catalog, String schema, String tableName) {
        Map<String, Boolean> nullableMap = new HashMap<>();
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int nullable = rs.getInt("NULLABLE");
                // DatabaseMetaData.columnNullable = 1, DatabaseMetaData.columnNoNulls = 0
                nullableMap.put(columnName, nullable == DatabaseMetaData.columnNullable);
            }
        } catch (SQLException e) {
            logger.warn("获取表 {} 的列可空性失败: {}", tableName, e.getMessage());
        }
        return nullableMap;
    }
    
    /**
     * 获取所有表的关系（外键关系）
     */
    public List<TableRelation> getAllTableRelations() {
        List<String> tableNames = getAllTableNames();
        return getTableRelations(tableNames);
    }
    
    /**
     * 获取表注释映射（自行管理连接）
     */
    private Map<String, String> getTableComments(List<String> tableNames) {
        Map<String, String> comments = new HashMap<>();
        QuerySQL sql = dataSource.getQuerySQL();
        
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = dataSource.getCon();
            pst = con.prepareStatement(sql.getTabComments());
            rs = pst.executeQuery();
            
            while (rs.next()) {
                String tabName = rs.getString(sql.getTbName());
                if (tableNames.contains(tabName)) {
                    comments.put(tabName, rs.getString(sql.getTbComment()));
                }
            }
        } catch (Exception e) {
            logger.warn("获取表注释失败", e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { }
            }
            if (pst != null) {
                try { pst.close(); } catch (SQLException e) { }
            }
            if (con != null) {
                try { con.close(); } catch (SQLException e) { }
            }
        }
        
        return comments;
    }
    
    /**
     * 获取表注释映射（使用传入的连接，由调用方管理连接生命周期）
     */
    private Map<String, String> getTableComments(List<String> tableNames, Connection con) {
        Map<String, String> comments = new HashMap<>();
        QuerySQL sql = dataSource.getQuerySQL();
        
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement(sql.getTabComments());
            rs = pst.executeQuery();
            
            while (rs.next()) {
                String tabName = rs.getString(sql.getTbName());
                if (tableNames.contains(tabName)) {
                    comments.put(tabName, rs.getString(sql.getTbComment()));
                }
            }
        } catch (Exception e) {
            logger.warn("获取表注释失败", e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { }
            }
            if (pst != null) {
                try { pst.close(); } catch (SQLException e) { }
            }
            // 不关闭 con，由调用方管理
        }
        
        return comments;
    }
    
    /**
     * 获取数据库 schema
     */
    private String getSchema(Connection con) throws SQLException {
        DBType dbType = dataSource.getDBType();
        if (dbType == DBType.POSTGRE_SQL) {
            return "public";
        } else if (dbType == DBType.ORACLE) {
            return con.getSchema();
        }
        // MySQL 默认使用 null（查询所有 schema）
        return null;
    }
}
