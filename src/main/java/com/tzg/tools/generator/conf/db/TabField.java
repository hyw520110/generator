package com.tzg.tools.generator.conf.db;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tzg.tools.generator.conf.StrategyConf;
import com.tzg.tools.generator.enums.FieldType;
import com.tzg.tools.generator.utils.StringUtils;

public class TabField {


    private boolean convert;
    private boolean keyFlag;
    /**
     * 主键是否为自增类型
     */
    private boolean keyIdentityFlag;
    private String name;
    private String type;
    private String propertyName;
    private FieldType fieldType;
    private String comment;

    public boolean isConvert() {
        return convert;
    }

    protected void setConvert(StrategyConf conf) {
        if (conf.isCapitalModeNaming(name)) {
            this.convert = false;
        } else {
            // 转换字段
            if (conf.columnUnderline) {
                // 包含大写处理
                if (StringUtils.containsUpperCase(name)) {
                    this.convert = true;
                }
            } else if (!name.equals(propertyName)) {
                this.convert = true;
            }
        }
    }

    public void setConvert(boolean convert) {
        this.convert = convert;
    }

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

    public void setPropertyName(StrategyConf strategyConfig, String propertyName) {
        this.propertyName = propertyName;
        this.setConvert(strategyConfig);
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
        if (Character.isLowerCase(firstChar.toCharArray()[0])
                && Character.isUpperCase(propertyName.substring(1, 2).toCharArray()[0])) {
            return firstChar.toLowerCase() + propertyName.substring(1);
        }
        return firstChar.toUpperCase() + propertyName.substring(1);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
