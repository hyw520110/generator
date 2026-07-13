package org.hyw.tools.generator.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hyw.tools.generator.conf.GlobalConf;
import org.junit.Test;

/**
 * 默认模板输出路径解析回归测试。
 */
public class DefaultPathTemplateResolverTest {

	/** 单模块工程不能让 API 模块 POM 覆盖可独立构建的实现模块 POM。 */
	@Test
	public void singleModuleSkipsApiModuleSkeleton() {
		GlobalConf config = new GlobalConf();
		config.setModules(new String[] { "admin" });
		TemplateModel model = TemplateModel.builder().config(config).build();
		DefaultPathTemplateResolver resolver = new DefaultPathTemplateResolver();

		assertNull(resolver.resolve("modules/{0}/pom.xml.ftl", model));
		assertEquals("admin/src/main/java/dto/Result.java",
				resolver.resolve("modules/{0}/[sourceDirectory]/dto/Result.java.ftl", model));
		assertEquals("admin/pom.xml", resolver.resolve("modules/{1}/pom.xml.ftl", model));
	}
}
