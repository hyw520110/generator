package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.SkipRuntimeFieldsRepresenter;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.exception.GeneratorException;
import org.hyw.tools.generator.export.DbToDoc;
import org.hyw.tools.generator.template.DefaultPathTemplateResolver;
import org.hyw.tools.generator.template.PathTemplateResolver;
import org.hyw.tools.generator.template.RenderContext;
import org.hyw.tools.generator.template.TemplateContextBuilder;
import org.hyw.tools.generator.template.TemplateModel;
import org.hyw.tools.generator.template.TemplateRenderer;
import org.hyw.tools.generator.template.TemplateResource;
import org.hyw.tools.generator.utils.FileUtils;
import org.hyw.tools.generator.utils.FontUtils;
import org.hyw.tools.generator.utils.NamingStrategy;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;

/**
 * 代码生成器核心类
 */
@Slf4j
public class Generator extends AbstractGenerator {

	private static Generator generator;
	private TemplateRenderer templateRenderer;
	private TemplateContextBuilder contextBuilder;
	private PathTemplateResolver pathResolver = new DefaultPathTemplateResolver();
	private static final String SEPARATOR = Consts.PATH_SEPARATOR;

	private static final String EXTERNAL_CONFIG_DIR = System.getProperty("user.home") + File.separator
			+ Consts.CONFIG_DIR_NAME;

	static {
		load(Consts.DEFAULT_CONFIG_FILE);
	}

	public static Generator getInstance() {
		if (null == generator)
			generator = new Generator();
		return generator;
	}

	public static void main(String[] args) {
		Generator.getInstance().execute();
	}

	public static void load(String conf) {
		// 优先加载用户目录下最新的配置文件
		File configDir = new File(EXTERNAL_CONFIG_DIR);
		File latestFile = FileUtils.getLatestFile(configDir, Consts.EXT_YAML, Consts.EXT_YML);
		if (latestFile != null && latestFile.exists() && latestFile.length() > 0) {
			try {
				load(latestFile.toURI().toURL());
				return;
			} catch (Exception e) {
				log.warn("加载外部配置文件失败，尝试加载内置配置");
			}
		}
		// 加载内置默认配置
		load(Generator.class.getResource(conf));
	}

	private static void load(URL url) {
		if (null == url) {
			return;
		}
		try (InputStream is = url.openStream()) {
			generator = new Yaml().loadAs(is, Generator.class);
			log.info("加载内置配置文件成功: {}", url);
		} catch (Exception e) {
			log.warn("加载内置配置文件失败: {}", url);
		}
	}

	public void execute() {
		log.info("开始执行代码生成，输出目录: {}", global != null ? global.getOutputDir() : "");
		long startTime = System.currentTimeMillis();
		try {
			validateConfig();
			prepare();
			generateCode();
			log.info("代码生成完成，耗时: {}ms", System.currentTimeMillis() - startTime);
		} catch (Exception e) {
			log.error("代码生成失败", e);
			throw new GeneratorException("代码生成失败", e);
		}
	}

	private void validateConfig() {
		if (global == null)
			throw new GeneratorException(Consts.ERR_GLOBAL_CONFIG_NULL);
		String outputDir = global.getOutputDir();
		if (StringUtils.isBlank(outputDir))
			throw new GeneratorException(Consts.ERR_OUTPUT_DIR_EMPTY);

		File dir = new File(outputDir);
		if (!dir.exists() && !dir.mkdirs())
			throw new GeneratorException(Consts.ERR_CREATE_OUTPUT_DIR + outputDir);

		if (dataSource == null)
			throw new GeneratorException(Consts.ERR_DATASOURCE_NULL);
		try (java.sql.Connection conn = dataSource.getCon()) {
			if (conn == null || conn.isClosed())
				throw new GeneratorException(Consts.ERR_DB_CONNECTION_FAILED);
			log.info("数据库连接正常: {}", dataSource.getDbName());
		} catch (Exception e) {
			throw new GeneratorException(Consts.ERR_DB_CONNECTION_ERROR + e.getMessage(), e);
		}
	}

	private void generateCode() {
		List<Table> tables = getTables();
		if (tables.isEmpty())
			return;

		templateRenderer = new TemplateRenderer();
		contextBuilder = new TemplateContextBuilder(global, dataSource, components);

		RenderContext globalContext = contextBuilder.buildGlobalContext();
		globalContext.put(Consts.CTX_TABLES, tables);

		URL templateUrl = global.getTemplateDirPath();
		// 静态资源: 扫描并立即分发
		if (templateUrl != null) {
			log.info("开始处理静态资源 ...");
			List<TemplateResource> assets = scanFilteredResources(templateUrl, Consts.ASSETS_DIR);
			for (TemplateResource res : assets) {
				distributeAsset(res);
			}
		}
		URL engineUrl = global.getEngineTemplateDirPath();
		if (engineUrl != null) {
			List<TemplateResource> moduleResources = scanFilteredResources(engineUrl, Consts.DIR_MODULES);
			List<TemplateResource> componentResources = scanFilteredResources(engineUrl, Consts.DIR_COMPONENTS);

			// 1. 业务级别 (Components): 排序并针对每张表渲染
			log.info("开始处理业务组件模板...");
			componentResources = filterComponentResources(componentResources);
			
			// 排序：先按模块排序，再按组件排序（确保依赖顺序正确）
			// 例如：api 模块的 entity > service > vo，然后才是 app 模块的 controller
			componentResources = sortComponentResources(componentResources);
			
			log.info("待处理业务组件模板: {}, 表数量: {}, 排序策略：先模块后组件", componentResources.size(), tables.size());

			// 通过正确的排序顺序，确保所有依赖的包名变量都已注册
			// 不再需要预注册包名变量
			// log.info("开始预注册包名变量...");
			// preRegisterPackageVariables(componentResources, moduleResources);
			// log.info("包名变量预注册完成");

			// 所有文件共用全局 context，按正确顺序渲染
			for (Table table : tables) {
				// 将当前表的上下文信息注入到全局 context
				globalContext.put("table", table);
				globalContext.putAll(org.hyw.tools.generator.utils.NamingStrategy.buildNamingMap(table.getBeanName()));
				renderResources(globalContext, componentResources, true);
			}

			// 2. 模块级别 (Modules): 在组件渲染后处理，此时包名变量已就绪
			log.info("开始处理模块模板 ...");
			// 对 moduleResources 也进行排序，确保基础类先渲染
			moduleResources = sortModuleResources(moduleResources);
			renderResources(globalContext, moduleResources, true);
		}
		openDir();
	}

	/**
	 * 改进的排序方法：先按模块排序，再按组件排序，最后按文件路径和文件名排序
	 * 
	 * 排序策略：
	 * 1. 第一级：模块排序（api > app），确保基础模块先渲染
	 * 2. 第二级：组件排序（entity > service > controller），确保依赖顺序正确
	 * 3. 第三级：文件路径排序（按路径字符串）
	 * 4. 第四级：文件名排序（在路径相等时）
	 */
	private List<TemplateResource> sortComponentResources(List<TemplateResource> resources) {
		return resources.stream().sorted((r1, r2) -> {
			String module1 = inferModuleNameFromPath(r1.getPath());
			String module2 = inferModuleNameFromPath(r2.getPath());
			
			// 获取模块在 modules 数组中的索引（越小越先）
			int moduleIndex1 = getModuleIndex(module1);
			int moduleIndex2 = getModuleIndex(module2);
			
			if (moduleIndex1 != moduleIndex2) {
				return Integer.compare(moduleIndex1, moduleIndex2);
			}
			
			// 2. 同一模块内，按组件 ordinal 排序
			int componentOrdinal1 = getComponentOrdinal(r1.getPath());
			int componentOrdinal2 = getComponentOrdinal(r2.getPath());
			
			if (componentOrdinal1 != componentOrdinal2) {
				return Integer.compare(componentOrdinal1, componentOrdinal2);
			}
			
			// 3. 文件类型优先级排序（确保 .java 在 .xml 之前渲染，以便先注册包名变量）
			int typePriority1 = getFileTypePriority(r1.getPath());
			int typePriority2 = getFileTypePriority(r2.getPath());
			if (typePriority1 != typePriority2) {
				return Integer.compare(typePriority1, typePriority2);
			}
			
			// 4. 组件和模块都相同，按路径字符串排序
			int pathCompare = r1.getPath().compareTo(r2.getPath());
			if (pathCompare != 0) {
				return pathCompare;
			}
			
			// 5. 路径相等时，按文件名排序
			String fileName1 = StringUtils.substringAfterLast(r1.getPath(), SEPARATOR);
			String fileName2 = StringUtils.substringAfterLast(r2.getPath(), SEPARATOR);
			return fileName1.compareTo(fileName2);
		}).collect(Collectors.toList());
	}
	
	/**
	 * 获取文件类型优先级（数值越小优先级越高）
	 * 确保 Java 源码先于资源文件渲染，以便包名变量先被注册
	 * @param path 文件路径
	 * @return 优先级数值（1=Java源码, 2=XML资源, 3=其他）
	 */
	private int getFileTypePriority(String path) {
		if (path == null) return 3;
		String lower = path.toLowerCase();
		// Java 源码优先级最高
		if (lower.endsWith(".java") || lower.endsWith(".java.ftl") || lower.endsWith(".java.vm")) {
			return 1;
		}
		// XML 资源文件次之
		if (lower.endsWith(".xml") || lower.endsWith(".xml.ftl") || lower.endsWith(".xml.vm")) {
			return 2;
		}
		return 3;
	}
	
	/**
	 * 排序模块级别资源
	 * @param resources 模块资源列表
	 * @return 排序后的资源列表
	 */
	private List<TemplateResource> sortModuleResources(List<TemplateResource> resources) {
		return resources.stream().sorted((r1, r2) -> {
			// 1. 按模块排序
			String module1 = inferModuleNameFromPath(r1.getPath());
			String module2 = inferModuleNameFromPath(r2.getPath());
			
			int moduleIndex1 = getModuleIndex(module1);
			int moduleIndex2 = getModuleIndex(module2);
			
			if (moduleIndex1 != moduleIndex2) {
				return Integer.compare(moduleIndex1, moduleIndex2);
			}
			
			// 2. 按路径字符串排序
			return r1.getPath().compareTo(r2.getPath());
		}).collect(Collectors.toList());
	}

	private int getComponentOrdinal(String path) {
		if (path == null)
			return 999;
		String norm = FileUtils.normalizePath(path);
		if (norm.contains("#")) {
			String[] parts = norm.split(SEPARATOR);
			for (String part : parts) {
				if (part.startsWith("#") && part.endsWith("#")) {
					String alias = part.substring(1, part.length() - 1);
					Component c = Component.getComponent(alias);
					if (c != null)
						return c.ordinal();
				}
			}
		}
		return 999; // 无组件标记的资源排在最后
	}

	/**
	 * 获取模块在配置数组中的索引
	 * @param moduleName 模块名称
	 * @return 模块索引，未找到返回 999
	 */
	private int getModuleIndex(String moduleName) {
		if (global.getModules() == null) {
			return 0;
		}
		for (int i = 0; i < global.getModules().length; i++) {
			if (global.getModules()[i].equals(moduleName)) {
				return i;
			}
		}
		return 999; // 未找到的模块排在最后
	}

	private void distributeAsset(TemplateResource resource) {
		String norm = FileUtils.normalizePath(resource.getPath());
		TemplateModel m = buildBasicModel();
		m.setModuleName(inferModuleNameFromPath(norm));
		try {
			String out = pathResolver.resolve(norm, m);
			if (StringUtils.isNotBlank(out) && !out.contains(Consts.PATH_PLACEHOLDER_START)) {
				distributeBinary(resource, new File(global.getOutputDir(), out));
			}
		} catch (Exception ignored) {
		}
	}

	private List<TemplateResource> filterComponentResources(List<TemplateResource> resources) {
		String[] componentNames = global.getComponentNames();
		if (componentNames != null && componentNames.length > 0) {
			List<String> list = Arrays.asList(componentNames);
			return resources.stream().filter(res -> {
				String rel = res.getPath().substring(Consts.DIR_COMPONENTS.length() + Consts.PATH_SEPARATOR.length());
				String first = StringUtils.substringBefore(rel, SEPARATOR);
				
				// 1. 模块占位符格式：{0}, {1}
				if (first.startsWith(Consts.PATH_PLACEHOLDER_START)) {
					return true;
				}
				
				// 2. 组件标记格式：#vue#, #mybatis# - 提取别名检查组件是否启用
				if (first.startsWith("#") && first.endsWith("#")) {
					String alias = first.substring(1, first.length() - 1);
					Component c = Component.getComponent(alias);
					return c != null && isComponentEnabled(c);
				}
				
				// 3. 普通组件名
				return list.contains(first);
			}).collect(Collectors.toList());
		}
		return resources;
	}

	private void prepare() {
		delDir();
		mkDirs();
	}

	private List<TemplateResource> scanFilteredResources(URL url, String subDir) {
		List<TemplateResource> resources = FileUtils.getTemplateResources(url, subDir, global.getResources());
		return resources.stream().filter(res -> !shouldSkipByComponent(res.getPath())).collect(Collectors.toList());
	}

	private void renderResources(RenderContext context, List<TemplateResource> resources, boolean render) {
		if (null == resources)
			return;
		for (TemplateResource resource : resources) {
			renderTemplateResource(context, resource, render);
		}
	}

	private void renderTemplateResource(RenderContext context, TemplateResource resource, boolean render) {
		TemplateModel model = buildBasicModel();
		String normalizedPath = FileUtils.normalizePath(resource.getPath());
		model.setModuleName(inferModuleNameFromPath(normalizedPath));

		if (context.containsKey("table")) {
			Table table = (Table) context.get("table");
			model.setTable(table);
			model.setNaming(NamingStrategy.buildNamingMap(table.getBeanName()));
		}

		String outputPath;
		try {
			outputPath = pathResolver.resolve(normalizedPath, model);
		} catch (Exception e) {
			return;
		}

		if (outputPath == null || outputPath.contains(Consts.PATH_PLACEHOLDER_START))
			return;

		String finalModuleName = inferFinalModuleName(outputPath);
		model.setModuleName(finalModuleName);
		context.put(Consts.CTX_MODULE_NAME, finalModuleName);

		// 直接向全局 context 注册包名变量
		registerPackageVariablesToContext(context, normalizedPath, outputPath, finalModuleName);
		
		// 设置命名变量（entityName, serviceName, className 等）
		if (context.containsKey("table")) {
			Table table = (Table) context.get("table");
			Map<String, String> naming = org.hyw.tools.generator.utils.NamingStrategy.buildNamingMap(table.getBeanName());
			// 将命名变量直接放入 context，而不是 model
			for (Map.Entry<String, String> entry : naming.entrySet()) {
				context.put(entry.getKey(), entry.getValue());
			}
			// 设置额外的命名变量
			context.put("entityName", table.getBeanName());
			context.put("entityNameLower", table.getLowercaseBeanName());
			context.put("className", table.getBeanName());
			context.put("serviceName", naming.get("service"));
			context.put("serviceNameLower", naming.get("serviceLower"));
			context.put("controllerName", naming.get("controller"));
			context.put("controllerNameLower", naming.get("controllerLower"));
		}
		
		File dest = new File(global.getOutputDir(), FileUtils.normalizePath(outputPath));
		if (dest.exists() && !global.isFileOverride())
			return;

try {
			if (resource.isBinary()) {
				distributeBinary(resource, dest);
			} else if (render && isTemplateFile(normalizedPath)) {
				log.info("渲染模板: {} -> {} ", normalizedPath, outputPath);
				// 调试：检查关键的包名变量是否存在
				if (outputPath.contains("Controller")) {
					log.debug("Controller 渲染时 context 中的包名变量:");
					log.debug("  servicePackage = {}", context.get("servicePackage"));
					log.debug("  voPackage = {}", context.get("voPackage"));
					log.debug("  entityPackage = {}", context.get("entityPackage"));
					log.debug("  controllerPackage = {}", context.get("controllerPackage"));
				}
				String data = templateRenderer.render(resource.getContent(), context, global.getEngineType());
				if (StringUtils.isNotBlank(data) && StringUtils.isNotBlank(data.trim())) {
					FileUtils.write(dest, data, global.getEncoding());
					log.info("生成文件: {}, 大小: {} bytes", dest.getPath(), dest.length());
				} else {
					log.warn("模板渲染结果为空: {}", normalizedPath);
				}
			} else {
				distributeBinary(resource, dest);
			}
		} catch (Exception e) {
			log.error("生成文件失败: {}", dest.getPath(), e);
		}
	}

	private void distributeBinary(TemplateResource resource, File dest) throws IOException {
		try (InputStream is = resource.openStream()) {
			log.debug("分发文件: {}", dest.getPath());
			FileUtils.copyInputStreamToFile(is, dest);
		}
	}

	private boolean isTemplateFile(String path) {
		return path.toLowerCase().endsWith(global.getEngineType().getExtension());
	}

	private String inferFinalModuleName(String outputPath) {
		String moduleName = StringUtils.substringBefore(outputPath, SEPARATOR);
		if (StringUtils.isBlank(moduleName) || moduleName.contains(Consts.FILE_EXTENSION_SEPARATOR) || moduleName.contains(Consts.PATH_WINDOWS_SEPARATOR)) {
			String[] modules = global.getModules();
			if (modules != null) {
				for (String m : modules) {
					if (outputPath.startsWith(m + SEPARATOR))
						return m;
				}
			}
		}
		return moduleName;
	}

	/**
 * 直接向全局 context 注册包名变量
 * 
 * @param context 全局 context
 * @param templatePath 模板路径
 * @param outputPath 输出路径
 * @param moduleName 模块名
 */
private void registerPackageVariablesToContext(RenderContext context, String templatePath, String outputPath, String moduleName) {
	if (outputPath == null || !outputPath.endsWith(".java")) {
		return;
	}
	
	// 提取包名
	String javaDirMark = Consts.DIR_JAVA + Consts.PATH_SEPARATOR;
	int index = outputPath.indexOf(javaDirMark);
	if (index == -1) return;
	
	String packagePath = outputPath.substring(index + javaDirMark.length());
	// 移除文件名（最后一个路径分隔符之后的部分）
	int lastSlashIndex = packagePath.lastIndexOf(Consts.PATH_SEPARATOR);
	if (lastSlashIndex != -1) {
		packagePath = packagePath.substring(0, lastSlashIndex);
	}
	String fullPackage = packagePath.replace(Consts.PATH_SEPARATOR, ".");
	
	if (fullPackage == null || fullPackage.isEmpty()) {
		return;
	}
	
	// 1. 根据父目录名注册包名变量
	String parentDir = org.apache.commons.lang3.StringUtils.substringAfterLast(
		org.apache.commons.lang3.StringUtils.substringBeforeLast(outputPath, Consts.PATH_SEPARATOR), 
		Consts.PATH_SEPARATOR);
	if (org.apache.commons.lang3.StringUtils.isNotBlank(parentDir)) {
		String varName = parentDir + "Package";
		context.put(varName, fullPackage);
		log.debug("注册包名变量: {} = {}", varName, fullPackage);
		// 同时注册模块前缀的变量
		if (org.apache.commons.lang3.StringUtils.isNotBlank(moduleName)) {
			context.put(moduleName + "_" + varName, fullPackage);
			log.debug("注册模块包名变量: {} = {}", moduleName + "_" + varName, fullPackage);
		}
	}
	
	// 2. 根据文件名注册包名变量
	String fileName = org.apache.commons.lang3.StringUtils.substringAfterLast(outputPath, Consts.PATH_SEPARATOR);
	String className = org.apache.commons.lang3.StringUtils.substringBefore(fileName, ".java");
	if (org.apache.commons.lang3.StringUtils.isNotBlank(className)) {
		context.put(className + "Package", fullPackage);
		context.put(className + "FullPackage", fullPackage + "." + className);
		// 同时注册模块前缀的变量
		if (org.apache.commons.lang3.StringUtils.isNotBlank(moduleName)) {
			context.put(moduleName + "_" + className + "Package", fullPackage);
			context.put(moduleName + "_" + className + "FullPackage", fullPackage + "." + className);
		}
		context.put(parentDir + "Name", className);
	}
}

private boolean shouldSkipByComponent(String path) {
	if (path == null)
		return false;
	String norm = FileUtils.normalizePath(path);
	if (!norm.contains("#")) {
		return false;
	}
	String[] parts = norm.split(SEPARATOR);
	for (String part : parts) {
		if (part.startsWith("#") && part.endsWith("#")) {
			String alias = part.substring(1, part.length() - 1);
			Component c = Component.getComponent(alias);
			if (c != null && !isComponentEnabled(c)) {
				return true;
			}
		}
	}
	return false;
}

	private boolean isComponentEnabled(Component c) {
		if (global.getComponents() == null)
			return false;
		for (Component enabled : global.getComponents()) {
			if (enabled == c)
				return true;
		}
		return false;
	}

	private String inferModuleNameFromPath(String path) {
		String remaining = StringUtils.substringAfter(FileUtils.normalizePath(path), SEPARATOR);
		String part = StringUtils.substringBefore(remaining, SEPARATOR);
		if (part.startsWith(Consts.PATH_PLACEHOLDER_START) && part.endsWith(Consts.PATH_PLACEHOLDER_END)) {
			try {
				int idx = Integer.parseInt(part.substring(Consts.PATH_PLACEHOLDER_START.length(), 
					part.length() - Consts.PATH_PLACEHOLDER_END.length()));
				if (global.getModules() != null && idx < global.getModules().length)
					return global.getModules()[idx];
			} catch (Exception ignored) {
			}
		}
		return part;
	}

	private TemplateModel buildBasicModel() {
		TemplateModel m = new TemplateModel();
		m.setConfig(global);
		m.setDataSource(dataSource);
		m.setRootPackage(global.getRootPackage());
		m.setProjectName(global.getProjectName());
		if (global.getComponents() != null)
			m.setComponents(new HashSet<>(Arrays.asList(global.getComponents())));
		return m;
	}

	public void generateDoc(File outputFile, String format, List<String> fontPaths) {
		try {
			List<Table> tables = getTables();
			if (tables.isEmpty())
				throw new GeneratorException(Consts.ERR_NO_TABLES_FOUND);

			ExportFormat exportFormat = ExportFormat.fromExtension(format);
			if (exportFormat == null)
				exportFormat = ExportFormat.WORD;

			if (fontPaths == null && exportFormat == ExportFormat.PDF) {
				fontPaths = FontUtils.getPdfFontPaths(global.getOSFontConfig());
			}
			DbToDoc.toDoc(tables, exportFormat, outputFile.getAbsolutePath(), fontPaths);
			log.info("文档生成完成: {}", outputFile.getAbsolutePath());
		} catch (Exception e) {
			log.error("生成文档失败", e);
			throw new GeneratorException(Consts.ERR_DOC_GENERATION_FAILED + e.getMessage(), e);
		}
	}

	public synchronized void save() {
		try {
			File dir = new File(EXTERNAL_CONFIG_DIR);
			if (!dir.exists())
				dir.mkdirs();

			String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			File configFile = new File(dir, "generator_" + timestamp + Consts.EXT_YAML);

			org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
			options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
			options.setPrettyFlow(true);
			Yaml yaml = new Yaml(new SkipRuntimeFieldsRepresenter(options), options);

			java.io.StringWriter stringWriter = new java.io.StringWriter();
			yaml.dump(this, stringWriter);
			String yamlContent = removeTypeTagsAndNulls(stringWriter.toString());

			org.apache.commons.io.FileUtils.writeStringToFile(configFile, yamlContent, Consts.DEFAULT_ENCODING);
			log.info("配置已持久化至: {}", configFile.getAbsolutePath());
		} catch (Exception e) {
			log.error("持久化配置失败", e);
		}
	}

	private static String removeTypeTagsAndNulls(String yamlContent) {
		String[] lines = yamlContent.split("\n");
		StringBuilder result = new StringBuilder();
		for (String line : lines) {
			if (line.trim().matches(".*:\\s*null$"))
				continue;
			line = line.replaceAll("^!![^\\s\\n]+\\s*", "");
			line = line.replaceAll("-\\s*!![^\\s\\n]+\\s+", "- ");
			result.append(line).append("\n");
		}
		return result.toString();
	}
}
