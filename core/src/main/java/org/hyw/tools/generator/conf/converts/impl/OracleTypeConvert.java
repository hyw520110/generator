package org.hyw.tools.generator.conf.converts.impl;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.db.OracleColumnType;

public class OracleTypeConvert implements TypeConvertor {

	@Override
	public KeyPair<String, FieldType> convert(String fieldType) {
		OracleColumnType.getMySqlColumnType(StringUtils.lowerCase(fieldType)).getJavaDataType();
		return null;
	}

}
