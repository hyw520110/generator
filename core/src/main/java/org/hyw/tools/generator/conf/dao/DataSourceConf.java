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
		try {
			super.setUrl(jdbcUrl);
			logger.info("数据库URL设置成功 - URL: {}", jdbcUrl);
		} catch (UnsupportedOperationException e) {
			logger.error("数据库URL设置失败 - 原URL: {}, 新URL: {}, 用户: {}, 错误: Druid连接池已初始化，不允许修改URL", 
				this.getUrl(), jdbcUrl, getUsername());
			// 抛出异常以便调用者知道修改失败
			throw e;
		}
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
		StringUtils.replace(getUrl(), getIpAndPort(), ipAndPort);
	}

	public String getIpAndPort() {
		return StringUtils.substringBetween(getUrl(), "//", "/");
	}

	public String getIp() {
		return StringUtils.substringBetween(getUrl(), "//", ":");
	}

	public String getPort() {
		return StringUtils.substringBetween(getUrl(), getIp() + ":", "/");
	}

	public String getDbName() {
		return StringUtils.substringBetween(getUrl(), getPort() + "/", "?");
	}

	public void setDbName(String dbName) {
		String newUrl = StringUtils.replace(getUrl(), getDbName(), dbName);
		setUrl(newUrl);
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
		String sql = switch (dbType) {
		case MYSQL -> "SHOW DATABASES";
		case POSTGRE_SQL -> "SELECT datname FROM pg_database WHERE datistemplate = false";
		case SQL_SERVER -> "SELECT name FROM sys.databases";
		case ORACLE -> "SELECT username FROM all_users";
		default -> null;
		};
		if (sql == null)
			return dbNames;
		try (Connection conn = getConnection();
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
