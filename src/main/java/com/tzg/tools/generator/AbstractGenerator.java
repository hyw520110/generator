package com.tzg.tools.generator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tzg.tools.generator.conf.BaseBean;
import com.tzg.tools.generator.conf.ComponentConf;
import com.tzg.tools.generator.conf.GlobalConf;
import com.tzg.tools.generator.conf.StrategyConf;
import com.tzg.tools.generator.conf.dao.DataSourceConf;
import com.tzg.tools.generator.conf.dao.QuerySQL;
import com.tzg.tools.generator.conf.db.TabField;
import com.tzg.tools.generator.conf.db.Table;
import com.tzg.tools.generator.enums.Component;
import com.tzg.tools.generator.enums.DBType;
import com.tzg.tools.generator.enums.Naming;
import com.tzg.tools.generator.utils.StringUtils;

public abstract class AbstractGenerator extends BaseBean {

    private static final long   serialVersionUID = 1L;
    private static final Logger logger           = LoggerFactory.getLogger(AbstractGenerator.class.getName());

    /**
     * 数据源配置
     */
    protected DataSourceConf dataSource;
    /**
     * 全局配置
     */
    protected GlobalConf     global;
    /**
     * 策略配置   
     */
    protected StrategyConf   strategy;

    /**
     * 包配置详情
     */
    protected Map<String, File> paths;

    public List<Table> getTables() throws Exception {
        Connection con = null;
        PreparedStatement pst = null;
        ArrayList<Table> tables = new ArrayList<>();
        try {
            con = dataSource.getCon();
            QuerySQL sql = dataSource.getQuerySQL();
            pst = con.prepareStatement(sql.getTabComments());
            ResultSet results = pst.executeQuery();
            while (results.next()) {
                String tabName = results.getString(sql.getTbName());
                if (StringUtils.isEmpty(tabName)) {
                    System.err.println("当前数据库为空！！！");
                    break;
                }
                if (!StringUtils.contains(strategy.getInclude(), tabName, true) || StringUtils.contains(strategy.getExclude(), tabName, false)) {
                    continue;
                }
                String tableComment = results.getString(sql.getTbComment());
                Table table = new Table(tabName);
                table.setComment(tableComment);
                table.setEntityName(StringUtils.capitalFirst(processName(tabName)));
                tables.add(this.setTableFields(table, con));
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (pst != null) {
                pst.close();
            }
            if (con != null) {
                con.close();
            }
        }
        return tables;
    }

    /**
     * <p>
     * 将字段信息与表信息关联
     * </p>
     *
     * @param table 表信息
     * @param strategy  命名策略
     * @return
     * @throws SQLException 
     */
    private Table setTableFields(Table table, Connection con) throws SQLException {
        List<TabField> fields = new ArrayList<>();
        List<TabField> commonFields = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            QuerySQL sql = dataSource.getQuerySQL();
            pst = con.prepareStatement(String.format(sql.getTbFields(), table.getName()));
            ResultSet results = pst.executeQuery();
            while (results.next()) {
                TabField field = new TabField();
                String key = results.getString(sql.getFieldKeyValue().getKey());
                //是否主键 TODO 复合主键处理
                field.setKey(StringUtils.equals(key, sql.getFieldKeyValue().getValue()));
                //其他数据库的字段是否为空以及自增 处理
                if (DBType.MYSQL == this.dataSource.getDbType()) {
                    field.setNullAble(Boolean.valueOf(results.getString(sql.getFieldNull())));
                    field.setIdentity(StringUtils.equals(results.getString(sql.getExtraKeyValue().getKey()), sql.getExtraKeyValue().getValue()));
                }
                field.setName(results.getString(sql.getFieldName()));
                field.setType(results.getString(sql.getFieldType()));

                //处理字段名
                field.setPropertyName(processName(field.getName()));
                //转换字段类型
                field.setColumnType(dataSource.getTypeConvertor().convert(field.getType()));
                field.setComment(results.getString(sql.getFieldComment()));
                if (StringUtils.contains(strategy.getSuperEntityColumns(), field.getName(), false)) {
                    // 跳过公共字段
                    commonFields.add(field);
                    continue;
                }
                fields.add(field);
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception：" + e.getMessage());
        } finally {
            if (pst != null) {
                pst.close();
            }
        }
        table.setFields(fields);
        table.setCommonFields(commonFields);
        return table;
    }

    /**
     * 处理字段名称
     * @param name
     * @return 根据策略返回处理后的名称
     */
    private String processName(String name) {
        if (!strategy.isCapitalMode() && StringUtils.isCapitalMode(name)) {
            name = name.toLowerCase();
        }
        String[] tablePrefix = strategy.getTablePrefix();
        if (tablePrefix != null && tablePrefix.length >= 1) {
            // 删除前缀
            name = StringUtils.removePrefix(name, tablePrefix);
        }
        if (strategy.getNaming() == Naming.NOCHANGE) {
            return name;
        }
        if (strategy.getNaming() == Naming.TOCAMEL) {
            // 删除前缀、下划线转驼峰
            return StringUtils.removePrefixAndCamel(name, tablePrefix, strategy.getSeparators());
        }
        return name;
    }

    public void mkdirs() {
        // 生成路径信息 TODO 
        paths = new HashMap<>();
        String[] dirs = new String[] { global.getSourceDirectory(), global.getTestSourceDirectory(), global.getResource(), global.getTestResource() };
        Collection<String> modules = global.getModules().values();
        for (String module : modules) {
            File dir = new File(global.getOutputDir(), module);
            if (paths.containsKey(module)) {
                continue;
            }
            paths.put(module, dir);
            for (String d : dirs) {
                File file = new File(dir, StringUtils.isBlank(d) ? "" : d);
                if (file.exists()) {
                    continue;
                }
                file.mkdirs();
            }
        }
    }

    protected void openDir() {
        String dir = global.getOutputDir();
        try {
            String osName = System.getProperty("os.name");
            if (!global.isDirOpen() || StringUtils.isEmpty(osName)) {
                logger.info("文件已生成:{}", dir);
                return;
            }
            //打开windows or Mac的输出目录
            Runtime.getRuntime().exec((osName.contains("Windows") ? "cmd /c start " : "open ") + dir);
        } catch (IOException e) {
            logger.error("打开目录:{},发生异常:{}", dir, e.getLocalizedMessage());
        }
    }

    protected void close(Closeable... args) {
        if (null == args || args.length == 0) {
            return;
        }
        for (Closeable arg : args) {
            if (null != arg) {
                try {
                    arg.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public DataSourceConf getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSourceConf dataSource) {
        this.dataSource = dataSource;
    }

    public GlobalConf getGlobal() {
        return global;
    }

    public void setGlobal(GlobalConf global) {
        this.global = global;
    }

    public StrategyConf getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyConf strategy) {
        this.strategy = strategy;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
