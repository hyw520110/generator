package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.exception.GeneratorException;
import org.hyw.tools.generator.template.RenderContext;
import org.hyw.tools.generator.template.TemplateContextBuilder;
import org.hyw.tools.generator.template.TemplateRenderer;
import org.hyw.tools.generator.utils.FileUtils;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;

/**
 * 代码生成器核心类
 * 
 * @author heyiwu
 */
@Slf4j
public class Generator extends AbstractGenerator {

	private static final String conf = "/generator.yaml";
	private static Generator generator;
	private TemplateRenderer templateRenderer;
	private TemplateContextBuilder contextBuilder;
	private org.hyw.tools.generator.template.PathTemplateResolver pathResolver = new org.hyw.tools.generator.template.DefaultPathTemplateResolver();
	private static final String SEPARATOR = "/";

	static {
		load(conf);
	}

	public static Generator getInstance() {
		if (null == generator) {
		}
		return generator;
	}

	public static void load(String conf) {
		InputStream is = Generator.class.getResourceAsStream(conf);
		if (null == is) {
			log.warn("{} config file not found!", conf);
			return;
		}
		generator = new Yaml().loadAs(is, Generator.class);
	}

	public static void main(String[] args) throws Exception {
		Generator generator = getInstance();
		long s = System.currentTimeMillis();
		generator.execute();
		log.info("generation completed:{}ms", System.currentTimeMillis() - s);
	}

	/**
	 * 生成代码核心入口
	 */
	public void execute() {
		long startTime = System.currentTimeMillis();
		try {
			validateConfig();
			prepare();
			generateCode();
			long duration = System.currentTimeMillis() - startTime;
			log.info("代码生成完成，耗时: {}ms", duration);
		} catch (Exception e) {
			long duration = System.currentTimeMillis() - startTime;
			log.error("代码生成失败，耗时: {}ms", duration, e);
			throw new GeneratorException("代码生成失败", e);
		}
	}

	/**
	 * 生成数据库表文档（Word/PDF）
	 * @param outputFile 输出文件
	 * @param format 文档格式：word 或 pdf
	 * @param fontPaths PDF 中文字体路径列表
	 */
	public void generateDoc(File outputFile, String format, List<String> fontPaths) {
		long startTime = System.currentTimeMillis();
		try {
			List<Table> tables = getTables();
			if (tables.isEmpty()) {
				throw new GeneratorException("没有找到要生成文档的表");
			}
			log.info("开始生成{}文档，表数量: {}", "word".equals(format) ? "Word" : "PDF", tables.size());
			ExportFormat exportFormat = "pdf".equals(format) ? ExportFormat.PDF : ExportFormat.DOCX;
			DbToDoc.toDoc(tables, exportFormat, outputFile.getAbsolutePath(), fontPaths);
			long duration = System.currentTimeMillis() - startTime;
			log.info("文档生成完成: {}, 耗时: {}ms", outputFile.getAbsolutePath(), duration);
		} catch (Exception e) {
			log.error("生成文档失败", e);
			throw new GeneratorException("生成文档失败: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 验证配置合法性及环境健康度
	 */
	private void validateConfig() {
		if (global == null) throw new GeneratorException("全局配置不能为空");
		
		String outputDir = global.getOutputDir();
		if (StringUtils.isBlank(outputDir)) throw new GeneratorException("输出目录不能为空");
		
		File dir = new File(outputDir);
		if (dir.exists() && !dir.canWrite()) throw new GeneratorException("输出目录无写权限: " + outputDir);

		if (global.getEngineTemplateDirPath() == null) {
			throw new GeneratorException("模板目录不存在: " + global.getTemplateDir());
		}

		if (dataSource == null) {
			throw new GeneratorException("数据源配置不能为空");
		}

		// 数据库连通性心跳检测 (关键修复：恢复被删逻辑)
		try (java.sql.Connection conn = dataSource.getCon()) {
			if (conn == null || conn.isClosed()) {
				throw new GeneratorException("数据库连接失败，请检查配置");
			}
			log.info("数据库连接正常: {}", dataSource.getDbName());
		} catch (Exception e) {
			throw new GeneratorException("数据库连接异常: " + e.getMessage(), e);
		}
	}

	/**
	 * 执行渲染逻辑
	 */
	private void generateCode() {
		List<Table> tables = getTables();
		if (tables.isEmpty()) return;
		
		templateRenderer = new TemplateRenderer();
		contextBuilder = new TemplateContextBuilder(global, dataSource, components);
		
		RenderContext globalContext = contextBuilder.buildGlobalContext();
		globalContext.put("tables", tables);
		
		// 1. 渲染工程模块 (全量加载，由 PathResolver 自动过滤 parent/vue 等)
		renderModules(globalContext, global.getComponentNames());
		
		// 2. 渲染业务组件模板
		Map<String, String> templates = getTemplates("components", true);
		log.info("已加载业务组件模板数量: {}, 表数量: {}", templates.size(), tables.size());
		
		for (Table table : tables) {
			RenderContext tableContext = contextBuilder.buildTableContext(table);
			renderComponentTemplates(tableContext, templates, table.getBeanName());
		}
		
		openDir();
	}
	
	private void renderModules(RenderContext context, String[] componentNames) {
		URL dir = global.getEngineTemplateDirPath();
		render(dir, context, getTemplates("modules", false), true);
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
			log.warn("{} 目录下没有找到模板文件", dir);
			return;
		}
		
		for (Entry<String, String> entry : templates.entrySet()) {
			String path = entry.getKey();
			String data = entry.getValue();
			try {
				renderTemplateFile(dir, context, path, data, render, name);
			} catch (Exception e) {
				log.error("处理模板失败: {}", path, e);
			}
		}
	}
		
	private void renderTemplateFile(final URL dir, RenderContext context, String path, String data, 
			boolean render, String name) {
		org.hyw.tools.generator.template.TemplateModel model = new org.hyw.tools.generator.template.TemplateModel();
		model.setConfig(global);
		model.setDataSource(dataSource);
		model.setRootPackage(global.getRootPackage());
		model.setProjectName(global.getProjectName());
		if (context.containsKey("table")) {
			model.setTable((org.hyw.tools.generator.conf.db.Table) context.get("table"));
		}
		// 设置启用的组件集合，用于 shouldSkip 方法判断
		if (global.getComponents() != null) {
			model.setComponents(new java.util.HashSet<>(java.util.Arrays.asList(global.getComponents())));
		}

		String outputPath = pathResolver.resolve(path, model);
		if (outputPath == null) return;
		
		if (StringUtils.isNotBlank(name)) {
			context.put("entityName", name);
			context.put("mapperName", name + "Mapper");
			context.put("serviceName", name + "Service");
			context.put("serviceImplName", name + "ServiceImpl");
			context.put("controllerName", name + "Controller");
		}

		// 从解析后的路径中提取实际的模块名，而不是原始模板路径中的占位符
		String moduleName = StringUtils.substringBefore(outputPath, SEPARATOR);
		// 如果 moduleName 包含路径分隔符或为空，尝试从原始路径解析
		if (StringUtils.isBlank(moduleName) || moduleName.contains(".") || moduleName.contains("-")) {
			// 尝试从全局模块配置中获取第一个模块名作为默认值
			String[] modules = global.getModules();
			if (modules != null && modules.length > 0) {
				// 检查输出路径是否包含已知模块名
				for (String m : modules) {
					if (outputPath.startsWith(m + SEPARATOR)) {
						moduleName = m;
						break;
					}
				}
				// 如果仍未找到，使用路径的第一个目录
				if (StringUtils.isBlank(moduleName) || !Arrays.asList(modules).contains(moduleName)) {
					moduleName = StringUtils.substringBefore(outputPath, SEPARATOR);
				}
			}
		}
		context.put("moduleName", moduleName);
		
		if (outputPath.endsWith(".java")) {
			String spackage = getPackageFromPath(outputPath);
			String pName = StringUtils.substringAfterLast(spackage, ".");
			context.put(pName + "Package", spackage);
			String className = StringUtils.substringBefore(StringUtils.substringAfterLast(outputPath, SEPARATOR), ".");
			context.put("className", className);
			context.put(className + "Package", spackage);
		}

		String extension = global.getEngineType().getExtension();
		boolean isTemplateFile = path.toLowerCase().endsWith(extension);
		boolean isBinaryResource = isResourceFile(path);
		
		boolean isExcluded = false;
		if (global.getExcludeDir() != null && outputPath.contains(global.getResourceDirectory())) {
			String dirInResources = StringUtils.substringBetween(
				StringUtils.substringAfter(outputPath, global.getResourceDirectory()), "/", "/");
			isExcluded = ArrayUtils.contains(global.getExcludeDir(), "/" + dirInResources);
		}
		
		String finalFileName = StringUtils.substringAfterLast(outputPath, SEPARATOR);
		if (StringUtils.isBlank(finalFileName)) finalFileName = outputPath;
		if (finalFileName.startsWith("_")) return;

		if (isBinaryResource) {
			writeBinaryFile(outputPath, Base64.getDecoder().decode(data));
		} else if (isTemplateFile && render && !isExcluded) {
			try {
				data = templateRenderer.render(data, context, global.getEngineType());
				// 渲染结果为空白内容则跳过写入（模板可输出空内容来跳过生成）
				if (StringUtils.isNotBlank(data) && StringUtils.isNotBlank(data.trim())) {
					writeToFile(outputPath, data, false);
				} else {
					log.debug("模板渲染结果为空，跳过写入: {}", outputPath);
				}
			} catch (Exception e) {
				log.error("渲染失败: {}, 错误: {}", path, e.getMessage());
			}
		} else {
			writeToFile(outputPath, data, true);
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
		if (dest.exists() && !global.isFileOverride()) return;
		try {
			org.apache.commons.io.FileUtils.writeByteArrayToFile(dest, bytes);
			log.info("生成资源文件: {}", dest);
		} catch (Exception e) {
			log.error("写入资源文件失败: {}", dest, e);
		}
	}
		
	private void writeToFile(String path, String data, boolean skipRender) {
		File dest = new File(global.getOutputDir(), path);
		if (StringUtils.isBlank(data) || (dest.exists() && !global.isFileOverride())) return;
		try {
			FileUtils.write(dest, data, global.getEncoding());
			log.info("生成文件: {}", dest);
		} catch (Exception e) {
			log.error("写入文件失败: {}", dest, e);
		}
	}

	private void prepare() {
		delDir();
		mkDirs();
		distributeAssets();
	}
	
	private void distributeAssets() {
		URL engineUrl = global.getEngineTemplateDirPath();
		if (engineUrl == null) return;
		log.info("开始分发静态资源...");
		try {
			java.net.URI engineUri = engineUrl.toURI();
			// 安全获取根目录：处理 getPath() 可能返回 null 的情况
			String enginePath = engineUri.getPath();
			java.net.URI rootUri;
			if (enginePath != null && enginePath.endsWith("/")) {
				rootUri = engineUri.resolve("..");
			} else {
				rootUri = engineUri.resolve(".");
			}
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
				// 修复：处理URI可能不是绝对路径的情况
				File templatesRootDir;
				if (rootUri.isAbsolute()) {
					templatesRootDir = new File(rootUri);
				} else {
					// 如果是相对路径，使用URL的path创建File
					templatesRootDir = new File(rootUrl.getPath());
				}
				File assetsDir = new File(templatesRootDir, "assets");
				if (assetsDir.exists() && assetsDir.isDirectory()) {
					java.util.Collection<File> files = org.apache.commons.io.FileUtils.listFiles(assetsDir, null, true);
					for (File file : files) {
						String relativePath = file.getAbsolutePath().replace(assetsDir.getAbsolutePath(), "");
						if (relativePath.startsWith(File.separator)) relativePath = relativePath.substring(1);
						processAssetFile("assets/" + relativePath, org.apache.commons.io.FileUtils.readFileToByteArray(file));
					}
					log.info("已从 {} 完成静态资源分发", assetsDir.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			log.warn("静态资源分发跳过: {}", e.getMessage());
		}
	}

	private void processAssetFile(String virtualPath, byte[] bytes) {
		if (bytes == null || bytes.length == 0) return;
		org.hyw.tools.generator.template.TemplateModel model = new org.hyw.tools.generator.template.TemplateModel();
		model.setConfig(global);
		model.setRootPackage(global.getRootPackage());
		model.setProjectName(global.getProjectName());
		String outputPath = pathResolver.resolve(virtualPath, model);
		writeBinaryFile(outputPath, bytes);
	}

	public Map<String, String> getTemplates(String subDirName, boolean buildPath, String... componentNames) {
		URL url = "components-basic".equals(subDirName) ? global.getTemplateDirPath() : global.getEngineTemplateDirPath();
		if (null == url) throw new RuntimeException(subDirName + " template dir not exist!");
		
		Map<String, String> templates = new LinkedHashMap<>();
		if ("jar".equals(url.getProtocol())) {
			String basePath = url.getPath();
			if (basePath.contains("!/")) basePath = StringUtils.substringAfter(basePath, "!/");
			if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);
			String entryName = basePath + "/" + subDirName;
			
			Map<String, String> map = FileUtils.getJarEntries(url, entryName, null, global.getResources(), componentNames);
			for (Entry<String, String> entry : map.entrySet()) {
				String path = entry.getKey().replace(entryName, "");
				if (path.startsWith(SEPARATOR)) path = path.substring(1);
				templates.put(subDirName + SEPARATOR + path, entry.getValue());
			}
		} else {
			File baseDir = new File(url.getPath(), subDirName);
			if (!baseDir.exists()) return templates;
			java.util.Collection<File> allFiles = org.apache.commons.io.FileUtils.listFiles(baseDir, null, true);
			List<String> componentList = componentNames != null ? Arrays.asList(componentNames) : new ArrayList<>();
			for (File file : allFiles) {
				String relativePath = file.getAbsolutePath().replace(baseDir.getAbsolutePath(), "");
				if (relativePath.startsWith(File.separator)) relativePath = relativePath.substring(1);
				if (!componentList.isEmpty()) {
					String firstDir = StringUtils.substringBefore(relativePath.replace("\\", "/"), "/");
					if (!componentList.contains(firstDir)) continue;
				}
				try {
					String data = isResourceFile(file.getName()) 
							? Base64.getEncoder().encodeToString(org.apache.commons.io.FileUtils.readFileToByteArray(file))
							: org.apache.commons.io.FileUtils.readFileToString(file, global.getEncoding());
					templates.put(subDirName + SEPARATOR + relativePath.replace("\\", "/"), data);
				} catch (IOException e) {
					log.error("读取失败: {}", file, e);
				}
			}
		}
		Map<String, String> resultMap = new TreeMap<>((s1, s2) -> s1.compareTo(s2));
		resultMap.putAll(templates);
		return resultMap;
	}

}
