package org.hyw.tools.generator.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;
import org.junit.Test;

public class CompatibilityResolverTest {

	@Test
	public void shouldApplyJava8Boot2Profile() {
		GlobalConf global = new GlobalConf();
		global.setJavaVersion("8");
		Map<Component, Map<String, Object>> components = new HashMap<>();

		ResolvedPlatform resolved = new CompatibilityResolver().apply(global, components, null);

		assertEquals("java8-boot2", resolved.getId());
		assertEquals("boot2", global.getTemplateFamily());
		assertEquals("javax", global.getNamespace());
		assertEquals("8", global.getBytecodeRelease());
		assertEquals("2.7.18", components.get(Component.SPRINGBOOT).get("springboot_version"));
		assertEquals("mybatis-plus-boot-starter",
				components.get(Component.MYBATIS).get("mybatis_plus_starter_artifact"));
		assertEquals("javax.servlet", global.getPlatformVariables().get("servletPackage"));
		assertEquals("javax.xml.bind", global.getPlatformVariables().get("jaxbApiGroupId"));
		assertEquals("2.3.1", global.getPlatformVariables().get("jaxbApiVersion"));
		assertEquals("2.3.8", global.getPlatformVariables().get("jaxbRuntimeVersion"));
	}

	@Test
	public void shouldApplyJava21Boot3ProfileByInheritance() {
		GlobalConf global = new GlobalConf();
		global.setJavaVersion("21");
		Map<Component, Map<String, Object>> components = new HashMap<>();

		ResolvedPlatform resolved = new CompatibilityResolver().apply(global, components, null);

		assertEquals("java21-boot3", resolved.getId());
		assertEquals("boot3", global.getTemplateFamily());
		assertEquals("jakarta", global.getNamespace());
		assertEquals("21", global.getBytecodeRelease());
		assertEquals("3.2.12", components.get(Component.SPRINGBOOT).get("springboot_version"));
		assertEquals("mybatis-plus-spring-boot3-starter",
				components.get(Component.MYBATIS).get("mybatis_plus_starter_artifact"));
		assertEquals("jakarta.servlet", global.getPlatformVariables().get("servletPackage"));
		assertEquals("jakarta.xml.bind", global.getPlatformVariables().get("jaxbApiGroupId"));
		assertEquals("4.0.0", global.getPlatformVariables().get("jaxbApiVersion"));
		assertEquals("4.0.2", global.getPlatformVariables().get("jaxbRuntimeVersion"));
	}

	@Test
	public void shouldAllowWhitelistedPatchOverride() {
		GlobalConf global = new GlobalConf();
		global.setJavaVersion("17");
		Map<Component, Map<String, Object>> components = new HashMap<>();
		Map<Component, Map<String, Object>> overrides = new HashMap<>();
		Map<String, Object> springBoot = new HashMap<>();
		springBoot.put("springboot_version", "3.2.13");
		overrides.put(Component.SPRINGBOOT, springBoot);

		new CompatibilityResolver().apply(global, components, overrides);

		assertEquals("3.2.13", components.get(Component.SPRINGBOOT).get("springboot_version"));
	}

	@Test
	public void shouldRejectCrossMajorOverride() {
		GlobalConf global = new GlobalConf();
		global.setJavaVersion("17");
		Map<Component, Map<String, Object>> components = new HashMap<>();
		Map<Component, Map<String, Object>> overrides = new HashMap<>();
		Map<String, Object> springBoot = new HashMap<>();
		springBoot.put("springboot_version", "2.7.18");
		overrides.put(Component.SPRINGBOOT, springBoot);

		try {
			new CompatibilityResolver().apply(global, components, overrides);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("不在允许范围"));
			return;
		}
		fail("Expected override validation failure");
	}
}
