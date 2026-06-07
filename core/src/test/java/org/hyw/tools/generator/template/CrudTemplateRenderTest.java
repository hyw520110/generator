package org.hyw.tools.generator.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	public void velocityVueCrudTemplatesRenderCompositePrimaryKeys() throws Exception {
		RenderContext context = context(compositePrimaryKeyTable());
		assertCrudOutput(render("velocity/components/#vue#/web/src/api/%s.js", context, EngineType.VELOCITY));
		assertCrudOutput(render("velocity/components/#vue#/web/src/views/%s/%sList.vue", context, EngineType.VELOCITY));
		assertCrudOutput(render("velocity/components/#vue#/web/src/views/%s/%sForm.vue", context, EngineType.VELOCITY));
	}

	private String render(String templatePath, RenderContext context, EngineType engineType) throws Exception {
		String template = new String(Files.readAllBytes(TEMPLATES.resolve(templatePath)), StandardCharsets.UTF_8);
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

	private RenderContext context(Table table) {
		return new RenderContext().table(table);
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

	private TabField field(String name, String propertyName, FieldType type, String comment, boolean nullable,
			boolean primaryKey) {
		TabField field = new TabField(name, type.getType());
		field.setPropertyName(propertyName);
		field.setFieldType(type);
		field.setComment(comment);
		field.setNullAble(nullable);
		field.setPrimarykey(primaryKey);
		return field;
	}
}
