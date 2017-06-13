package com.tzg.tools.generator.conf.dao;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class QuerySQL {
    /**
     * 查看所有表
     */
    private String showTables;
    /**
     * 表注释
     */
    private String tabComments;
    /**
     * 表字段
     */
    private String tbFields;
    /**
     * 表名
     */
    private String tbName;
    /**
     * 表注释
     */
    private String tbComment;
    /**
     * 字段类型
     */
    private String fieldName;
    /**
     * 字段类型
     */
    private String fieldType;
    /**
     * 字段注释
     */
    private String fieldComment;
    /**
     * key
     */
    private String fieldKey;

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

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
