package org.hyw.tools.generator.enums;

import org.hyw.tools.generator.utils.StringUtils;

/**
 * 组件定义
 * <p>
 * 注意：枚举定义的顺序决定了代码生成的渲染优先级（基于 ordinal() 排序）。
 * 建议遵循自底向上的顺序：持久层(ORM) -> 中间件 -> 业务层(MVC) -> 前端。
 * 这确保了基础包名变量（如 entityPackage）能最先被注入缓存供后续组件引用。
 * </p>
 */
public enum Component {
	// 持久层/基础层
	MYBATIS, JPA, 
	
	// 中间件/插件
	REDIS, DUBBO, ZIPKIN, SKYWALKING, ZOOKEEPER, NACOS, ROCKETMQ, SENTINEL,
	
	// 业务框架/应用层
	SPRINGBOOT, SPRINGCLOUD, SPRINGMVC, SWAGGER2, THYMELEAF, JWT, SHIRO, 
	
	// 前端/Web
	VUE("web");

	private String alias;

	Component() {
	}

	Component(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return StringUtils.isBlank(alias) ? this.name().toLowerCase() : alias;
	}

	public static Component getComonent(String name) {
		if (name == null) {
			return null;
		}
		for (Component c : values()) {
			if (c.name().equalsIgnoreCase(name) || (c.alias != null && c.alias.equalsIgnoreCase(name))) {
				return c;
			}
		}
		return null;
	}

	public static Component getComponent(String name) {
		return getComonent(name);
	}
}
