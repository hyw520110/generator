package org.hyw.tools.generator.conf;

import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.representer.Representer;

/**
 * 自定义 YAML Representer，用于排除运行时字段以避免序列化错误
 * 
 * @author heyiwu
 */
public class SkipRuntimeFieldsRepresenter extends Representer {

	// 需要排除的运行时字段列表
	private static final String[] EXCLUDED_FIELDS = { "templateRenderer", // 模板渲染器（运行时对象）
			"contextBuilder", // 上下文构建器（运行时对象）
			"pathResolver", // 路径解析器（运行时对象）
			"tableCache" // 缓存对象（运行时对象，包含无法序列化的内部对象）
	};

	// DataSourceConf 只序列化这些字段（排除父类 DruidDataSource 的所有字段）
	private static final String[] DATASOURCE_CONF_FIELDS = { "dbType", // 数据库类型
			"pwd", // 密码副本
			"filter", // 过滤器配置
			"conProperties", // 连接属性
			"currentDbName", // 当前数据库名
			"url", // JDBC URL（从父类继承）
			"username", // 用户名（从父类继承）
			"driverClassName" // 驱动类名（从父类继承）
	};

	public SkipRuntimeFieldsRepresenter(DumperOptions options) {
		super(options);

		// 配置不输出类型标签
		this.getPropertyUtils().setSkipMissingProperties(true);

		// 配置枚举类型不带类型标签
		this.addClassTag(Enum.class, org.yaml.snakeyaml.nodes.Tag.STR);
	}

	@Override
	protected Set<Property> getProperties(java.lang.Class<? extends java.lang.Object> type) {
		Set<Property> properties = super.getProperties(type);
		if (properties == null) {
			return properties;
		}

		// 特殊处理 DataSourceConf：只保留允许的字段，排除父类所有字段
		if (type == org.hyw.tools.generator.conf.dao.DataSourceConf.class) {
			properties.removeIf(prop -> {
				String name = prop.getName();
				for (String allowed : DATASOURCE_CONF_FIELDS) {
					if (allowed.equals(name)) {
						return false; // 保留允许的字段
					}
				}
				return true; // 排除其他所有字段（包括父类的）
			});
		} else {
			// 其他类：过滤掉运行时字段
			properties.removeIf(prop -> {
				String name = prop.getName();
				for (String excluded : EXCLUDED_FIELDS) {
					if (excluded.equals(name)) {
						return true;
					}
				}
				return false;
			});
		}

		// 过滤掉 null 值的属性
		properties.removeIf(prop -> {
			try {
				Object value = prop.get(new java.lang.reflect.Method[0]);
				return value == null;
			} catch (Exception e) {
				return false;
			}
		});

		return properties;
	}
}