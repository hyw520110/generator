package org.hyw.tools.generator;

import java.io.File;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Description: 代码生成器 Copyright: Copyright (c) 2017-2020 All Rights Reserved.
 * Company: org.hyw.cn Inc.
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
	private VelocityEngine engine;

	private Generator() {
	}

	public static void main(String[] args) throws Exception {
		Generator generator = getInstance();
		long s = System.currentTimeMillis();
		generator.execute(false);
		logger.info("generation completed:{}ms", System.currentTimeMillis() - s);
	}

	public static Generator getInstance() {
		return getInstance(conf);
	}

	/**
	 * 生成代码
	 * 
	 * @throws Exception
	 */
	public void execute(boolean dbMeta) throws Exception {
		prepare();
		final File dir = global.getTemplateFile();
		if (dir == null || !dir.exists()) {
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
		String encoding = global.getEncoding();
		if (dbMeta) {
			writer(context, encoding, new File(global.getOutputDir(), global.getResource() + "/static/index.html"),
					new File(global.getTemplateFile(), "dbMeta.html"),false);
			return;
		}
		createScriptFile(engine, context);
		String projectName = global.getProjectName();
		projectName = StringUtils.contains(projectName, "-") ? StringUtils.substringAfter(projectName, "-")
				: projectName;

		// 根据组件配置过滤所需的模板文件
		File[] templates = getTemplateFiles();
		if (null == templates || templates.length == 0) {
			logger.error("{}目录下没有所需的模板文件！", dir);
			return;
		}
		for (Table table : tables) {
			for (File file : templates) {
				context.put("date", getDate());
				context.put("table", table);
				// 模板文件名
				String name = String.format(file.getName(), table.getBeanName());
				// 模板文件名前缀
				String prefix = StringUtils.substringBeforeLast(name, ".");
				boolean isJava = name.endsWith(".java");
				// 子路径（以组件开头）
				String subPath = file.getParent().replace(dir.getPath(), "").substring(1);
				String subDir = getSubDir(subPath, table);
				if (isJava) {
					String pName = file.getParentFile().getName();
					if (!StringUtils.startsWith(name, "Base")) {
						// 类名
						context.put("className", prefix);
						context.put(pName + "Name", prefix);
					}
					// 类的包名
					context.put(pName + "Package",
							StringUtils.toPackage(global.getRootPackage(), projectName, StringUtils.toPackage(subDir)));
				}
				String moduleName = (null == global.getModules()) ? ""
						: StringUtils.substringBefore(subDir, File.separator);

				context.put("moduleName", moduleName);
				File f = new File(getDir(projectName, isJava, subDir, moduleName, subPath), name);
				writer(context, encoding, f, file,isVue(subPath));
			}
		}
		openDir();
	}

	private void prepare() {
		delDir();
		mkDirs();
	}

	public void createScriptFile(VelocityEngine engine, VelocityContext context) {
		ProjectBuilder builder = global.getProjectBuilder();
		if (null == builder) {
			return;
		}
		File dir = global.getTemplateFile();
		String encoding = global.getEncoding();
		context.put("version", global.getVersion());
		if (null == global.getModules()) {
			return;
		}
		Collection<File> modules = FileUtils.listFiles(new File(dir, "modules"), FileFilterUtils.fileFileFilter(),
				FileFilterUtils.trueFileFilter());
		context.put("modules", global.getModules());
		for (File file : modules) {
			String tPath = file.getPath().replace(dir.getParent(), "");
			String path = MessageFormat.format(StringUtils.substringAfter(tPath, "modules"), global.getModules());
			context.put("moduleName", StringUtils.substring(path, 1, StringUtils.indexOf(path, File.separator, 1)));
			writer(context, encoding, new File(global.getOutputDir(), path), file,false);
		}
	}

	private String getDir(String projectName, boolean isJava, String subDir, String moduleName, String subPath) {
		if (isVue(subPath)) {
			return StringUtils.toPath(global.getOutputDir(), "vue", subDir).toLowerCase();
		}
		boolean isTest = StringUtils.startsWith(subDir, "".equals(moduleName) ? "test"
				: moduleName + (System.getProperty("os.name").startsWith("Win") ? "\\" : "/") + "test");
		if (isJava) {
			return StringUtils.toPath(global.getOutputDir(), moduleName,
					isTest ? global.getTestSourceDirectory() : global.getSourceDirectory(),
					global.getRootPackage().replace(".", File.separator) + File.separator + projectName, subDir);
		}
		String s = StringUtils.substringAfter(subDir, moduleName);
		return StringUtils.toPath(global.getOutputDir(), moduleName,
				isTest ? global.getTestResource() : global.getResource(),
				isTest ? StringUtils.substringAfter(s, "test") : s);
	}

	private boolean isVue(String subPath) {
		return subPath.startsWith("vue/");
	}

	private String getSubDir(String subPath,Table table) {
		// 子目录(去除组件路径)
		subPath = StringUtils.substringAfter(subPath, File.separator);
		if (null != global.getModules()) {
			subPath = MessageFormat.format(subPath, global.getModules());
		} else {
			subPath = StringUtils.substringAfter(subPath, File.separator);
		}
		return String.format(subPath, table.getBeanName());
	}

	private File[] getTemplateFiles() {
		final File dir = global.getTemplateFile();
		final Component[] components = global.getComponents();
		File[] files = FileUtils.listFiles(dir, new DirectoryFileFilter() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean accept(File file) {
				String path = file.getPath().replace(dir.getAbsolutePath(), "").substring(1);
				int index = StringUtils.indexOf(path, File.separator);
				if (index != -1) {
					path = StringUtils.substring(path, 0, index);
				}
				return ArrayUtils.contains(components, Component.getComonent(path));
			}
		}, FileFilterUtils.trueFileFilter()).toArray(new File[] {});
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && o2.isFile())
					return 1;
				if (o1.isFile() && o2.isDirectory())
					return -1;
				int value = o1.getParent().compareToIgnoreCase(o2.getParent());
				return value == 0 ? o1.getName().compareToIgnoreCase(o2.getName()) : value;
			}
		});
		return files;
	}

	private void writer(VelocityContext context, String encoding, File f, File src, boolean isVue) {
		try {
			if (f.exists() && (!global.isFileOverride())) {
				return;
			}
			// 静态文件
			if (!isVue&&ArrayUtils.contains(global.getResources(), StringUtils.substringAfterLast(f.getName(), "."))) {
				File destDir = f.getParentFile();
				if (!destDir.exists()) {
					File srcDir = src.getParentFile();
					logger.info("copy dir:{}->{}", srcDir, destDir);
					FileUtils.copyDirectory(srcDir, destDir);
					return;
				}
				if (!f.exists()) {
					logger.info("copy file:{}", f);
					FileUtils.copyFile(src, f);
				}
				return;
			}
			File dir = global.getTemplateFile();
			// 获取模板对象
			Template t = engine.getTemplate(src.getPath().replace(dir.getParent(), ""), encoding);
			StringWriter writer = new StringWriter();
			t.merge(context, writer);
			// 模板渲染为空则不生成文件
			if (StringUtils.isBlank(writer.toString())) {
				return;
			}
			logger.info("generator file:{}", f);
			mkdir(f);
			FileUtils.write(f, writer.toString(), encoding);
		} catch (Exception e) {
			logger.error("write file:{} {}", f, e.getClass(), e);
		}
	}

	private void mkdir(File f) {
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
	}

	private VelocityContext createVelocityContext() {
		Map<Component, Map<String, String>> map = getComponents();
		VelocityContext context = new VelocityContext();
		Component[] components = global.getComponents();
		for (Component key : map.keySet()) {
			Boolean flag = ArrayUtils.contains(components, key);
			context.put(key.toString(), flag);
			if (!flag) {
				continue;
			}
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
		context.put("javaVersion", global.getJavaVersion());
		context.put("rootPackage", global.getRootPackage());
		context.put("dbType", dataSource.getDBType().getValue());
		context.put("global", global);
		return context;
	}

}
