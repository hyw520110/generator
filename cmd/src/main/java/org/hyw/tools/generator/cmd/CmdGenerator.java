package org.hyw.tools.generator.cmd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.ArrayUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.db.DBType;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdGenerator {
	private static final Logger logger = LoggerFactory.getLogger(CmdGenerator.class);

	private static final String IP = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";
	private static final String PORT = "([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])";
	private static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		Generator generator = Generator.getInstance();
		DataSourceConf ds = generator.getDataSource();
		System.err.println("使用帮助：");
		System.err.println("以下输入项,带[]为默认值/可选值，如保持默认不更改可直接回车\n");
		initDataSourceConf(ds);
		initGlobalConf(generator);
		try {
			System.out.println("开始生成...\n");
			generator.execute();
		} catch (Exception e) {
			logger.error("代码生成异常:",e);
		}finally {
			scanner.close();
		}
	}

	private static void initGlobalConf(Generator generator) {
		GlobalConf global = generator.getGlobal();
		global.setOutputDir(scanner("生成目录", true, global.getOutputDir()));
		global.setFileOverride(Boolean.valueOf(scanner("文件覆盖", true, "true","false")));

		try {
			List<String> tables = generator.getAllTableNames();
			String tabName = null;
			do {
				tabName = scanner("表名", true, "*");
			} while (!"*".equals(tabName)&&!tables.contains(tabName));
			if("*".equals(tabName)) {
				logger.info("准备开始生成表:{}",tables);
				global.setInclude(tables.toArray(new String[0]));
			}else {
				global.setInclude(tabName);
			}
			//TODO 
		} catch (Exception e) {
			logger.error("查询数据库表异常:", e);
		}
	}

	private static void initDataSourceConf(DataSourceConf ds) {
		String dbName = scanner("数据库类型", true, DBType.getDbNames());
		DBType dbType = DBType.getDbType(dbName);
		String ip = scanner("数据库IP", IP, false, ds.getIp());
		String port = scanner("数据库端口", PORT, false, ds.getPort());
		String dataBaseName = scanner("数据库名", false, ds.getDbName());
		String usr = scanner("数据库用户", false, ds.getUsername());
		//TODO 密码必须输入
		String pwd = scanner("数据库密码", true,ds.getPassword());
		String url = String.format(dbType.getUrl(), ip, port, dataBaseName);
		boolean isEffective = effective(dbType.getDriver(), url, usr, pwd);
		if (!isEffective) {
			initDataSourceConf(ds);
			return;
		}
		ds.setDriverClassName(dbType.getDriver());
		ds.setUrl(url);
		ds.setUsername(usr);
		ds.setPassword(pwd);
		try {
			ds.init();
		} catch (SQLException e) {
			logger.error("driver:{},url:{},usr:{} connect fail:{}", dbType.getDriver(), url, usr,
					e.getLocalizedMessage());
		}
	}

	public static boolean effective(String driver, String url, String usr, String pwd) {
		try {
			Class.forName(driver);
			try (Connection con = DriverManager.getConnection(url, usr, pwd);
					Statement st = con.createStatement();
					ResultSet rs = st.executeQuery("select 1");) {
				if (rs.next()) {
					return 1 == rs.getInt(1);
				}
			}
		} catch (Exception e) {
			logger.error("driver:{},url:{},usr:{} connect fail:{}", driver, url, usr, e.getLocalizedMessage());
		}
		return false;
	}

	public static String scanner(String tip, boolean require) {
		return scanner(tip, "", require);
	}

	public static String scanner(String tip, boolean require, String... values) {
		return scanner(tip, "", false, values);
	}

	public static String scanner(String tip, String regex, boolean require, String... sDefault) {
		print(tip, sDefault);
		while (scanner.hasNextLine()) {
			String ipt = StringUtils.trim(scanner.nextLine());
			if (StringUtils.isBlank(ipt) && !require) {
				return sDefault[0];
			}
			if ((StringUtils.isNotBlank(ipt) && StringUtils.isBlank(regex))
					|| (StringUtils.isNotBlank(ipt) && StringUtils.isNotBlank(regex) && ipt.matches(regex))) {
				if (null == sDefault || sDefault.length <= 1
						|| (sDefault.length > 1 && ArrayUtils.contains(sDefault, ipt))) {
					return ipt;
				}
			}
			print(tip, sDefault);
		}
		return sDefault[0];
	}

	private static void print(String tip, String... sDefault) {
		System.out.println("请输入" + tip
				+ ((null == sDefault || sDefault.length == 0) ? "" : Arrays.toString(sDefault).replace(", ", "/"))
				+ ":");
	}
}
