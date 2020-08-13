package org.hyw.tools.generator.enums.db;

import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.conf.converts.impl.MySqlTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.OracleTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.PostgreSqlTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.SqlServerTypeConvert;

public enum DBType {
	MYSQL("mysql", "com.mysql.jdbc.Driver","jdbc:mysql://%s:%s/%s?connectTimeout=3000&socketTimeout=10000&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false",new MySqlTypeConvert()), 
	ORACLE("oracle", "oracle.jdbc.driver.OracleDriver","jdbc:oracle:thin:@%s:%s:%s",new OracleTypeConvert()),
	SQL_SERVER("sqlserver","com.microsoft.sqlserver.jdbc.SQLServerDriver","jdbc:sqlserver://%s:%s", new SqlServerTypeConvert()), 
	POSTGRE_SQL("postgresql","org.postgresql.Driver","jdbc:postgresql://%s/%s", new PostgreSqlTypeConvert());

	private final String name;
	private String driver;
	private String url;
	private TypeConvertor convertor;

	DBType(String value,String driver,String url, TypeConvertor convertor) {
		this.name = value;
		this.driver=driver;
		this.url=url;
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
}
