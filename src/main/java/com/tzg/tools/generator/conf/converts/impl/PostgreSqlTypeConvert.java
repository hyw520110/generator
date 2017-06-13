package com.tzg.tools.generator.conf.converts.impl;

import org.apache.commons.lang.StringUtils;

import com.tzg.tools.generator.conf.converts.TypeConvertor;
import com.tzg.tools.generator.enums.FieldType;
import com.tzg.tools.generator.enums.PostgreSqlColumnType;

public class PostgreSqlTypeConvert implements TypeConvertor {

    @Override
    public FieldType convert(String fieldType) {
        return PostgreSqlColumnType.getMySqlColumnType(StringUtils.lowerCase(fieldType)).getJavaDataType();
    }

}
