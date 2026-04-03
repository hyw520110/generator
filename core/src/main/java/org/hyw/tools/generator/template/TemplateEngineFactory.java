package org.hyw.tools.generator.template;

import org.hyw.tools.generator.enums.EngineType;
import org.hyw.tools.generator.template.impl.FreeMarkerEngineImpl;
import org.hyw.tools.generator.template.impl.VelocityEngineImpl;

import java.util.EnumMap;
import java.util.Map;

/**
 * 模板引擎工厂（懒加载模式）
 * 
 * @author heyiwu
 */
public class TemplateEngineFactory {

	/**
	 * 引擎实例缓存
	 */
	private static final Map<EngineType, TemplateEngine> engines = new EnumMap<>(EngineType.class);

	/**
	 * 获取指定类型的引擎（懒加载）
	 */
	public static TemplateEngine getEngine(EngineType type) {
		return engines.computeIfAbsent(type, t -> {
			switch (t) {
			case VELOCITY:
				return new VelocityEngineImpl();
			case FREEMARKER:
				return new FreeMarkerEngineImpl();
			default:
				throw new IllegalArgumentException("不支持的模板引擎类型：" + t);
			}
		});
	}

	/**
	 * 获取 Velocity 引擎
	 */
	public static VelocityEngineImpl getVelocityEngine() {
		return (VelocityEngineImpl) getEngine(EngineType.VELOCITY);
	}

	/**
	 * 获取 FreeMarker 引擎
	 */
	public static FreeMarkerEngineImpl getFreeMarkerEngine() {
		return (FreeMarkerEngineImpl) getEngine(EngineType.FREEMARKER);
	}

	/**
	 * 关闭所有引擎
	 */
	public static void shutdown() {
		engines.clear();
	}
}
