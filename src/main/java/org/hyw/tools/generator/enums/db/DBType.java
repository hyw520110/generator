package org.hyw.tools.generator.enums.db;

import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.conf.converts.impl.MySqlTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.OracleTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.PostgreSqlTypeConvert;
import org.hyw.tools.generator.conf.converts.impl.SqlServerTypeConvert;

public enum DBType {
	MYSQL("mysql", new MySqlTypeConvert()), 
	ORACLE("oracle", new OracleTypeConvert()),
	SQL_SERVER("sqlserver",new SqlServerTypeConvert()), 
	POSTGRE_SQL("postgresql",new PostgreSqlTypeConvert());

	private final String value;
	private TypeConvertor convertor;

	DBType(String value, TypeConvertor convertor) {
		this.value = value;
		this.convertor = convertor;
	}

	public static DBType getDbType(String driverName) {
		DBType[] values = values();
		for (DBType dbType : values) {
			if (driverName.contains(dbType.getValue())) {
				return dbType;
			}
		}
		throw new IllegalArgumentException("unknown type of database!");
	}

	public String getValue() {
		return value;
	}

	public TypeConvertor getConvertor() {
		return convertor;
	}

	public void setConvertor(TypeConvertor convertor) {
		this.convertor = convertor;
	}
}
