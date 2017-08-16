package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.StrategyConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.dao.QuerySQL;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;
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
	 * 策略配置
	 */
	protected StrategyConf strategy;

	/**
	 * 包配置详情
	 */
	protected Map<String, File> paths;

	public List<Table> getTables() throws Exception {
		Connection con = null;
		PreparedStatement pst = null;
		ArrayList<Table> tables = new ArrayList<>();
		try {
			con = dataSource.getCon();
			QuerySQL sql = dataSource.getQuerySQL();
			pst = con.prepareStatement(sql.getTabComments());
			ResultSet results = pst.executeQuery();
			while (results.next()) {
				String tabName = results.getString(sql.getTbName());
				if (StringUtils.isEmpty(tabName)) {
					System.err.println("当前数据库为空！！！");
					break;
				}
				if (!StringUtils.contains(strategy.getInclude(), tabName, true)
						|| StringUtils.contains(strategy.getExclude(), tabName, false)) {
					continue;
				}
				Table table = new Table(tabName, results.getString(sql.getTbComment()));
				table.setBeanName(StringUtils.capitalFirst(processName(tabName)));
				tables.add(this.setTableFields(table, con));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			close(pst, con);
		}
		return tables;
	}

	/**
	 * 设置表字段信息
	 * 
	 * @param table
	 *            表信息
	 * @param con
	 *            数据库连接对象
	 * @return
	 * @throws SQLException
	 */
	private Table setTableFields(Table table, Connection con) throws SQLException {
		List<TabField> fields = new ArrayList<>();
		PreparedStatement pst = null;
		try {
			QuerySQL sql = dataSource.getQuerySQL();
			pst = con.prepareStatement(String.format(sql.getTbFields(), table.getName()));
			ResultSet results = pst.executeQuery();
			while (results.next()) {
				TabField field = new TabField(results.getString(sql.getFieldName()),
						results.getString(sql.getFieldType()));
				// 处理字段名
				field.setPropertyName(processName(field.getName()));
				// 转换字段类型
				KeyPair<String, FieldType> pair = dataSource.getTypeConvertor().convert(field.getType());
				field.setJdbcType(pair.getKey());
				field.setFieldType(pair.getValue());

				field.setComment(results.getString(sql.getFieldComment()));

				String key = results.getString(sql.getFieldKeyValue().getKey());
				// 是否主键 TODO 复合主键处理
				field.setPrimarykey(StringUtils.equals(key, sql.getFieldKeyValue().getValue()));
				// 其他数据库的字段是否为空以及自增 处理
				if (DBType.MYSQL == this.dataSource.getDBType()) {
					field.setNullAble(Boolean.valueOf(results.getString(sql.getFieldNull())));
					field.setIdentity(StringUtils.equals(results.getString(sql.getExtraKeyValue().getKey()),
							sql.getExtraKeyValue().getValue()));
				}
				field.setCommonField(StringUtils.contains(strategy.getSuperEntityColumns(), field.getName(), false));
				fields.add(field);
			}
		} catch (SQLException e) {
			logger.error("SQL Exception：{}", e.getMessage());
		} finally {
			close(pst);
		}
		table.setFields(fields);
		return table;
	}

	/**
	 * 处理字段名称
	 * 
	 * @param name
	 * @return 根据策略返回处理后的名称
	 */
	private String processName(String name) {
		if (!strategy.isCapitalMode() && StringUtils.isCapitalMode(name)) {
			name = name.toLowerCase();
		}
		String[] tablePrefix = strategy.getTablePrefix();
		if (tablePrefix != null && tablePrefix.length >= 1) {
			// 删除前缀
			name = StringUtils.removePrefix(name, tablePrefix);
		}
		if (strategy.getNaming() == Naming.NOCHANGE) {
			return name;
		}
		if (strategy.getNaming() == Naming.TOCAMEL) {
			// 删除前缀、下划线转驼峰
			return StringUtils.removePrefixAndCamel(name, tablePrefix, strategy.getSeparators());
		}
		return name;
	}

	public void mkDirs() {
		// 生成路径信息
		String[] modules = global.getModules();
		if (null == modules || modules.length == 0) {
			return;
		}
		for (String module : modules) {
			File dir = new File(global.getOutputDir(), module);
			mkdirs(dir, global.getSourceDirectory(), global.getResource(), global.getTestSourceDirectory(),
					global.getTestResource());
		}
	}

	public void delDir() {
		File outputDir = new File(global.getOutputDir());
		if (!global.isDelOutputDir() || outputDir.exists()) {
			return;
		}
		try {
			logger.info("删除目录:{}", outputDir);
			FileUtils.deleteDirectory(outputDir);
		} catch (IOException ignore) {
		}
	}

	private void mkdirs(File dir, String... dirs) {
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

	public StrategyConf getStrategy() {
		return strategy;
	}

	public void setStrategy(StrategyConf strategy) {
		this.strategy = strategy;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
