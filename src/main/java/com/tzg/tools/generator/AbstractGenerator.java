package com.tzg.tools.generator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tzg.tools.generator.conf.ComponentConf;
import com.tzg.tools.generator.conf.Consts;
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

public abstract class AbstractGenerator {
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
    protected Map<String, String> packages;

    /**
     * 包配置详情
     */
    protected Map<String, String> paths;

    public void initConf() {
        initPackages();
        initPaths();
    }

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
        boolean haveId = false;
        List<TabField> fields = new ArrayList<>();
        List<TabField> commonFields = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            QuerySQL sql = dataSource.getQuerySQL();
            pst = con.prepareStatement(String.format(sql.getTbFields(), table.getName()));
            ResultSet results = pst.executeQuery();
            while (results.next()) {
                TabField field = new TabField();
                String key = results.getString(sql.getFieldKey());
                // 避免多重主键设置，目前只取第一个找到ID，并放到list中的索引为0的位置
                boolean isId = StringUtils.isNotEmpty(key) && key.toUpperCase().equals("PRI");
                // 处理ID
                if (isId && !haveId) {
                    field.setKeyFlag(true);
                    if (DBType.MYSQL == this.dataSource.getDbType() && "auto_increment".equals(results.getString("Extra"))) {
                        field.setKeyIdentityFlag(true);
                    }
                    haveId = true;
                } else {
                    field.setKeyFlag(false);
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
        if (StringUtils.isCapitalMode(name) && !strategy.isCapitalMode()) {
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
            return StringUtils.removePrefixAndCamel(name, tablePrefix);
        }
        return name;
    }

    public void mkdirs() {
        // TODO 
    }

    private void initPaths() {
        // 生成路径信息 TODO 
        paths = new HashMap<>();
        String template = getClass().getResource("/templates").getFile();
        Map<Component, ComponentConf> map = global.getComponentConfs();
        for (Entry<Component, ComponentConf> entry : map.entrySet()) {
            String name = entry.getKey().name();
            File[] files = new File(template, name).listFiles();
            for (File file : files) {
                paths.put(concat(name, file.getName()), file.getAbsolutePath());
            }
        }
    }

    private void initPackages() {
        packages = new HashMap<>();
        packages.put(Consts.MODULENAME, strategy.getModuleName());
        packages.put(Consts.ENTITY, concat(strategy.getRootPackage(), strategy.getEntity()));
        //TODO
        //        packages.put(Consts.MAPPER, concat(strategy.getRootPackage(), global.getComponentConfs().values().iterator().next().getResourceDir()));
        //        packages.put(Consts.XML, concat(strategy.getRootPackage(), global.getComponentConfs().values().iterator().next().getResourceDir()));

        packages.put(Consts.SERIVCE, concat(strategy.getRootPackage(), strategy.getService()));
        packages.put(Consts.SERVICEIMPL, concat(strategy.getRootPackage(), strategy.getServiceImpl()));
    }

    /**
     * 连接父子包名
     * @param rootPackage     父包名
     * @param subPackage 子包名
     * @return 连接后的包名
     */
    private String concat(String rootPackage, String subPackage) {
        if (StringUtils.isEmpty(rootPackage)) {
            return subPackage;
        }
        return rootPackage.concat(".").concat(subPackage);
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

    public Map<String, String> getPackages() {
        return packages;
    }

    public void setPackages(Map<String, String> packages) {
        this.packages = packages;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
