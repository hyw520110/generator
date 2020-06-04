package org.hyw.tools.generator.enums.db;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.enums.FieldType;

/**
 * MySql字段类型与java属性类型映射
 */
public enum PostgreSqlColumnType  {
    chars("char",FieldType.STRING),
    TEXT("text",FieldType.STRING),
    BIGINT("bigint",FieldType.LONG), 
    INT("int",FieldType.INTEGER),
    
    DATE("date",FieldType.DATE),
    TIME("time",FieldType.DATE),
    YEAR("year",FieldType.DATE),
    BIT("bit",FieldType.BOOLEAN),
    
    BLOB("blob",FieldType.BYTE_ARRAY),
    FLOAT("float",FieldType.FLOAT),
    DOUBLE("double",FieldType.DOUBLE),
    JSON("json",FieldType.STRING),
    ENUM("enum",FieldType.STRING);
//	 DECIMAL("decimal",FieldType.BIG_DECIMAL),    
    private final String columnType;
    private FieldType javaDataType;

    PostgreSqlColumnType(String columnType, FieldType javaDataType) {
        this.columnType = columnType;
        this.javaDataType = javaDataType;
    }
    
    public static PostgreSqlColumnType getMySqlColumnType(String fieldType){
        PostgreSqlColumnType[] values = values();
        for (PostgreSqlColumnType columnType : values) {
            if(StringUtils.contains(fieldType, columnType.getColumnType())){
                return columnType;
            }
        }
       return TEXT;
    }
    public FieldType getJavaDataType() {
        return javaDataType;
    }

    public void setJavaDataType(FieldType javaDataType) {
        this.javaDataType = javaDataType;
    }

    public String getColumnType() {
        return columnType;
    }
}
