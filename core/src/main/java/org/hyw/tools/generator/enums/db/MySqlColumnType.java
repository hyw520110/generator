package org.hyw.tools.generator.enums.db;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.enums.FieldType;

/**
 * MySql字段类型与java属性类型映射
 * 
JDBC Type           Java Type  
CHAR                String  
VARCHAR             String  
LONGVARCHAR         String  
NUMERIC             java.math.BigDecimal  
DECIMAL             java.math.BigDecimal  
BIT             boolean  
BOOLEAN             boolean  
TINYINT             byte  
SMALLINT            short  
INTEGER             int  
BIGINT              long  
REAL                float  
FLOAT               double  
DOUBLE              double  
BINARY              byte[]  
VARBINARY           byte[]  
LONGVARBINARY               byte[]  
DATE                java.sql.Date  
TIME                java.sql.Time  
TIMESTAMP           java.sql.Timestamp  
CLOB                Clob  
BLOB                Blob  
ARRAY               Array  
DISTINCT            mapping of underlying type  
STRUCT              Struct  
REF                         Ref  
DATALINK            java.net.URL
 */
public enum MySqlColumnType  {
    chars("char","CHAR",FieldType.STRING),
    VARCHAR("varchar","VARCHAR",FieldType.STRING),
    TINYTEXT("tinytext","CLOB",FieldType.STRING),
    TEXT("text","CLOB",FieldType.STRING),
    MEDIUMTEXT("mediumtext","CLOB",FieldType.STRING),
    LONGTEXT("longtext","CLOB",FieldType.STRING),
    TINYBLOB("tinyblob","BLOB",FieldType.BYTE_ARRAY),
    BLOB("blob","BLOB",FieldType.BYTE_ARRAY),
    MEDIUMBLOB("mediumblob","BLOB",FieldType.BYTE_ARRAY),
    LONGBLOB("LONGBLOB","BLOB",FieldType.BYTE_ARRAY),
    TINYINT("tinyint","TINYINT",FieldType.INTEGER),
    SMALLINT("smallint","SMALLINT",FieldType.INTEGER),
    MEDIUMINT("mediumint","INTEGER",FieldType.INTEGER),
	INT("int","INTEGER",FieldType.LONG),
    BIGINT("bigint","BIGINT",FieldType.LONG), 
    FLOAT("float","FLOAT",FieldType.FLOAT),
    DOUBLE("double","DOUBLE",FieldType.DOUBLE),
    DATE("date","DATE",FieldType.DATE),
    TIME("time","TIME",FieldType.DATE),
    YEAR("year","YEAR",FieldType.DATE),
    DATETIME("datetime","TIMESTAMP",FieldType.TIMESTAMP),
    TIMESTAMP("TIMESTAMP","TIMESTAMP",FieldType.TIMESTAMP),
    DECIMAL("decimal","DECIMAL",FieldType.BIG_DECIMAL),
    BIT("bit","BIT",FieldType.BOOLEAN);
 
    

    private final String columnType;
    private final String jdbcType;
    private FieldType javaType;

    MySqlColumnType(String columnType, String jdbcType,FieldType javaType) {
        this.columnType = columnType;
        this.jdbcType=jdbcType;
        this.javaType = javaType;
    }
    
    public static MySqlColumnType getMySqlColumnType(String fieldType){
        MySqlColumnType[] values = values();
        for (MySqlColumnType type : values) {
            if(StringUtils.equalsIgnoreCase(fieldType, type.getColumnType())){
                return type;
            }
        }
       return TEXT;
    }
    public FieldType getJavaType() {
        return javaType;
    }

    public void setJavaType(FieldType javaType) {
        this.javaType = javaType;
    }

    public String getColumnType() {
        return columnType;
    }

	public String getJdbcType() {
		return jdbcType;
	}
}
