package org.hyw.tools.generator.enums;

/**
 * java数据类型Local*属于java8中新增的类 
 */
public enum FieldType {
    STRING("String", null),
    LONG("Long", null),
    INTEGER("Integer", null),
    FLOAT("Float", null),
    DOUBLE("Double", null),
    BOOLEAN("Boolean", null),
    BYTE_ARRAY("byte[]", null),
    CHARACTER("Character", null),
    OBJECT("Object", null),
    DATE("Date", java.util.Date.class),
    TIME("Time", java.sql.Time.class),
    BLOB("Blob", java.sql.Blob.class),
    CLOB("Clob",java.sql.Clob.class),
    TIMESTAMP("Timestamp",java.sql.Timestamp.class);
/*  ,BIG_INTEGER("BigInteger",java.math.BigInteger.class),
    BIG_DECIMAL("BigDecimal",java.math.BigDecimal.class),
    LOCAL_DATE("LocalDate",java.time.LocalDate.class),
    LOCAL_TIME("LocalTime",java.time.LocalTime.class),
    LOCAL_DATE_TIME("LocalDateTime",java.time.LocalDateTime.class);
*/
    /**
     * 类型
     */
    private final String type;
    /**
     * 类
     */
    private final Class<?> claz;
    
    private FieldType(String type,Class<?> claz) {
        this.type=type;
        this.claz=claz;
    }

    public String getType() {
        return type;
    }

    public Class<?> getClaz() {
        return claz;
    }
}
