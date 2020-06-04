package org.hyw.tools.generator.enums.db;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.enums.FieldType;

/**
 * Oracle字段类型与java属性类型映射
 */
public enum OracleColumnType  {
    chars("CHAR",FieldType.STRING),
    DATE("DATE",FieldType.DATE),
    TIMESTAMP("TIMESTAMP",FieldType.DATE),
    NUMBER("NUMBER",FieldType.DOUBLE),
    FLOAT("FLOAT",FieldType.FLOAT),
    BLOB("BLOB",FieldType.BYTE_ARRAY),
    RAW("RAW",FieldType.BYTE_ARRAY);
    
    private final String columnType;
    private FieldType javaDataType;

    OracleColumnType(String columnType, FieldType javaDataType) {
        this.columnType = columnType;
        this.javaDataType = javaDataType;
    }
    
    public static OracleColumnType getMySqlColumnType(String fieldType){
        OracleColumnType[] values = values();
        for (OracleColumnType columnType : values) {
            if(!StringUtils.contains(fieldType, columnType.getColumnType())){
                continue;
            }
            if(!columnType.getColumnType().equals(NUMBER.getColumnType())){
                return columnType; 
            }    
            if (fieldType.matches("NUMBER\\(+\\d\\)")) {
                columnType.setJavaDataType(FieldType.INTEGER);
            } else if (fieldType.matches("NUMBER\\(+\\d{2}+\\)")) {
                columnType.setJavaDataType(FieldType.LONG); 
            }else{
                columnType.setJavaDataType(FieldType.LONG); 
            }
            return columnType;
        }
       return chars;
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
