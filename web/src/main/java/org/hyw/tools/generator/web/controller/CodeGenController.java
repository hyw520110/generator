package org.hyw.tools.generator.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.conf.db.TableRelation;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ComponentGroup;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.metadata.DatabaseMetadataReader;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.utils.FileUtils;
import org.hyw.tools.generator.web.enums.StatusCode;
import org.hyw.tools.generator.web.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

	/** 下载目录（从配置文件读取） */
	@Value("${app.download-dir:${user.home}/Downloads/generator}")
	private String downloadDir;

	/** 默认输出目录（从配置文件读取） */
	@Value("${app.output-dir:${user.home}/output/demo}")
	private String defaultOutputDir;

	private Generator generator;

	@PostConstruct
	public void init() {
		generator = Generator.getInstance();
	}

	@PostMapping("/tables")
	public Result<String> getTables(@RequestParam(name = "ipAndPort") String ipAndPort,
			@RequestParam(name = "dbName") String dbName,
			@RequestParam(name = "username") String username,
			@RequestParam(name = "pwd") String pwd,
			@RequestParam(name = "include") String include,
			@RequestParam(name = "exclude") String exclude,
			@RequestParam(name = "tablePrefix") String tablePrefix) {
		logger.info("[tables] 输入 - ipAndPort: {}, dbName: {}, username: {}, include: {}, exclude: {}, tablePrefix: {}",
				ipAndPort, dbName, username, include, exclude, tablePrefix);
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
		logger.info("[databases] 输入 - ipAndPort: {}, username: {}", ipAndPort, username);
		if (StringUtils.isBlank(ipAndPort)) {
			logger.warn("[databases] 输出 - 错误: 数据库地址不能为空");
			return Result.error("数据库地址不能为空");
		}
		DataSourceConf ds = generator.getDataSource();
		ds.setIpAndPort(ipAndPort);
		ds.setUsername(username);
		ds.setPwd(pwd);
		// 不指定数据库名连接数据库实例
		ds.setDbName("");
		try {
			List<String> databases = ds.getDataBaseNames();
			logger.info("[databases] 输出 - 数据库数量: {}, 列表: {}", databases.size(), databases);
			return Result.ok(databases);
		} catch (Exception e) {
			logger.error("[databases] 输出 - 错误: {}", e.getMessage());
			return Result.error("连接数据库失败: " + e.getMessage());
		}
	}

	/**
	 * 序列化客户端所需数据
	 * 
	 * @return
	 */
	private Result<String> toJson() {
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
		Map<String, Object> config = new HashMap<>();

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

		// DataSourceConf 中有些字段无法序列化，只返回需要的字段
		Map<String, Object> dataSource = new HashMap<>();
		dataSource.put("ipAndPort", ds.getIpAndPort());
		dataSource.put("dbName", ds.getDbName());
		dataSource.put("username", ds.getUsername());
		dataSource.put("pwd", ds.getPwd());
		dataSource.put("dbType", ds.getDBType() != null ? ds.getDBType().name() : null);
		config.put("dataSource", dataSource);

		// 默认值（用于初始化）
		Map<String, Object> defaults = new HashMap<>();
		defaults.put("outputDir", defaultOutputDir);
		defaults.put("userHome", System.getProperty("user.home"));
		config.put("defaults", defaults);

		logger.info("[config] 输出 - 成功");
		return Result.ok(config);
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
		logger.info(
				"[step1] 输入 - outputDir: {}, description: {}, rootPackage: {}, modules: {}, delOutputDir: {}, fileOverride: {}, openDir: {}",
				outputDir, description, rootPackage, modules, delOutputDir, fileOverride, openDir);
		GlobalConf global = generator.getGlobal();
		global.setOutputDir(outputDir);
		global.setDescription(description);
		global.setRootPackage(rootPackage);
		global.setModules(StringUtils.split(modules, ","));

		global.setDelOutputDir(delOutputDir);
		global.setFileOverride(fileOverride);
		global.setOpenDir(openDir);
		generator.save(); // 持久化配置
		logger.info("[step1] 输出 - 成功");
		return Result.ok();
	}

	@PostMapping("/step2")
	public Result<Object> step2(@RequestParam(name = "view") String view,
			@RequestParam(name = "projectBuilder", required = false, defaultValue = "MAVEN") String projectBuilder,
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
		logger.info(
				"[step2] 输入 - view: {}, projectBuilder: {}, microservice: {}, springBootVersion: {}, dubboVersion: {}, mybatisType: {}, registryCenter: {}",
				view, projectBuilder, microservice, springBootVersion, dubboVersion, mybatisType, registryCenter);
		GlobalConf global = generator.getGlobal();
		ArrayUtils.removeElement(global.getComponents(), Component.VUE);
		ArrayUtils.removeElement(global.getComponents(), Component.THYMELEAF);
		Component viewComponent = Component.getComonent(view);
		global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), viewComponent));
		if (viewComponent == Component.VUE) {
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.SHIRO));
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.JWT));
		}

		// 处理微服务框架选择
		ArrayUtils.removeElement(global.getComponents(), Component.SPRINGCLOUD);
		ArrayUtils.removeElement(global.getComponents(), Component.DUBBO);
		if ("SPRINGCLOUD".equals(microservice)) {
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.SPRINGCLOUD));
		} else if ("DUBBO".equals(microservice)) {
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.DUBBO));
		}

		// projectBuilder 保持 MAVEN 或 GRADLE
		global.setProjectBuilder(ProjectBuilder.valueOf(projectBuilder));
		Map<Component, Map<String, Object>> map = generator.getComponents();
		map.get(Component.SPRINGBOOT).put(Component.SPRINGBOOT.name().toLowerCase() + "_version", springBootVersion);
		map.get(Component.SPRINGCLOUD).put(Component.SPRINGCLOUD.name().toLowerCase() + "_version", springCloudVersion);
		map.get(Component.SPRINGCLOUD).put("springcloud_alibaba_version", springCloudAlibabaVersion);

		// 处理 Dubbo
		if (StringUtils.isBlank(dubboVersion)) {
			ArrayUtils.removeElement(global.getComponents(), Component.DUBBO);
		} else {
			map.get(Component.DUBBO).put(Component.DUBBO.name().toLowerCase() + "_version", dubboVersion);
		}

		// 处理注册中心/配置中心
		if ("nacos".equals(registryCenter)) {
			map.get(Component.NACOS).put("nacos.addr", nacosAddr);
			map.get(Component.NACOS).put("nacos.username", nacosUsername);
			map.get(Component.NACOS).put("nacos.password", nacosPassword);
			ArrayUtils.removeElement(global.getComponents(), Component.ZOOKEEPER);
		} else if ("zookeeper".equals(registryCenter)) {
			map.get(Component.ZOOKEEPER).put("connect-string", zookeeperAddr);
			ArrayUtils.removeElement(global.getComponents(), Component.NACOS);
		} else {
			// 不需要注册中心
			ArrayUtils.removeElement(global.getComponents(), Component.NACOS);
			ArrayUtils.removeElement(global.getComponents(), Component.ZOOKEEPER);
		}

		map.get(Component.MYBATIS).put("mapperType", mybatisType);
		map.get(Component.REDIS).put("spring_redis_cluster_nodes", redisHost);
		map.get(Component.REDIS).put("spring_redis_password", redisPassword);
		map.get(Component.SENTINEL).put("sentinel_version", sentinelVersion);
		map.get(Component.SENTINEL).put("dashboard.server", sentinelAddr);
		map.get(Component.SKYWALKING).put("skywalking.addr", skywalkingAddr);
		generator.save(); // 持久化配置
		logger.info("[step2] 输出 - 成功, viewComponent: {}, registryCenter: {}", viewComponent, registryCenter);
		return Result.ok();
	}

	@PostMapping("/exec")
	@ResponseBody
	public Result<?> exec(@RequestParam(name = "tabName") String tabName,
			@RequestParam(name = "pack") Boolean pack) throws IOException {
		logger.info("[exec] 输入 - tabName: {}, pack: {}", tabName, pack);
		String[] tables = StringUtils.isBlank(tabName) ? null : tabName.split(",");
		generator.getGlobal().setInclude(tables);
		generator.getGlobal().setMatchMode(false);
		generator.save(); // 持久化配置

		long startTime = System.currentTimeMillis();
		generator.execute();
		long duration = System.currentTimeMillis() - startTime;

		String outputDir = generator.getGlobal().getOutputDir();
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
		DataSourceConf ds = generator.getDataSource();
		String ipAndPort = ds.getIpAndPort();
		String dbName = StringUtils.defaultString(ds.getDbName(), "unknown");
		int tableCount = tables != null ? tables.length : generator.getTables().size();

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

			String subDir = ipAndPort.replace(":", "_") + "/" + dbName;
			File targetDir = new File(downloadDir, subDir);
			if (!targetDir.exists()) {
				targetDir.mkdirs();
			}

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
			result.put("zipPath", subDir + "/" + zipFileName);
			result.put("packed", true);
			logger.info("代码已打包: {}/{}, 表数量: {}, 耗时: {}ms", subDir, zipFileName, tableCount, duration);
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
			// 获取数据库连接信息
			DataSourceConf ds = generator.getDataSource();
			String ipAndPort = ds.getIpAndPort();
			String dbName = StringUtils.defaultString(ds.getDbName(), "unknown");
			int tableCount = tables.length;
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

			// 下载子目录：ip_port/数据库名/
			String subDir = ipAndPort.replace(":", "_") + "/" + dbName;
			File targetDir = new File(downloadDir, subDir);
			if (!targetDir.exists()) {
				targetDir.mkdirs();
			}

			// 文档文件名：与 zip 文件命名前缀一致（数据库名_N表_日期时间）
			String docFileName = String.format("%s_%dt_%s.%s", dbName, tableCount, timestamp,
					exportFormat.getExtension());
			File docFile = new File(targetDir, docFileName);

			// 设置要生成文档的表
			generator.getGlobal().setInclude(tables);
			generator.getGlobal().setMatchMode(false);

			// 生成文档
			generator.generateDoc(docFile, docFormat, null);

			long duration = System.currentTimeMillis() - startTime;

			Map<String, Object> result = new HashMap<>();
			result.put("tables", String.join(", ", tables));
			result.put("format", docFormat);
			result.put("duration", duration);
			result.put("docFile", docFileName);
			result.put("docPath", subDir + "/" + docFileName);

			logger.info("[doc] 输出 - docPath: {}, 表数量: {}, 耗时: {}ms", subDir + "/" + docFileName, tableCount, duration);
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
		logger.info("[downloads] 输入 - ipAndPort: {}, dbName: {}, username: {}", ipAndPort, dbName, username);

		// 参数校验
		if (StringUtils.isBlank(ipAndPort) || StringUtils.isBlank(dbName) || StringUtils.isBlank(username)) {
			logger.warn("[downloads] 输出 - 错误: 请填写完整的数据库连接信息");
			return Result.error("请填写完整的数据库连接信息");
		}

		// 验证数据库连接
		DataSourceConf ds = generator.getDataSource();
		String originalIpAndPort = ds.getIpAndPort();
		String originalDbName = ds.getDbName();
		String originalUsername = ds.getUsername();
		String originalPwd = ds.getPwd();

		try {
			ds.setIpAndPort(ipAndPort);
			ds.setUsername(username);
			ds.setPwd(pwd);
			ds.setDbName(dbName);
			// 尝试连接验证
			ds.getDataBaseNames();
		} catch (Exception e) {
			// 恢复原连接信息
			ds.setIpAndPort(originalIpAndPort);
			ds.setDbName(originalDbName);
			ds.setUsername(originalUsername);
			ds.setPwd(originalPwd);
			logger.error("[downloads] 输出 - 数据库连接失败: {}", e.getMessage());
			return Result.error("数据库连接失败: " + e.getMessage());
		}

		// 构建子目录路径：ip_port/数据库名/
		String subDir = ipAndPort.replace(":", "_") + "/" + dbName;
		File targetDir = new File(downloadDir, subDir);

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
			item.put("path", subDir + "/" + file.getName());
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
			// 下载指定文件（路径已包含 ip_port/dbname/）
			zipFile = new File(downloadDir, path);
			String fileName = zipFile.getName().toLowerCase();
			if (!zipFile.exists()
					|| (!fileName.endsWith(".zip") && !fileName.endsWith(".docx") && !fileName.endsWith(".pdf"))) {
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

		if (StringUtils.isBlank(path)) {
			return Result.error("请指定要删除的文件");
		}

		File file = new File(downloadDir, path);
		String fileName = file.getName().toLowerCase();

		// 安全校验：只允许删除 zip、docx、pdf 文件
		if (!file.exists()) {
			return Result.error("文件不存在");
		}
		if (!fileName.endsWith(".zip") && !fileName.endsWith(".docx") && !fileName.endsWith(".pdf")) {
			return Result.error("不支持的文件类型");
		}

		// 确保文件在下载目录内（防止路径遍历攻击）
		try {
			String canonicalPath = file.getCanonicalPath();
			String canonicalDownloadDir = new File(downloadDir).getCanonicalPath();
			if (!canonicalPath.startsWith(canonicalDownloadDir)) {
				return Result.error("非法路径");
			}
		} catch (IOException e) {
			return Result.error("路径解析失败");
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
		logger.info("[relations] 输入 - ipAndPort: {}, dbName: {}, tabNames: {}", ipAndPort, dbName, tabNames);
		long startTime = System.currentTimeMillis();

		// 参数校验
		if (StringUtils.isBlank(ipAndPort) || StringUtils.isBlank(dbName) || StringUtils.isBlank(username)) {
			logger.warn("[relations] 输出 - 错误: 请填写完整的数据库连接信息");
			return Result.error("请填写完整的数据库连接信息");
		}

		try {
			// 设置数据源连接参数
			DataSourceConf ds = generator.getDataSource();
			ds.setIpAndPort(ipAndPort);
			ds.setDbName(dbName);
			ds.setUsername(username);
			ds.setPwd(pwd);

			// 创建 DatabaseMetadataReader
			DatabaseMetadataReader metadataReader = new DatabaseMetadataReader(ds, generator.getGlobal());

			// 确定要查询关系的表
			List<String> tableNames;
			if (StringUtils.isNotBlank(tabNames)) {
				tableNames = Arrays.asList(tabNames.split(","));
			} else {
				// 获取所有表名
				tableNames = generator.getAllTableNames();
			}

			// 获取表关系
			List<TableRelation> relations = metadataReader.getTableRelations(tableNames);

			// 构建节点数据
			List<Map<String, Object>> nodes = new ArrayList<>();
			Map<String, Boolean> addedTables = new HashMap<>();

			// 获取所有表信息用于显示注释
			Map<String, Table> tableMap = new HashMap<>();
			for (Table table : generator.getTables(true)) {
				tableMap.put(table.getName(), table);
			}

			for (String tableName : tableNames) {
				if (!addedTables.containsKey(tableName)) {
					Map<String, Object> node = new HashMap<>();
					node.put("id", tableName);
					Table table = tableMap.get(tableName);
					String comment = table != null ? table.getComment() : "";
					node.put("label", StringUtils.isNotBlank(comment) ? comment : tableName);
					node.put("tableName", tableName);
					node.put("comment", comment);
					nodes.add(node);
					addedTables.put(tableName, true);
				}
			}

			// 添加关系中的表（如果有不在列表中的表）
			for (TableRelation rel : relations) {
				if (!addedTables.containsKey(rel.getTargetTable())) {
					Map<String, Object> node = new HashMap<>();
					node.put("id", rel.getTargetTable());
					Table table = tableMap.get(rel.getTargetTable());
					String comment = table != null ? table.getComment() : "";
					node.put("label", StringUtils.isNotBlank(comment) ? comment : rel.getTargetTable());
					node.put("tableName", rel.getTargetTable());
					node.put("comment", comment);
					nodes.add(node);
					addedTables.put(rel.getTargetTable(), true);
				}
			}

			// 构建边数据
			List<Map<String, Object>> edges = new ArrayList<>();
			for (TableRelation rel : relations) {
				Map<String, Object> edge = new HashMap<>();
				edge.put("source", rel.getSourceTable());
				edge.put("target", rel.getTargetTable());
				edge.put("label", rel.getFkColumn());
				edge.put("fkColumn", rel.getFkColumn());
				edge.put("pkColumn", rel.getPkColumn());
				edge.put("fkName", rel.getFkName());
				edge.put("nullable", rel.isNullable());
				edges.add(edge);
			}

			// 获取所有表的字段信息（用于右侧详情面板）
			// 使用 DriverManager 获取独立连接，避免占用连接池
			Map<String, List<Map<String, Object>>> tableColumnsMap = new HashMap<>();
			Map<String, List<Map<String, Object>>> tableForeignKeysMap = new HashMap<>();

			java.sql.Connection directConn = null;
			try {
				String jdbcUrl = ds.getDBType().buildUrl(ds.getIp(), ds.getPort(), dbName);
				directConn = java.sql.DriverManager.getConnection(jdbcUrl, username, pwd);
				logger.info("[relations] 直连数据库获取元数据 - URL: {}", jdbcUrl);

				DatabaseMetaData metaData = directConn.getMetaData();
				String catalog = dbName;
				String schema = null;

				for (String tableName : addedTables.keySet()) {
					// 获取列信息
					List<Map<String, Object>> columns = new ArrayList<>();
					try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
						while (rs.next()) {
							Map<String, Object> column = new HashMap<>();
							String columnName = rs.getString("COLUMN_NAME");
							String dataType = rs.getString("TYPE_NAME");
							int columnSize = rs.getInt("COLUMN_SIZE");
							int nullable = rs.getInt("NULLABLE");
							String defaultValue = rs.getString("COLUMN_DEF");
							String remark = rs.getString("REMARKS");
							boolean isPrimaryKey = false;

							// 检查是否是主键
							try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
								while (pkRs.next()) {
									if (columnName.equals(pkRs.getString("COLUMN_NAME"))) {
										isPrimaryKey = true;
										break;
									}
								}
							}

							column.put("columnName", columnName);
							column.put("dataType", dataType + (columnSize > 0 ? "(" + columnSize + ")" : ""));
							column.put("isPrimary", isPrimaryKey);
							column.put("isForeignKey", false);
							column.put("isNullable", nullable == DatabaseMetaData.columnNullable);
							column.put("comment", StringUtils.defaultString(remark, ""));
							column.put("defaultValue", defaultValue);

							columns.add(column);
						}
					}
					tableColumnsMap.put(tableName, columns);

					// 获取外键关系
					List<Map<String, Object>> foreignKeys = new ArrayList<>();
					try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
						while (rs.next()) {
							Map<String, Object> fk = new HashMap<>();
							fk.put("column", rs.getString("FKCOLUMN_NAME"));
							fk.put("referenceTable", rs.getString("PKTABLE_NAME"));
							fk.put("referenceColumn", rs.getString("PKCOLUMN_NAME"));
							fk.put("fkName", rs.getString("FK_NAME"));
							foreignKeys.add(fk);
						}
					}
					tableForeignKeysMap.put(tableName, foreignKeys);
				}
			} finally {
				// 关闭独立连接
				if (directConn != null && !directConn.isClosed()) {
					try {
						directConn.close();
					} catch (SQLException e) {
						logger.warn("关闭直连连接失败", e);
					}
				}
			}

			// 返回结果
			Map<String, Object> result = new HashMap<>();
			result.put("nodes", nodes);
			result.put("edges", edges);
			result.put("tableCount", tableNames.size());
			result.put("relationCount", relations.size());
			// 添加表详情数据（字段列表和外键关系）
			result.put("tableDetails", tableColumnsMap);
			result.put("tableForeignKeys", tableForeignKeysMap);

			long duration = System.currentTimeMillis() - startTime;
			logger.info("[relations] 输出 - 节点数: {}, 边数: {}, 耗时: {}ms", nodes.size(), edges.size(), duration);

			return Result.ok(result);

		} catch (Exception e) {
			logger.error("[relations] 获取表关系失败", e);
			return Result.error("获取表关系失败: " + e.getMessage());
		}
	}
}