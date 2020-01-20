package org.hyw.tools.generator.enums;

import org.hyw.tools.generator.utils.StringUtils;

public enum Component {
	MYBATIS, JPA, DUBBO, ZIPKIN, ZOOKEEPER, ROCKETMQ, SPRINGBOOT, SPRINGCLOUD, SPRINGMVC, REDIS, SWAGGER2, THYMELEAF,VUE;
	public static Component getComonent(String name) {
		try {
			return valueOf(StringUtils.upperCase(name));
		} catch (Exception e) {
		}
		return null;
	}
}
