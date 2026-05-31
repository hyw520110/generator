package org.hyw.tools.generator.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.compat.CompatibilityMatrix;
import org.hyw.tools.generator.compat.CompatibilityProfile;
import org.hyw.tools.generator.compat.ResolvedPlatform;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ComponentGroup;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.utils.FileUtils;
import org.hyw.tools.generator.web.model.Result;
import org.hyw.tools.generator.web.service.DatabaseMetadataService;
import org.hyw.tools.generator.web.service.DownloadFileService;
import org.hyw.tools.generator.web.service.UserGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;

/**
 * 代码生成器
 * 
 * @author heyw
 * @since 2020-05-26
 * @copyright: hyw Copyright (c) 2017-2020 All Rights Reserved.
 */
@RestController
@RequestMapping("/v1/gen")
public class CodeGenController {

	private static final Logger logger = LoggerFactory.getLogger(CodeGenController.class);

	/** 默认输出目录（从配置文件读取） */
	@Value("${app.output-dir:${user.home}/output/demo}")
	private String defaultOutputDir;

	@Autowired
	private UserGeneratorService userGeneratorService;

	@Autowired
	private DownloadFileService downloadFileService;

	@Autowired
	private DatabaseMetadataService databaseMetadataService;

	private Generator currentGenerator() {
		return userGeneratorService.getGenerator(currentRequest());
	}

	private HttpServletRequest currentRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return attrs != null ? attrs.getRequest() : null;
	}

	private void saveCurrentGenerator() {
		userGeneratorService.saveGenerator(currentRequest());
	}

	private File userConfigFile(String clientKey) {
		return userGeneratorService.userConfigFile(clientKey);
	}

	private String resolveClientKey(HttpServletRequest request) {
		return userGeneratorService.resolveClientKey(request);
	}

	private Component[] addComponents(Component[] components, Component... additions) {
		Set<Component> result = new LinkedHashSet<>();
		if (components != null) {
			result.addAll(Arrays.asList(components));
		}
		if (additions != null) {
			for (Component component : additions) {
				if (component != null) {
					result.add(component);
				}
			}
		}
		return result.toArray(new Component[0]);
	}

	private Component[] removeComponents(Component[] components, Component... removals) {
		Set<Component> result = new LinkedHashSet<>();
		if (components != null) {
			result.addAll(Arrays.asList(components));
		}
		if (removals != null) {
			for (Component component : removals) {
				result.remove(component);
			}
		}
		return result.toArray(new Component[0]);
	}

	private Map<String, Object> versionOverride(Generator generator, Component component) {
		Map<Component, Map<String, Object>> overrides = generator.getVersionOverrides();
		if (overrides == null) {
			overrides = new HashMap<>();
			generator.setVersionOverrides(overrides);
		}
		Map<String, Object> values = overrides.get(component);
		if (values == null) {
			values = new HashMap<>();
			overrides.put(component, values);
		}
		return values;
	}

	private File resolveDownloadFile(String path) throws IOException {
		return downloadFileService.resolveUserFile(resolveClientKey(currentRequest()), path);
	}

	@GetMapping("/health")
	public Result<?> health() {
		return Result.ok("ok");
	}

	@PostMapping("/tables")
	public Result<String> getTables(@RequestParam(name = "ipAndPort") String ipAndPort,
			@RequestParam(name = "dbName") String dbName,
			@RequestParam(name = "username") String username,
			@RequestParam(name = "pwd") String pwd,
			@RequestParam(name = "include") String include,
			@RequestParam(name = "exclude") String exclude,
			@RequestParam(name = "tablePrefix") String tablePrefix) {
		Generator generator = currentGenerator();
		logger.info("[tables] 输入 - ipAndPort: {}, dbName: {}, username: {}, include: {}, exclude: {}, tablePrefix: {}",
				ipAndPort, dbName, username, include, exclude, tablePrefix);
		synchronized (userGeneratorService.lockFor(currentRequest())) {
		if (StringUtils.isNotBlank(tablePrefix)) {
			generator.getGlobal().setTablePrefix(StringUtils.split(tablePrefix, ","));
		}
		generator.getGlobal().setMatchMode(true);
		generator.getGlobal().setInclude(StringUtils.isNotBlank(include) ? include.split(",") : null);
		generator.getGlobal().setExclude(StringUtils.isNotBlank(exclude) ? exclude.split(",") : null);
		if (StringUtils.isBlank(ipAndPort)) {
			Result<String> result = toJson();
			logger.info("[tables] 输出 - 表数量: {}", generator.getTables() != null ? generator.getTables().size() : 0);
			return result;
		}
		DataSourceConf ds = generator.getDataSource();
		ds.setIpAndPort(ipAndPort);
		ds.setDbName(dbName);
		ds.setUsername(username);
		ds.setPwd(pwd);
		Result<String> result = toJson();
		logger.info("[tables] 输出 - 表数量: {}", generator.getTables() != null ? generator.getTables().size() : 0);
		return result;
		}
	}

	/**
	 * 获取数据库列表
	 * 
	 * @param ipAndPort 数据库IP和端口
	 * @param username  用户名
	 * @param pwd       密码
	 * @return 数据库列表
	 */
	@PostMapping("/databases")
	public Result<List<String>> getDatabases(@RequestParam(name = "ipAndPort") String ipAndPort,
			@RequestParam(name = "username") String username,
			@RequestParam(name = "pwd") String pwd) {
		Generator generator = currentGenerator();
		logger.info("[databases] 输入 - ipAndPort: {}, username: {}", ipAndPort, username);
		if (StringUtils.isBlank(ipAndPort)) {
			logger.warn("[databases] 输出 - 错误: 数据库地址不能为空");
			return Result.error("数据库地址不能为空");
		}
		synchronized (userGeneratorService.lockFor(currentRequest())) {
			try {
				List<String> databases = databaseMetadataService.getDatabases(generator, ipAndPort, username, pwd);
				logger.info("[databases] 输出 - 数据库数量: {}, 列表: {}", databases.size(), databases);
				return Result.ok(databases);
			} catch (Exception e) {
				logger.error("[databases] 输出 - 错误: {}", e.getMessage());
				return Result.error("连接数据库失败: " + e.getMessage());
			}
		}
	}

	/**
	 * 序列化客户端所需数据
	 * 
	 * @return
	 */
	private Result<String> toJson() {
		Generator generator = currentGenerator();
		return new Result<>(JSON.toJSONString(generator, new SimplePropertyPreFilter("dataSource", "ipAndPort",
				"dbName", "username", "pwd", "tables", "name", "comment", "createTime")));
	}

	/**
	 * 获取当前配置（供三个步骤页面共用）
	 * 
	 * @return 当前配置信息
	 */
	@GetMapping("/config")
	public Result<?> getConfig() {
		Generator generator = currentGenerator();
		HttpServletRequest request = currentRequest();
		String clientKey = resolveClientKey(request);
		Map<String, Object> config = new HashMap<>();
		ResolvedPlatform resolvedPlatform = generator.applyCompatibility();

		GlobalConf global = generator.getGlobal();
		Map<Component, Map<String, Object>> allComponents = generator.getComponents();
		DataSourceConf ds = generator.getDataSource();

		// 只返回 global.components 中启用的组件配置
		Map<String, Map<String, Object>> enabledComponents = new HashMap<>();
		Component[] enabledArray = global.getComponents();
		if (enabledArray != null) {
			for (Component comp : enabledArray) {
				Map<String, Object> compConfig = allComponents.get(comp);
				if (compConfig != null) {
					enabledComponents.put(comp.name(), compConfig);
				}
			}
		}

		config.put("global", global);
		config.put("components", enabledComponents);
		config.put("versionOverrides", generator.getVersionOverrides());
		if (resolvedPlatform != null) {
			Map<String, Object> platform = new HashMap<>();
			platform.put("id", resolvedPlatform.getId());
			platform.put("name", resolvedPlatform.getName());
			platform.put("javaVersion", resolvedPlatform.getJavaVersion());
			platform.put("release", resolvedPlatform.getRelease());
			platform.put("templateFamily", resolvedPlatform.getTemplateFamily());
			platform.put("namespace", resolvedPlatform.getNamespace());
			platform.put("variables", resolvedPlatform.getVariables());
			config.put("platform", platform);
		}

		// DataSourceConf 中有些字段无法序列化，只返回需要的字段
		Map<String, Object> dataSource = new HashMap<>();
		dataSource.put("ipAndPort", ds.getIpAndPort());
		dataSource.put("dbName", ds.getDbName());
		dataSource.put("username", ds.getUsername());
		dataSource.put("pwd", userGeneratorService.isPersistPassword() ? ds.getPwd() : "");
		dataSource.put("dbType", ds.getDBType() != null ? ds.getDBType().name() : null);
		config.put("dataSource", dataSource);

		Map<String, Object> client = new HashMap<>();
		client.put("key", clientKey);
		client.put("strategy", userGeneratorService.getUserConfigStrategy());
		client.put("configFile", userConfigFile(clientKey).getAbsolutePath());
		client.put("persistPassword", userGeneratorService.isPersistPassword());
		client.put("trustedProxyEnabled", userGeneratorService.isTrustedProxyEnabled());
		config.put("client", client);

		// 默认值（用于初始化）
		Map<String, Object> defaults = new HashMap<>();
		defaults.put("outputDir", defaultOutputDir);
		defaults.put("userHome", System.getProperty("user.home"));
		config.put("defaults", defaults);

		logger.info("[config] 输出 - 成功");
		return Result.ok(config);
	}

	@GetMapping("/platforms")
	public Result<?> getPlatforms() {
		CompatibilityMatrix matrix = CompatibilityMatrix.loadDefault();
		List<Map<String, Object>> profiles = new ArrayList<>();
		for (CompatibilityProfile raw : matrix.getProfiles()) {
			CompatibilityProfile profile = matrix.resolveByJava(raw.getJava());
			Map<String, Object> item = new HashMap<>();
			item.put("javaVersion", profile.getJava());
			item.put("id", profile.getId());
			item.put("name", profile.getName());
			item.put("templateFamily", profile.getTemplateFamily());
			item.put("namespace", profile.getNamespace());
			item.put("release", profile.getRelease());
			item.put("versions", profile.getVersions());
			item.put("allowOverride", profile.getAllowOverride());
			profiles.add(item);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("defaultJava", matrix.getDefaultJava());
		result.put("profiles", profiles);
		return Result.ok(result);
	}

	@PostMapping("/config/default")
	public Result<?> saveAsGlobalDefault() {
		userGeneratorService.saveGlobalDefault(currentRequest());
		logger.info("[config/default] 输出 - 已保存全局默认配置");
		return Result.ok("全局默认配置已保存");
	}

	/**
	 * 获取组件分组信息（用于前端分组展示）
	 *
	 * @return 组件分组列表
	 */
	@GetMapping("/component-groups")
	public Result<List<Map<String, Object>>> getComponentGroups() {
		List<Map<String, Object>> groups = new ArrayList<>();

		for (ComponentGroup group : ComponentGroup.values()) {
			Map<String, Object> groupMap = new HashMap<>();
			groupMap.put("group", group.name());
			groupMap.put("name", group.getGroupName());
			groupMap.put("required", group.isRequired());
			groupMap.put("exclusive", group.isExclusive());

			// 组件列表
			List<Map<String, String>> components = new ArrayList<>();
			for (Component comp : group.getComponents()) {
				Map<String, String> compMap = new HashMap<>();
				compMap.put("value", comp.name());
				compMap.put("label", comp.getLabel());
				components.add(compMap);
			}
			groupMap.put("components", components);

			groups.add(groupMap);
		}

		// 添加构建工具分组
		Map<String, Object> buildGroup = new HashMap<>();
		buildGroup.put("group", "BUILD");
		buildGroup.put("name", "构建工具");
		buildGroup.put("required", true);
		buildGroup.put("exclusive", true);
		List<Map<String, String>> buildComponents = new ArrayList<>();
		Map<String, String> mavenOption = new HashMap<>();
		mavenOption.put("value", ProjectBuilder.MAVEN.name());
		mavenOption.put("label", ProjectBuilder.MAVEN.name());
		buildComponents.add(mavenOption);
		Map<String, String> gradleOption = new HashMap<>();
		gradleOption.put("value", ProjectBuilder.GRADLE.name());
		gradleOption.put("label", ProjectBuilder.GRADLE.name());
		buildComponents.add(gradleOption);
		buildGroup.put("components", buildComponents);
		groups.add(buildGroup);

		logger.info("[component-groups] 输出 - 成功，共 {} 个分组", groups.size());
		return Result.ok(groups);
	}

	/**
	 * 验证输出目录权限
	 * 
	 * @param outputDir 输出目录路径（可选，为空时返回默认配置）
	 * @return 验证结果
	 */
	@GetMapping("/validateOutputDir")
	public Result<?> validateOutputDir(@RequestParam(name = "outputDir", required = false) String outputDir) {
		String userHome = System.getProperty("user.home");

		// 如果未传入目录，返回默认配置
		if (StringUtils.isBlank(outputDir)) {
			Map<String, Object> defaults = new HashMap<>();
			defaults.put("outputDir", defaultOutputDir);
			defaults.put("userHome", userHome);
			logger.info("[validateOutputDir] 输出 - 返回默认配置, outputDir: {}, userHome: {}", defaultOutputDir, userHome);
			return Result.ok(defaults);
		}

		logger.info("[validateOutputDir] 输入 - outputDir: {}", outputDir);

		// 检查非法字符
		if (outputDir.contains("..") || outputDir.contains("~")) {
			return Result.error("目录路径包含非法字符");
		}

		File dir = new File(outputDir);

		// 如果目录存在，检查权限
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				return Result.error("路径不是目录");
			}
			if (!dir.canRead()) {
				return Result.error("目录无读取权限");
			}
			if (!dir.canWrite()) {
				return Result.error("目录无写入权限");
			}
			logger.info("[validateOutputDir] 输出 - 目录存在且有读写权限");
			return Result.ok("目录验证通过");
		}

		// 目录不存在，检查父目录权限
		File parent = dir.getParentFile();
		if (parent == null) {
			return Result.error("无法确定父目录");
		}

		// 递归查找存在的父目录
		while (parent != null && !parent.exists()) {
			parent = parent.getParentFile();
		}

		if (parent == null) {
			return Result.error("无法找到有效的父目录");
		}

		if (!parent.canWrite()) {
			return Result.error("父目录无写入权限，无法创建: " + outputDir);
		}

		logger.info("[validateOutputDir] 输出 - 父目录有写入权限，可创建目录");
		return Result.ok("目录将自动创建");
	}

	@PostMapping("/step1")
	public Result<Object> step1(@RequestParam(name = "outputDir") String outputDir,
			@RequestParam(name = "description") String description,
			@RequestParam(name = "rootPackage") String rootPackage,
			@RequestParam(name = "modules") String modules,
			@RequestParam(name = "delOutputDir") boolean delOutputDir,
			@RequestParam(name = "fileOverride") boolean fileOverride,
			@RequestParam(name = "openDir") boolean openDir) {
		Generator generator = currentGenerator();
		logger.info(
				"[step1] 输入 - outputDir: {}, description: {}, rootPackage: {}, modules: {}, delOutputDir: {}, fileOverride: {}, openDir: {}",
				outputDir, description, rootPackage, modules, delOutputDir, fileOverride, openDir);
		synchronized (userGeneratorService.lockFor(currentRequest())) {
		GlobalConf global = generator.getGlobal();
		global.setOutputDir(outputDir);
		global.setDescription(description);
		global.setRootPackage(rootPackage);
		global.setModules(StringUtils.split(modules, ","));

		global.setDelOutputDir(delOutputDir);
		global.setFileOverride(fileOverride);
		global.setOpenDir(openDir);
		saveCurrentGenerator(); // 持久化当前用户配置
		logger.info("[step1] 输出 - 成功");
		return Result.ok();
		}
	}

	@PostMapping("/step2")
	public Result<Object> step2(@RequestParam(name = "view") String view,
			@RequestParam(name = "projectBuilder", required = false, defaultValue = "MAVEN") String projectBuilder,
			@RequestParam(name = "javaVersion", required = false) String javaVersion,
			@RequestParam(name = "microservice", required = false, defaultValue = "") String microservice,
			@RequestParam(name = "springBootVersion") String springBootVersion,
			@RequestParam(name = "springCloudVersion") String springCloudVersion,
			@RequestParam(name = "springCloudAlibabaVersion") String springCloudAlibabaVersion,
			@RequestParam(name = "dubboVersion") String dubboVersion,
			@RequestParam(name = "mybatisType") String mybatisType,
			@RequestParam(name = "registryCenter") String registryCenter,
			@RequestParam(name = "zookeeperAddr") String zookeeperAddr,
			@RequestParam(name = "nacosAddr") String nacosAddr,
			@RequestParam(name = "nacosUsername") String nacosUsername,
			@RequestParam(name = "nacosPassword") String nacosPassword,
			@RequestParam(name = "redisHost") String redisHost,
			@RequestParam(name = "redisPassword") String redisPassword,
			@RequestParam(name = "sentinelVersion") String sentinelVersion,
			@RequestParam(name = "sentinelAddr") String sentinelAddr,
			@RequestParam(name = "skywalkingAddr") String skywalkingAddr,
			@RequestParam(name = "secure") String secure) {
		Generator generator = currentGenerator();
		logger.info(
				"[step2] 输入 - view: {}, projectBuilder: {}, microservice: {}, springBootVersion: {}, dubboVersion: {}, mybatisType: {}, registryCenter: {}",
				view, projectBuilder, microservice, springBootVersion, dubboVersion, mybatisType, registryCenter);
		synchronized (userGeneratorService.lockFor(currentRequest())) {
		GlobalConf global = generator.getGlobal();
		if (StringUtils.isNotBlank(javaVersion)) {
			global.setJavaVersion(javaVersion);
		}
		global.setComponents(removeComponents(global.getComponents(), Component.VUE, Component.THYMELEAF));
		Component viewComponent = Component.getComonent(view);
		global.setComponents(addComponents(global.getComponents(), viewComponent));
		if (viewComponent == Component.VUE) {
			global.setComponents(addComponents(global.getComponents(), Component.SHIRO, Component.JWT));
		}

		// 处理微服务框架选择
		global.setComponents(removeComponents(global.getComponents(), Component.SPRINGCLOUD, Component.DUBBO));
		if ("SPRINGCLOUD".equals(microservice)) {
			global.setComponents(addComponents(global.getComponents(), Component.SPRINGCLOUD));
		} else if ("DUBBO".equals(microservice)) {
			global.setComponents(addComponents(global.getComponents(), Component.DUBBO));
		}

		// projectBuilder 保持 MAVEN 或 GRADLE
		global.setProjectBuilder(ProjectBuilder.valueOf(projectBuilder));
		Map<Component, Map<String, Object>> map = generator.getComponents();
		if (StringUtils.isNotBlank(springBootVersion)) {
			versionOverride(generator, Component.SPRINGBOOT).put("springboot_version", springBootVersion);
		}
		if (StringUtils.isNotBlank(springCloudVersion)) {
			versionOverride(generator, Component.SPRINGCLOUD).put("springcloud_version", springCloudVersion);
		}
		if (StringUtils.isNotBlank(springCloudAlibabaVersion)) {
			versionOverride(generator, Component.SPRINGCLOUD).put("springcloud_alibaba_version", springCloudAlibabaVersion);
		}
		map.get(Component.SPRINGBOOT).put(Component.SPRINGBOOT.name().toLowerCase() + "_version", springBootVersion);
		map.get(Component.SPRINGCLOUD).put(Component.SPRINGCLOUD.name().toLowerCase() + "_version", springCloudVersion);
		map.get(Component.SPRINGCLOUD).put("springcloud_alibaba_version", springCloudAlibabaVersion);

		// 处理 Dubbo
		if (StringUtils.isBlank(dubboVersion)) {
			global.setComponents(removeComponents(global.getComponents(), Component.DUBBO));
		} else {
			versionOverride(generator, Component.DUBBO).put("dubbo_version", dubboVersion);
			map.get(Component.DUBBO).put(Component.DUBBO.name().toLowerCase() + "_version", dubboVersion);
		}

		// 处理注册中心/配置中心
		if ("nacos".equals(registryCenter)) {
			map.get(Component.NACOS).put("nacos.addr", nacosAddr);
			map.get(Component.NACOS).put("nacos.username", nacosUsername);
			map.get(Component.NACOS).put("nacos.password", nacosPassword);
			global.setComponents(removeComponents(global.getComponents(), Component.ZOOKEEPER));
			global.setComponents(addComponents(global.getComponents(), Component.NACOS));
		} else if ("zookeeper".equals(registryCenter)) {
			map.get(Component.ZOOKEEPER).put("connect-string", zookeeperAddr);
			global.setComponents(removeComponents(global.getComponents(), Component.NACOS));
			global.setComponents(addComponents(global.getComponents(), Component.ZOOKEEPER));
		} else {
			// 不需要注册中心
			global.setComponents(removeComponents(global.getComponents(), Component.NACOS, Component.ZOOKEEPER));
		}

		map.get(Component.MYBATIS).put("mapperType", mybatisType);
		map.get(Component.REDIS).put("spring_redis_cluster_nodes", redisHost);
		map.get(Component.REDIS).put("spring_redis_password", redisPassword);
		if (StringUtils.isNotBlank(sentinelVersion)) {
			versionOverride(generator, Component.SENTINEL).put("sentinel_version", sentinelVersion);
		}
		map.get(Component.SENTINEL).put("sentinel_version", sentinelVersion);
		map.get(Component.SENTINEL).put("dashboard.server", sentinelAddr);
		map.get(Component.SKYWALKING).put("skywalking.addr", skywalkingAddr);
		saveCurrentGenerator(); // 持久化当前用户配置
		logger.info("[step2] 输出 - 成功, viewComponent: {}, registryCenter: {}", viewComponent, registryCenter);
		return Result.ok();
		}
	}

	@PostMapping("/exec")
	@ResponseBody
	public Result<?> exec(@RequestParam(name = "tabName") String tabName,
			@RequestParam(name = "pack") Boolean pack) throws IOException {
		logger.info("[exec] 输入 - tabName: {}, pack: {}", tabName, pack);
		String[] tables = StringUtils.isBlank(tabName) ? null : tabName.split(",");
		Generator jobGenerator;
		synchronized (userGeneratorService.lockFor(currentRequest())) {
			Generator generator = currentGenerator();
			generator.getGlobal().setInclude(tables);
			generator.getGlobal().setMatchMode(false);
			saveCurrentGenerator(); // 持久化当前用户配置
			jobGenerator = generator.copy();
		}

		long startTime = System.currentTimeMillis();
		jobGenerator.execute();
		long duration = System.currentTimeMillis() - startTime;

		String outputDir = jobGenerator.getGlobal().getOutputDir();
		File outputFolder = new File(outputDir);

		// 检查输出目录是否存在且有内容
		if (!outputFolder.exists()) {
			logger.error("[exec] 输出目录不存在: {}", outputDir);
			return Result.error("代码生成失败：输出目录不存在");
		}

		int fileCount = FileUtils.countFiles(outputFolder);
		if (fileCount == 0) {
			logger.error("[exec] 输出目录为空: {}", outputDir);
			return Result.error("代码生成失败：输出目录为空");
		}
		logger.info("[exec] 输出目录文件数: {}", fileCount);

		// 获取数据库连接信息
		DataSourceConf ds = jobGenerator.getDataSource();
		String ipAndPort = ds.getIpAndPort();
		String dbName = StringUtils.defaultString(ds.getDbName(), "unknown");
		int tableCount = tables != null ? tables.length : jobGenerator.getTables().size();

		Map<String, Object> result = new HashMap<>();
		result.put("tableCount", tableCount);
		result.put("outputDir", outputDir);
		result.put("duration", duration);

		// 默认打包（pack 为 null 或 true 时打包）
		boolean shouldPack = pack == null || pack;

		if (shouldPack) {
			// 打包模式
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			String zipFileName = String.format("%s_%dt_%s.zip", dbName, tableCount, timestamp);

			String clientKey = resolveClientKey(currentRequest());
			File targetDir = downloadFileService.createTargetDir(clientKey, ipAndPort, dbName);

			File zipFile = new File(targetDir, zipFileName);

			try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
					ZipOutputStream zos = new ZipOutputStream(fos)) {
				FileUtils.zipFolder(outputFolder, zos);
				zos.finish();
				fos.flush();
			}

			if (!zipFile.exists() || zipFile.length() == 0) {
				logger.error("[exec] zip文件创建失败: {}", zipFile.getAbsolutePath());
				return Result.error("代码打包失败：zip文件创建失败");
			}
			logger.info("[exec] zip文件大小: {} bytes", zipFile.length());

			// 删除生成目录
			org.apache.commons.io.FileUtils.deleteDirectory(outputFolder);
			logger.info("已删除生成目录: {}", outputDir);

			result.put("zipFile", zipFileName);
			result.put("zipPath", downloadFileService.relativePath(clientKey, ipAndPort, dbName, zipFileName));
			result.put("packed", true);
			logger.info("代码已打包: {}, 表数量: {}, 耗时: {}ms", zipFile.getAbsolutePath(), tableCount, duration);
		} else {
			// 不打包模式
			result.put("packed", false);
			logger.info("代码已生成（不打包）: {}, 表数量: {}, 文件数: {}, 耗时: {}ms", outputDir, tableCount, fileCount, duration);
		}

		logger.info("[exec] 输出 - packed: {}, 表数量: {}, 耗时: {}ms", shouldPack, tableCount, duration);
		return Result.ok(result);
	}

	/**
	 * 生成数据库表文档（Word/PDF）
	 * 
	 * @param tabName 表名，多个用逗号分隔
	 * @param format  文档格式：word 或 pdf
	 * @return 文档下载路径
	 */
	@PostMapping("/doc")
	@ResponseBody
	public Result<?> genDoc(@RequestParam(name = "tabName") String tabName,
			@RequestParam(name = "format") String format) {
		logger.info("[doc] 输入 - tabName: {}, format: {}", tabName, format);

		// 参数校验
		if (StringUtils.isBlank(tabName)) {
			logger.warn("[doc] 输出 - 错误: 请选择要生成文档的表");
			return Result.error("请选择要生成文档的表");
		}

		String[] tables = tabName.split(",");
		String docFormat = (StringUtils.isBlank(format) ? "word" : format).toLowerCase();
		ExportFormat exportFormat = ExportFormat.fromExtension(docFormat);
		if (exportFormat == null) {
			exportFormat = ExportFormat.WORD;
			docFormat = exportFormat.getExtension();
		}

		long startTime = System.currentTimeMillis();

		try {
			String ipAndPort;
			String dbName;
			Generator jobGenerator;
			synchronized (userGeneratorService.lockFor(currentRequest())) {
				Generator generator = currentGenerator();
				DataSourceConf ds = generator.getDataSource();
				ipAndPort = ds.getIpAndPort();
				dbName = StringUtils.defaultString(ds.getDbName(), "unknown");
				generator.getGlobal().setInclude(tables);
				generator.getGlobal().setMatchMode(false);
				saveCurrentGenerator();
				jobGenerator = generator.copy();
			}
			int tableCount = tables.length;
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

			String clientKey = resolveClientKey(currentRequest());
			File targetDir = downloadFileService.createTargetDir(clientKey, ipAndPort, dbName);

			// 文档文件名：与 zip 文件命名前缀一致（数据库名_N表_日期时间）
			String docFileName = String.format("%s_%dt_%s.%s", dbName, tableCount, timestamp,
					exportFormat.getExtension());
			File docFile = new File(targetDir, docFileName);

			jobGenerator.generateDoc(docFile, docFormat, null);

			long duration = System.currentTimeMillis() - startTime;

			Map<String, Object> result = new HashMap<>();
			result.put("tables", String.join(", ", tables));
			result.put("format", docFormat);
			result.put("duration", duration);
			result.put("docFile", docFileName);
			result.put("docPath", downloadFileService.relativePath(clientKey, ipAndPort, dbName, docFileName));

			logger.info("[doc] 输出 - docPath: {}, 表数量: {}, 耗时: {}ms", docFile.getAbsolutePath(), tableCount, duration);
			return Result.ok(result);

		} catch (Exception e) {
			logger.error("[doc] 生成文档失败", e);
			return Result.error("生成文档失败: " + e.getMessage());
		}
	}

	/**
	 * 获取下载文件列表（需要验证数据库连接）
	 * 
	 * @param ipAndPort 数据库IP和端口
	 * @param dbName    数据库名
	 * @param username  用户名
	 * @param pwd       密码
	 * @return 压缩包列表
	 */
	@GetMapping("/downloads")
	public Result<List<Map<String, Object>>> getDownloads(@RequestParam(name = "ipAndPort") String ipAndPort,
			@RequestParam(name = "dbName") String dbName,
			@RequestParam(name = "username") String username,
			@RequestParam(name = "pwd") String pwd) {
		Generator generator = currentGenerator().copy();
		logger.info("[downloads] 输入 - ipAndPort: {}, dbName: {}, username: {}", ipAndPort, dbName, username);

		// 参数校验
		if (StringUtils.isBlank(ipAndPort) || StringUtils.isBlank(dbName) || StringUtils.isBlank(username)) {
			logger.warn("[downloads] 输出 - 错误: 请填写完整的数据库连接信息");
			return Result.error("请填写完整的数据库连接信息");
		}

		// 验证数据库连接
		try {
			databaseMetadataService.validateConnection(generator, ipAndPort, dbName, username, pwd);
		} catch (Exception e) {
			logger.error("[downloads] 输出 - 数据库连接失败: {}", e.getMessage());
			return Result.error("数据库连接失败: " + e.getMessage());
		}

		File targetDir;
		String clientKey = resolveClientKey(currentRequest());
		try {
			targetDir = downloadFileService.listTargetDir(clientKey, ipAndPort, dbName);
		} catch (IOException e) {
			return Result.error(e.getMessage());
		}

		if (!targetDir.exists() || !targetDir.isDirectory()) {
			return Result.ok(new ArrayList<>());
		}

		File[] files = targetDir
				.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".docx") || name.endsWith(".pdf"));
		if (files == null || files.length == 0) {
			return Result.ok(new ArrayList<>());
		}

		// 按修改时间倒序排列
		List<File> fileList = Arrays.asList(files);
		fileList.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

		List<Map<String, Object>> result = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		for (File file : fileList) {
			Map<String, Object> item = new HashMap<>();
			item.put("name", file.getName());
			item.put("size", formatFileSize(file.length()));
			item.put("time", sdf.format(new Date(file.lastModified())));
			try {
				item.put("path", downloadFileService.relativePath(clientKey, file));
			} catch (IOException e) {
				logger.warn("[downloads] 跳过非法文件: {}", file.getAbsolutePath());
				continue;
			}
			result.add(item);
		}

		logger.info("[downloads] 输出 - 文件数量: {}", result.size());
		return Result.ok(result);
	}

	/**
	 * 格式化文件大小
	 */
	private String formatFileSize(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		if (bytes < 1024 * 1024)
			return String.format("%.1f KB", bytes / 1024.0);
		return String.format("%.1f MB", bytes / (1024.0 * 1024));
	}

	/**
	 * 下载生成的代码（打包为zip）
	 * 
	 * @param path     文件相对路径（如 localhost_3306/dbuser/dbuser_3t_20260320.zip）
	 * @param response HTTP响应
	 */
	@GetMapping("/download")
	public void download(@RequestParam(name = "path", required = false) String path, HttpServletResponse response) {
		logger.info("[download] 输入 - path: {}", path);
		File zipFile = null;

		if (StringUtils.isNotBlank(path)) {
			try {
				// 下载指定文件（路径已包含 ip_port/dbname/）
				zipFile = resolveDownloadFile(path);
			} catch (IOException e) {
				logger.warn("[download] 输出 - 非法路径: {}", path);
				sendErrorResponse(response, e.getMessage());
				return;
			}
			if (!zipFile.exists()) {
				logger.warn("[download] 输出 - 文件不存在: {}", path);
				sendErrorResponse(response, "文件不存在");
				return;
			}
		} else {
			logger.warn("[download] 输出 - 错误: 请指定要下载的文件");
			sendErrorResponse(response, "请指定要下载的文件");
			return;
		}

		try {
			// 根据文件扩展名设置 Content-Type
			String fileName = zipFile.getName().toLowerCase();
			String contentType;
			if (fileName.endsWith(".docx")) {
				contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			} else if (fileName.endsWith(".pdf")) {
				contentType = "application/pdf";
			} else {
				contentType = "application/zip";
			}

			// 设置响应头
			response.setContentType(contentType);
			response.setHeader("Content-Disposition",
					"attachment; filename=" + URLEncoder.encode(zipFile.getName(), "UTF-8"));
			response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

			try (FileInputStream fis = new FileInputStream(zipFile); OutputStream os = response.getOutputStream()) {
				byte[] buffer = new byte[8192];
				int len;
				while ((len = fis.read(buffer)) > 0) {
					os.write(buffer, 0, len);
				}
			}

			logger.info("文件下载完成: {}", zipFile.getAbsolutePath());
		} catch (IOException e) {
			logger.error("下载失败", e);
			sendErrorResponse(response, "下载失败: " + e.getMessage());
		}
	}

	/**
	 * 删除生成的文件
	 * 
	 * @param path 文件相对路径
	 * @return 删除结果
	 */
	@DeleteMapping("/download")
	public Result<?> deleteFile(@RequestParam String path) {
		logger.info("[delete] 输入 - path: {}", path);

		File file;
		try {
			file = resolveDownloadFile(path);
		} catch (IOException e) {
			return Result.error(e.getMessage());
		}

		// 安全校验：只允许删除 zip、docx、pdf 文件
		if (!file.exists()) {
			return Result.error("文件不存在");
		}

		// 删除文件
		if (file.delete()) {
			logger.info("[delete] 输出 - 删除成功: {}", path);
			return Result.ok("删除成功");
		} else {
			logger.warn("[delete] 输出 - 删除失败: {}", path);
			return Result.error("删除失败");
		}
	}

	/**
	 * 发送错误响应
	 */
	private void sendErrorResponse(HttpServletResponse response, String message) {
		try {
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"status\":500,\"message\":\"" + message + "\"}");
		} catch (IOException e) {
			logger.error("写入错误响应失败", e);
		}
	}

	/**
	 * 获取表关系图数据
	 * 
	 * @param ipAndPort 数据库IP和端口
	 * @param dbName    数据库名
	 * @param username  用户名
	 * @param pwd       密码
	 * @param tabNames  表名列表（逗号分隔，可选）
	 * @return 表关系数据（nodes 和 edges）
	 */
	@GetMapping("/relations")
	public Result<Map<String, Object>> getTableRelations(@RequestParam(name = "ipAndPort") String ipAndPort,
			@RequestParam(name = "dbName") String dbName,
			@RequestParam(name = "username") String username,
			@RequestParam(name = "pwd") String pwd,
			@RequestParam(name = "tabNames") String tabNames) {
		Generator generator;
		synchronized (userGeneratorService.lockFor(currentRequest())) {
			generator = currentGenerator().copy();
		}
		logger.info("[relations] 输入 - ipAndPort: {}, dbName: {}, tabNames: {}", ipAndPort, dbName, tabNames);
		long startTime = System.currentTimeMillis();

		// 参数校验
		if (StringUtils.isBlank(ipAndPort) || StringUtils.isBlank(dbName) || StringUtils.isBlank(username)) {
			logger.warn("[relations] 输出 - 错误: 请填写完整的数据库连接信息");
			return Result.error("请填写完整的数据库连接信息");
		}

		try {
			Map<String, Object> result = databaseMetadataService.buildTableRelations(generator, ipAndPort, dbName,
					username, pwd, tabNames);
			long duration = System.currentTimeMillis() - startTime;
			List<?> nodes = (List<?>) result.get("nodes");
			List<?> edges = (List<?>) result.get("edges");
			logger.info("[relations] 输出 - 节点数: {}, 边数: {}, 耗时: {}ms", nodes.size(), edges.size(), duration);

			return Result.ok(result);

		} catch (Exception e) {
			logger.error("[relations] 获取表关系失败", e);
			return Result.error("获取表关系失败: " + e.getMessage());
		}
	}
}
