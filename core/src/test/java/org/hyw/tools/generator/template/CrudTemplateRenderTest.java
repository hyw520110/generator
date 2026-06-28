package org.hyw.tools.generator.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.EngineType;
import org.hyw.tools.generator.enums.FieldType;
import org.junit.Test;

public class CrudTemplateRenderTest {

	private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
	private static final Path TEMPLATES = PROJECT_ROOT.resolve("core/src/main/resources/templates");
	private final TemplateRenderer renderer = new TemplateRenderer();

	@Test
	public void freemarkerVueCrudTemplatesRenderCompositePrimaryKeys() throws Exception {
		RenderContext context = context(compositePrimaryKeyTable());
		assertCrudOutput(render("freemarker/components/#vue#/web/src/api/%s.js.ftl", context, EngineType.FREEMARKER));
		assertCrudOutput(render("freemarker/components/#vue#/web/src/views/%s/%sList.vue.ftl", context,
				EngineType.FREEMARKER));
		assertCrudOutput(render("freemarker/components/#vue#/web/src/views/%s/%sForm.vue.ftl", context,
				EngineType.FREEMARKER));
	}

	@Test
	public void freemarkerMybatisPlusEntityDoesNotPretendCompositePrimaryKeyIsSingleId() throws Exception {
		RenderContext context = entityContext(compositePrimaryKeyTable()).put("mapperType", "plus");
		String output = render("freemarker/components/{0}/[sourceDirectory]/#mybatis#/entity/%s.java.ftl", context,
				EngineType.FREEMARKER);
		assertCompositePlusEntityOutput(output);
	}

	@Test
	public void freemarkerCompositePrimaryKeyTemplateRendersKeyClass() throws Exception {
		RenderContext context = backendContext(compositePrimaryKeyTable(), EngineType.FREEMARKER)
				.put("lombok", true);
		String output = render("freemarker/components/{1}/[sourceDirectory]/#mybatis#/key/${table.beanName}Key.java.ftl",
				context, EngineType.FREEMARKER);
		assertCompositeKeyOutput(output);
	}

	@Test
	public void velocityVueCrudTemplatesRenderCompositePrimaryKeys() throws Exception {
		RenderContext context = context(compositePrimaryKeyTable());
		assertCrudOutput(render("velocity/components/#vue#/web/src/api/%s.js", context, EngineType.VELOCITY));
		assertCrudOutput(render("velocity/components/#vue#/web/src/views/%s/%sList.vue", context, EngineType.VELOCITY));
		assertCrudOutput(render("velocity/components/#vue#/web/src/views/%s/%sForm.vue", context, EngineType.VELOCITY));
	}

	@Test
	public void velocityMybatisPlusEntityDoesNotPretendCompositePrimaryKeyIsSingleId() throws Exception {
		RenderContext context = entityContext(compositePrimaryKeyTable()).put("mapperType", "plus");
		String output = render("velocity/components/{0}/[sourceDirectory]/#mybatis#/entity/%s.java", context,
				EngineType.VELOCITY);
		assertCompositePlusEntityOutput(output);
	}

	@Test
	public void velocityCompositePrimaryKeyTemplateRendersKeyClassWithLombokFields() throws Exception {
		RenderContext context = backendContext(compositePrimaryKeyTable(), EngineType.VELOCITY)
				.put("lombok", true);
		String output = render("velocity/components/{1}/[sourceDirectory]/#mybatis#/key/${table.beanName}Key.java.vm",
				context, EngineType.VELOCITY);
		assertCompositeKeyOutput(output);
		assertTrue(output, output.contains("@Data"));
	}

	@Test
	public void freemarkerCrudTemplatesRenderNoPrimaryKeyWithoutSyntheticId() throws Exception {
		RenderContext context = backendContext(noPrimaryKeyTable(), EngineType.FREEMARKER);
		assertNoPrimaryKeyVueOutput(render("freemarker/components/#vue#/web/src/api/%s.js.ftl", context,
				EngineType.FREEMARKER));
		assertNoPrimaryKeyVueOutput(render("freemarker/components/#vue#/web/src/views/%s/%sList.vue.ftl", context,
				EngineType.FREEMARKER));
		assertNoPrimaryKeyVueOutput(render("freemarker/components/#vue#/web/src/views/%s/%sForm.vue.ftl", context,
				EngineType.FREEMARKER));
		assertNoPrimaryKeyMapperOutput(render(
				"freemarker/components/{1}/[sourceDirectory]/#mybatis#/mapper/%sMapper.java.ftl", context,
				EngineType.FREEMARKER));
		assertNoPrimaryKeyMapperOutput(render(
				"freemarker/components/{1}/[resourceDirectory]/#mybatis#/mappers/%sMapper.xml.ftl", context,
				EngineType.FREEMARKER));
		assertNoPrimaryKeyControllerOutput(render(
				"freemarker/components/{1}/[sourceDirectory]/#springmvc#/controller/%sController.java.ftl", context,
				EngineType.FREEMARKER));
		assertNoPrimaryKeyEntityOutput(render(
				"freemarker/components/{0}/[sourceDirectory]/#mybatis#/entity/%s.java.ftl",
				entityContext(noPrimaryKeyTable()).put("mapperType", "plus"), EngineType.FREEMARKER));
	}

	@Test
	public void velocityCrudTemplatesRenderNoPrimaryKeyWithoutSyntheticId() throws Exception {
		RenderContext context = backendContext(noPrimaryKeyTable(), EngineType.VELOCITY);
		assertNoPrimaryKeyVueOutput(render("velocity/components/#vue#/web/src/api/%s.js", context, EngineType.VELOCITY));
		assertNoPrimaryKeyVueOutput(render("velocity/components/#vue#/web/src/views/%s/%sList.vue", context,
				EngineType.VELOCITY));
		assertNoPrimaryKeyVueOutput(render("velocity/components/#vue#/web/src/views/%s/%sForm.vue", context,
				EngineType.VELOCITY));
		assertNoPrimaryKeyMapperOutput(render(
				"velocity/components/{1}/[sourceDirectory]/#mybatis#/mapper/%sMapper.java", context,
				EngineType.VELOCITY));
		assertNoPrimaryKeyMapperOutput(render(
				"velocity/components/{1}/[resourceDirectory]/#mybatis#/mappers/%sMapper.xml", context,
				EngineType.VELOCITY));
		assertNoPrimaryKeyControllerOutput(render(
				"velocity/components/{1}/[sourceDirectory]/#springmvc#/controller/%sController.java", context,
				EngineType.VELOCITY));
		assertNoPrimaryKeyEntityOutput(render("velocity/components/{0}/[sourceDirectory]/#mybatis#/entity/%s.java",
				entityContext(noPrimaryKeyTable()).put("mapperType", "plus"), EngineType.VELOCITY));
	}

	private String render(String templatePath, RenderContext context, EngineType engineType) throws Exception {
		String template = new String(Files.readAllBytes(TEMPLATES.resolve(templatePath)), StandardCharsets.UTF_8);
		if (engineType == EngineType.VELOCITY) {
			template = template.replace("#parse('/templates/comments/comment.vm')", "");
		}
		return renderer.render(template, context, engineType);
	}

	private void assertCrudOutput(String output) {
		assertFalse(output, output.contains("moudulePath"));
		assertFalse(output, output.contains("record.id"));
		assertFalse(output, output.contains("console.log"));
		assertTrue(output, output.contains("primaryKeyFields"));
		assertTrue(output, output.contains("tenantId"));
		assertTrue(output, output.contains("orderId"));
	}

	private void assertNoPrimaryKeyVueOutput(String output) {
		assertFalse(output, output.contains("record.id"));
		assertFalse(output, output.contains("['id']"));
		assertTrue(output, output.contains("const primaryKeyFields = []"));
		assertTrue(output, output.contains("const hasPrimaryKey = primaryKeyFields.length > 0"));
	}

	private void assertNoPrimaryKeyMapperOutput(String output) {
		assertFalse(output, output.contains("findById("));
		assertFalse(output, output.contains("deleteById("));
		assertFalse(output, output.contains("<select id=\"findById\""));
		assertFalse(output, output.contains("<delete id=\"deleteById\""));
		assertFalse(output, output.contains("WHERE  \n"));
		assertFalse(output, output.contains("ORDER BY  DESC"));
		assertFalse(output, output.contains("keyProperty=\"id\""));
	}

	private void assertNoPrimaryKeyControllerOutput(String output) {
		assertFalse(output, output.contains("extends BaseController"));
		assertFalse(output, output.contains("@GetMapping(value=\"/{id}\""));
		assertFalse(output, output.contains("@DeleteMapping(value=\"/{id}\""));
		assertFalse(output, output.contains("@GetMapping(value=\"/del/\""));
	}

	private void assertCompositePlusEntityOutput(String output) {
		assertFalse(output, output.contains("@TableId"));
		assertFalse(output, output.contains("pkVal()"));
		assertTrue(output, output.contains("@TableField(value = \"tenant_id\")"));
		assertTrue(output, output.contains("@TableField(value = \"order_id\")"));
	}

	private void assertCompositeKeyOutput(String output) {
		assertTrue(output, output.contains("package com.example.demo.key;"));
		assertTrue(output, output.contains("public class OrderKey implements Serializable"));
		assertTrue(output, output.contains("private Long tenantId;"));
		assertTrue(output, output.contains("private Long orderId;"));
	}

	private void assertNoPrimaryKeyEntityOutput(String output) {
		assertFalse(output, output.contains("@TableId"));
		assertFalse(output, output.contains("pkVal()"));
	}

	private RenderContext context(Table table) {
		return new RenderContext().table(table);
	}

	private RenderContext backendContext(Table table, EngineType engineType) {
		GlobalConf global = new GlobalConf();
		global.setModules(new String[] { "app" });
		RenderContext context = context(table)
				.put("mapperType", "xml")
				.put("mapperPackage", "com.example.demo.mapper")
				.put("mapperName", table.getBeanName() + "Mapper")
				.put("entityPackage", "com.example.demo.entity")
				.put("entityName", table.getBeanName())
				.put("controllerPackage", "com.example.demo.controller")
				.put("controllerName", table.getBeanName() + "Controller")
				.put("dtoPackage", "com.example.demo.dto")
				.put("dtoName", table.getBeanName() + "DTO")
				.put("servicePackage", "com.example.demo.service")
				.put("serviceName", table.getBeanName() + "Service")
				.put("rootPackage", "com.example.demo")
				.put("servletPackage", "javax.servlet")
				.put("validationPackage", "javax.validation")
				.put("baseResultMap", true)
				.put("columns", true)
				.put("findById", true)
				.put("findOne", true)
				.put("count", true)
				.put("findPage", true)
				.put("findAll", true)
				.put("insert", true)
				.put("update", true)
				.put("deleteById", true)
				.put("VUE", true)
				.put("global", global)
				.put("StringUtils", new org.hyw.tools.generator.utils.StringUtils())
				.put("superControllerClass", "BaseController");
		if (engineType == EngineType.VELOCITY) {
			context.put("StringUtils", new org.hyw.tools.generator.utils.StringUtils());
		}
		return context;
	}

	private RenderContext entityContext(Table table) {
		return context(table)
				.put("entityPackage", "com.example.demo.entity")
				.put("className", table.getBeanName())
				.put("validationPackage", "javax.validation")
				.put("StringUtils", new org.hyw.tools.generator.utils.StringUtils());
	}

	private Table compositePrimaryKeyTable() {
		Table table = new Table("sample_order", "订单");
		table.setBeanName("Order");
		table.addField(field("tenant_id", "tenantId", FieldType.LONG, "租户ID", false, true));
		table.addField(field("order_id", "orderId", FieldType.LONG, "订单ID", false, true));
		table.addField(field("order_name", "orderName", FieldType.STRING, "订单名称", false, false));
		table.addField(field("total_amount", "totalAmount", FieldType.BIG_DECIMAL, "订单金额", true, false));
		return table;
	}

	private Table noPrimaryKeyTable() {
		Table table = new Table("sample_log", "操作日志");
		table.setBeanName("Log");
		table.addField(field("user_name", "userName", FieldType.STRING, "用户名", false, false));
		table.addField(field("operation", "operation", FieldType.STRING, "操作", false, false));
		table.addField(field("created_time", "createdTime", FieldType.DATE, "创建时间", true, false));
		return table;
	}

	private TabField field(String name, String propertyName, FieldType type, String comment, boolean nullable,
			boolean primaryKey) {
		TabField field = new TabField(name, type.getType());
		field.setPropertyName(propertyName);
		field.setFieldType(type);
		field.setComment(comment);
		field.setNullAble(nullable);
		field.setPrimarykey(primaryKey);
		field.setJdbcType(jdbcType(type));
		return field;
	}

	private String jdbcType(FieldType type) {
		if (type == FieldType.LONG) {
			return "BIGINT";
		}
		if (type == FieldType.DATE) {
			return "TIMESTAMP";
		}
		if (type == FieldType.BIG_DECIMAL) {
			return "DECIMAL";
		}
		return "VARCHAR";
	}
}
