package org.hyw.tools.generator.template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.utils.NamingStrategy;
import org.hyw.tools.generator.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 模板上下文构建器 - 差异化包名分发版
 */
@Slf4j
public class TemplateContextBuilder {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private final GlobalConf global;
	private final DataSourceConf dataSource;
	private final Map<Component, Map<String, Object>> components;

	public TemplateContextBuilder(GlobalConf global, DataSourceConf dataSource,
			Map<Component, Map<String, Object>> components) {
		this.global = global;
		this.dataSource = dataSource;
		this.components = components;
	}

	public RenderContext buildGlobalContext() {
		RenderContext.Builder builder = RenderContext.builder()
				.variable("author", global.getAuthor())
				.variable("encoding", global.getEncoding())
				.variable("projectName", global.getProjectName())
				.variable("version", global.getVersion())
				.variable("javaVersion", global.getJavaVersion())
				.variable("rootPackage", global.getRootPackage())
				.variable("copyright", global.getCopyright())
				.variable("projectPackage", global.getRootPackage() + "." + global.getProjectName())
				.variable("date", LocalDateTime.now().format(DATE_FORMATTER))
				.variable("StringUtils", new StringUtilsBean())
				.variable("global", global)
				.variable("dataSource", dataSource);

		if (global.getModules() != null) {
			builder.variable("modules", Arrays.asList(global.getModules()));
		}

		String dbType = (dataSource.getDBType() != null) ? dataSource.getDBType().getName() : "mysql";
		builder.variable("dbType", dbType);
		builder.variable("sqlType", dbType);
		builder.variable("projectBuilder", global.getProjectBuilder().name());

		Map<String, Object> project = new HashMap<>();
		project.put("artifactId", global.getProjectName());
		project.put("name", global.getProjectName());
		project.put("version", global.getVersion());
		project.put("groupId", global.getRootPackage());
		Map<String, Object> build = new HashMap<>();
		build.put("sourceEncoding", global.getEncoding());
		project.put("build", build);
		builder.variable("project", project);

		injectDynamicComponentConfigs(builder);

		return builder.build();
	}

	private void injectDynamicComponentConfigs(RenderContext.Builder builder) {
		Set<Component> selectedSet = global.getComponents() != null ? 
				new HashSet<>(Arrays.asList(global.getComponents())) : new HashSet<>();
		
		for (Component c : Component.values()) {
			boolean isSelected = selectedSet.contains(c);
			builder.variable(c.name(), isSelected);
			builder.variable(c.name().toLowerCase(), isSelected);
		}

		if (components != null) {
			components.forEach((component, props) -> {
				if (props != null) {
					props.forEach((key, value) -> {
						// 增加带组件名前缀的注入 (如 JPA_version)，避免冲突
						builder.variable(component.name() + "_" + key, value);
						
						// 如果该 key 在 global 中不存在，才注入到顶层，保证 global 配置优先级最高
						if (key.contains(".") || key.contains("-") || !isGlobalProperty(key)) {
							builder.variable(key, value);
						}
						
						if (key.contains(".")) {
							builder.variable(key.replace(".", "_"), value);
						}
						if (key.contains("-")) {
							builder.variable(StringUtils.toCamelCase(key, '-'), value);
						}
					});
				}
			});
		}
	}

	private boolean isGlobalProperty(String key) {
		// 检查 key 是否是 GlobalConf 的属性，避免被组件配置覆盖 (如 version, author, copyright)
		return "version".equals(key) || "author".equals(key) || "copyright".equals(key) || "projectName".equals(key);
	}

	public RenderContext buildTableContext(Table table) {
		RenderContext context = buildGlobalContext().createChildContext();
		context.put("global", global);
		context.put("dataSource", dataSource);
		context.table(table);
		
		TemplateModel model = context.getModel();
		model.setAuthor(global.getAuthor());
		model.setCopyright(global.getCopyright());
		model.setProjectName(global.getProjectName());
		model.setRootPackage(global.getRootPackage());
		
		Map<String, String> naming = NamingStrategy.buildNamingMap(table.getBeanName());
		context.put("naming", naming);
		context.put("entityName", table.getBeanName());
		context.put("entityNameLower", table.getLowercaseBeanName());
		context.put("tableName", table.getName());
		return context;
	}

	 
	private String deriveFullPackage(String outputPath) {
		String javaDirMark = Consts.DIR_JAVA + Consts.PATH_SEPARATOR;
		int index = outputPath.indexOf(javaDirMark);
		if (index == -1) return null;
		String packagePath = StringUtils.substringBeforeLast(outputPath.substring(index + javaDirMark.length()), Consts.PATH_SEPARATOR);
		return packagePath.replace(Consts.PATH_SEPARATOR, ".");
	}

	public static class StringUtilsBean {
		public boolean isNotBlank(String s) { return org.apache.commons.lang3.StringUtils.isNotBlank(s); }
		public int indexOf(String s, String c) { return org.apache.commons.lang3.StringUtils.indexOf(s, c); }
		public String lowercaseFirst(String s) { return org.hyw.tools.generator.utils.StringUtils.lowercaseFirst(s); }
		public String capitalFirst(String s) { return org.hyw.tools.generator.utils.StringUtils.capitalFirst(s); }
	}
}
