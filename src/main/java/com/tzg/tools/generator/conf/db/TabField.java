package com.tzg.tools.generator.conf.db;

import com.tzg.tools.generator.conf.BaseBean;
import com.tzg.tools.generator.enums.FieldType;

public class TabField extends BaseBean {
    private static final long serialVersionUID = 1L;
    private boolean           keyFlag;
    /**
     * 主键是否为自增类型
     */
    private boolean           keyIdentityFlag;
    /**
     * 字段名
     */
    private String            name;
    /**
     * 字段类型
     */
    private String            type;
    /**
     * java属性名
     */
    private String            propertyName;
    /**
     * 属性类型
     */
    private FieldType         fieldType;
    /**
     * 字段说明
     */
    private String            comment;

    public boolean isKeyFlag() {
        return keyFlag;
    }

    public void setKeyFlag(boolean keyFlag) {
        this.keyFlag = keyFlag;
    }

    public boolean isKeyIdentityFlag() {
        return keyIdentityFlag;
    }

    public void setKeyIdentityFlag(boolean keyIdentityFlag) {
        this.keyIdentityFlag = keyIdentityFlag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public FieldType getColumnType() {
        return fieldType;
    }

    public void setColumnType(FieldType columnType) {
        this.fieldType = columnType;
    }

    public String getPropertyType() {
        if (null != fieldType) {
            return fieldType.getType();
        }
        return null;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * 按JavaBean规则来生成get和set方法
     */
    public String getCapitalName() {
        if (propertyName.length() <= 1) {
            return propertyName.toUpperCase();
        }
        // 第一个字母 小写、 第二个字母 大写 ，特殊处理
        String firstChar = propertyName.substring(0, 1);
        if (Character.isLowerCase(firstChar.toCharArray()[0]) && Character.isUpperCase(propertyName.substring(1, 2).toCharArray()[0])) {
            return firstChar.toLowerCase() + propertyName.substring(1);
        }
        return firstChar.toUpperCase() + propertyName.substring(1);
    }

    public boolean isNameChange() {
        return !getName().equals(getPropertyName());
    }

}
