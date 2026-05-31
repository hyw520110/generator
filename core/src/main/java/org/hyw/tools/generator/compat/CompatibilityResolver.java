package org.hyw.tools.generator.compat;

import java.util.HashMap;
import java.util.Map;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;

public class CompatibilityResolver {

	private final CompatibilityMatrix matrix;

	public CompatibilityResolver() {
		this(CompatibilityMatrix.loadDefault());
	}

	public CompatibilityResolver(CompatibilityMatrix matrix) {
		this.matrix = matrix;
	}

	public ResolvedPlatform apply(GlobalConf global, Map<Component, Map<String, Object>> components,
			Map<Component, Map<String, Object>> versionOverrides) {
		if (global == null) {
			return null;
		}
		// 优先按 platformId 选档（用户显式指定栈），否则回退到 javaVersion 默认档
		CompatibilityProfile profile;
		String platformId = global.getPlatformId();
		if (platformId != null && !platformId.trim().isEmpty()) {
			profile = matrix.resolveById(platformId.trim());
		} else {
			profile = matrix.resolveByJava(global.getJavaVersion());
		}
		applyProfileVersions(profile, components);
		applyOverrides(profile, components, versionOverrides);
		validateConstraints(profile, global, components);

		int release = profile.getRelease() == null ? profile.getJava() : profile.getRelease();
		Map<String, Object> variables = buildVariables(profile, release);
		global.setJavaVersion(String.valueOf(profile.getJava()));
		global.setPlatformId(profile.getId());
		global.setTemplateFamily(profile.getTemplateFamily());
		global.setNamespace(profile.getNamespace());
		global.setBytecodeRelease(String.valueOf(release));
		global.setPlatformVariables(variables);

		return new ResolvedPlatform(profile.getId(), profile.getName(), profile.getJava(), release,
				profile.getTemplateFamily(), profile.getNamespace(), variables);
	}

	public CompatibilityMatrix getMatrix() {
		return matrix;
	}

	private void applyProfileVersions(CompatibilityProfile profile, Map<Component, Map<String, Object>> components) {
		if (components == null || profile.getVersions() == null) {
			return;
		}
		for (Map.Entry<Component, Map<String, Object>> entry : profile.getVersions().entrySet()) {
			Map<String, Object> target = components.get(entry.getKey());
			if (target == null) {
				target = new HashMap<>();
				components.put(entry.getKey(), target);
			}
			if (entry.getValue() != null) {
				target.putAll(entry.getValue());
			}
		}
	}

	private void applyOverrides(CompatibilityProfile profile, Map<Component, Map<String, Object>> components,
			Map<Component, Map<String, Object>> versionOverrides) {
		if (components == null || versionOverrides == null || versionOverrides.isEmpty()) {
			return;
		}
		for (Map.Entry<Component, Map<String, Object>> componentEntry : versionOverrides.entrySet()) {
			Component component = componentEntry.getKey();
			Map<String, Object> values = componentEntry.getValue();
			if (component == null || values == null) {
				continue;
			}
			Map<String, Object> target = components.get(component);
			if (target == null) {
				target = new HashMap<>();
				components.put(component, target);
			}
			for (Map.Entry<String, Object> override : values.entrySet()) {
				validateOverride(profile, component, override.getKey(), override.getValue());
				target.put(override.getKey(), override.getValue());
			}
		}
	}

	private void validateOverride(CompatibilityProfile profile, Component component, String key, Object value) {
		String overrideKey = component.name() + "." + key;
		String pattern = profile.getAllowOverride() == null ? null : profile.getAllowOverride().get(overrideKey);
		if (pattern == null) {
			throw new IllegalArgumentException("当前 Java 平台不允许覆盖版本项: " + overrideKey);
		}
		String version = value == null ? "" : String.valueOf(value);
		if (!matches(pattern, version)) {
			throw new IllegalArgumentException("版本 " + overrideKey + "=" + version + " 不在允许范围: " + pattern);
		}
	}

	private boolean matches(String pattern, String version) {
		if (pattern == null || pattern.trim().isEmpty()) {
			return false;
		}
		String regex = pattern.trim().replace(".", "\\.").replace("*", "[0-9A-Za-z_.-]*");
		return version != null && version.matches(regex);
	}

	private Map<String, Object> buildVariables(CompatibilityProfile profile, int release) {
		Map<String, Object> variables = new HashMap<>();
		String namespace = profile.getNamespace() == null ? "jakarta" : profile.getNamespace();
		variables.put("platformId", profile.getId());
		variables.put("platformName", profile.getName());
		variables.put("templateFamily", profile.getTemplateFamily());
		variables.put("namespace", namespace);
		variables.put("eeNamespace", namespace);
		variables.put("javaVersion", String.valueOf(profile.getJava()));
		variables.put("bytecodeRelease", String.valueOf(release));
		variables.put("servletPackage", namespace + ".servlet");
		variables.put("validationPackage", namespace + ".validation");
		variables.put("persistencePackage", namespace + ".persistence");
		variables.put("annotationPackage", "jakarta".equals(namespace) ? "jakarta.annotation" : "javax.annotation");
		variables.put("druidSupportPackage",
				"jakarta".equals(namespace) ? "com.alibaba.druid.support.jakarta" : "com.alibaba.druid.support.http");
		variables.put("servletApiGroupId", "jakarta".equals(namespace) ? "jakarta.servlet" : "javax.servlet");
		variables.put("servletApiArtifactId", "jakarta".equals(namespace) ? "jakarta.servlet-api" : "javax.servlet-api");
		variables.put("validationApiGroupId", "jakarta".equals(namespace) ? "jakarta.validation" : "javax.validation");
		variables.put("validationApiArtifactId",
				"jakarta".equals(namespace) ? "jakarta.validation-api" : "validation-api");
		variables.put("jaxbApiGroupId", "jakarta".equals(namespace) ? "jakarta.xml.bind" : "javax.xml.bind");
		variables.put("jaxbApiArtifactId", "jakarta".equals(namespace) ? "jakarta.xml.bind-api" : "jaxb-api");
		variables.put("shiroClassifier", "jakarta".equals(namespace) ? "jakarta" : "");
		variables.put("springBootMajor", "boot3".equals(profile.getTemplateFamily()) ? "3" : "2");
		return variables;
	}

	/**
	 * 校验组件间约束（requires / conflicts）。
	 * 只校验当前 global.components 中"已启用"的组件，避免误报。
	 */
	private void validateConstraints(CompatibilityProfile profile, GlobalConf global,
			Map<Component, Map<String, Object>> components) {
		if (profile == null || profile.getComponentConstraints() == null
				|| profile.getComponentConstraints().isEmpty()) {
			return;
		}
		java.util.Set<Component> enabled = enabledComponents(global);
		for (Map.Entry<Component, ComponentConstraint> entry : profile.getComponentConstraints().entrySet()) {
			Component owner = entry.getKey();
			if (!enabled.contains(owner)) {
				continue;
			}
			ComponentConstraint constraint = entry.getValue();
			if (constraint == null) {
				continue;
			}
			for (String require : constraint.getRequires()) {
				checkRequire(owner, require, enabled, components);
			}
			for (String conflict : constraint.getConflicts()) {
				Component other = parseComponent(conflict);
				if (other != null && enabled.contains(other)) {
					throw new IllegalStateException("组件冲突：" + owner + " 与 " + other + " 不能同时启用");
				}
			}
		}
	}

	private java.util.Set<Component> enabledComponents(GlobalConf global) {
		java.util.Set<Component> set = new java.util.HashSet<>();
		if (global == null || global.getComponents() == null) {
			return set;
		}
		for (Component c : global.getComponents()) {
			if (c != null) {
				set.add(c);
			}
		}
		return set;
	}

	private Component parseComponent(String token) {
		if (token == null) {
			return null;
		}
		try {
			return Component.valueOf(token.trim().toUpperCase());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 解析 require 表达式：
	 * <ul>
	 *   <li>"SPRINGBOOT" → 仅要求组件启用</li>
	 *   <li>"SPRINGBOOT.springboot_version=3.*" → 要求该组件版本匹配通配模式</li>
	 * </ul>
	 */
	private void checkRequire(Component owner, String require, java.util.Set<Component> enabled,
			Map<Component, Map<String, Object>> components) {
		if (require == null || require.trim().isEmpty()) {
			return;
		}
		String expr = require.trim();
		int eq = expr.indexOf('=');
		String left = eq >= 0 ? expr.substring(0, eq).trim() : expr;
		String pattern = eq >= 0 ? expr.substring(eq + 1).trim() : null;

		int dot = left.indexOf('.');
		String compName = dot >= 0 ? left.substring(0, dot) : left;
		String key = dot >= 0 ? left.substring(dot + 1) : null;

		Component required = parseComponent(compName);
		if (required == null) {
			throw new IllegalStateException("组件 " + owner + " 的 requires 中存在未知组件: " + compName);
		}
		if (!enabled.contains(required)) {
			throw new IllegalStateException("组件 " + owner + " 要求启用组件 " + required + "，当前未启用");
		}
		if (key == null || pattern == null) {
			return;
		}
		Map<String, Object> map = components == null ? null : components.get(required);
		Object actual = map == null ? null : map.get(key);
		if (actual == null) {
			throw new IllegalStateException("组件 " + owner + " 要求 " + required + "." + key
					+ " 匹配 " + pattern + "，但当前值缺失");
		}
		if (!matches(pattern, String.valueOf(actual))) {
			throw new IllegalStateException("组件 " + owner + " 要求 " + required + "." + key
					+ " 匹配 " + pattern + "，当前值: " + actual);
		}
	}
}
