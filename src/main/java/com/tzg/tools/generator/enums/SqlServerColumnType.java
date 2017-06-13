package com.tzg.tools.generator.enums;

import org.apache.commons.lang.StringUtils;

/**
 * SqlServer字段类型与java属性类型映射
 */
public enum SqlServerColumnType  {
    
    chars("char",FieldType.STRING),
    TEXT("text",FieldType.STRING),
    XML("xml",FieldType.STRING),
    BIGINT("bigint",FieldType.LONG), 
    INT("int",FieldType.INTEGER),
    DATE("date",FieldType.DATE),
    TIME("time",FieldType.DATE),
    BIT("bit",FieldType.BOOLEAN),
    NUMERIC("numeric",FieldType.DOUBLE),
    DECIMAL("decimal",FieldType.BIG_DECIMAL),
    MONEY("money",FieldType.BIG_DECIMAL),
    BINARY("binary",FieldType.BYTE_ARRAY),
    IMAGE("image",FieldType.BYTE_ARRAY),
    FLOAT("float",FieldType.FLOAT),    
    REAL("real",FieldType.FLOAT);

    private final String columnType;
    private FieldType javaDataType;

    SqlServerColumnType(String columnType, FieldType javaDataType) {
        this.columnType = columnType;
        this.javaDataType = javaDataType;
    }
    
    public static SqlServerColumnType getMySqlColumnType(String fieldType){
        SqlServerColumnType[] values = values();
        for (SqlServerColumnType columnType : values) {
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
