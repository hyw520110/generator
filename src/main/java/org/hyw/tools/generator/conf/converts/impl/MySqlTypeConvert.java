package org.hyw.tools.generator.conf.converts.impl;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.db.MySqlColumnType;

public class MySqlTypeConvert implements TypeConvertor {

    @Override
    public KeyPair<String, FieldType> convert(String columnType) {
        MySqlColumnType type = MySqlColumnType.getMySqlColumnType(StringUtils.substringBefore(columnType,"("));
        return new KeyPair<>(type.getJdbcType(),type.getJavaType());
    }

}
