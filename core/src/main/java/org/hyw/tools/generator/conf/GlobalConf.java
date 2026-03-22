package org.hyw.tools.generator.conf;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.Naming;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.template.EngineType;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * 
 * Filename: GlobalConf.java Description: 全局配置 Copyright: Copyright (c)
 * 2015-2018 All Rights Reserved. Company: org.hyw.cn Inc.
 * 
 * @author: heyiwu
 * @version: 1.0 Create at: 2017年6月13日 上午10:03:10
 *
 */
public class GlobalConf extends BaseBean {

	private static final long serialVersionUID = -2678498334219769129L;

	private static final Logger logger = LoggerFactory.getLogger(GlobalConf.class.getName());

	/**
	 * 生成文件的输出目录,默认根目录
	 */
	private String outputDir = "/";

	/**
	 * 生成文件前是否删除输出目录
	 */
	private boolean delOutputDir = false;

	/**
	 * 是否覆盖已有文件
	 */
	private boolean fileOverride = false;

	/**
	 * 是否打开输出目录
	 */
	private boolean openDir = true;

	/**
	 * 父包名,如果为空，将下面子包名必须写全部， 否则就只需写子包名
	 */
	private String rootPackage = "com.hyw";
	/**
	 * 模块信息
	 */
	private String[] modules;

	/**
	 * 开发人员
	 */
	private String author;

	/**
	 * 版权信息
	 */
	private String copyright = "";
	/**
	 * 项目描述
	 */
	private String description;
	/**
	 * 组件配置
	 */
	private Component[] components;

	/**
	 * 需要包含的表名（与exclude二选一配置）
	 */
	private String[] include = null;

	/**
	 * 需要排除的表名
	 */
	private String[] exclude = null;
	/**
	 * 表名是否匹配模式
	 */
	private boolean matchMode=true;
	/**
	 * 表前缀
	 */
	private String[] tablePrefix;

	/**
	 * 数据库表映射到实体的命名策略
	 */
	private Naming naming = Naming.TOCAMEL;

	/**
	 * 单词分隔符,常见分隔符:下划线(_)和横线(-),转换驼峰命名时用
	 */
	private char[] separators;
	/**
	 * 是否大写命名
	 */
	private boolean isCapitalMode = false;

	/**
	 * 是否生成实体字段常量（默认 false）
	 */
	private boolean columnConstant = false;

	/**
	 * 工程构建工具
	 */
	private ProjectBuilder projectBuilder = ProjectBuilder.MAVEN;
	/**
	 * 工程默认版本号
	 */
	private String version="1.0.0";

	/**
	 * jdk版本
	 */
	private String javaVersion;

	/**
	 * 生成文件的编码
	 */
	private String encoding = "UTF-8";

	/**
	 * 是否为模拟模式（不生成文件，仅打印日志）
	 */
	private boolean dryRun = false;

	/**
	 * 模板目录
	 */
	private String templateDir = "/templates";

	/**
	 * 工程目录，默认遵循maven目录结构
	 */
	private String sourceDirectory = "src/main/java";
	private String resourceDirectory = "src/main/resources";
	private String testSourceDirectory = "src/test/java";
	private String testResourceDirectory = "src/test/resources";
	/**
	 * 非模板文件排除渲染
	 */
	private String[] resources;
	/**
	 * 非模板文件排除渲染目录
	 */
	private String[] excludeDir;
	/**
	 * 模板引擎类型
	 */
	private String templateEngine = "freemarker";
	
	public String[] getExcludeDir() {
		return excludeDir;
	}

	public void setExcludeDir(String[] excludeDir) {
		this.excludeDir = excludeDir;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(String outputDir) {
		if (StringUtils.isBlank(outputDir)) {
			throw new IllegalArgumentException("输出目录不能为空");
		}
		// 验证路径安全性，防止路径遍历攻击
		if (outputDir.contains("..") || outputDir.contains("~")) {
			throw new IllegalArgumentException("输出目录包含非法字符: " + outputDir);
		}
		this.outputDir = outputDir;
	}

	/**
	 * 获取项目名获取输出路径的子目录名
	 * 
	 * @author: heyiwu
	 * @return
	 */
	public String getProjectName() {
		File file = new File(outputDir);
		return file.getName();
	}

	public boolean isDelOutputDir() {
		return delOutputDir;
	}

	public void setDelOutputDir(boolean delOutputDir) {
		this.delOutputDir = delOutputDir;
	}

	public boolean isFileOverride() {
		return fileOverride;
	}

	public void setFileOverride(boolean fileOverride) {
		this.fileOverride = fileOverride;
	}

	public boolean isOpenDir() {
		return openDir;
	}

	public void setOpenDir(boolean openDir) {
		this.openDir = openDir;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Component[] getComponents() {
		return components;
	}
	public String[] getComponentNames() {
		Component[] components = getComponents();
		String[] names = new String[components.length];
		for (int i = 0; i < components.length;i++) {
			try {
				names[i]=components[i].name().toLowerCase();
			} catch (Exception e) {
				logger.error("components:{}",Arrays.toString(components),e);
				
			}
		}
		return names;
	}
	public void setComponents(Component[] components) {
		this.components = components;
	}

	public String[] getModules() {
		return modules;
	}

	public void setModules(String[] modules) {
		this.modules = modules;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public String getTestSourceDirectory() {
		return testSourceDirectory;
	}

	public void setTestSourceDirectory(String testSourceDirectory) {
		this.testSourceDirectory = testSourceDirectory;
	}

	public String getResourceDirectory() {
		return resourceDirectory;
	}

	public void setResourceDirectory(String resourceDirectory) {
		this.resourceDirectory = resourceDirectory;
	}

	public String getTestResourceDirectory() {
		return testResourceDirectory;
	}

	public void setTestResourceDirectory(String testResourceDirectory) {
		this.testResourceDirectory = testResourceDirectory;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public static Logger getLogger() {
		return logger;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		if (StringUtils.isBlank(encoding)) {
			throw new IllegalArgumentException("编码不能为空");
		}
		// 验证编码格式
		if (!isValidEncoding(encoding)) {
			throw new IllegalArgumentException("不支持的编码格式: " + encoding);
		}
		this.encoding = encoding;
	}
	
	private boolean isValidEncoding(String encoding) {
		try {
			"test".getBytes(encoding);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void setTemplateDir(String templateDir) {
		this.templateDir = templateDir;
	}

	public String getTemplateDir() {
		return templateDir;
	}

	public URL getTemplateDirPath() {
		URL url = GlobalConf.class.getResource(templateDir);
		if (null == url) {
			return null;
		}
		return url;
	}

	/**
	 * 获取基于模板引擎类型的模板目录路径
	 * @return 模板目录路径（包含引擎子目录）
	 */
	public URL getEngineTemplateDirPath() {
		String engineDir = templateDir + "/" + getEngineType().getName();
		URL url = GlobalConf.class.getResource(engineDir);
		if (null == url) {
			logger.warn("模板引擎目录不存在: {}，使用默认模板目录: {}", engineDir, templateDir);
			url = GlobalConf.class.getResource(templateDir);
		}
		return url;
	}

	public String getCopyright() {
		return copyright;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isCapitalMode() {
		return isCapitalMode;
	}

	public void setCapitalMode(boolean isCapitalMode) {
		this.isCapitalMode = isCapitalMode;
	}

	public Naming getNaming() {
		return naming;
	}

	public void setNaming(Naming naming) {
		this.naming = naming;
	}

	public String[] getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String[] tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public String[] getInclude() {
		return include;
	}

	public void setInclude(String include) {
		this.include = new String[] { include };
	}

	public void setInclude(String[] include) {
		this.include = include;
	}

	public String[] getExclude() {
		return exclude;
	}

	public void setExclude(String[] exclude) {
		this.exclude = exclude;
	}

	public boolean isColumnConstant() {
		return columnConstant;
	}

	public void setColumnConstant(boolean columnConstant) {
		this.columnConstant = columnConstant;
	}

	public String getRootPackage() {
		return rootPackage;
	}

	public void setRootPackage(String rootPackage) {
		this.rootPackage = rootPackage;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public boolean isCapitalModeNaming(String name) {
		return isCapitalMode && StringUtils.isCapitalMode(name);
	}

	public char[] getSeparators() {
		return separators;
	}

	public void setSeparators(char[] separators) {
		this.separators = separators;
	}

	public ProjectBuilder getProjectBuilder() {
		return projectBuilder;
	}

	public void setProjectBuilder(ProjectBuilder projectBuilder) {
		this.projectBuilder = projectBuilder;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	public String[] getResources() {
		return resources;
	}

	public void setResources(String[] resources) {
		this.resources = resources;
	}

	public String getTemplateEngine() {
		return templateEngine;
	}

	public void setTemplateEngine(String templateEngine) {
		if (StringUtils.isBlank(templateEngine)) {
			throw new IllegalArgumentException("模板引擎不能为空");
		}
		// 验证模板引擎类型
		if (!isValidTemplateEngine(templateEngine)) {
			throw new IllegalArgumentException("不支持的模板引擎类型: " + templateEngine);
		}
		this.templateEngine = templateEngine;
	}
	
	private boolean isValidTemplateEngine(String engine) {
		return "freemarker".equalsIgnoreCase(engine) || "velocity".equalsIgnoreCase(engine);
	}

	/**
	 * 获取模板引擎类型（枚举）
	 * @return 模板引擎类型
	 */
	public EngineType getEngineType() {
		return EngineType.fromName(templateEngine);
	}

	public boolean isMatchMode() {
		return matchMode;
	}

	public void setMatchMode(boolean matchMode) {
		this.matchMode = matchMode;
	}
}
