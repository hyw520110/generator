package org.hyw.tools.generator.enums.db;

import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.conf.converts.impl.MySqlTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.OracleTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.PostgreSqlTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.SqlServerTypeConvert;

public enum DBType {
	MYSQL("mysql", "com.mysql.jdbc.Driver","jdbc:mysql://%s:%s/%s?connectTimeout=3000&socketTimeout=10000&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false","jdbc:mysql://%s:%s?connectTimeout=3000&socketTimeout=10000&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false",new MySqlTypeConvert()), 
	ORACLE("oracle", "oracle.jdbc.driver.OracleDriver","jdbc:oracle:thin:@%s:%s:%s","jdbc:oracle:thin:@%s:%s",new OracleTypeConvert()),
	SQL_SERVER("sqlserver","com.microsoft.sqlserver.jdbc.SQLServerDriver","jdbc:sqlserver://%s:%s;databaseName=%s","jdbc:sqlserver://%s:%s", new SqlServerTypeConvert()), 
	POSTGRE_SQL("postgresql","org.postgresql.Driver","jdbc:postgresql://%s/%s","jdbc:postgresql://%s", new PostgreSqlTypeConvert());

	private final String name;
	private String driver;
	private String url;
	private String urlWithoutDb;
	private TypeConvertor convertor;

	DBType(String value,String driver,String url, String urlWithoutDb, TypeConvertor convertor) {
		this.name = value;
		this.driver=driver;
		this.url=url;
		this.urlWithoutDb = urlWithoutDb;
		this.convertor = convertor;
	}

	public static String[] getDbNames() {
		DBType[] values = values();
		String[] names = new String[values.length];
		for (int i = 0; i < names.length; ) {
			names[i]=values[i++].getName();
		}
		return names;
	}

	public static DBType getDbType(String name) {
		DBType[] values = values();
		for (DBType dbType : values) {
			if (name.equalsIgnoreCase(dbType.getName())) {
				return dbType;
			}
		}
		throw new IllegalArgumentException("unknown type of database!");
	}

	public String getName() {
		return name;
	}

	public TypeConvertor getConvertor() {
		return convertor;
	}

	public void setConvertor(TypeConvertor convertor) {
		this.convertor = convertor;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getUrlWithoutDb() {
		return urlWithoutDb;
	}
	
	/**
	 * 判断是否为 MySQL 数据库
	 */
	public boolean isMySQL() {
		return this == MYSQL;
	}
	
	/**
	 * 获取驱动类名
	 */
	public String getDriverClassName() {
		return driver;
	}
	
	/**
	 * 构建服务器URL（不包含数据库名）
	 */
	public String buildUrl(String host, String port) {
		return String.format(urlWithoutDb, host, port);
	}
	
	/**
	 * 构建完整的数据库URL
	 */
	public String buildUrl(String host, String port, String dbName) {
		return String.format(url, host, port, dbName);
	}
	
	/**
	 * 获取数据库列表查询SQL
	 */
	public String getDatabaseListSql() {
		switch (this) {
			case MYSQL:
				return "SHOW DATABASES";
			case ORACLE:
				return "SELECT username FROM all_users ORDER BY username";
			case SQL_SERVER:
				return "SELECT name FROM sys.databases WHERE database_id > 4 ORDER BY name";
			case POSTGRE_SQL:
				return "SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname";
			default:
				throw new IllegalArgumentException("不支持的数据库类型: " + this.name);
		}
	}
	
	/**
	 * 获取创建数据库的 SQL
	 */
	public String getCreateDatabaseSql(String dbName) {
		switch (this) {
			case MYSQL:
				return "CREATE DATABASE IF NOT EXISTS `" + dbName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
			case ORACLE:
				return "CREATE TABLESPACE " + dbName + " DATAFILE '" + dbName + ".dbf' SIZE 100M";
			case SQL_SERVER:
				return "CREATE DATABASE [" + dbName + "]";
			case POSTGRE_SQL:
				return "CREATE DATABASE " + dbName + " WITH ENCODING='UTF8'";
			default:
				throw new IllegalArgumentException("不支持的数据库类型: " + this.name);
		}
	}
}
