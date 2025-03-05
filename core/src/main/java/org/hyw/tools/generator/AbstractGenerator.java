package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.velocity.app.VelocityEngine;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.dao.QuerySQL;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.Naming;
import org.hyw.tools.generator.enums.db.DBType;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGenerator extends BaseBean {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(AbstractGenerator.class.getName());

	/**
	 * 数据源配置
	 */
	protected DataSourceConf dataSource;
	/**
	 * 全局配置
	 */
	protected GlobalConf global;
	/**
	 * 组件配置
	 */
	protected Map<Component, Map<String, String>> components;

	public List<Table> getTables()  {
		return getTables(false);
	}

	public List<Table> getTables(boolean all)  {
		Connection con = null;
		PreparedStatement pst = null;
		Statement st = null;
		ResultSet results = null;
		ArrayList<Table> tables = new ArrayList<>();
		try {
			con = dataSource.getCon();
			QuerySQL sql = dataSource.getQuerySQL();
//			ResultSet catalogs = con.getMetaData().getCatalogs();
//			while(catalogs.next()) {
//				System.out.println(catalogs.getString(1));
//			}
			logger.debug("开始读取数据库元数据...");
			pst = con.prepareStatement(sql.getTabComments());
			results = pst.executeQuery();
			while (results.next()) {
				String tabName = results.getString(sql.getTbName());
				if (StringUtils.isEmpty(tabName)) {
					System.err.println(Arrays.toString(global.getInclude()) + "数据库为空！！！");
					break;
				}
				if (!all && (!match(global.getInclude(), tabName, true)
						|| match(global.getExclude(), tabName, false))) {
					continue;
				}
				Table table = new Table(tabName, results.getString(sql.getTbComment()));
				table.setBeanName(StringUtils.capitalFirst(processName(tabName)));
				table.setCreateTime(results.getString(sql.getTbCreateTime()));
				if (null == st) {
					st = con.createStatement();
				}
				tables.add(this.setTableFields(table, st));
			}
		} catch (Exception e) {
			logger.error("query database:{} tables fail!",dataSource.getDbName());
		} finally {
			close(st, results, pst, con);
		}
		return tables;
	}

	private  boolean match(String[] array,String tabName,boolean bDefault) {
		if (array == null || array.length == 0) {
			return bDefault;
		}
		for (String item : array) {
			if(StringUtils.isBlank(item)||tabName.equalsIgnoreCase(item)||(global.isMatchMode()&&tabName.startsWith(item))) {
				return true;
			}
		}
		return false;
	}
	public List<String> getAllTableNames() {
		List<String> list = new ArrayList<>();
		try {
			List<Table> tables = getTables(true);
			for (Table table : tables) {
				list.add(table.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 设置表字段信息
	 * 
	 * @param table 表信息
	 * @param con   数据库连接对象
	 * @return
	 * @throws SQLException
	 */
	private Table setTableFields(Table table, Statement st) throws SQLException {
		ResultSet results = null;
		try {
			QuerySQL sql = dataSource.getQuerySQL();
			results = st.executeQuery(String.format(sql.getTbFields(), table.getName()));
			while (results.next()) {
				TabField field = new TabField(results.getString(sql.getFieldName()),
						results.getString(sql.getFieldType()));
				field.setComment(results.getString(sql.getFieldComment()));
				// 转换字段类型
				KeyPair<String, FieldType> pair = dataSource.getTypeConvertor().convert(field.getType());
				field.setJdbcType(pair.getKey());
				field.setFieldType(pair.getValue());
				// 处理字段名
				field.setPropertyName(processName(field.getName()));
				String key = results.getString(sql.getFieldKeyValue().getKey());
				// 是否主键
				field.setPrimarykey(StringUtils.equals(key, sql.getFieldKeyValue().getValue()));
				// 其他数据库的字段是否为空以及自增 处理
				if (DBType.MYSQL == this.dataSource.getDBType()) {
					field.setNullAble(BooleanUtils.toBoolean(results.getString(sql.getFieldNull())));
					field.setIdentity(StringUtils.equals(results.getString(sql.getExtraKeyValue().getKey()),
							sql.getExtraKeyValue().getValue()));
				}
				table.addField(field);
				// 字段名处理后是否重名
				if (table.containField(field)) {
					field.setPropertyName(StringUtils.toCamelCase(field.getName(), global.getSeparators(), false));
				}
			}
		} catch (SQLException e) {
			logger.error("SQL Exception：{}", e.getMessage());
		} finally {
			close(results);
		}
		return table;
	}

	/**
	 * 处理字段名称
	 * 
	 * @param name
	 * @return 根据策略返回处理后的名称
	 */
	private String processName(String name) {
		if (!global.isCapitalMode() && StringUtils.isCapitalMode(name)) {
			name = name.toLowerCase();
		}
		String[] tablePrefix = global.getTablePrefix();
		if (tablePrefix != null && tablePrefix.length >= 1) {
			// 删除前缀
			name = StringUtils.removePrefix(name, tablePrefix);
		}
		if (global.getNaming() == Naming.NOCHANGE) {
			return name;
		}
		if (global.getNaming() == Naming.TOCAMEL) {
			// 删除前缀、下划线转驼峰
			return StringUtils.removePrefixAndCamel(name, tablePrefix, global.getSeparators());
		}
		return name;
	}

	public void mkDirs() {
		// 生成路径信息
		String[] modules = global.getModules();
		if (null == modules || modules.length == 0) {
			mkdirs(new File(global.getOutputDir()), global.getSourceDirectory(), global.getResourceDirectory(),
					global.getTestSourceDirectory(), global.getTestResourceDirectory());
			return;
		}
		for (String module : modules) {
			File dir = new File(global.getOutputDir(), module);
			mkdirs(dir, global.getSourceDirectory(), global.getResourceDirectory(), global.getTestSourceDirectory(),
					global.getTestResourceDirectory());
		}
	}

	public void delDir() {
		File outputDir = new File(global.getOutputDir());
		if (!global.isDelOutputDir() || !outputDir.exists()) {
			return;
		}
		try {
			logger.info("删除目录:{}", outputDir);
			FileUtils.deleteDirectory(outputDir);
		} catch (IOException ignore) {
		}
	}

	private void mkdirs(File dir, String... dirs) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
		for (String d : dirs) {
			File file = new File(dir, StringUtils.isBlank(d) ? "" : d);
			if (file.exists()) {
				continue;
			}
			logger.debug("mkdir:{}", file);
			file.mkdirs();
		}
	}

	protected void openDir() {
		String dir = global.getOutputDir();
		try {
			String osName = System.getProperty("os.name");
			if (!global.isOpenDir() || StringUtils.isEmpty(osName)) {
				logger.info("文件已生成:{}", dir);
				return;
			}
			// 打开windows or Mac的输出目录
			Runtime.getRuntime().exec((osName.contains("Windows") ? "cmd /c start " : "open ") + dir);
		} catch (IOException e) {
			logger.error("打开目录:{},发生异常:{}", dir, e.getLocalizedMessage());
		}
	}

	/**
	 * 设置模版引擎，主要指向获取模版路径
	 */
	protected VelocityEngine getVelocityEngine() {
		Properties p = new Properties();
		String conf = "/conf/velocity.properties";
		try {
			p.load(Generator.class.getResourceAsStream(conf));
		} catch (IOException e) {
			logger.error("load {} {}", conf, e.getClass(), e);
		}
		return new VelocityEngine(p);
	}

	public static boolean isWin() {
		return getOsName().toLowerCase().startsWith("win");
	}

	public static String getOsName() {
		return System.getProperty("os.name");
	}

	public DataSourceConf getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSourceConf dataSource) {
		this.dataSource = dataSource;
	}

	public GlobalConf getGlobal() {
		return global;
	}

	public void setGlobal(GlobalConf global) {
		this.global = global;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public Map<Component, Map<String, String>> getComponents() {
		return components;
	}

	public void setComponents(Map<Component, Map<String, String>> components) {
		this.components = components;
	}
}
