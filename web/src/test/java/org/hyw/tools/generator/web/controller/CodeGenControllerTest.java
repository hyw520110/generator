package org.hyw.tools.generator.web.controller;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.compat.CompatibilityProfile;
import org.hyw.tools.generator.enums.Component;
import org.junit.Test;

/**
 * 代码生成配置接口回归测试。
 */
public class CodeGenControllerTest {

	/** Java 21 + Spring Boot 4 默认版本首次提交时不应因覆盖配置未初始化而失败。 */
	@Test
	public void defaultVersionDoesNotRequireVersionOverrides() throws Exception {
		CodeGenController controller = new CodeGenController();
		Generator generator = new Generator();
		CompatibilityProfile profile = new CompatibilityProfile();
		Map<String, Object> springBootVersions = new HashMap<>();
		springBootVersions.put("springboot_version", "4.0.7");
		profile.getVersions().put(Component.SPRINGBOOT, springBootVersions);
		Method checkAndOverride = CodeGenController.class.getDeclaredMethod("checkAndOverride",
				Generator.class, CompatibilityProfile.class, Component.class, String.class, String.class);
		checkAndOverride.setAccessible(true);

		checkAndOverride.invoke(controller, generator, profile, Component.SPRINGBOOT,
				"springboot_version", "4.0.7");
	}
}
