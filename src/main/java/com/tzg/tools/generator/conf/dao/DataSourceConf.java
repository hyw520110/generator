package com.tzg.tools.generator.conf.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.yaml.snakeyaml.Yaml;

import com.tzg.tools.generator.conf.BaseBean;
import com.tzg.tools.generator.conf.converts.TypeConvertor;
import com.tzg.tools.generator.conf.converts.impl.MySqlTypeConvert;
import com.tzg.tools.generator.conf.converts.impl.OracleTypeConvert;
import com.tzg.tools.generator.conf.converts.impl.PostgreSqlTypeConvert;
import com.tzg.tools.generator.conf.converts.impl.SqlServerTypeConvert;
import com.tzg.tools.generator.enums.DBType;

/**
 * 
 * Filename:    DataSourceConf.java  
 * Description: 数据源配置    
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     tzg.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年6月13日 上午10:03:24  
 *
 */
public class DataSourceConf extends BaseBean{
     
    private static final long serialVersionUID = 1L;
    /**
     * 驱动名称
     */
    private String driverName;
    /**
     * 驱动连接的URL
     */
    private String url;

    /**
     * 数据库连接用户名
     */
    private String userName;
    /**
     * 数据库连接密码
     */
    private String password;

    /**
     * 数据库类型
     */
    private DBType        dbType;
    /**
     * 类型转换
     */
    private TypeConvertor typeConvertor;

    private QuerySQL querySQL;

    public Connection getCon() throws Exception {
        try {
            Class.forName(getDriverName());
            return DriverManager.getConnection(getUrl(), getUserName(), getPassword());
        } catch (ClassNotFoundException | SQLException e) {
            throw e;
        }
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
        this.setDbType(DBType.getDbType(driverName));
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DBType getDbType() {
        return dbType;
    }

    public void setDbType(DBType dbType) {
        this.dbType = dbType;
        this.querySQL = new Yaml().loadAs(getClass().getResourceAsStream(String.format("/conf/%s.yaml", dbType.getValue())), QuerySQL.class);
        TypeConvertor typeConvertor = null;
        switch (dbType) {
            case ORACLE:
                typeConvertor = new OracleTypeConvert();
                break;
            case SQL_SERVER:
                typeConvertor = new SqlServerTypeConvert();
                break;
            case POSTGRE_SQL:
                typeConvertor = new PostgreSqlTypeConvert();
                break;
            default:
                typeConvertor = new MySqlTypeConvert();
                break;
        }
        this.setTypeConvertor(typeConvertor);
    }

    public TypeConvertor getTypeConvertor() {
        return typeConvertor;
    }

    public void setTypeConvertor(TypeConvertor typeConvertor) {
        this.typeConvertor = typeConvertor;
    }

    public QuerySQL getQuerySQL() {
        return querySQL;
    }

    public void setQuerySQL(QuerySQL querySQL) {
        this.querySQL = querySQL;
    }
}
