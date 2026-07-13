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
import org.hyw.tools.generator.enums.Feature;

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
				.variable("description", global.getDescription())
				.variable("version", global.getVersion())
				.variable("javaVersion", global.getJavaVersion())
				.variable("security", global.getSecurity() == null ? "NONE" : global.getSecurity().name())
				.variable("platformId", global.getPlatformId())
				.variable("templateFamily", global.getTemplateFamily())
				.variable("namespace", global.getNamespace())
				.variable("eeNamespace", global.getNamespace())
				.variable("bytecodeRelease", global.getBytecodeRelease())
				.variable("rootPackage", global.getRootPackage())
				.variable("copyright", global.getCopyright())
				.variable("projectPackage", global.getRootPackage() + "." + global.getProjectName())
				.variable("date", LocalDateTime.now().format(DATE_FORMATTER))
				.variable("StringUtils", new StringUtilsBean())
				.variable("mapperPackage", global.getRootPackage() + "." + global.getProjectName() + ".mapper")
				.variable(Consts.CTX_GLOBAL, global)
				.variable(Consts.CTX_DATA_SOURCE, dataSource);

		if (global.getModules() != null) {
			builder.variable("modules", Arrays.asList(global.getModules()));
		}
		injectDeterministicPackageVariables(builder);

		String dbType = (dataSource.getDBType() != null) ? dataSource.getDBType().getName() : "mysql";
		builder.variable("dbType", dbType);
		String sqlType = resolveMybatisSqlType();
		builder.variable("sqlType", sqlType);
		// 兼容旧模板和历史配置，统一指向同一持久层生成模式。
		builder.variable("mapperType", sqlType);
		builder.variable("projectBuilder", global.getProjectBuilder().name());
        
        // Inject validation API coordinates based on namespace
        String namespace = global.getNamespace();
        if ("jakarta".equals(namespace)) {
            builder.variable("validationApiGroupId", "jakarta.validation");
            builder.variable("validationApiArtifactId", "jakarta.validation-api");
            builder.variable("servletPackage", "jakarta.servlet");
            builder.variable("annotationPackage", "jakarta.annotation");
        } else {
            builder.variable("validationApiGroupId", "javax.validation");
            builder.variable("validationApiArtifactId", "validation-api");
            builder.variable("servletPackage", "javax.servlet");
            builder.variable("annotationPackage", "javax.annotation");
        }
        
		if (global.getPlatformVariables() != null) {
			global.getPlatformVariables().forEach(builder::variable);
		}

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

	/**
	 * 根据稳定配置预先计算模板包名，避免并行渲染依赖其他文件先注册包变量。
	 */
	private void injectDeterministicPackageVariables(RenderContext.Builder builder) {
		String[] modules = global.getModules();
		if (modules == null || modules.length == 0) {
			return;
		}
		String projectBasePackage = global.getRootPackage() + "." + global.getProjectName();
		String apiBasePackage = projectBasePackage + "." + modules[0];
		String implementationModule = modules.length > 1 ? modules[1] : modules[0];
		String implementationBasePackage = projectBasePackage + "." + implementationModule;

		builder.variable("entityPackage", apiBasePackage + ".entity")
				.variable("dtoPackage", apiBasePackage + ".dto")
				.variable("servicePackage", apiBasePackage + ".service")
				.variable("BaseServicePackage", apiBasePackage + ".service")
				.variable("api_entityPackage", apiBasePackage + ".entity")
				.variable("api_dtoPackage", apiBasePackage + ".dto")
				.variable("api_servicePackage", apiBasePackage + ".service")
				.variable("mapperPackage", implementationBasePackage + ".mapper")
				.variable("implPackage", implementationBasePackage + ".service.impl")
				.variable("controllerPackage", implementationBasePackage + ".controller")
				.variable("commonsPackage", implementationBasePackage + ".controller.commons");
	}

	private String resolveMybatisSqlType() {
		Map<String, Object> mybatis = components == null ? null : components.get(Component.MYBATIS);
		if (mybatis == null) {
			return "xml";
		}
		Object configured = mybatis.get("sqlType");
		if (configured == null || StringUtils.isBlank(String.valueOf(configured))) {
			configured = mybatis.get("mapperType");
		}
		return configured == null || StringUtils.isBlank(String.valueOf(configured))
				? "xml" : String.valueOf(configured).trim().toLowerCase();
	}

	private void injectDynamicComponentConfigs(RenderContext.Builder builder) {
		Set<Component> selectedSet = global.getComponents() != null ? 
				new HashSet<>(Arrays.asList(global.getComponents())) : new HashSet<>();
		
		for (Component c : Component.values()) {
			boolean isSelected = selectedSet.contains(c);
			builder.variable(c.name(), isSelected);
			builder.variable(c.name().toLowerCase(), isSelected);
		}

		Set<Feature> selectedFeatures = global.getFeatures() != null ? 
				new HashSet<>(Arrays.asList(global.getFeatures())) : new HashSet<>();
		
		for (Feature f : Feature.values()) {
			boolean isSelected = selectedFeatures.contains(f);
			builder.variable(f.name(), isSelected);
			builder.variable(f.name().toLowerCase(), isSelected);
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
		context.put(Consts.CTX_GLOBAL, global);
		context.put(Consts.CTX_DATA_SOURCE, dataSource);
		context.table(table);
		
		TemplateModel model = context.getModel();
		model.setAuthor(global.getAuthor());
		model.setCopyright(global.getCopyright());
		model.setProjectName(global.getProjectName());
		model.setRootPackage(global.getRootPackage());
		
		context.put(Consts.CTX_ENTITY_NAME, table.getBeanName());
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
