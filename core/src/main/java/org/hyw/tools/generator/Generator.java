package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.compat.CompatibilityResolver;
import org.hyw.tools.generator.compat.ResolvedPlatform;
import org.hyw.tools.generator.conf.SkipRuntimeFieldsRepresenter;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.Feature;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.exception.GeneratorException;
import org.hyw.tools.generator.export.DbToDoc;
import org.hyw.tools.generator.platform.PlatformAdapters;
import org.hyw.tools.generator.template.DefaultPathTemplateResolver;
import org.hyw.tools.generator.template.PathTemplateResolver;
import org.hyw.tools.generator.template.RenderContext;
import org.hyw.tools.generator.template.TemplateContextBuilder;
import org.hyw.tools.generator.template.TemplateModel;
import org.hyw.tools.generator.template.TemplateRenderer;
import org.hyw.tools.generator.template.TemplateResource;
import org.hyw.tools.generator.utils.FileUtils;
import org.hyw.tools.generator.utils.FontUtils;

import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;

/**
 * 代码生成器核心类
 */
@Slf4j
public class Generator extends AbstractGenerator {

	private static volatile Generator generator;
	private TemplateRenderer templateRenderer;
	private TemplateContextBuilder contextBuilder;
	private PathTemplateResolver pathResolver = new DefaultPathTemplateResolver();
	private ResolvedPlatform resolvedPlatform;
	private GenerationReport report;
	private static final String SEPARATOR = Consts.PATH_SEPARATOR;

	private static final String EXTERNAL_CONFIG_DIR = System.getProperty("user.home") + File.separator
			+ Consts.CONFIG_DIR_NAME;

	static {
		load(Consts.DEFAULT_CONFIG_FILE);
	}

	public static Generator getInstance() {
		if (generator == null) {
			synchronized (Generator.class) {
				if (generator == null) {
					generator = new Generator();
				}
			}
		}
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
		try {
			String yamlText = org.hyw.tools.generator.utils.YamlIncludeLoader.loadAndMerge(url);
			generator = new Yaml().loadAs(yamlText, Generator.class);
			if (generator != null) {
				generator.applyCompatibility();
			}
			log.info("加载内置配置文件成功: {}", url);
		} catch (Exception e) {
			log.warn("加载内置配置文件失败: {}", url);
		}
	}

	public static Generator loadFrom(File configFile) {
		if (configFile == null || !configFile.exists() || configFile.length() == 0) {
			return null;
		}
		try {
			String yamlText = org.hyw.tools.generator.utils.YamlIncludeLoader.loadAndMerge(configFile);
			Generator loaded = new Yaml().loadAs(yamlText, Generator.class);
			if (loaded != null) {
				loaded.applyCompatibility();
			}
			return loaded;
		} catch (Exception e) {
			log.warn("加载配置文件失败: {}", configFile.getAbsolutePath(), e);
			return null;
		}
	}

	public Generator copy() {
		try {
			Yaml yaml = createConfigYaml();
			java.io.StringWriter stringWriter = new java.io.StringWriter();
			yaml.dump(this, stringWriter);
			String yamlContent = removeTypeTagsAndNulls(stringWriter.toString());
			Generator copied = new Yaml().loadAs(yamlContent, Generator.class);
			if (copied != null) {
				copied.applyCompatibility();
				return copied;
			}
			return new Generator();
		} catch (Exception e) {
			log.warn("复制生成器配置失败，使用空配置兜底", e);
			return new Generator();
		}
	}

	public void execute() {
		log.info("开始执行代码生成，输出目录: {}", global != null ? global.getOutputDir() : "");
		long startTime = System.currentTimeMillis();
		this.report = new GenerationReport();
		try {
			applyCompatibility();
			validateConfig();
			generateAtomically();
			long elapsed = System.currentTimeMillis() - startTime;
			logSummary(elapsed);
			log.info("代码生成完成，耗时: {}ms", elapsed);
		} catch (Exception e) {
			log.error("代码生成失败", e);
			throw new GeneratorException("代码生成失败", e);
		}
	}

	/**
	 * 输出生成结果摘要：成功/跳过文件数 + 失败列表（带原因）。
	 * 失败时只警告，不阻断流程——避免百表场景下因个别模板失误丢失全部产物。
	 */
	private void logSummary(long elapsedMs) {
		if (report == null) {
			return;
		}
		int tables = report.processedTables();
		int success = report.successFiles();
		int skipped = report.skippedFiles();
		int fail = report.failures().size();
		log.info("生成摘要：表 {} 个 | 文件 成功 {} / 跳过 {} / 失败 {} | 耗时 {}ms",
				tables, success, skipped, fail, elapsedMs);
		if (report.hasFailures()) {
			log.warn("生成失败明细（共 {} 项）：", fail);
			int idx = 0;
			for (GenerationReport.Failure f : report.failures()) {
				log.warn("  [{}] table={}, path={}, reason={}", ++idx, f.tableName(), f.templatePath(), f.reason());
			}
		}
	}

	/**
	 * 仅供测试和外部诊断使用：返回最近一次 execute 的累加结果。
	 */
	public GenerationReport getReport() {
		return report;
	}

	/**
	 * 原子性生成：先写到 .tmp 临时目录，全部成功后再替换正式目录。
	 * 异常时清理临时目录，正式目录保持不变；保证生成失败不会污染已有产物。
	 */
	private void generateAtomically() throws java.io.IOException {
		String finalDir = global.getOutputDir();
		File finalFile = new File(finalDir);
		File parent = finalFile.getAbsoluteFile().getParentFile();
		String tmpName = finalFile.getName() + ".tmp." + System.currentTimeMillis();
		File tmpDir = new File(parent, tmpName);

		boolean originalDelOutputDir = global.isDelOutputDir();
		try {
			// 重定向到临时目录，强制清空并重建（即使用户配置了 delOutputDir=false）
			global.setOutputDir(tmpDir.getAbsolutePath());
			global.setDelOutputDir(true);
			prepare();
			generateCode();
			if (report != null && report.hasFailures()) {
				throw new GeneratorException("存在 " + report.failures().size() + " 个文件生成失败，未替换正式输出目录");
			}

			// dry-run：保留 tmp，不替换正式目录，便于用户 diff 预览
			if (global.isDryRun()) {
				log.warn("dry-run 模式：产物保留在临时目录，未覆盖正式输出: {}", tmpDir.getAbsolutePath());
				return;
			}

			// 原子替换：移走旧目录 → 临时目录改名为正式目录
			File backup = null;
			if (finalFile.exists()) {
				backup = new File(parent, finalFile.getName() + ".bak." + System.currentTimeMillis());
				if (!finalFile.renameTo(backup)) {
					throw new java.io.IOException("无法重命名已存在的输出目录: " + finalFile);
				}
			}
			try {
				java.nio.file.Files.move(tmpDir.toPath(), finalFile.toPath(),
						java.nio.file.StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				// 跨文件系统时回退到非原子 move
				java.nio.file.Files.move(tmpDir.toPath(), finalFile.toPath());
			}
			if (backup != null) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(backup);
				} catch (Exception ex) {
					log.warn("清理备份目录失败: {}", backup, ex);
				}
			}
		} catch (Exception e) {
			// 清理临时目录，正式目录保持不变
			if (tmpDir.exists()) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(tmpDir);
				} catch (Exception ex) {
					log.warn("清理临时目录失败: {}", tmpDir, ex);
				}
			}
			throw e instanceof java.io.IOException ? (java.io.IOException) e : new java.io.IOException(e);
		} finally {
			global.setOutputDir(finalDir);
			global.setDelOutputDir(originalDelOutputDir);
		}
		if (!global.isDryRun()) {
			openDir();
		}
	}

	public ResolvedPlatform applyCompatibility() {
		log.debug("[applyCompatibility] global={}, components={}, versionOverrides={}", global == null ? "null" : "non-null", components == null ? "null" : "non-null", versionOverrides == null ? "null" : versionOverrides.size() + " entries");
		if (global == null || components == null) {
			log.warn("[applyCompatibility] SKIP - global={}, components={}", global == null, components == null);
			return null;
		}
		org.hyw.tools.generator.utils.ConfigValidator.normalizeSecuritySelection(global);
		resolvedPlatform = new CompatibilityResolver().apply(global, components, versionOverrides);
		return resolvedPlatform;
	}

	public ResolvedPlatform getResolvedPlatform() {
		if (resolvedPlatform == null) {
			return applyCompatibility();
		}
		return resolvedPlatform;
	}

	protected void validateConfig() {
		if (global == null)
			throw new GeneratorException(Consts.ERR_GLOBAL_CONFIG_NULL);
		try {
			org.hyw.tools.generator.utils.ConfigValidator.validate(global);
		} catch (org.hyw.tools.generator.exception.ConfigurationException e) {
			throw new GeneratorException(e.getMessage(), e);
		}
		String outputDir = global.getOutputDir();
		if (StringUtils.isBlank(outputDir))
			throw new GeneratorException(Consts.ERR_OUTPUT_DIR_EMPTY);

		File dir = new File(outputDir);
		if (!dir.exists() && !dir.mkdirs())
			throw new GeneratorException(Consts.ERR_CREATE_OUTPUT_DIR + outputDir);

		// 启动环境预检：JDK 版本、字符集、输出目录写权限、目标 JDK 合法性
		try {
			List<String> warnings = org.hyw.tools.generator.utils.EnvChecker.check(outputDir,
					global.getJavaVersion());
			for (String w : warnings) {
				log.warn("环境预检告警: {}", w);
			}
		} catch (IllegalStateException e) {
			throw new GeneratorException(e.getMessage(), e);
		}

		if (dataSource == null)
			throw new GeneratorException(Consts.ERR_DATASOURCE_NULL);
		if (!"SQL_FILE".equalsIgnoreCase(dataSource.getSourceType())) {
			try (java.sql.Connection conn = dataSource.getCon()) {
				if (conn == null || conn.isClosed())
					throw new GeneratorException(Consts.ERR_DB_CONNECTION_FAILED);
				log.info("数据库连接正常: {}", dataSource.getDbName());
			} catch (Exception e) {
				throw new GeneratorException(Consts.ERR_DB_CONNECTION_ERROR + e.getMessage(), e);
			}
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
			List<TemplateResource> componentResources = new java.util.ArrayList<>(
					scanFilteredResources(engineUrl, Consts.DIR_COMPONENTS));

			// 历史模板中少量按表生成的测试文件位于 modules 目录，通过路径占位符识别并转入逐表渲染。
			List<TemplateResource> globalModuleResources = new java.util.ArrayList<>();
			for (TemplateResource resource : moduleResources) {
				if (resource.getPath().contains("${beanName}") || resource.getPath().contains("${table.beanName}")) {
					componentResources.add(resource);
				} else {
					globalModuleResources.add(resource);
				}
			}
			moduleResources = globalModuleResources;

			// 业务级别 (Components): 排序并针对每张表渲染
			log.info("开始处理业务组件模板...");
			componentResources = filterComponentResources(componentResources);

			// 排序：先按模块排序，再按组件排序（确保依赖顺序正确）如：api 模块的 entity > service > vo，然后才是 app 模块的
			// controller
			componentResources = sort(componentResources);
			log.info("待处理业务组件模板: {}, 表数量: {}, 排序策略：先模块后组件", componentResources.size(), tables.size());
			// 模块级别 (Modules): 在组件渲染后处理，此时包名变量已就绪
			log.info("开始处理模块模板 ...");
			moduleResources = sort(moduleResources);

			java.util.stream.Stream<Table> tableStream = global.isParallelTables()
					? tables.parallelStream() : tables.stream();
			final List<TemplateResource> finalComponentResources = componentResources;
			final int totalTables = tables.size();
			tableStream.forEach(table -> {
				long tStart = System.currentTimeMillis();
				// 每张表使用独立 ctx 副本，避免并行渲染时共享变量被覆盖
				RenderContext tableContext = globalContext.snapshot();
				tableContext.put(Consts.CTX_TABLE, table);
				renderResources(tableContext, finalComponentResources, true);
				int idx = report == null ? 0 : report.incrementTableCounter();
				log.info("[{}/{}] {} → 渲染完成 ({}ms)", idx, totalTables, table.getName(),
						System.currentTimeMillis() - tStart);
			});
			renderResources(globalContext, moduleResources, true);
		}
	}

	/**
	 * 模板排序：先按模块排序，再按组件排序，最后按文件路径和文件名排序
	 * 
	 * 排序策略： 1. 第一级：模块排序（api > app），确保基础模块先渲染 2. 第二级：组件排序（entity > service >
	 * controller），确保依赖顺序正确 3. 第三级：文件路径排序（按路径字符串） 4. 第四级：文件名排序（在路径相等时）
	 */
	private List<TemplateResource> sort(List<TemplateResource> resources) {
		Stream<TemplateResource> sorted = resources.stream().sorted((r1, r2) -> {
			String module1 = inferModuleNameFromPath(r1.getPath());
			String module2 = inferModuleNameFromPath(r2.getPath());

			// 获取模块在 modules 数组中的索引（越小越先）
			int moduleIndex1 = getModuleIndex(module1);
			int moduleIndex2 = getModuleIndex(module2);

			if (moduleIndex1 != moduleIndex2) {
				return Integer.compare(moduleIndex1, moduleIndex2);
			}
			// 同一模块内，按组件 ordinal 排序
			int componentOrdinal1 = getComponentOrdinal(r1.getPath());
			int componentOrdinal2 = getComponentOrdinal(r2.getPath());

			if (componentOrdinal1 != componentOrdinal2) {
				return Integer.compare(componentOrdinal1, componentOrdinal2);
			}
			// 文件类型优先级排序（确保 .java 在 .xml 之前渲染，以便先注册包名变量）
			int typePriority1 = getFileTypePriority(r1.getPath());
			int typePriority2 = getFileTypePriority(r2.getPath());
			if (typePriority1 != typePriority2) {
				return Integer.compare(typePriority1, typePriority2);
			}
			// 组件和模块都相同，按路径字符串排序
			int pathCompare = r1.getPath().compareTo(r2.getPath());
			if (pathCompare != 0) {
				return pathCompare;
			}
			// 路径相等时，按文件名排序
			String fileName1 = StringUtils.substringAfterLast(r1.getPath(), SEPARATOR);
			String fileName2 = StringUtils.substringAfterLast(r2.getPath(), SEPARATOR);
			return fileName1.compareTo(fileName2);
		});
		return PlatformAdapters.current().toList(sorted);
	}

	/**
	 * 获取文件类型优先级（数值越小优先级越高） 确保 Java 源码先于资源文件渲染，以便包名变量先被注册
	 * 
	 * @param path 文件路径
	 * @return 优先级数值（1=Java源码, 2=XML资源, 3=其他）
	 */
	private int getFileTypePriority(String path) {
		if (path == null)
			return 3;
		String lower = path.toLowerCase();
		// Java 源码优先级最高
		if (lower.contains(".java")) {
			return 1;
		}
		// XML 资源文件次之
		if (lower.contains(".xml")) {
			return 2;
		}
		return 3;
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
	 * 
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
			Stream<TemplateResource> filtered = resources.stream().filter(res -> {
				String rel = res.getPath().substring(Consts.DIR_COMPONENTS.length() + Consts.PATH_SEPARATOR.length());
				String first = StringUtils.substringBefore(rel, SEPARATOR);

				// 模块占位符格式：{0}, {1}
				if (first.startsWith(Consts.PATH_PLACEHOLDER_START)) {
					return true;
				}

				// 组件标记格式：#vue#, #mybatis# - 提取别名检查组件是否启用
				if (first.startsWith("#") && first.endsWith("#")) {
					String alias = first.substring(1, first.length() - 1);
					Component c = Component.getComponent(alias);
					if (c != null) return isComponentEnabled(c);
					Feature f = Feature.getFeature(alias);
					if (f != null) return isFeatureEnabled(f);
					return false;
				}
				// 3. 普通组件名
				return list.contains(first);
			});
			return PlatformAdapters.current().toList(filtered);
		}
		return resources;
	}

	private void prepare() {
		delDir();
		mkDirs();
	}

	private List<TemplateResource> scanFilteredResources(URL url, String subDir) {
		List<TemplateResource> resources = FileUtils.getTemplateResources(url, subDir, global.getResources());
		return PlatformAdapters.current().toList(resources.stream().filter(res -> !shouldSkipByComponent(res.getPath())));
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

		if (context.containsKey(Consts.CTX_TABLE)) {
			Table table = (Table) context.get(Consts.CTX_TABLE);
			model.setTable(table);

		}

		String outputPath;
		try {
			outputPath = pathResolver.resolve(normalizedPath, model);
		} catch (Exception e) {
			log.error("解析模板输出路径失败: {}", normalizedPath, e);
			recordFailure(null, normalizedPath,
					"输出路径解析失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
			return;
		}

		if (outputPath == null)
			return;
		if (outputPath.contains(Consts.PATH_PLACEHOLDER_START)) {
			String reason = "输出路径仍包含未解析占位符: " + outputPath;
			log.error("{}，模板: {}", reason, normalizedPath);
			recordFailure(null, normalizedPath, reason);
			return;
		}

		String finalModuleName = inferFinalModuleName(outputPath);
		model.setModuleName(finalModuleName);
		context.put(Consts.CTX_MODULE_NAME, finalModuleName);

		// 向全局 context 注册包名变量
		registerPackageVariablesToContext(context, normalizedPath, outputPath, finalModuleName);

		// 设置命名变量（entityName, entityNameLower, className 等）
		if (context.containsKey(Consts.CTX_TABLE)) {
			Table table = (Table) context.get(Consts.CTX_TABLE);
			String beanName = table.getBeanName();
			// 设置命名变量
			context.put(Consts.CTX_ENTITY_NAME, beanName);
			context.put("entityNameLower", table.getLowercaseBeanName());
			context.put(Consts.CTX_CLASS_NAME, beanName);
		}

		File dest = new File(global.getOutputDir(), FileUtils.normalizePath(outputPath));
		if (dest.exists() && !global.isFileOverride())
			return;

		String tableName = null;
		if (context.containsKey(Consts.CTX_TABLE)) {
			Table t = (Table) context.get(Consts.CTX_TABLE);
			if (t != null) {
				tableName = t.getName();
			}
		}

		try {
			if (resource.isBinary()) {
				distributeBinary(resource, dest);
				recordSuccess();
			} else if (render && isTemplateFile(normalizedPath)) {
				log.debug("渲染模板: {} -> {} ", normalizedPath, outputPath);
				String data = templateRenderer.render(resource.getContent(), context, global.getEngineType());
				if (StringUtils.isNotBlank(data) && StringUtils.isNotBlank(data.trim())) {
					FileUtils.write(dest, data, global.getEncoding());
					log.info("生成文件: {}, 大小: {} bytes", dest.getPath(), dest.length());
					recordSuccess();
				} else {
					log.warn("模板渲染结果为空: {}", normalizedPath);
					recordSkipped();
				}
			} else {
				distributeBinary(resource, dest);
				recordSuccess();
			}
		} catch (Exception e) {
			log.error("生成文件失败: {}", dest.getPath(), e);
			recordFailure(tableName, normalizedPath, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	private void recordSuccess() {
		if (report != null) report.recordSuccess();
	}

	private void recordSkipped() {
		if (report != null) report.recordSkipped();
	}

	private void recordFailure(String tableName, String templatePath, String reason) {
		if (report != null) report.recordFailure(tableName, templatePath, reason);
	}

	private void distributeBinary(TemplateResource resource, File dest) throws IOException {
		try (InputStream is = resource.openStream()) {
			if(!dest.exists()||global.isFileOverride()) {
				log.debug("分发文件: {}", dest.getPath());
				FileUtils.copyInputStreamToFile(is, dest);
				applyOutputPermissions(dest);
			}
		}
	}

	static void applyOutputPermissions(File dest) throws IOException {
		if (dest == null || isWin() || !dest.getName().endsWith(".sh")) {
			return;
		}
		if (!dest.setExecutable(true, false)) {
			throw new IOException("无法设置脚本执行权限: " + dest.getPath());
		}
	}

	private boolean isTemplateFile(String path) {
		return path.toLowerCase().endsWith(global.getEngineType().getExtension());
	}

	private String inferFinalModuleName(String outputPath) {
		String moduleName = StringUtils.substringBefore(outputPath, SEPARATOR);
		if (StringUtils.isBlank(moduleName) || moduleName.contains(Consts.FILE_EXTENSION_SEPARATOR)
				|| moduleName.contains(Consts.PATH_WINDOWS_SEPARATOR)) {
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
	 * @param context      全局 context
	 * @param templatePath 模板路径
	 * @param outputPath   输出路径
	 * @param moduleName   模块名
	 */
	private void registerPackageVariablesToContext(RenderContext context, String templatePath, String outputPath,
			String moduleName) {
		if (outputPath == null || !outputPath.endsWith(".java")) {
			return;
		}

		// 提取包名
		String javaDirMark = Consts.DIR_JAVA + Consts.PATH_SEPARATOR;
		int index = outputPath.indexOf(javaDirMark);
		if (index == -1)
			return;

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

		// 根据父目录名注册包名变量
		String parentDir = org.apache.commons.lang3.StringUtils.substringAfterLast(
				org.apache.commons.lang3.StringUtils.substringBeforeLast(outputPath, Consts.PATH_SEPARATOR),
				Consts.PATH_SEPARATOR);
		if (org.apache.commons.lang3.StringUtils.isNotBlank(parentDir)) {
			String varName = parentDir + "Package";
			context.put(varName, fullPackage);
			context.put("packagePath", fullPackage);
			log.debug("注册包名变量: {} = {}", varName, fullPackage);
			// 同时注册模块前缀的变量
			if (org.apache.commons.lang3.StringUtils.isNotBlank(moduleName)) {
				context.put(moduleName + "_" + varName, fullPackage);
				log.debug("注册模块包名变量: {} = {}", moduleName + "_" + varName, fullPackage);
			}
		}

		// 根据文件名注册包名变量
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
				Feature f = Feature.getFeature(alias);
				if (f != null && !isFeatureEnabled(f)) {
					return true;
				}
			}
		}
		return false;
	}

	
	private boolean isFeatureEnabled(Feature f) {
		if (global.getFeatures() == null)
			return false;
		for (Feature enabled : global.getFeatures()) {
			if (enabled == f) {
				return true;
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
			if (global.getModules() != null && global.getModules().length == 1) {
				return global.getModules()[0];
			}
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
			save(configFile);
		} catch (Exception e) {
			log.error("持久化配置失败", e);
		}
	}

	public synchronized void save(File configFile) {
		save(configFile, true);
	}

	public synchronized void save(File configFile, boolean persistPassword) {
		try {
			File parent = configFile.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			Yaml yaml = createConfigYaml();
			java.io.StringWriter stringWriter = new java.io.StringWriter();
			yaml.dump(this, stringWriter);
			String yamlContent = removeTypeTagsAndNulls(stringWriter.toString());
			if (!persistPassword) {
				yamlContent = removePasswordFields(yamlContent);
			}

			org.apache.commons.io.FileUtils.writeStringToFile(configFile, yamlContent, Consts.DEFAULT_ENCODING);
			log.info("配置已持久化至: {}", configFile.getAbsolutePath());
		} catch (Exception e) {
			log.error("持久化配置失败", e);
		}
	}

	private static Yaml createConfigYaml() {
		org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
		options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		return new Yaml(new SkipRuntimeFieldsRepresenter(options), options);
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

	private static String removePasswordFields(String yamlContent) {
		String[] lines = yamlContent.split("\n");
		StringBuilder result = new StringBuilder();
		for (String line : lines) {
			if (isSensitiveYamlLine(line)) {
				continue;
			}
			result.append(line).append("\n");
		}
		return result.toString();
	}

	private static boolean isSensitiveYamlLine(String line) {
		String trimmed = line.trim();
		if (trimmed.startsWith("- ")) {
			trimmed = trimmed.substring(2).trim();
		}
		int colonIndex = trimmed.indexOf(':');
		if (colonIndex <= 0) {
			return false;
		}
		String key = trimmed.substring(0, colonIndex).toLowerCase();
		return key.contains("password") || key.contains("pwd") || key.contains("secret") || key.contains("token");
	}
}
