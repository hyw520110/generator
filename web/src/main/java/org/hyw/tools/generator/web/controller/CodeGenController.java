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
import org.hyw.tools.generator.metadata.DatabaseMetadataReader;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.utils.FileUtils;
import org.hyw.tools.generator.web.enums.StatusCode;
import org.hyw.tools.generator.web.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	
	/** PDF 字体配置（格式：目录路径:文件名模式，逗号分隔多项） */
	@Value("${app.pdf.fonts.macos:}")
	private String fontConfigsMacOS;
	
	@Value("${app.pdf.fonts.windows:}")
	private String fontConfigsWindows;
	
	@Value("${app.pdf.fonts.linux:}")
	private String fontConfigsLinux;
	
	private Generator generator;
	
	/**
	 * 获取当前操作系统的 PDF 字体路径列表
	 * 配置格式：目录路径:文件名模式（支持 * 通配符）
	 */
	private List<String> getPdfFontPaths() {
		List<String> fontPaths = new ArrayList<>();
		String osName = System.getProperty("os.name", "").toLowerCase();
		
		// 根据操作系统选择配置
		String fontConfigs;
		if (osName.contains("mac")) {
			fontConfigs = fontConfigsMacOS;
			logger.debug("macOS 字体配置: {}", fontConfigsMacOS);
		} else if (osName.contains("windows")) {
			fontConfigs = fontConfigsWindows;
			logger.debug("Windows 字体配置: {}", fontConfigsWindows);
		} else if (osName.contains("linux") || osName.contains("nix")) {
			fontConfigs = fontConfigsLinux;
			logger.debug("Linux 字体配置: {}", fontConfigsLinux);
		} else {
			logger.warn("未知操作系统: {}", osName);
			return fontPaths;
		}
		
		// 解析配置项列表（逗号分隔）
		if (StringUtils.isNotBlank(fontConfigs)) {
			logger.debug("解析字体配置，配置项: {}", fontConfigs);
			for (String config : fontConfigs.split(",")) {
				String trimmed = config.trim();
				if (StringUtils.isBlank(trimmed)) {
					continue;
				}
				
				// 解析 目录:文件名模式
				String[] parts = trimmed.split(":", 2);
				if (parts.length != 2) {
					continue;
				}
				
				String dir = parts[0].trim();
				String pattern = parts[1].trim();
				
				File fontFile = findFontFile(new File(dir), pattern);
				if (fontFile != null) {
					String path = fontFile.getAbsolutePath();
					if (!fontPaths.contains(path)) {
						fontPaths.add(path);
						logger.debug("找到字体: {}", path);
					}
				}
			}
		}
		
		logger.info("PDF 字体配置: 找到 {} 个字体", fontPaths.size());
		return fontPaths;
	}
	
	/**
	 * 在目录中查找匹配模式的字体文件
	 * @param dir 字体目录
	 * @param pattern 文件名匹配模式（支持 * 通配符）
	 * @return 匹配的字体文件，未找到返回 null
	 */
	private File findFontFile(File dir, String pattern) {
		if (!dir.exists() || !dir.isDirectory()) {
			return null;
		}
		
		// 先尝试精确匹配
		File exactMatch = new File(dir, pattern);
		if (exactMatch.exists()) {
			return exactMatch;
		}
		
		// 将通配符模式转换为正则表达式
		String regex = pattern.replace(".", "\\.").replace("*", ".*");
		
		File[] files = dir.listFiles();
		if (files == null) {
			return null;
		}
		
		// 尝试通配符匹配
		for (File file : files) {
			if (file.isFile() && file.getName().matches(regex)) {
				return file;
			}
		}
		
		return null;
	}

	@PostConstruct
	public void init() {
		generator = Generator.getInstance();
	}

	@PostMapping("/tables")
	public Result<String> getTables(String ipAndPort, String dbName, String username, String pwd, String include,
			String exclude, String tablePrefix) {
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
	 * @param ipAndPort 数据库IP和端口
	 * @param username 用户名
	 * @param pwd 密码
	 * @return 数据库列表
	 */
	@PostMapping("/databases")
	public Result<List<String>> getDatabases(String ipAndPort, String username, String pwd) {
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

	@PostMapping("/step1")
	public Result<Object> step1(String outputDir, String description, String rootPackage, String modules,
			boolean delOutputDir, boolean fileOverride, boolean openDir) {
		logger.info("[step1] 输入 - outputDir: {}, description: {}, rootPackage: {}, modules: {}, delOutputDir: {}, fileOverride: {}, openDir: {}", 
				outputDir, description, rootPackage, modules, delOutputDir, fileOverride, openDir);
		GlobalConf global = generator.getGlobal();
		global.setOutputDir(outputDir);
		global.setDescription(description);
		global.setRootPackage(rootPackage);
		global.setModules(StringUtils.split(modules, ","));

		global.setDelOutputDir(delOutputDir);
		global.setFileOverride(fileOverride);
		global.setOpenDir(openDir);
		logger.info("[step1] 输出 - 成功");
		return Result.ok();
	}

	@PostMapping("/step2")
	public Result<Object> step2(String view, String projectBuilder, String springBootVersion, String springCloudVersion,
			String springCloudAlibabaVersion, String dubboVersion, String mybatisType, String connectString,
			String redisHost, String redisPassword, String sentinelAddr, String skywalkingAddr, String secure) {
		logger.info("[step2] 输入 - view: {}, projectBuilder: {}, springBootVersion: {}, mybatisType: {}", 
				view, projectBuilder, springBootVersion, mybatisType);
		GlobalConf global = generator.getGlobal();
		ArrayUtils.removeElement(global.getComponents(), Component.VUE);
		ArrayUtils.removeElement(global.getComponents(), Component.THYMELEAF);
		Component viewComponent = Component.getComonent(view);
		global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), viewComponent));
		if (viewComponent == Component.VUE) {
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.SHIRO));
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.JWT));
		}
		global.setProjectBuilder(ProjectBuilder.valueOf(projectBuilder));
		Map<Component, Map<String, Object>> map = generator.getComponents();
		map.get(Component.SPRINGBOOT).put(Component.SPRINGBOOT.name().toLowerCase()+"_version", springBootVersion);
		map.get(Component.SPRINGCLOUD).put(Component.SPRINGCLOUD.name().toLowerCase()+"_version", springCloudVersion);
		map.get(Component.SPRINGCLOUD).put("springcloud_alibaba_version", springCloudAlibabaVersion);
		if (StringUtils.isBlank(dubboVersion)) {
			ArrayUtils.removeElement(global.getComponents(), Component.DUBBO);
		} else {
			map.get(Component.DUBBO).put(Component.DUBBO.name().toLowerCase() + "_version", dubboVersion);
		}
		map.get(Component.MYBATIS).put("mapperType", mybatisType);
		map.get(Component.ZOOKEEPER).put("connect-string", connectString);
		map.get(Component.REDIS).put("spring_redis_cluster_nodes", redisHost);
		map.get(Component.REDIS).put("spring_redis_password", redisPassword);
		map.get(Component.SENTINEL).put("dashboard.server", sentinelAddr);
		map.get(Component.SKYWALKING).put("skywalking.addr", skywalkingAddr);
		logger.info("[step2] 输出 - 成功, viewComponent: {}", viewComponent);
		return Result.ok();
	}

	@PostMapping("/exec")
	@ResponseBody
	public Result<?> exec(String tabName) throws IOException {
		logger.info("[exec] 输入 - tabName: {}", tabName);
		String[] tables = StringUtils.isBlank(tabName) ? null : tabName.split(",");
		generator.getGlobal().setInclude(tables);
		generator.getGlobal().setMatchMode(false);
		
		long startTime = System.currentTimeMillis();
		generator.execute();
		long duration = System.currentTimeMillis() - startTime;
		
		String outputDir = generator.getGlobal().getOutputDir();
		File outputFolder = new File(outputDir);
		
		// 获取数据库连接信息
		DataSourceConf ds = generator.getDataSource();
		String ipAndPort = ds.getIpAndPort();
		String dbName = StringUtils.defaultString(ds.getDbName(), "unknown");
		int tableCount = tables != null ? tables.length : generator.getTables().size();
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		
		// 压缩包文件名：数据库名_N表_日期时间.zip
		String zipFileName = String.format("%s_%dt_%s.zip", dbName, tableCount, timestamp);
		
		// 下载子目录：ip_port/数据库名/
		String subDir = ipAndPort.replace(":", "_") + "/" + dbName;
		File targetDir = new File(downloadDir, subDir);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		File zipFile = new File(targetDir, zipFileName);
		
		// 打包并保存到下载目录
		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
			 ZipOutputStream zos = new ZipOutputStream(fos)) {
			FileUtils.zipFolder(outputFolder, zos);
			zos.finish();
		}
		
		// 删除生成目录
		if (outputFolder.exists()) {
			org.apache.commons.io.FileUtils.deleteDirectory(outputFolder);
			logger.info("已删除生成目录: {}", outputDir);
		}
		
		logger.info("代码已打包: {}/{}, 表数量: {}, 耗时: {}ms", subDir, zipFileName, tableCount, duration);
		
		Map<String, Object> result = new HashMap<>();
		result.put("tables", tables != null ? String.join(", ", tables) : "全部表");
		result.put("outputDir", outputDir);
		result.put("duration", duration);
		result.put("zipFile", zipFileName);
		result.put("zipPath", subDir + "/" + zipFileName);
		
		logger.info("[exec] 输出 - zipPath: {}, 表数量: {}, 耗时: {}ms", subDir + "/" + zipFileName, tableCount, duration);
		return Result.ok(result);
	}

	/**
	 * 生成数据库表文档（Word/PDF）
	 * @param tabName 表名，多个用逗号分隔
	 * @param format 文档格式：word 或 pdf
	 * @return 文档下载路径
	 */
	@PostMapping("/doc")
	@ResponseBody
	public Result<?> genDoc(String tabName, String format) {
		logger.info("[doc] 输入 - tabName: {}, format: {}", tabName, format);
		
		// 参数校验
		if (StringUtils.isBlank(tabName)) {
			logger.warn("[doc] 输出 - 错误: 请选择要生成文档的表");
			return Result.error("请选择要生成文档的表");
		}
		
		String[] tables = tabName.split(",");
		String docFormat = (StringUtils.isBlank(format) ? "word" : format).toLowerCase();
		if (!"word".equals(docFormat) && !"pdf".equals(docFormat)) {
			docFormat = "word";
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
			String extension = "word".equals(docFormat) ? "docx" : "pdf";
			String docFileName = String.format("%s_%dt_%s.%s", dbName, tableCount, timestamp, extension);
			File docFile = new File(targetDir, docFileName);
			
			// 设置要生成文档的表
			generator.getGlobal().setInclude(tables);
			generator.getGlobal().setMatchMode(false);
			
			// 生成文档
			generator.generateDoc(docFile, docFormat, getPdfFontPaths());
			
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
	 * @param ipAndPort 数据库IP和端口
	 * @param dbName 数据库名
	 * @param username 用户名
	 * @param pwd 密码
	 * @return 压缩包列表
	 */
	@GetMapping("/downloads")
	public Result<List<Map<String, Object>>> getDownloads(
			String ipAndPort, String dbName, String username, String pwd) {
		logger.info("[downloads] 输入 - ipAndPort: {}, dbName: {}, username: {}", ipAndPort, dbName, username);
		
		// 参数校验
		if (StringUtils.isBlank(ipAndPort) || StringUtils.isBlank(dbName) 
				|| StringUtils.isBlank(username)) {
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
		
		File[] files = targetDir.listFiles((dir, name) -> 
				name.endsWith(".zip") || name.endsWith(".docx") || name.endsWith(".pdf"));
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
		if (bytes < 1024) return bytes + " B";
		if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
		return String.format("%.1f MB", bytes / (1024.0 * 1024));
	}

	/**
	 * 下载生成的代码（打包为zip）
	 * @param path 文件相对路径（如 localhost_3306/dbuser/dbuser_3t_20260320.zip）
	 * @param response HTTP响应
	 */
	@GetMapping("/download")
	public void download(@RequestParam(required = false) String path, HttpServletResponse response) {
		logger.info("[download] 输入 - path: {}", path);
		File zipFile = null;
		
		if (StringUtils.isNotBlank(path)) {
			// 下载指定文件（路径已包含 ip_port/dbname/）
			zipFile = new File(downloadDir, path);
			String fileName = zipFile.getName().toLowerCase();
			if (!zipFile.exists() || (!fileName.endsWith(".zip") && !fileName.endsWith(".docx") && !fileName.endsWith(".pdf"))) {
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
			response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(zipFile.getName(), "UTF-8"));
			response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
			
			try (FileInputStream fis = new FileInputStream(zipFile);
				 OutputStream os = response.getOutputStream()) {
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
	 * @param ipAndPort 数据库IP和端口
	 * @param dbName 数据库名
	 * @param username 用户名
	 * @param pwd 密码
	 * @param tabNames 表名列表（逗号分隔，可选）
	 * @return 表关系数据（nodes 和 edges）
	 */
	@GetMapping("/relations")
	public Result<Map<String, Object>> getTableRelations(
			String ipAndPort, String dbName, String username, String pwd, String tabNames) {
		logger.info("[relations] 输入 - ipAndPort: {}, dbName: {}, tabNames: {}", ipAndPort, dbName, tabNames);
		long startTime = System.currentTimeMillis();
		
		// 参数校验
		if (StringUtils.isBlank(ipAndPort) || StringUtils.isBlank(dbName) 
				|| StringUtils.isBlank(username)) {
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
				edges.add(edge);
			}
			
			// 返回结果
			Map<String, Object> result = new HashMap<>();
			result.put("nodes", nodes);
			result.put("edges", edges);
			result.put("tableCount", tableNames.size());
			result.put("relationCount", relations.size());
			
			long duration = System.currentTimeMillis() - startTime;
			logger.info("[relations] 输出 - 节点数: {}, 边数: {}, 耗时: {}ms", nodes.size(), edges.size(), duration);
			
			return Result.ok(result);
			
		} catch (Exception e) {
			logger.error("[relations] 获取表关系失败", e);
			return Result.error("获取表关系失败: " + e.getMessage());
		}
	}
}