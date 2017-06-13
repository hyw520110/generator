package com.tzg.tools.generator.enums;

public enum DBType {
    MYSQL("mysql"), ORACLE("oracle"), SQL_SERVER("sqlserver"), POSTGRE_SQL("postgresql");

    private final String value;

    DBType(String value) {
        this.value = value;
    }

    public static DBType getDbType(String driverName) {
        DBType[] values = values();
        for (DBType dbType : values) {
            if(driverName.contains(dbType.getValue())){
                return dbType;
            }
        }
        throw new IllegalArgumentException("unknown type of database!");
    }

    public String getValue() {
        return value;
    }
}
