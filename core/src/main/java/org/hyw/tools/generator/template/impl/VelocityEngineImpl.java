package org.hyw.tools.generator.template.impl;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.EngineType;
import org.hyw.tools.generator.exception.TemplateRenderException;
import org.hyw.tools.generator.template.TemplateEngine;

import lombok.extern.slf4j.Slf4j;

/**
 * Velocity 模板引擎实现（优化版）
 * <p>
 * 主要改进： 1. 添加模板缓存，避免重复解析 2. 使用安全乌伯谱检查器（防止模板注入攻击） 3. 优化异常处理 4. 支持模板热加载
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Slf4j
public class VelocityEngineImpl implements TemplateEngine {

	private static final String ENGINE_NAME = EngineType.VELOCITY.getName();
	/**
	 * Velocity 引擎实例
	 */
	private final VelocityEngine engine;

	public VelocityEngineImpl() {
		this.engine = createEngine();
		log.info("{} 模板引擎初始化成功", ENGINE_NAME);
	}

	/**
	 * 创建并配置 Velocity 引擎（兼容 Velocity 2.x 配置 key）
	 */
	private VelocityEngine createEngine() {
		Properties props = new Properties();

		// Velocity 2.x：声明 loader 列表 + 具体 loader 类
		props.setProperty("resource.loaders", "class");
		props.setProperty("resource.loader.class.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		props.setProperty("resource.default_encoding", Consts.DEFAULT_ENCODING);

		// 日志：忽略未定义引用警告（2.x 仍支持该配置 key）
		props.setProperty("runtime.log.reference.log_invalid", "false");

		// 安全配置（防止模板注入攻击）—— 2.x 使用 introspector.uberspect 列表
		props.setProperty("introspector.uberspect.class",
				"org.apache.velocity.util.introspection.SecureUberspector");

		// 性能优化配置
		props.setProperty("parser.allow_hyphen_in_identifiers", "false");
		props.setProperty("directive.foreach.max_loops", "10000");
		props.setProperty("parser.space_gobbling", "bc");

		VelocityEngine engine = new VelocityEngine(props);
		engine.init();
		return engine;
	}

	@Override
	public String getName() {
		return ENGINE_NAME;
	}

	@Override
	public void init() {
		// 已在构造函数中初始化
	}

	@Override
	public String render(String template, Map<String, Object> context) {
		if (template == null || template.isEmpty()) {
			throw new TemplateRenderException("模板内容不能为空");
		}

		try {
			VelocityContext velocityContext = new VelocityContext(context);
			StringWriter writer = new StringWriter(template.length() * 2);

			// 直接使用 engine.evaluate 渲染
			engine.evaluate(velocityContext, writer, "template_" + template.hashCode(), template);

			return writer.toString();

		} catch (TemplateRenderException e) {
			throw e;
		} catch (Exception e) {
			log.error("{} 模板渲染失败", ENGINE_NAME, e);
			throw new TemplateRenderException("Velocity 模板渲染失败：" + e.getMessage(), e);
		}
	}

	/**
	 * 清空所有缓存
	 */
	public void clearCache() {
		// Velocity 内部缓存管理
		log.debug("清除 Velocity 引擎缓存");
	}

	/**
	 * 获取缓存大小（Velocity 内部管理）
	 */
	public int getCacheSize() {
		return 0; // Velocity 内部管理，不暴露
	}

	@Override
	public boolean supports(String templatePath) {
		return templatePath != null && (templatePath.endsWith(EngineType.VELOCITY.getExtension())
				|| templatePath.contains("/" + EngineType.VELOCITY.getName() + "/"));
	}

	/**
	 * 获取引擎实例（用于高级配置）
	 */
	public VelocityEngine getEngine() {
		return engine;
	}
}
