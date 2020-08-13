package org.hyw.tools.generator.conf.converts.impl;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.db.MySqlColumnType;

public class SqlServerTypeConvert implements TypeConvertor {

	@Override
	public KeyPair<String, FieldType> convert(String fieldType) {
		MySqlColumnType.getMySqlColumnType(StringUtils.lowerCase(fieldType)).getJavaType();
		return null;
	}

}
