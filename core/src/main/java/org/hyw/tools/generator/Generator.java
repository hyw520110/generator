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
import org.apache.velocity.app.VelocityEngine;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.exception.GeneratorException;
import org.hyw.tools.generator.template.EngineType;
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
	private VelocityEngine engine;
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
	}
	
	private void validateConfig() {
		if (global == null) {
			throw new GeneratorException("全局配置不能为空");
		}
		
		if (StringUtils.isBlank(global.getOutputDir())) {
			throw new GeneratorException("输出目录不能为空");
		}
		
		if (global.getEngineTemplateDirPath() == null) {
			throw new GeneratorException("模板目录不存在: " + global.getTemplateDir());
		}
		
		if (dataSource == null) {
			throw new GeneratorException("数据源配置不能为空");
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
		
		engine = getVelocityEngine();
		VelocityContext context = createVelocityContext();
		context.put("tables", tables);
		
		// 创建工程模块
		String[] componentNames = global.getComponentNames();
		renderModules(context, componentNames);
		
		// 渲染组件模板
		Map<String, String> templates = getTemplates("components", true, componentNames);
		for (Table table : tables) {
			context.put("table", table);
			renderComponentTemplates(context, templates, table.getBeanName());
		}
		
		openDir();
	}
	
	private void renderModules(VelocityContext context, String[] componentNames) {
		URL dir = global.getEngineTemplateDirPath();
		render(dir, context, getTemplates("modules", false), true);
		render(dir, context, getTemplates("modules", false, Component.VUE.name().toLowerCase()), false);
		render(dir, context, getTemplates("commons", true, componentNames), true);
	}
	
	private void renderComponentTemplates(VelocityContext context, Map<String, String> templates, String tableName) {
		URL dir = global.getEngineTemplateDirPath();
		render(dir, context, templates, true, tableName);
	}

	private void render(final URL dir, VelocityContext context, Map<String, String> templates, boolean render) {
		render(dir, context, templates, render, null);
	}

	private void render(final URL dir, VelocityContext context, Map<String, String> templates, boolean render,
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
		
	private void renderTemplateFile(final URL dir, VelocityContext context, String path, String data, 
			boolean render, String name) {
		// 只有以模板后缀（.ftl 或 .vm）结尾的文件才需要渲染，其他文件直接跳过
		boolean skipRender = !path.toLowerCase().endsWith(global.getEngineType().getExtension());
		
		if (!skipRender && render && StringUtils.isNotBlank(name)) {
			prepareTemplateContext(context, path, name);
		}
		
		if (!skipRender && render) {
			try (StringWriter writer = new StringWriter()) {
				engine.evaluate(context, writer, "", data);
				data = writer.toString();
			} catch (Exception e) {
				logger.error("模板文件渲染失败: {}, 错误: {}", path, e.getMessage());
				return;
			}
		}
		
		writeToFile(path, data, skipRender);
	}
		
	private void prepareTemplateContext(VelocityContext context, String path, String name) {
		try {
			if (StringUtils.countMatches(path, "%s") > 1) {
				path = String.format(path, StringUtils.lowercaseFirst(name), name);
			} else {
				path = String.format(path, name);
			}
		} catch (Exception e) {
			logger.error("路径格式化失败: {}, name: {}", path, name);
		}
		
		context.put("entityName", name);
		context.put("mapperName", name + "Mapper");
		context.put("serviceName", name + "Service");
		context.put("serviceImplName", name + "ServiceImpl");
		context.put("controllerName", name + "Controller");
		
		String moduleName = StringUtils.substringBefore(path, File.separator);
		context.put("moduleName", moduleName);
		
		if (path.endsWith(".java")) {
			prepareJavaContext(context, path);
		}
	}
		
	private void prepareJavaContext(VelocityContext context, String path) {
		String spackage = getPackage(path);
		String pName = StringUtils.substringAfterLast(spackage, ".");
		context.put(pName + "Package", spackage);
		
		String className = StringUtils.substringBefore(StringUtils.substringAfterLast(path, SEPARATOR), ".");
		context.put("className", className);
		context.put(className + "Package", spackage);
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
	private String getPackage(String path) {
		String spackage = (path.contains(global.getSourceDirectory())
				? StringUtils.substringAfter(path, global.getSourceDirectory())
				: StringUtils.substringAfter(path, global.getTestSourceDirectory()));
		return StringUtils.substring(spackage, 1, spackage.lastIndexOf(SEPARATOR)).replaceAll(SEPARATOR, ".");
	}

	private void prepare() {
		delDir();
		mkDirs();
	}

	public Map<String, String> getTemplates(String subDirName, boolean buildPath, String... componentNames) {
		URL url = global.getTemplateDirPath();
		if (null == url) {
			logger.error("{} template dir not exist!", subDirName);
			throw new RuntimeException(subDirName + " template dir not exist!");
		}
		Map<String, String> templates = new LinkedHashMap<String, String>();
		if ("jar".equals(url.getProtocol())) {
			String entryName = "templates/" + subDirName;
			Map<String, String> map = FileUtils.getJarEntries(url, entryName,
					(null == componentNames || componentNames.length == 0)
							? (entryName + SEPARATOR + Component.VUE.name().toLowerCase())
							: null,
					global.getResources(), componentNames);
			for (Entry<String, String> entry : map.entrySet()) {
				String path = entry.getKey().replace(entryName, "").substring(1);
				String newPath = buildPath(buildPath, path);
				// logger.debug("{}-->{}", path, newPath);
				templates.put(newPath, entry.getValue());
			}
			Map<String, String> resultMap = new TreeMap<>((str1, str2) -> str1.compareTo(str2));
			resultMap.putAll(templates);
			return resultMap;
		}
		File dir = new File(url.getPath(), subDirName);
		File[] files = null;
		if (null != componentNames && componentNames.length > 0) {
			files = FileUtils.listFiles(dir, componentNames);
		} else {
			String[] modules = global.getModules();
			List<String> list = new ArrayList<>();
			list.addAll(Arrays.asList(modules));
			if (list.size() > 1) {
				list.add("parent");
			}
			List<File> moduleFiles = (List<File>) FileUtils.listFiles(dir, new FileFileFilter() {
				@Override
				public boolean accept(File file) {
					String dirName = StringUtils.substringAfter(file.getPath().replace(dir.getPath(), ""),
							File.separator);
					dirName = format(StringUtils.substringBefore(dirName, File.separator), global.getModules());
					return list.contains(dirName);
				}
			}, FileFilterUtils.trueFileFilter());
			files = FileUtils.sort(moduleFiles.toArray(new File[] {}));
		}

		for (File file : files) {
			String path = file.getPath().replace(dir.getPath(), "").substring(1);
			try {
				path = buildPath(buildPath, path);
				String data = null;
				if (ArrayUtils.contains(global.getResources(), StringUtils.substringAfter(file.getName(), "."))) {
					data = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));
				} else {
					data = FileUtils.readFileToString(file, global.getEncoding());
				}
				templates.put(path, data);
			} catch (IOException e) {
				logger.error("read file:{}", file, e);
			}
		}
		return templates;
	}

	private String buildPath(boolean buildPath, String path) {
		path = format(path, global.getModules());
		path = isWin() ? path.replace("\\", "/") : path;
		String moduleName = StringUtils.substringBefore(path, SEPARATOR);
		boolean skipBuild = false;
		if (path.startsWith(Component.VUE.name().toLowerCase() + SEPARATOR)) {
			skipBuild = true;
		}

		if (buildPath && !skipBuild) {
			boolean isJava = path.endsWith(".java");
			String subPath = StringUtils.substringAfter(path, SEPARATOR);
			moduleName = StringUtils.substringBefore(subPath, SEPARATOR);
			subPath = StringUtils.substringAfter(subPath, moduleName + SEPARATOR);
			boolean isTest = subPath.startsWith("test");
			if (isJava) {
				path = StringUtils.toPath(moduleName,
						isTest ? global.getTestSourceDirectory() : global.getSourceDirectory(),
						global.getRootPackage().replace(".", SEPARATOR), global.getProjectName(), moduleName, subPath);
			} else {
				path = StringUtils.toPath(moduleName,
						isTest ? global.getTestResourceDirectory() : global.getResourceDirectory(),
						isTest ? StringUtils.substringAfter(subPath, SEPARATOR) : subPath);
			}
		}
		return path;
	}

	private VelocityContext createVelocityContext() {
		Map<Component, Map<String, String>> map = getComponents();
		VelocityContext context = new VelocityContext();
		Component[] components = global.getComponents();
		for (Component key : map.keySet()) {
			Boolean flag = ArrayUtils.contains(components, key);
			// 设置当前包含的组件
			context.put(key.toString(), flag);
			if (!flag) {
				continue;
			}
			// 加载组件配置
			Map<String, String> pars = map.get(key);
			for (Entry<String, String> entry : pars.entrySet()) {
				context.put(entry.getKey(), entry.getValue());
			}
		}
		context.put("dataSource", dataSource);
		context.put("author", global.getAuthor());
		context.put("encoding", global.getEncoding());
		context.put("copyright", global.getCopyright());
		context.put("description", global.getDescription());
		context.put("StringUtils", StringUtils.class);
		context.put("projectName", global.getProjectName());
		context.put("version", global.getVersion());
		context.put("javaVersion", global.getJavaVersion());
		context.put("rootPackage", global.getRootPackage());
		context.put("dbType", dataSource.getDBType().getName());
		context.put("global", global);
		context.put("modules", global.getModules());
		return context;
	}

}
