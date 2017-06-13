package com.tzg.tools.generator.conf.converts.impl;

import org.apache.commons.lang.StringUtils;

import com.tzg.tools.generator.conf.converts.TypeConvertor;
import com.tzg.tools.generator.enums.FieldType;
import com.tzg.tools.generator.enums.MySqlColumnType;

public class MySqlTypeConvert implements TypeConvertor {

    @Override
    public FieldType convert(String fieldType) {
        return MySqlColumnType.getMySqlColumnType(StringUtils.lowerCase(fieldType)).getJavaDataType();
    }

}
