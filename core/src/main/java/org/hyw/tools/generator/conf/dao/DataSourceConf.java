package org.hyw.tools.generator.conf.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.enums.db.DBType;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Filename: DataSourceConf.java Description: 数据源配置 Copyright: Copyright (c)
 * 2015-2018 All Rights Reserved. Company: org.hyw.cn Inc.
 * 
 * @author: heyiwu
 * @version: 1.0 Create at: 2017年6月13日 上午10:03:24
 *
 */
@Slf4j
public class DataSourceConf extends DruidDataSource {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DataSourceConf.class);
	/**
	 * 数据库类型
	 */
	private DBType dbType;
	/**
	 * 密码副本(密文密码),用于生成的配置文件配置密文密码,因为配置的密文密码，在属性设值时密码已解密，后续会获取不到密文密码
	 */
	private String pwd;
	private String filter;

	private String conProperties;
	/**
	 * 当前选择的数据库名（独立于URL，用于动态切换数据库）
	 */
	private String currentDbName;
	/**
	 * 类型转换
	 */
	private TypeConvertor typeConvertor;

	private QuerySQL querySQL;

	public Connection getCon() throws Exception {
		try {
			return getConnection();
		} catch (SQLException e) {
			throw e;
		}
	}

	public String getPropertiesStr() {
		Properties props = super.getConnectProperties();
		StringBuilder builder = new StringBuilder();
		for (Object key : props.keySet()) {
			builder.append(key.toString() + "=" + props.get(key));
		}
		return builder.toString();
	}

	@Override
	public void setUrl(String jdbcUrl) {
		// 如果 URL 相同，不需要修改
		if (jdbcUrl != null && jdbcUrl.equals(this.getUrl())) {
			return;
		}
		logger.info("设置数据库URL - 原URL: {}, 新URL: {}, 用户: {}", this.getUrl(), jdbcUrl, getUsername());
		// 连接池已初始化，需要关闭后重新设置
		if (super.isInited()) {
			logger.info("连接池已初始化，关闭连接池后重新设置URL");
			super.close();
			this.jdbcUrl = jdbcUrl;
		} else {
			// 未初始化，直接设置
			super.setUrl(jdbcUrl);
		}
		// 从 URL 中解析数据库名并同步到 currentDbName
		String dbNameFromUrl = StringUtils.substringBetween(jdbcUrl, getPort() + "/", "?");
		if (StringUtils.isNotBlank(dbNameFromUrl)) {
			this.currentDbName = dbNameFromUrl;
			logger.info("从URL解析数据库名: {}", dbNameFromUrl);
		}
		
		logger.info("数据库URL设置成功 - URL: {}", jdbcUrl);
	}

	public DBType getDBType() {
		if (dbType != null) {
			return dbType;
		}

		// 如果 URL 为空，无法自动推断数据库类型
		String jdbcUrl = this.getUrl();
		if (jdbcUrl == null || jdbcUrl.isEmpty()) {
			logger.warn("数据库 URL 未设置，无法自动推断数据库类型");
			return null;
		}

		// 直接从 JDBC URL 解析数据库类型
		dbType = parseDBTypeFromUrl(jdbcUrl);
		if (dbType != null) {
			logger.info("从 URL 解析数据库类型 - 类型: {}, URL: {}", dbType, jdbcUrl);
			return dbType;
		}

		// 如果无法解析，尝试通过 Druid 连接池识别
		logger.info("初始化数据库连接池 - URL: {}, 用户: {}", jdbcUrl, getUsername());
		try {
			super.init();
			logger.info("数据库连接池初始化成功 - IP: {}, 端口: {}, 用户: {}", getIp(), getPort(), getUsername());
		} catch (SQLException e) {
			logger.error("数据库连接池初始化失败 - URL: {}, 用户: {}, 错误类型: {}, 错误信息: {}", 
				jdbcUrl, getUsername(), e.getClass(), e.getMessage(), e);
		}

		String dbTypeName = super.getDbType();
		if (dbTypeName != null && !dbTypeName.isEmpty()) {
			dbType = DBType.getDbType(dbTypeName);
			logger.info("成功识别数据库类型 - 类型: {}", dbType);
		}

		return dbType;
	}

	/**
	 * 从 JDBC URL 解析数据库类型
	 */
	private DBType parseDBTypeFromUrl(String url) {
		if (url == null) return null;
		if (url.contains(":mysql:")) return DBType.MYSQL;
		if (url.contains(":oracle:")) return DBType.ORACLE;
		if (url.contains(":sqlserver:")) return DBType.SQL_SERVER;
		if (url.contains(":postgresql:")) return DBType.POSTGRE_SQL;
		return null;
	}

	public void setDBType(DBType dbType) {
		this.dbType = dbType;
	}

	public TypeConvertor getTypeConvertor() {
		if (null != typeConvertor) {
			return typeConvertor;
		}
		DBType dbType = getDBType();
		if (null == dbType) {
			return null;
		}
		this.typeConvertor = dbType.getConvertor();
		return typeConvertor;
	}

	public void setTypeConvertor(TypeConvertor typeConvertor) {
		this.typeConvertor = typeConvertor;
	}

	public QuerySQL getQuerySQL() {
		if (null == querySQL) {
			String conf = String.format("/conf/%s.yml", getDBType().getName());
			this.querySQL = new Yaml().loadAs(DataSourceConf.class.getResourceAsStream(conf), QuerySQL.class);
		}
		return querySQL;
	}

	public void setQuerySQL(QuerySQL querySQL) {
		this.querySQL = querySQL;
	}

	public String getPwd() {
		if (StringUtils.isEmpty(pwd)) {
			pwd = getPassword();
		}
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
		super.setPassword(pwd);
	}

	public void setEncryptedPassword(String encryptedPassword) {
		this.pwd = encryptedPassword;
		super.setPassword(encryptedPassword);
	}

	public String getProperty() {
		Properties properties = super.getConnectProperties();
		if (properties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (Object key : properties.keySet()) {
			if (key.toString().contains("password")) {
				continue;
			}
			builder.append(key + "=" + properties.getProperty(key.toString(), "") + ";");
		}
		return builder.deleteCharAt(builder.length() - 1).toString();
	}

	public String getFilter() {
		return this.filter;
	}

	public void setFilters(String filters) throws SQLException {
		super.setFilters(filters);
		this.filter = filters;
	}

	public boolean isEncrypt() {
		return StringUtils.contains(getPropertiesStr(), "config.decrypt.key");
	}

	public String getConProperties() {
		return conProperties;
	}

	public void setConProperties(String connectionProperties) {
		this.conProperties = connectionProperties;
		super.setConnectionProperties(connectionProperties);
	}

	public void setIpAndPort(String ipAndPort) {
		String currentIpAndPort = getIpAndPort();
		
		// 如果相同，不需要修改
		if (ipAndPort != null && ipAndPort.equals(currentIpAndPort)) {
			logger.debug("IP和端口相同，跳过设置 - {}", ipAndPort);
			return;
		}
		
		if (currentIpAndPort == null || currentIpAndPort.isEmpty()) {
			logger.warn("无法获取当前IP和端口，跳过设置");
			return;
		}
		
		// 关闭现有连接池
		if (super.isInited()) {
			logger.info("关闭现有连接池，准备切换数据库服务器 - 当前: {}, 新: {}", currentIpAndPort, ipAndPort);
			super.close();
		}
		
		// 使用正则替换 IP:端口 部分，避免误替换 URL 中的其他部分
		String currentUrl = getUrl();
		String newUrl = currentUrl.replaceFirst("//([^/?]+)", "//" + ipAndPort);
		
		logger.info("设置数据库URL - 原URL: {}, 新URL: {}", currentUrl, newUrl);
		this.jdbcUrl = newUrl;
		this.dbType = parseDBTypeFromUrl(jdbcUrl);
	}

	public String getIpAndPort() {
		String url = getUrl();
		if (url == null) return null;
		// 使用正则匹配 //后面的 IP:端口 部分
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("//([^/?]+)").matcher(url);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	public String getIp() {
		String ipAndPort = getIpAndPort();
		if (ipAndPort == null) return null;
		int colonIndex = ipAndPort.lastIndexOf(":");
		if (colonIndex > 0) {
			return ipAndPort.substring(0, colonIndex);
		}
		return ipAndPort;
	}

	public String getPort() {
		String ipAndPort = getIpAndPort();
		if (ipAndPort == null) return null;
		int colonIndex = ipAndPort.lastIndexOf(":");
		if (colonIndex > 0) {
			return ipAndPort.substring(colonIndex + 1);
		}
		return null;
	}

	public String getDbName() {
		// 优先返回当前设置的数据库名
		if (StringUtils.isNotBlank(currentDbName)) {
			return currentDbName;
		}
		return StringUtils.substringBetween(getUrl(), getPort() + "/", "?");
	}

	public void setDbName(String dbName) {
		// trim 处理，空字符串转为 null（表示不指定数据库）
		String trimmedDbName = StringUtils.trimToNull(dbName);
		
		// 值没有变化则不处理
		if ((trimmedDbName == null && this.currentDbName == null) 
				|| (trimmedDbName != null && trimmedDbName.equals(this.currentDbName))) {
			return;
		}
		
		String oldDbName = this.currentDbName;
		this.currentDbName = trimmedDbName;
		logger.info("切换数据库 - 当前库: {}, 新库: {}", oldDbName, trimmedDbName);
		
		// 如果连接池已初始化，需要关闭
		if (super.isInited()) {
			logger.info("连接池已初始化，关闭连接池后更新URL");
			super.close();
		}
		
		// 更新 URL（无论连接池是否初始化都要更新，保持 URL 与 currentDbName 同步）
		String newUrl = (trimmedDbName == null) 
			? getDBType().buildUrl(getIp(), getPort()) 
			: getDBType().buildUrl(getIp(), getPort(), trimmedDbName);
		logger.info("设置数据库URL - 新URL: {}", newUrl);
		this.jdbcUrl = newUrl;
	}
	
	/**
	 * 重置数据源（关闭连接池，下次获取连接时自动重新初始化）
	 */
	public void resetDataSource() {
		if (super.isInited()) {
			logger.info("重置数据源 - 关闭连接池");
			super.close();
		}
	}

	public Connection getConnection(String dbName) throws SQLException {
		String url;
		if (StringUtils.isBlank(dbName)) {
			url = getDBType().buildUrl(getIp(), getPort());
			logger.info("获取服务器连接 - IP: {}, 端口: {}, 用户: {}", getIp(), getPort(), getUsername());
		} else {
			url = getDBType().buildUrl(getIp(), getPort(), dbName);
			logger.info("获取数据库连接 - IP: {}, 端口: {}, 数据库: {}, 用户: {}", getIp(), getPort(), dbName, getUsername());
		}
		return DriverManager.getConnection(url, getUsername(), getPassword());
	}

	public boolean isExists(String dbName) throws SQLException {
		try (Connection con = getConnection(dbName)) {
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public boolean createDatabase(String dbName) {
		logger.info("开始检查数据库是否存在 - 数据库名: {}, IP: {}, 端口: {}, 用户: {}", dbName, getIp(), getPort(), getUsername());
		try {
			if (isExists(dbName)) {
				logger.info("数据库已存在，无需创建 - 数据库名: {}", dbName);
				return false;
			}
		} catch (SQLException e) {
			logger.warn("检查数据库是否存在时发生异常 - 数据库名: {}, 错误: {}", dbName, e.getLocalizedMessage());
		}
		
		String sql = switch (dbType) {
		case MYSQL -> "CREATE DATABASE IF NOT EXISTS `" + dbName + "` DEFAULT CHARACTER SET utf8mb4";
		case POSTGRE_SQL -> "CREATE DATABASE \"" + dbName + "\" ENCODING 'UTF8'";
		case SQL_SERVER -> "CREATE DATABASE [" + dbName + "]";
		default -> null;
		};
		
		if (sql == null) {
			logger.warn("当前数据库类型不支持自动创建数据库 - 数据库类型: {}", dbType);
			return false;
		}
		
		logger.info("准备执行创建数据库SQL - SQL: {}, IP: {}, 端口: {}, 用户: {}", sql, getIp(), getPort(), getUsername());
		try (Connection conn = getConnection(null); Statement st = conn.createStatement()) {
			st.execute(sql);
			logger.info("数据库创建成功 - 数据库名: {}", dbName);
			return true;
		} catch (SQLException e) {
			logger.error("创建数据库失败 - 数据库名: {}, IP: {}, 端口: {}, 用户: {}, SQL: {}, 错误: {}", 
				dbName, getIp(), getPort(), getUsername(), sql, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 查询所有数据库名
	 */
	public List<String> getDataBaseNames() {
		List<String> dbNames = new ArrayList<>();
		// 确保 dbType 已初始化
		DBType currentDbType = getDBType();
		if (currentDbType == null) {
			logger.error("无法识别数据库类型，URL: {}", getUrl());
			return dbNames;
		}
		String sql = switch (currentDbType) {
		case MYSQL -> "SHOW DATABASES";
		case POSTGRE_SQL -> "SELECT datname FROM pg_database WHERE datistemplate = false";
		case SQL_SERVER -> "SELECT name FROM sys.databases";
		case ORACLE -> "SELECT username FROM all_users";
		default -> null;
		};
		if (sql == null)
			return dbNames;
		
		// 使用 DriverManager 直接获取连接（不依赖连接池），避免连接池已关闭的问题
		try (Connection conn = getConnection((String) null);
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				String name = rs.getString(1);
				// 过滤系统数据库
				if (!"information_schema".equalsIgnoreCase(name) && !"mysql".equalsIgnoreCase(name)
						&& !"performance_schema".equalsIgnoreCase(name) && !"sys".equalsIgnoreCase(name)
						&& !"pg_catalog".equalsIgnoreCase(name) && !"template0".equalsIgnoreCase(name)
						&& !"template1".equalsIgnoreCase(name)) {
					dbNames.add(name);
				}
			}
		} catch (SQLException e) {
			logger.error("查询数据库列表失败: {}", e.getMessage());
		}
		return dbNames;
	}

	/**
	 * 检查当前数据库是否为空
	 */
	public boolean isEmptyDataBase() {
		String sql = switch (dbType) {
		case MYSQL -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()";
		case POSTGRE_SQL -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema()";
		case SQL_SERVER -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = SCHEMA_NAME()";
		case ORACLE -> "SELECT COUNT(*) FROM user_tables WHERE username = USER";
		default -> null;
		};
		if (sql == null)
			return true;
		try (Connection conn = getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			if (rs.next()) {
				return rs.getInt(1) == 0;
			}
		} catch (SQLException e) {
			logger.error("检查数据库是否为空失败: {}", e.getMessage());
		}
		return true;
	}

	/**
	 * 导入 SQL 文件
	 */
	public boolean importSql(List<String> sqlFiles) {
		if (sqlFiles == null || sqlFiles.isEmpty()) {
			logger.warn("没有提供 SQL 文件");
			return false;
		}
		try (Connection conn = getConnection()) {
			for (String fileName : sqlFiles) {
				try (InputStream is = getClass().getClassLoader().getResourceAsStream("examples/" + fileName)) {
					if (is == null) {
						logger.warn("找不到 SQL 文件: {}", fileName);
						continue;
					}
					String sqlContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
					// 执行 SQL 脚本（可能包含多个语句）
					Statement st = conn.createStatement();
					// 分割 SQL 语句（以分号分隔）
					String[] statements = sqlContent.split(";");
					for (String stmt : statements) {
						String trimmedStmt = stmt.trim();
						if (!trimmedStmt.isEmpty() && !trimmedStmt.startsWith("--")) {
							st.execute(trimmedStmt);
						}
					}
					logger.info("成功导入 SQL 文件: {}", fileName);
				} catch (Exception e) {
					logger.error("导入 SQL 文件失败: {} - {}", fileName, e.getMessage());
					return false;
				}
			}
			return true;
		} catch (SQLException e) {
			logger.error("导入 SQL 失败: {}", e.getMessage());
			return false;
		}
	}
}
