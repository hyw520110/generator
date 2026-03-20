package org.hyw.tools.generator;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.velocity.VelocityContext;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.exception.GeneratorException;
import org.hyw.tools.generator.template.EngineType;
import org.hyw.tools.generator.template.RenderContext;
import org.hyw.tools.generator.template.TemplateContextBuilder;
import org.hyw.tools.generator.template.TemplateRenderer;
import org.hyw.tools.generator.utils.FileUtils;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * 代码生成器
 * 
 * @author: heyiwu
 * @version: 1.0 Create at: 2017年6月12日 上午10:53:49
 *
 */
public class Generator extends AbstractGenerator {

	private static final Logger logger = LoggerFactory.getLogger(Generator.class.getName());
	private static final long serialVersionUID = 345083252519120430L;

	/**
	 * 生成器默认配置文件
	 */
	private static String conf = "/generator.yaml";
	private static Generator generator;
	private TemplateRenderer templateRenderer;
	private TemplateContextBuilder contextBuilder;
	private static final String SEPARATOR = "/";
	static {
		load(conf);
	}

	private Generator() {
	}

	public static Generator getInstance() {
		return generator;
	}

	private static void load(String conf) {
		InputStream is = Generator.class.getResourceAsStream(conf);
		if (null == is) {
			logger.warn("{} config file not found!", conf);
			return;
		}
		generator = new Yaml().loadAs(is, Generator.class);
	}

	public static void main(String[] args) throws Exception {
		Generator generator = getInstance();
		long s = System.currentTimeMillis();
		generator.execute();
		logger.info("generation completed:{}ms", System.currentTimeMillis() - s);
	}

	/**
	 * 生成代码
	 * @throws IOException 
	 * 
	 * @throws Exception
	 */
	public void execute() {
		long startTime = System.currentTimeMillis();
		try {
			validateConfig();
			prepare();
			generateCode();
			long duration = System.currentTimeMillis() - startTime;
			logger.info("代码生成完成，耗时: {}ms", duration);
		} catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			logger.error("代码生成失败，耗时: {}ms", duration, e);
			throw new GeneratorException("代码生成失败", e);
		} finally {
			if (dataSource != null) {
				dataSource.close();
			}
		}
	}

	/**
	 * 生成数据库表文档（Word/PDF）
	 * @param outputFile 输出文件
	 * @param format 文档格式：word 或 pdf
	 * @param fontPaths PDF 中文字体路径列表（按优先级排序）
	 */
	public void generateDoc(File outputFile, String format, List<String> fontPaths) {
		long startTime = System.currentTimeMillis();
		try {
			// 获取表列表
			List<Table> tables = getTables();
			if (tables.isEmpty()) {
				throw new GeneratorException("没有找到要生成文档的表");
			}
			
			logger.info("开始生成{}文档，表数量: {}", "word".equals(format) ? "Word" : "PDF", tables.size());
			
			// 使用 DbToDoc 工具类生成文档
			ExportFormat exportFormat = "pdf".equals(format) ? ExportFormat.PDF : ExportFormat.DOCX;
			DbToDoc.toDoc(tables, exportFormat, outputFile.getAbsolutePath(), fontPaths);
			
			long duration = System.currentTimeMillis() - startTime;
			logger.info("文档生成完成: {}, 耗时: {}ms", outputFile.getAbsolutePath(), duration);
		} catch (Exception e) {
			logger.error("生成文档失败", e);
			throw new GeneratorException("生成文档失败: " + e.getMessage(), e);
		}
	}
	
	private void validateConfig() {
		if (global == null) {
			throw new GeneratorException("全局配置不能为空");
		}
		
		String outputDir = global.getOutputDir();
		if (StringUtils.isBlank(outputDir)) {
			throw new GeneratorException("输出目录不能为空");
		}
		
		File dir = new File(outputDir);
		if (dir.exists() && !dir.canWrite()) {
			throw new GeneratorException("输出目录无写权限: " + outputDir);
		}
		
		if (global.getEngineTemplateDirPath() == null) {
			throw new GeneratorException("模板目录不存在: " + global.getTemplateDir());
		}
		
		if (dataSource == null) {
			throw new GeneratorException("数据源配置不能为空");
		}
		
		// 数据库连通性心跳检测
		try (java.sql.Connection conn = dataSource.getCon()) {
			if (conn == null || conn.isClosed()) {
				throw new GeneratorException("数据库连接失败，请检查配置");
			}
			logger.info("数据库连接正常: {}", dataSource.getDbName());
		} catch (Exception e) {
			throw new GeneratorException("数据库连接异常: " + e.getMessage(), e);
		}
		
		if (global.getComponents() == null || global.getComponents().length == 0) {
			logger.warn("未配置任何组件，将生成基础代码结构");
		}
	}
	
	private void generateCode() {
		List<Table> tables = getTables();
		if (tables.isEmpty()) {
			logger.warn("查询表为空！");
			return;
		}
		
		// 初始化模板渲染器和上下文构建器
		templateRenderer = new TemplateRenderer();
		contextBuilder = new TemplateContextBuilder(global, dataSource, components);
		
		// 创建全局上下文
		RenderContext globalContext = contextBuilder.buildGlobalContext();
		globalContext.put("tables", tables);
		
		// 创建工程模块
		String[] componentNames = global.getComponentNames();
		renderModules(globalContext, componentNames);
		
		// 渲染组件模板
		Map<String, String> templates = getTemplates("components", true, componentNames);
		logger.info("已加载组件模板数量: {}, 表数量: {}", templates.size(), tables.size());
		if (logger.isDebugEnabled()) {
			templates.keySet().forEach(path -> logger.debug("组件模板路径: {}", path));
		}
		for (Table table : tables) {
			RenderContext tableContext = contextBuilder.buildTableContext(table);
			renderComponentTemplates(tableContext, templates, table.getBeanName());
		}
		
		openDir();
	}
	
	private void renderModules(RenderContext context, String[] componentNames) {
		URL dir = global.getEngineTemplateDirPath();
		render(dir, context, getTemplates("modules", false), true);
		render(dir, context, getTemplates("modules", false, Component.VUE.name().toLowerCase()), false);
		render(dir, context, getTemplates("commons", true, componentNames), true);
		// 当生成 vue 模块时，也需要生成 commons/vue 目录下的文件
		render(dir, context, getTemplates("commons", true, Component.VUE.name().toLowerCase()), true);
	}
	
	private void renderComponentTemplates(RenderContext context, Map<String, String> templates, String tableName) {
		URL dir = global.getEngineTemplateDirPath();
		render(dir, context, templates, true, tableName);
	}

	private void render(final URL dir, RenderContext context, Map<String, String> templates, boolean render) {
		render(dir, context, templates, render, null);
	}

	private void render(final URL dir, RenderContext context, Map<String, String> templates, boolean render,
			String name) {
		if (null == templates || templates.isEmpty()) {
			logger.error("{}目录下没有所需的模板文件！", dir);
			return;
		}
		
		for (Entry<String, String> entry : templates.entrySet()) {
			String path = entry.getKey();
			String data = entry.getValue();
			try {
				renderTemplateFile(dir, context, path, data, render, name);
			} catch (Exception e) {
				logger.error("渲染文件失败: {}", path, e);
			}
		}
	}
		
	private void renderTemplateFile(final URL dir, RenderContext context, String path, String data, 
			boolean render, String name) {
		// 1. 动态路径格式化 (处理占位符)
		if (StringUtils.isNotBlank(name)) {
			// 处理 %s 占位符
			if (path.contains("%s")) {
				try {
					path = String.format(path, 
						StringUtils.countMatches(path, "%s") > 1 ? StringUtils.lowercaseFirst(name) : name, 
						name);
				} catch (Exception e) {
					logger.warn("路径 %s 格式化跳过: {}", path);
				}
			}
			// 处理模板风格占位符 ${table.beanName}
			if (path.contains("${table.beanName}")) {
				path = path.replace("${table.beanName}", name);
			}
			// 设置实体相关的上下文变量（与原代码保持一致）
			context.put("entityName", name);
			context.put("mapperName", name + "Mapper");
			context.put("serviceName", name + "Service");
			context.put("serviceImplName", name + "ServiceImpl");
			context.put("controllerName", name + "Controller");
		}

		// 2. 注入当前文件的动态上下文变量
		String moduleName = StringUtils.substringBefore(path, SEPARATOR);
		context.put("moduleName", moduleName);
		
		// 对于 Java 文件（.java 或 .java.ftl），提取类名和包名
		String pathForClassCheck = path;
		if (pathForClassCheck.endsWith(".ftl")) {
			pathForClassCheck = pathForClassCheck.substring(0, pathForClassCheck.length() - 4);
		}
		if (pathForClassCheck.endsWith(".java")) {
			String spackage = getPackage(pathForClassCheck);
			String pName = StringUtils.substringAfterLast(spackage, ".");
			context.put(pName + "Package", spackage);
			String className = StringUtils.substringBefore(StringUtils.substringAfterLast(pathForClassCheck, SEPARATOR), ".");
			context.put("className", className);
			context.put(className + "Package", spackage);
		}

		// 3. 判定是否为模板文件 - 仅根据文件扩展名判断
		// 不根据内容判断，避免将 Shell 脚本、Spring 配置等文件错误地当作模板处理
		String extension = global.getEngineType().getExtension();
		boolean isTemplateFile = path.toLowerCase().endsWith(extension);
		
		// 检查是否为资源文件（即使有 .ftl 扩展名）
		// 如 bootstrap.min.js.ftl 应该被当作资源文件，而不是模板
		String[] resourceExts = global.getResources();
		if (resourceExts != null && isTemplateFile) {
			String pathWithoutExt = path.substring(0, path.length() - extension.length());
			for (String resExt : resourceExts) {
				if (pathWithoutExt.toLowerCase().endsWith("." + resExt.toLowerCase())) {
					isTemplateFile = false;
					break;
				}
			}
		}
		
		// 检查是否在排除目录中
		boolean isExcluded = false;
		if (global.getExcludeDir() != null && path.contains(global.getResourceDirectory())) {
			String dirInResources = StringUtils.substringBetween(
				StringUtils.substringAfter(path, global.getResourceDirectory()), "/", "/");
			isExcluded = ArrayUtils.contains(global.getExcludeDir(), "/" + dirInResources);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("处理文件: {}, 是否模板: {}, 渲染模式: {}, 是否排除: {}", path, isTemplateFile, render, isExcluded);
		}

		// 4. 执行渲染（排除目录中的文件不渲染）
		if (isTemplateFile && render && !isExcluded) {
			try {
				EngineType engineType = global.getEngineType();
				data = templateRenderer.render(data, context, engineType);
			} catch (Exception e) {
				logger.error("模板文件渲染失败: {}, 错误: {}", path, e.getMessage(), e);
				return;
			}
		}
		
		// 5. 资源文件与结果写入
		boolean isResource = ArrayUtils.contains(global.getResources(), StringUtils.substringAfterLast(path, "."));
		if (isResource) {
			writeBinaryFile(path, Base64.getDecoder().decode(data));
		} else {
			String outputPath = isTemplateFile ? global.getEngineType().removeExtension(path) : path;
			writeToFile(outputPath, data, !isTemplateFile);
		}
	}

	private boolean isResourceFile(String path) {
		return ArrayUtils.contains(global.getResources(),
				StringUtils.lowerCase(StringUtils.substringAfterLast(path, ".")));
	}

	private String getPackage(String path) {
		String spackage = (path.contains(global.getSourceDirectory())
				? StringUtils.substringAfter(path, global.getSourceDirectory())
				: StringUtils.substringAfter(path, global.getTestSourceDirectory()));
		return StringUtils.substring(spackage, 1, spackage.lastIndexOf(SEPARATOR)).replaceAll(SEPARATOR, ".");
	}
		
	private void writeBinaryFile(String path, byte[] bytes) {
		File dest = new File(global.getOutputDir(), path);
		if (dest.exists() && !global.isFileOverride()) {
			return;
		}
		try {
			org.apache.commons.io.FileUtils.writeByteArrayToFile(dest, bytes);
			logger.info("生成资源文件: {}", dest);
		} catch (Exception e) {
			logger.error("写入资源文件失败: {}", dest, e);
		}
	}
		
	private void writeToFile(String path, String data, boolean skipRender) {
		// 如果文件是模板文件（需要渲染），移除模板扩展名
		String outputPath = path;
		if (!skipRender) {
			outputPath = global.getEngineType().removeExtension(path);
		}
		
		File dest = new File(global.getOutputDir(), outputPath);
		
		if ((!skipRender && StringUtils.isBlank(data)) || (dest.exists() && !global.isFileOverride())) {
			return;
		}
		
		if (dest.exists()) {
			logger.warn("覆盖文件: {}", dest);
		} else {
			logger.info("生成文件: {}", dest);
		}
		
		try {
			FileUtils.write(dest, data);
		} catch (Exception e) {
			logger.error("写入文件失败: {}", dest, e);
		}
	}

	private void prepare() {
		delDir();
		mkDirs();
	}

	public Map<String, String> getTemplates(String subDirName, boolean buildPath, String... componentNames) {
		URL url = global.getEngineTemplateDirPath();
		if (null == url) {
			logger.error("{} template dir not exist!", subDirName);
			throw new RuntimeException(subDirName + " template dir not exist!");
		}
		
		Map<String, String> templates = new LinkedHashMap<>();
		String subPathPrefix = subDirName + SEPARATOR;
		
		if ("jar".equals(url.getProtocol())) {
			// 获取基础路径：如 templates/freemarker/
			String basePath = url.getPath();
			logger.debug("JAR URL getPath: {}", basePath);
			if (basePath.contains("!/")) {
				basePath = StringUtils.substringAfter(basePath, "!/");
			}
			logger.debug("basePath after extraction: {}", basePath);
			// 确保basePath不以/结尾，以便正确拼接
			if (basePath.endsWith("/")) {
				basePath = basePath.substring(0, basePath.length() - 1);
			}
			String entryName = basePath + "/" + subDirName;
			logger.debug("entryName: {}", entryName);
			logger.debug("componentNames: {}", Arrays.toString(componentNames));
			
			Map<String, String> map = FileUtils.getJarEntries(url, entryName,
					(null == componentNames || componentNames.length == 0)
							? (entryName + SEPARATOR + Component.VUE.name().toLowerCase())
							: null,
					global.getResources(), componentNames);
			for (Entry<String, String> entry : map.entrySet()) {
				String path = entry.getKey().replace(entryName, "");
				if (path.startsWith(SEPARATOR)) {
					path = path.substring(1);
				}
				String newPath = buildPath(buildPath, path);
				templates.put(newPath, entry.getValue());
			}
		} else {
			File baseDir = new File(url.getPath(), subDirName);
			if (!baseDir.exists()) {
				logger.warn("模板目录不存在: {}", baseDir);
				return templates;
			}
			
			// 递归获取所有文件
			java.util.Collection<File> allFiles = org.apache.commons.io.FileUtils.listFiles(baseDir, null, true);
			List<String> componentList = componentNames != null ? Arrays.asList(componentNames) : new ArrayList<>();
			
			for (File file : allFiles) {
				// 获取相对于 subDirName 的相对路径
				String relativePath = file.getAbsolutePath().replace(baseDir.getAbsolutePath(), "");
				if (relativePath.startsWith(File.separator)) {
					relativePath = relativePath.substring(1);
				}
				
				// 过滤组件：如果指定了组件名，则相对路径的第一级目录必须匹配其中之一
				if (!componentList.isEmpty()) {
					String firstDir = StringUtils.substringBefore(relativePath.replace("\\", "/"), "/");
					if (!componentList.contains(firstDir)) {
						continue;
					}
				}
				
				try {
					String path = buildPath(buildPath, relativePath);
					String data;
					if (ArrayUtils.contains(global.getResources(), StringUtils.substringAfterLast(file.getName(), "."))) {
						data = Base64.getEncoder().encodeToString(org.apache.commons.io.FileUtils.readFileToByteArray(file));
					} else {
						data = org.apache.commons.io.FileUtils.readFileToString(file, global.getEncoding());
					}
					templates.put(path, data);
				} catch (IOException e) {
					logger.error("读取模板文件失败: {}", file, e);
				}
			}
		}
		
		// 排序以保证生成顺序一致
		Map<String, String> resultMap = new TreeMap<>((str1, str2) -> str1.compareTo(str2));
		resultMap.putAll(templates);
		return resultMap;
	}

	private String buildPath(boolean buildPath, String path) {
		// 手动替换 {0}, {1} 等模块占位符，避免 MessageFormat 冲突
		String[] modules = global.getModules();
		if (modules != null && modules.length > 0) {
			for (int i = 0; i < modules.length; i++) {
				path = path.replace("{" + i + "}", modules[i]);
			}
		}
		
		path = isWin() ? path.replace("\\", "/") : path;
		String moduleName = StringUtils.substringBefore(path, SEPARATOR);
		boolean skipBuild = false;
		if (path.startsWith(Component.VUE.name().toLowerCase() + SEPARATOR)) {
			skipBuild = true;
		}

		if (buildPath && !skipBuild) {
			// 判定是否为 Java 文件，需考虑模板后缀 (.ftl 或 .vm)
			String engineExt = global.getEngineType().getExtension();
			boolean isJava = path.endsWith(".java") || path.endsWith(".java" + engineExt);
			
			String subPath = StringUtils.substringAfter(path, SEPARATOR);
			moduleName = StringUtils.substringBefore(subPath, SEPARATOR);
			subPath = StringUtils.substringAfter(subPath, moduleName + SEPARATOR);
			boolean isTest = subPath.startsWith("test");
			if (isJava) {
				// 标准 Maven Java 路径：module/src/main/java/rootPackage/projectName/module/relativePath
				path = StringUtils.toPath(moduleName,
						isTest ? global.getTestSourceDirectory() : global.getSourceDirectory(),
						global.getRootPackage().replace(".", SEPARATOR), 
						global.getProjectName(), moduleName, subPath);
			} else {
				// 资源文件路径：module/src/main/resources/relativePath
				path = StringUtils.toPath(moduleName,
						isTest ? global.getTestResourceDirectory() : global.getResourceDirectory(),
						isTest ? StringUtils.substringAfter(subPath, SEPARATOR) : subPath);
			}
		}
		return path;
	}

}
