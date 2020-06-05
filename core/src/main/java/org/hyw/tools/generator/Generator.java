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
	public void execute() throws Exception {
		prepare();
		URL dir = global.getTemplateDirPath();
		if (dir == null) {
			logger.error("模板目录：{}不存在!", dir);
			return;
		}
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
		render(dir, context, getTemplates("modules", false), true);
		render(dir, context, getTemplates("modules", false, Component.VUE.name().toLowerCase()), false);
		// 根据组件配置过滤所需的模板文件
		render(dir, context, getTemplates("commons", true, componentNames), true);
		Map<String, String> templates = getTemplates("components", true, componentNames);
		for (Table table : tables) {
			context.put("table", table);
			render(dir, context, templates, true, table.getBeanName());
		}
		openDir();
	}

	private void render(final URL dir, VelocityContext context, Map<String, String> templates, boolean render)
			throws IOException {
		render(dir, context, templates, render, null);
	}

	private void render(final URL dir, VelocityContext context, Map<String, String> templates, boolean render,
			String name) throws IOException {
		if (null == templates || templates.isEmpty()) {
			logger.error("{}目录下没有所需的模板文件！", dir);
			return;
		}
		for (Entry<String, String> entry : templates.entrySet()) {
			String path = entry.getKey();
			String data = entry.getValue();
			// 判断文件是否渲染
			boolean skipRender = ArrayUtils.contains(global.getResources(),
					StringUtils.lowerCase(StringUtils.substringAfterLast(path, ".")));
			if (!skipRender) {
				if (render) {
					if (StringUtils.isNotBlank(name)) {
						try {
							path = String.format(path,
									StringUtils.countMatches(path, "%s") > 1 ? StringUtils.lowercaseFirst(name) : name,
									name);
						} catch (Exception e) {
							logger.error("path:{},name:{}", path, name);
						}
						context.put("entityName", name);
						context.put("mapperName", name + "Mapper");
						context.put("serviceName", name + "Service");
						context.put("serviceImplName", name + "ServiceImpl");
						context.put("controllerName", name + "Controller");
					}
					String moduleName = StringUtils.substringBefore(path, File.separator);
					if (path.endsWith(".java")) {
						String spackage = getPackage(path);
						String pName = StringUtils.substringAfterLast(spackage, ".");
						context.put(pName + "Package", spackage);
					}
					context.put("moduleName", moduleName);
					context.put("className",
							StringUtils.substringBefore(StringUtils.substringAfterLast(path, File.separator), "."));
					// 渲染文件
					StringWriter writer = new StringWriter();
					try {
						engine.evaluate(context, writer, "", data);
						data = writer.toString();
					} catch (Exception e) {
						logger.error("template file:{} render {}:{}", path, e.getClass(), e.getLocalizedMessage());
						continue;
					}
				}
			}
			File dest = new File(global.getOutputDir(), path);
			if ((!skipRender && StringUtils.isBlank(data)) || (dest.exists() && !global.isFileOverride())) {
				continue;
			}
			if (dest.exists()) {
				logger.warn("override:{}", dest);
			} else {
				logger.info("generator file:{}", dest);
			}
			if (skipRender) {
				FileUtils.writeByteArrayToFile(dest, Base64.getDecoder().decode(data));
				continue;
			}
			FileUtils.write(dest, data);
		}
	}

	private String getPackage(String path) {
		String spackage = (path.contains(global.getSourceDirectory())
				? StringUtils.substringAfter(path, global.getSourceDirectory())
				: StringUtils.substringAfter(path, global.getTestSourceDirectory()));
		return StringUtils.substring(spackage, 1, spackage.lastIndexOf(File.separator)).replaceAll(File.separator, ".");
	}

	private void prepare() {
		delDir();
		mkDirs();
	}

	public Map<String, String> getTemplates(String subDirName, boolean buildPath, String... componentNames) {
		URL url = global.getTemplateDirPath();
		if (null == url) {
			throw new RuntimeException(subDirName + " template dir not exist!");
		}
		Map<String, String> templates = new LinkedHashMap<String, String>();
		if ("jar".equals(url.getProtocol())) {
			String entryName = "templates/" + subDirName;
			Map<String, String> map = FileUtils.getJarEntries(url, entryName,
					(null == componentNames || componentNames.length == 0)
							? (entryName + "/" + Component.VUE.name().toLowerCase())
							: null,
					global.getResources(), componentNames);
			for (Entry<String, String> entry : map.entrySet()) {
				String path = entry.getKey().replace(entryName, "").substring(1);
				templates.put(buildPath(buildPath, path), entry.getValue());
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
		String moduleName = StringUtils.substringBefore(path, File.separator);
		boolean skipBuild = false;
		if (path.startsWith(Component.VUE.name().toLowerCase() + File.separator)) {
			skipBuild = true;
		}

		if (buildPath && !skipBuild) {
			boolean isJava = path.endsWith(".java");
			String subPath = StringUtils.substringAfter(path, File.separator);
			moduleName = StringUtils.substringBefore(subPath, File.separator);
			subPath = StringUtils.substringAfter(subPath, moduleName + File.separator);
			boolean isTest = subPath.startsWith("test");
			if (isJava) {
				path = StringUtils.toPath(moduleName,
						isTest ? global.getTestSourceDirectory() : global.getSourceDirectory(),
						global.getRootPackage().replace(".", File.separator), global.getProjectName(), moduleName,
						subPath);
			} else {
				path = StringUtils.toPath(moduleName,
						isTest ? global.getTestResourceDirectory() : global.getResourceDirectory(),
						isTest ? StringUtils.substringAfter(subPath, File.separator) : subPath);
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
			//设置当前包含的组件
			context.put(key.toString(), flag);
			if (!flag) {
				continue;
			}
			//加载组件配置
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
