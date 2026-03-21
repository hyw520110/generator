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
	private org.hyw.tools.generator.template.PathTemplateResolver pathResolver = new org.hyw.tools.generator.template.DefaultPathTemplateResolver();
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
		}
		// 注意：不在此关闭 dataSource，因为 Generator 是单例，dataSource 应保持连接池状态
		// dataSource 的生命周期由外部管理（如 Spring 容器或应用关闭时）
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
		// 1. 获取渲染模型
		org.hyw.tools.generator.template.TemplateModel model = new org.hyw.tools.generator.template.TemplateModel();
		model.setConfig(global);
		model.setDataSource(dataSource);
		if (context.containsKey("table")) {
			model.setTable((org.hyw.tools.generator.conf.db.Table) context.get("table"));
		}

		// 2. 解析输出路径 (统一使用 pathResolver)
		String outputPath = pathResolver.resolve(path, model);
		
		// 3. 设置上下文变量
		if (StringUtils.isNotBlank(name)) {
			context.put("entityName", name);
			context.put("mapperName", name + "Mapper");
			context.put("serviceName", name + "Service");
			context.put("serviceImplName", name + "ServiceImpl");
			context.put("controllerName", name + "Controller");
		}

		String moduleName = StringUtils.substringBefore(path, SEPARATOR);
		context.put("moduleName", moduleName);
		
		if (outputPath.endsWith(".java")) {
			String spackage = getPackageFromPath(outputPath);
			String pName = StringUtils.substringAfterLast(spackage, ".");
			context.put(pName + "Package", spackage);
			String className = StringUtils.substringBefore(StringUtils.substringAfterLast(outputPath, SEPARATOR), ".");
			context.put("className", className);
			context.put(className + "Package", spackage);
		}

		// 4. 判定是否为模板文件
		String extension = global.getEngineType().getExtension();
		boolean isTemplateFile = path.toLowerCase().endsWith(extension);
		
		// 检查资源文件扩展名
		boolean isBinaryResource = isResourceFile(path);
		
		// 检查是否在排除目录中
		boolean isExcluded = false;
		if (global.getExcludeDir() != null && outputPath.contains(global.getResourceDirectory())) {
			String dirInResources = StringUtils.substringBetween(
				StringUtils.substringAfter(outputPath, global.getResourceDirectory()), "/", "/");
			isExcluded = ArrayUtils.contains(global.getExcludeDir(), "/" + dirInResources);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("处理文件: {}, 输出: {}, 是否模板: {}, 渲染模式: {}, 是否排除: {}", path, outputPath, isTemplateFile, render, isExcluded);
		}

		// 5. 执行渲染或直接写入
		
		// 过滤模板片段文件：如果输出路径的文件名以 _ 开头，则跳过生成
		String finalFileName = StringUtils.substringAfterLast(outputPath, SEPARATOR);
		if (StringUtils.isBlank(finalFileName)) finalFileName = outputPath;
		if (finalFileName.startsWith("_")) {
			if (logger.isDebugEnabled()) {
				logger.debug("跳过模板片段文件: {}", outputPath);
			}
			return;
		}

		if (isBinaryResource) {
			// 二进制资源：执行流式拷贝（当前 data 仍为 Base64 字符串，后续优化为直接读取字节）
			writeBinaryFile(outputPath, Base64.getDecoder().decode(data));
		} else if (isTemplateFile && render && !isExcluded) {
			try {
				data = templateRenderer.render(data, context, global.getEngineType());
				// 移除模板后缀写入
				writeToFile(outputPath, data, false);
			} catch (Exception e) {
				logger.error("模板文件渲染失败: {}, 错误: {}", path, e.getMessage(), e);
			}
		} else {
			// 普通非模板文件（如脚本、静态配置）
			// 如果文件名以 _ 开头，通常是 include 片段，跳过直接生成
			String fileName = StringUtils.substringAfterLast(outputPath, "/");
			if (!fileName.startsWith("_")) {
				writeToFile(outputPath, data, true);
			}
		}
	}

	private boolean isResourceFile(String path) {
		return ArrayUtils.contains(global.getResources(),
				StringUtils.lowerCase(StringUtils.substringAfterLast(path, ".")));
	}

	private String getPackageFromPath(String path) {
		String spackage = (path.contains(global.getSourceDirectory())
				? StringUtils.substringAfter(path, global.getSourceDirectory())
				: StringUtils.substringAfter(path, global.getTestSourceDirectory()));
		if (StringUtils.isBlank(spackage)) return "";
		int lastSlash = spackage.lastIndexOf(SEPARATOR);
		if (lastSlash <= 1) return "";
		return StringUtils.substring(spackage, 1, lastSlash).replaceAll(SEPARATOR, ".");
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
		File dest = new File(global.getOutputDir(), path);
		
		if (StringUtils.isBlank(data) || (dest.exists() && !global.isFileOverride())) {
			return;
		}
		
		if (dest.exists()) {
			logger.warn("覆盖文件: {}", dest);
		} else {
			logger.info("生成文件: {}", dest);
		}
		
		try {
			FileUtils.write(dest, data, global.getEncoding());
		} catch (Exception e) {
			logger.error("写入文件失败: {}", dest, e);
		}
	}

	private void prepare() {
		delDir();
		mkDirs();
		distributeAssets();
	}
	
	/**
	 * 分发静态资源 (支持 templates/assets 目录)
	 */
	private void distributeAssets() {
		URL engineUrl = global.getEngineTemplateDirPath();
		if (engineUrl == null) return;
		
		logger.info("开始分发静态资源...");
		
		try {
			// 修正：从引擎目录 URL 向上回退到 templates 根目录
			// 示例: file:/.../templates/freemarker/ -> file:/.../templates/
			java.net.URI engineUri = engineUrl.toURI();
			java.net.URI rootUri = engineUri.getPath().endsWith("/") ? engineUri.resolve("..") : engineUri.resolve(".");
			URL rootUrl = rootUri.toURL();
			
			if ("jar".equals(rootUrl.getProtocol())) {
				String basePath = rootUrl.getPath();
				if (basePath.contains("!/")) basePath = StringUtils.substringAfter(basePath, "!/");
				String assetsEntry = (basePath.endsWith("/") ? basePath : basePath + "/") + "assets";
				Map<String, String> assets = FileUtils.getJarEntries(rootUrl, assetsEntry, null, global.getResources());
				if (assets != null) {
					for (Entry<String, String> entry : assets.entrySet()) {
						String relativePath = entry.getKey().substring(assetsEntry.length() + 1);
						processAssetFile("assets/" + relativePath, Base64.getDecoder().decode(entry.getValue()));
					}
				}
			} else {
				File templatesRootDir = new File(rootUri);
				File assetsDir = new File(templatesRootDir, "assets");
				if (assetsDir.exists() && assetsDir.isDirectory()) {
					java.util.Collection<File> files = org.apache.commons.io.FileUtils.listFiles(assetsDir, null, true);
					for (File file : files) {
						String relativePath = file.getAbsolutePath().replace(assetsDir.getAbsolutePath(), "");
						if (relativePath.startsWith(File.separator)) relativePath = relativePath.substring(1);
						processAssetFile("assets/" + relativePath, org.apache.commons.io.FileUtils.readFileToByteArray(file));
					}
					logger.info("已从 {} 完成静态资源分发", assetsDir.getAbsolutePath());
				} else {
					logger.warn("未找到静态资源目录: {}", assetsDir.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			logger.warn("静态资源分发跳过: {}", e.getMessage());
		}
	}

	/**
	 * 统一处理资源文件路径解析与写入
	 */
	private void processAssetFile(String virtualPath, byte[] bytes) {
		if (bytes == null || bytes.length == 0) return;

		org.hyw.tools.generator.template.TemplateModel model = new org.hyw.tools.generator.template.TemplateModel();
		model.setConfig(global);
		
		// 通过 pathResolver 解析最终路径 (确保 virtualPath 包含 assets/ 等一级前缀以适配剥离逻辑)
		String outputPath = pathResolver.resolve(virtualPath, model);
		writeBinaryFile(outputPath, bytes);
	}

	public Map<String, String> getTemplates(String subDirName, boolean buildPath, String... componentNames) {
		URL url = global.getEngineTemplateDirPath();
		if (null == url) {
			logger.error("{} template dir not exist!", subDirName);
			throw new RuntimeException(subDirName + " template dir not exist!");
		}
		
		Map<String, String> templates = new LinkedHashMap<>();
		
		if ("jar".equals(url.getProtocol())) {
			// 获取基础路径：如 templates/freemarker/
			String basePath = url.getPath();
			if (basePath.contains("!/")) {
				basePath = StringUtils.substringAfter(basePath, "!/");
			}
			if (basePath.endsWith("/")) {
				basePath = basePath.substring(0, basePath.length() - 1);
			}
			String entryName = basePath + "/" + subDirName;
			
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
				// 不再在此调用 buildPath，保留 subDirName/relativePath
				templates.put(subDirName + SEPARATOR + path, entry.getValue());
			}
		} else {
			File baseDir = new File(url.getPath(), subDirName);
			if (!baseDir.exists()) {
				logger.warn("模板目录不存在: {}", baseDir);
				return templates;
			}
			
			java.util.Collection<File> allFiles = org.apache.commons.io.FileUtils.listFiles(baseDir, null, true);
			List<String> componentList = componentNames != null ? Arrays.asList(componentNames) : new ArrayList<>();
			
			for (File file : allFiles) {
				String relativePath = file.getAbsolutePath().replace(baseDir.getAbsolutePath(), "");
				if (relativePath.startsWith(File.separator)) {
					relativePath = relativePath.substring(1);
				}
				
				if (!componentList.isEmpty()) {
					String firstDir = StringUtils.substringBefore(relativePath.replace("\\", "/"), "/");
					if (!componentList.contains(firstDir)) {
						continue;
					}
				}
				
				try {
					String data;
					if (isResourceFile(file.getName())) {
						data = Base64.getEncoder().encodeToString(org.apache.commons.io.FileUtils.readFileToByteArray(file));
					} else {
						data = org.apache.commons.io.FileUtils.readFileToString(file, global.getEncoding());
					}
					// 统一使用相对路径作为 Key
					templates.put(subDirName + SEPARATOR + relativePath.replace("\\", "/"), data);
				} catch (IOException e) {
					logger.error("读取模板文件失败: {}", file, e);
				}
			}
		}
		
		Map<String, String> resultMap = new TreeMap<>((str1, str2) -> str1.compareTo(str2));
		resultMap.putAll(templates);
		return resultMap;
	}

	private String getPackage(String path) {
		String spackage = (path.contains(global.getSourceDirectory())
				? StringUtils.substringAfter(path, global.getSourceDirectory())
				: StringUtils.substringAfter(path, global.getTestSourceDirectory()));
		return StringUtils.substring(spackage, 1, spackage.lastIndexOf(SEPARATOR)).replaceAll(SEPARATOR, ".");
	}

}
