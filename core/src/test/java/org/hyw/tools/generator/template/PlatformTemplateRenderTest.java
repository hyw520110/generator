package org.hyw.tools.generator.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.EngineType;
import org.junit.Test;

public class PlatformTemplateRenderTest {

	private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
	private static final Path TEMPLATES = PROJECT_ROOT.resolve("core/src/main/resources/templates");
	private final TemplateRenderer renderer = new TemplateRenderer();

	@Test
	public void globalExceptionHandlerUsesJavaxValidationForBoot2() throws Exception {
		String freemarker = render("freemarker/modules/{1}/[sourceDirectory]/#springboot#/handler/GlobalExceptionHandler.java.ftl",
				context("javax"), EngineType.FREEMARKER);
		String velocity = render("velocity/modules/{1}/[sourceDirectory]/#springboot#/handler/GlobalExceptionHandler.java",
				context("javax"), EngineType.VELOCITY);

		assertTrue(freemarker, freemarker.contains("import javax.validation.ConstraintViolationException;"));
		assertTrue(velocity, velocity.contains("import javax.validation.ConstraintViolationException;"));
	}

	@Test
	public void globalExceptionHandlerUsesJakartaValidationForBoot3() throws Exception {
		String freemarker = render("freemarker/modules/{1}/[sourceDirectory]/#springboot#/handler/GlobalExceptionHandler.java.ftl",
				context("jakarta"), EngineType.FREEMARKER);
		String velocity = render("velocity/modules/{1}/[sourceDirectory]/#springboot#/handler/GlobalExceptionHandler.java",
				context("jakarta"), EngineType.VELOCITY);

		assertTrue(freemarker, freemarker.contains("import jakarta.validation.ConstraintViolationException;"));
		assertTrue(velocity, velocity.contains("import jakarta.validation.ConstraintViolationException;"));
		assertFalse(freemarker, freemarker.contains("javax.validation"));
		assertFalse(velocity, velocity.contains("javax.validation"));
	}

	@Test
	public void vueWorkplaceRendersProjectDashboardWithoutMockDependencies() throws Exception {
		RenderContext context = context("javax");
		String freemarker = render("freemarker/modules/#vue#/web/src/views/dashboard/Workplace.vue",
				context, EngineType.FREEMARKER);
		String velocity = render("velocity/modules/#vue#/web/src/views/dashboard/Workplace.vue",
				context, EngineType.VELOCITY);

		assertWorkplaceOutput(freemarker);
		assertWorkplaceOutput(velocity);
	}

	private void assertWorkplaceOutput(String output) {
		assertTrue(output, output.contains("工程概览"));
		assertTrue(output, output.contains("运行检查"));
		assertTrue(output, output.contains("this.$store"));
		assertTrue(output, output.contains("this.$router"));
		assertFalse(output, output.contains("蚂蚁金服"));
		assertFalse(output, output.contains("/workplace/radar"));
		assertFalse(output, output.contains("/list/search/projects"));
		assertFalse(output, output.contains("@antv/data-set"));
	}

	private RenderContext context(String namespace) {
		GlobalConf global = new GlobalConf();
		global.setModules(new String[] { "app" });
		return RenderContext.builder()
				.variable("rootPackage", "com.example")
				.variable("projectName", "demo")
				.variable("moduleName", "app")
				.variable("global", global)
				.variable("validationPackage", namespace + ".validation")
				.variable("dtoPackage", "com.example.demo.app.dto")
				.build();
	}

	private String render(String templatePath, RenderContext context, EngineType engineType) throws Exception {
		String template = new String(Files.readAllBytes(TEMPLATES.resolve(templatePath)), StandardCharsets.UTF_8);
		return renderer.render(template, context, engineType);
	}
}
