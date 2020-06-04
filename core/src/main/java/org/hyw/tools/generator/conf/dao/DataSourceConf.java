package org.hyw.tools.generator.conf.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.enums.db.DBType;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * 
 * Filename: DataSourceConf.java Description: 数据源配置 Copyright: Copyright (c)
 * 2015-2018 All Rights Reserved. Company: org.hyw.cn Inc.
 * 
 * @author: heyiwu
 * @version: 1.0 Create at: 2017年6月13日 上午10:03:24
 *
 */
public class DataSourceConf extends DruidDataSource {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DataSourceConf.class);
	/**
	 * 数据库类型
	 */
	private DBType dbType;
	/**
	 * 密码副本(密文密码),用于生成的配置文件配置密文密码,因为配置的密文密码，在属性设值时密码已解密，后续会获取不到密文密码
	 */
	private String pwd;
	private String filter;

	private String conProperties;
	/**
	 * 类型转换
	 */
	private TypeConvertor typeConvertor;

	private QuerySQL querySQL;

	public Connection getCon() throws Exception {
		try {
			return getConnection();
		} catch (SQLException e) {
			throw e;
		}
	}

	public String getPropertiesStr() {
		Properties props = super.getConnectProperties();
		StringBuilder builder = new StringBuilder();
		for (Object key : props.keySet()) {
			builder.append(key.toString() + "=" + props.get(key));
		}
		return builder.toString();
	}

	@Override
	public void setUrl(String jdbcUrl) {
		super.setUrl(jdbcUrl);

	}

	public DBType getDBType() {
		if (dbType != null) {
			return dbType;
		}
		try {
			super.init();
		} catch (SQLException e) {
			logger.error("数据库连接池初始化异常{}", e.getClass(), e);
		}
		dbType = DBType.getDbType(super.getDbType());
		return dbType;
	}

	public void setDBType(DBType dbType) {
		this.dbType = dbType;
	}

	public TypeConvertor getTypeConvertor() {
		if (null != typeConvertor) {
			return typeConvertor;
		}
		DBType dbType = getDBType();
		if (null == dbType) {
			return null;
		}
		this.typeConvertor = dbType.getConvertor();
		return typeConvertor;
	}

	public void setTypeConvertor(TypeConvertor typeConvertor) {
		this.typeConvertor = typeConvertor;
	}

	public QuerySQL getQuerySQL() {
		if (null == querySQL) {
			String conf = String.format("/conf/%s.yml", getDBType().getName());
			System.out.println("load conf:" + conf);
			this.querySQL = new Yaml().loadAs(DataSourceConf.class.getResourceAsStream(conf), QuerySQL.class);
		}
		return querySQL;
	}

	public void setQuerySQL(QuerySQL querySQL) {
		this.querySQL = querySQL;
	}

	public String getPwd() {
		if (StringUtils.isEmpty(pwd)) {
			pwd = getPassword();
		}
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
		super.setPassword(pwd);
	}

	public String getProperty() {
		Properties properties = super.getConnectProperties();
		if (properties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (Object key : properties.keySet()) {
			if (key.toString().contains("password")) {
				continue;
			}
			builder.append(key + "=" + properties.getProperty(key.toString(), "") + ";");
		}
		return builder.deleteCharAt(builder.length() - 1).toString();
	}

	public String getFilter() {
		return this.filter;
	}

	public void setFilters(String filters) throws SQLException {
		super.setFilters(filters);
		this.filter = filters;
	}
	
	public boolean isEncrypt() {
		return StringUtils.contains(getPropertiesStr(), "config.decrypt.key");
	}
	
	public String getConProperties() {
		return conProperties;
	}
	
	public void setConProperties(String connectionProperties) {
		this.conProperties = connectionProperties;
		super.setConnectionProperties(connectionProperties);
	}
	
	public String getIp() {
		return StringUtils.substringBetween(getUrl(), "//", ":");
	}
	
	public String getPort() {
		return StringUtils.substringBetween(getUrl(), getIp()+":","/");
	}
	public String getDbName() {
		return StringUtils.substringBetween(getUrl(), getPort()+"/","?");
	}
}
