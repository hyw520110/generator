package org.hyw.tools.generator.cmd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.cmd.enums.ValueType;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.constants.ValidationConstants;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ComponentGroup;
import org.hyw.tools.generator.enums.ExportFormat;
import org.hyw.tools.generator.enums.db.DBType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 命令行代码生成器
 */
public class CmdGenerator {
	private static final Logger logger = LoggerFactory.getLogger(CmdGenerator.class);

	private static final String ARG_DB_PASSWORD = "--db-password";
	private static final String ARG_DB_NAME = "--db-name";
	private static final String ARG_QUICK = "--quick";
	private static final String ARG_HELP = "--help";

	public static void main(String[] args) {
		Map<String, String> argMap = parseArgs(args);
		if (argMap.containsKey(ARG_HELP)) {
			printHelp();
			return;
		}

		boolean quickMode = argMap.containsKey(ARG_QUICK);

		try (Scanner scanner = new Scanner(System.in)) {
			Generator generator = Generator.getInstance();

			if (!quickMode)
				displayWelcomeMessage();

			// 1. 初始化数据源 (第一步，所有操作的前提)
			DataSourceConf ds = generator.getDataSource();
			if (quickMode)
				applyCommandLineArgsToDataSource(ds, argMap);
			initDataSourceConf(ds, scanner, argMap, quickMode);

			// 2. 初始化全局基础配置 (输出目录等)
			initGlobalProjectConf(generator.getGlobal(), scanner, quickMode);

			// 3. 业务配置 (选择表)
			initTableSelectionConf(generator, scanner, quickMode);

			// 4. 进入操作循环
			if (quickMode) {
				executeGeneration(generator);
			} else {
				operationLoop(generator, scanner);
			}

		} catch (Exception e) {
			logger.error("程序运行异常:", e);
			System.exit(1);
		}
	}

	// 使用BufferedReader替代Scanner解决交互式输入问题
	private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	
	private static String readLine() throws IOException {
		return br.readLine();
	}
	
	private static void operationLoop(Generator generator, Scanner scanner) throws Exception {
		while (true) {
			System.out.println("\n=== 请选择操作类型 ===");
			System.out.println("1. 生成代码");
			System.out.println("2. 导出 Word 文档");
			System.out.println("3. 导出 PDF 文档");
			System.out.println("0. 退出");
			String action = ask(scanner, "选择序号", ValueType.REQUIRE_SINGLE, "1", "2", "3", "4", "0");
			if (StringUtils.isAllBlank(action) || "0".equals(action)) {
				System.out.println("再见！");
				break;
			}

			if ("4".equals(action)) {
				initTableSelectionConf(generator, scanner, false);
				continue;
			}

			if ("1".equals(action)) {
				// 生成代码需要额外的包名配置
				configureCodeProject(generator.getGlobal(), scanner);
				generator.save(); // 保存配置快照
				executeGeneration(generator);
			} else {
				generator.save(); // 保存配置快照
				executeDocTask(generator, action.equals("2") ? "word" : "pdf", scanner);
			}

			System.out.println("\n[提示] 任务执行完毕，您可以继续选择其他操作或退出。");
		}
	}

	private static void initGlobalProjectConf(GlobalConf global, Scanner scanner, boolean quickMode) {
		if (quickMode)
			return;
		System.out.println("\n=== 基础配置 ===");
		global.setOutputDir(ask(scanner, "输出目录", ValueType.REQUIRE_SINGLE, global.getOutputDir()));
	}

	private static void configureCodeProject(GlobalConf global, Scanner scanner) {
		System.out.println("\n=== 代码工程配置 ===");
		global.setRootPackage(ask(scanner, "项目根包名", ValueType.REQUIRE_SINGLE, global.getRootPackage()));
		global.setFileOverride(
				Boolean.parseBoolean(ask(scanner, "是否覆盖已有文件", ValueType.REQUIRE_SINGLE, "true", "false")));
		global.setDescription(ask(scanner, "项目描述", ValueType.NOT_REQUIRE_SINGLE, global.getDescription()));

		String modules = ask(scanner, "工程模块(逗号分隔)", ValueType.REQUIRE_MULTIPLE, String.join(",", global.getModules()));
		global.setModules(modules.split(","));
		selectComponentPreset(global, scanner);
	}

	private static void initDataSourceConf(DataSourceConf ds, Scanner scanner, Map<String, String> argMap,
			boolean quickMode) throws SQLException {
		if (quickMode) {
			if (StringUtils.isBlank(ds.getPassword())) {
				System.err.println("错误：快速模式必须提供密码！使用 --db-password 参数");
				System.exit(1);
			}
			String dbName = argMap.get(ARG_DB_NAME);
			if (StringUtils.isBlank(dbName)) {
				dbName = ds.getDbName();
			}
			ds.setDbName(dbName);
			boolean isNew = ds.createDatabase(dbName);
			if (!isNew) {
				return;
			}
		}
		setupDataSourceInteractive(ds, scanner);
	}

	private static void initTableSelectionConf(Generator generator, Scanner scanner, boolean quickMode)
			throws Exception {
		List<String> tables = generator.getAllTableNames();
		if ((tables == null || tables.isEmpty())) {
			System.exit(0);
		}

		if (tables == null || tables.isEmpty()) {
			System.err.println("错误：数据库中没有任何表，无法继续操作！");
			System.exit(1);
		}

		if (quickMode) {
			generator.getGlobal().setInclude(tables.toArray(new String[0]));
			return;
		}

		System.out.println("\n=== 选择范围 ===");
		System.out.println("当前可用表: " + String.join(", ", tables));
		String tabInput = ask(scanner, "输入表名 (* 全部, 多表逗号分隔)", ValueType.REQUIRE_SINGLE_OR_MULTIPLE, "*");

		generator.getGlobal().setInclude("*".equals(tabInput) ? tables.toArray(new String[0]) : tabInput.split(","));

		String prefix = ask(scanner, "移除表前缀 (如有)", ValueType.NOT_REQUIRE_SINGLE, "");
		if (StringUtils.isNotBlank(prefix)) {
			generator.getGlobal().setTablePrefix(prefix.split(","));
		}
	}

	private static boolean setupDataSourceInteractive(DataSourceConf ds, Scanner scanner) throws SQLException {
		// 先检测 IP 端口连通性
		String ip = ds.getIp();
		String port = ds.getPort();
		do {
			ip = scanner(scanner, "数据库IP", ValidationConstants.IP_PATTERNS, ValueType.REQUIRE_SINGLE, ds.getIp());
			port = scanner(scanner, "端口", ValidationConstants.PORT_PATTERNS, ValueType.REQUIRE_SINGLE, ds.getPort());
			ds.setIpAndPort(ip + ":" + port);
		} while (!isPortReachable(ip, Integer.parseInt(port)));
		// 验证用户名和密码是否有效（不指定数据库）
		boolean isValid = false;
		do {
			String user = ask(scanner, "用户名", ValueType.REQUIRE_SINGLE, ds.getUsername());
			String pwd = ask(scanner, "密码", ValueType.REQUIRE_SINGLE);
			if (StringUtils.isBlank(pwd)) {
				System.err.println("错误：密码不能为空！");
				continue;
			}
			ds.setUsername(user);
			ds.setPassword(pwd);
			if (null == ds.getDBType()) {
				String typeStr = ask(scanner, "数据库类型", ValueType.REQUIRE_SINGLE, DBType.getDbNames());
				DBType dbType = DBType.getDbType(typeStr);
				ds.setDBType(dbType);
			}
			try {
				String testUrl = ds.getDBType().buildUrl(ds.getIp(), ds.getPort());
				try (Connection testConn = DriverManager.getConnection(testUrl, user, pwd)) {
					if (testConn != null && !testConn.isClosed()) {
						isValid = true;
					}
				}
			} catch (SQLException e) {
				System.err.println("数据库连接验证失败: " + e.getMessage());
			}
		} while (!isValid);

		Connection[] holder = new Connection[1];
		String dbName = selectOrCreateDbWorkflow(ds, scanner, holder);
		DBType dbType = ds.getDBType();
		if (dbName != null) {
			ds.setDriverClassName(dbType.getDriverClassName());
			ds.setUrl(dbType.buildUrl(ds.getIp(), ds.getPort(), dbName));
			ds.init();
		}
		return dbName != null;
	}

	/**
	 * 检测 IP:端口 是否可达
	 */
	private static boolean isPortReachable(String host, int port) {
		try {
			java.net.Socket socket = new java.net.Socket();
			socket.connect(new java.net.InetSocketAddress(host, port), 3000);
			socket.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String selectOrCreateDbWorkflow(DataSourceConf ds, Scanner scanner, Connection[] holder) {
		List<String> dbs = ds.getDataBaseNames();
		System.out.println("可用数据库: " + String.join(", ", dbs));
		String input = ask(scanner, "选择数据库 (序号/名称, 回车自动创建)", ValueType.NOT_REQUIRE_SINGLE, "");

		if (StringUtils.isBlank(input)) {
			String newDb = "db_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmm"));
			if (ds.createDatabase(newDb)) {
				ds.setDbName(newDb);
				// 自动导入 SQL 示例到新数据库
				ds.importSql(scanSqlFilesInExamples());
				return newDb;
			}
			return newDb;
		}
		if (dbs.contains(input)) {
			ds.setDbName(input);
			// 判断数据库是否为空，为空则导入SQL示例
			if (ds.isEmptyDataBase()) {
				ds.importSql(scanSqlFilesInExamples());
			}
			return input;
		}
		try {
			int idx = Integer.parseInt(input) - 1;
			String dbName = (idx >= 0 && idx < dbs.size()) ? dbs.get(idx) : input;
			ds.setDbName(dbName);
			// 判断数据库是否为空，为空则导入SQL示例
			if (ds.isEmptyDataBase()) {
				ds.importSql(scanSqlFilesInExamples());
			}
			return dbName;
		} catch (NumberFormatException e) {
		}
		return input;
	}

	private static void selectComponentPreset(GlobalConf global, Scanner scanner) {
		System.out.println("选择组件套装:");
		System.out.println("1. 标准版:" + Arrays.toString(global.getComponentNames()));
		// 使用ComponentGroup构建全家桶组件，每个互斥组选择一个代表性组件
		List<Component> fullStackComponents = new ArrayList<>();
		for (ComponentGroup group : ComponentGroup.values()) {
			List<Component> components = group.getComponents();
			// 选择每个组的第一个组件作为全家桶的代表性组件
			if (!components.isEmpty()) {
				fullStackComponents.add(components.get(0));
			}
		}
		Component[] fullStack = fullStackComponents.toArray(new Component[0]);
		System.out.println("2. 全家桶:" + Arrays.toString(fullStack));
		
		String choice = ask(scanner, "选择序号", ValueType.REQUIRE_SINGLE, "1", "2");
		if ("2".equals(choice)) {
			global.setComponents(fullStack);
		}
	}

	private static List<String> scanSqlFilesInExamples() {
		List<String> sqlFiles = new ArrayList<>();
		try {
			Enumeration<URL> resources = CmdGenerator.class.getClassLoader().getResources("examples");
			while (resources.hasMoreElements()) {
				URL res = resources.nextElement();
				if ("file".equals(res.getProtocol())) {
					java.io.File dir = new java.io.File(res.toURI());
					String[] list = dir.list((d, name) -> name.toLowerCase().endsWith(".sql"));
					if (list != null)
						sqlFiles.addAll(Arrays.asList(list));
				} else if ("jar".equals(res.getProtocol())) {
					String path = res.getPath();
					int split = path.indexOf("!");
					if (split < 0)
						continue;
					String jarPath = path.substring(5, split);
					try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
						Enumeration<java.util.jar.JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							String name = entries.nextElement().getName();
							if (name.startsWith("examples/") && name.endsWith(".sql")) {
								sqlFiles.add(name.substring(name.lastIndexOf("/") + 1));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("扫描 SQL 失败", e);
		}
		return sqlFiles;
	}

	private static void executeGeneration(Generator generator) {
		try {
			System.out.println("\n>>> 配置就绪，开始生成代码...");
			long start = System.currentTimeMillis();
			generator.execute();
			System.out.println("\n[完成] 耗时：" + (System.currentTimeMillis() - start) + "ms");
		} catch (Exception e) {
			logger.error("代码生成失败:", e);
		}
	}

	private static void executeDocTask(Generator generator, String format, Scanner scanner) {
		try {
			ExportFormat exportFormat = ExportFormat.fromExtension(format);
			if (exportFormat == null) {
				exportFormat = ExportFormat.WORD;
			}
			String defaultName = generator.getDataSource().getDbName() + "." + exportFormat.getExtension();
			String fileName = ask(scanner, "保存文件名", ValueType.REQUIRE_SINGLE, defaultName);
			File outputFile = new File(generator.getGlobal().getOutputDir(), fileName);

			System.out.println("\n>>> 正在生成 " + format.toUpperCase() + " 文档...");
			long start = System.currentTimeMillis();
			generator.generateDoc(outputFile, format, null);
			System.out.println("\n[完成] 文档已生成至: " + outputFile.getAbsolutePath());
			System.out.println("耗时：" + (System.currentTimeMillis() - start) + "ms");
		} catch (Exception e) {
			logger.error("导出文档失败:", e);
		}
	}

	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> map = new HashMap<>();
		if (args == null)
			return map;
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--")) {
				String val = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i] : null;
				map.put(args[i - (val == null ? 0 : 1)], val);
			}
		}
		return map;
	}

	private static void applyCommandLineArgsToDataSource(DataSourceConf ds, Map<String, String> argMap) {
		String pwd = argMap.get(ARG_DB_PASSWORD);
		if (StringUtils.isNotBlank(pwd))
			ds.setEncryptedPassword(pwd);
	}

	private static void displayWelcomeMessage() {
		try (InputStream is = CmdGenerator.class.getResourceAsStream("/welcome.txt")) {
			if (is != null)
				IOUtils.readLines(is, StandardCharsets.UTF_8).forEach(System.err::println);
		} catch (IOException ignored) {
		}
	}

	private static void printHelp() {
		System.out.println("用法: ./run.sh --quick --db-password <password>");
	}

	private static String ask(Scanner scanner, String tip, ValueType valType, String... defaults) {
		return scanner(scanner, tip, null, valType, defaults);
	}

	private static String scanner(Scanner scanner, String tip, String[] regex, ValueType valType, String... defaults) {
		String dInfo = (defaults != null && defaults.length > 0) ? " [" + String.join("/", defaults) + "]" : "";
		System.out.print(tip + (valType.isRequired() ? "(*)" : "") + dInfo + ": ");
		System.out.flush();

		try {
			while (true) {
				String input = readLine();
				if (input == null) {
					return "";
				}
				input = input.trim();
				if (StringUtils.isBlank(input)) {
					if (valType.isRequired() && (defaults == null || defaults.length == 0))
						continue;
					return (defaults != null && defaults.length > 0)
							? (valType.isSingle() ? defaults[0] : String.join(",", defaults))
							: "";
				}
				if (regex == null || Arrays.stream(regex).anyMatch(input::matches)) {
					return input;
				}
				System.out.print("输入格式错误，请重新输入 " + dInfo + ": ");
				System.out.flush();
			}
		} catch (IOException e) {
			return "";
		}
	}
}
