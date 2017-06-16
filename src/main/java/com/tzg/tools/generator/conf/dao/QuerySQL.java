package com.tzg.tools.generator.conf.dao;

import com.tzg.tools.generator.conf.BaseBean;
import com.tzg.tools.generator.conf.KeyPair;

public class QuerySQL extends BaseBean {

    private static final long       serialVersionUID = 1L;
    /**
     * 查看所有表
     */
    private String                  showTables;
    /**
     * 表注释
     */
    private String                  tabComments;
    /**
     * 表字段
     */
    private String                  tbFields;
    /**
     * 表名
     */
    private String                  tbName;
    /**
     * 表注释
     */
    private String                  tbComment;
    /**
     * 字段名
     */
    private String                  fieldName;
    /**
     * 字段类型
     */
    private String                  fieldType;
    /**
     * 字段注释
     */
    private String                  fieldComment;
    /**
     * 字段是否为主键的key TODO 更新配置  
     */
    private KeyPair<String, String> fieldKeyValue;
    /**
     * 字段是否为null
     */
    private String                  fieldNull;
    /**
     * 特别信息 是否自增 mysql值为auto_increment表示自增
     */
    private KeyPair<String, String> extraKeyValue;

    public String getShowTables() {
        return showTables;
    }

    public void setShowTables(String showTables) {
        this.showTables = showTables;
    }

    public String getTabComments() {
        return tabComments;
    }

    public void setTabComments(String tabComments) {
        this.tabComments = tabComments;
    }

    public String getTbFields() {
        return tbFields;
    }

    public void setTbFields(String tbFields) {
        this.tbFields = tbFields;
    }

    public String getTbName() {
        return tbName;
    }

    public void setTbName(String tbName) {
        this.tbName = tbName;
    }

    public String getTbComment() {
        return tbComment;
    }

    public void setTbComment(String tbComment) {
        this.tbComment = tbComment;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldComment() {
        return fieldComment;
    }

    public void setFieldComment(String fieldComment) {
        this.fieldComment = fieldComment;
    }

    public String getFieldNull() {
        return fieldNull;
    }

    public void setFieldNull(String fieldNull) {
        this.fieldNull = fieldNull;
    }

    public KeyPair<String, String> getFieldKeyValue() {
        return fieldKeyValue;
    }

    public void setFieldKeyValue(KeyPair<String, String> fieldKeyValue) {
        this.fieldKeyValue = fieldKeyValue;
    }

    public KeyPair<String, String> getExtraKeyValue() {
        return extraKeyValue;
    }

    public void setExtraKeyValue(KeyPair<String, String> extraKeyValue) {
        this.extraKeyValue = extraKeyValue;
    }

}
