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
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.velocity.app.VelocityEngine;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.StrategyConf;
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
import org.yaml.snakeyaml.Yaml;

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
     * 组件配置 
     */
    protected Map<Component, Map<String, String>> components;
    
    /**
     * 加载配置、初始化
     * @author:  heyiwu 
     * @param yamlConf
     * @return
     */
    public static Generator getInstance(String yamlConf) {
        return new Yaml().loadAs(getResourceAsStream(yamlConf), Generator.class);
    }
    
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
		PreparedStatement pst = null;
		try {
			QuerySQL sql = dataSource.getQuerySQL();
			pst = con.prepareStatement(String.format(sql.getTbFields(), table.getName()));
			ResultSet results = pst.executeQuery();
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
				field.setCommonField(StringUtils.contains(strategy.getSuperEntityColumns(), field.getName(), false));
				table.addField(field);
				//字段名处理后是否重名
				if (table.containField(field)) {
					field.setPropertyName(StringUtils.toCamelCase(field.getName(),strategy.getSeparators(),false));
				}
			}
		} catch (SQLException e) {
			logger.error("SQL Exception：{}", e.getMessage());
		} finally {
			close(pst);
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

    public Map<Component, Map<String, String>> getComponents() {
        return components;
    }

    public void setComponents(Map<Component, Map<String, String>> components) {
        this.components = components;
    }
}
