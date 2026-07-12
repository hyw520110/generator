package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.metadata.MetadataReader;
import org.hyw.tools.generator.metadata.MetadataReaderFactory;
import org.hyw.tools.generator.metadata.TableFilter;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.Naming;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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
	protected Map<Component, Map<String, Object>> components;

	/**
	 * 用户允许的小版本覆盖配置，按组件分组。
	 */
	protected Map<Component, Map<String, Object>> versionOverrides;
	
	/**
	 * 表元数据缓存
	 */
	protected Cache<String, List<Table>> tableCache = Caffeine.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(30, TimeUnit.MINUTES)
			.recordStats()
			.build();

	private List<Table> mockedTables = null;

	public void setTablesForTest(List<Table> tables) {
		this.mockedTables = tables;
	}

	public List<Table> getTables() {
		return getTables(false);
	}

	public List<Table> getTables(boolean all) {
		if (mockedTables != null) {
			return mockedTables;
		}
		logger.debug("获取表列表，全部表: {}, 数据库: {}", all, dataSource.getDbName());
		// 缓存开关：未启用时直接走 DB，避免表结构变更后读到旧元数据
		if (global == null || !global.isEnableCache()) {
			return queryTablesFromDatabase(all);
		}
		String cacheKey = buildCacheKey(all);
		
		// 先尝试从缓存获取
		List<Table> cachedTables = tableCache.getIfPresent(cacheKey);
		if (cachedTables != null) {
			logger.debug("从缓存获取表元数据，缓存命中: {}, 表数量: {}", cacheKey, cachedTables.size());
			return cachedTables;
		}
		
		// 缓存未命中，从数据库查询
		List<Table> tables = queryTablesFromDatabase(all);
		
		// 存入缓存
		if (!tables.isEmpty()) {
			tableCache.put(cacheKey, tables);
			logger.debug("表元数据已缓存: {}, 表数量: {}", cacheKey, tables.size());
		}
		
		logger.debug("获取表列表完成，返回 {} 个表", tables.size());
		return tables;
	}
	
	private String buildCacheKey(boolean all) {
		StringBuilder key = new StringBuilder();
		key.append(dataSource.getDbName()).append(":");
		key.append(all ? "all" : "filtered").append(":");
		
		if (!all) {
			key.append(Arrays.toString(global.getInclude())).append(":");
					key.append(Arrays.toString(global.getExclude())).append(":");
				}
				
				key.append(global.isMatchMode()).append(":");
				key.append(Arrays.toString(global.getTablePrefix())).append(":");
				key.append(global.getNaming()).append(":");
				key.append(global.isCapitalMode());		
		return key.toString();
	}
	
	private List<Table> queryTablesFromDatabase(boolean all) {
		logger.debug("开始从数据源读取表元数据... 来源类型: {}", dataSource.getSourceType());
		
		MetadataReader metadataReader = MetadataReaderFactory.create(dataSource, global);
		
		if (all) {
			return metadataReader.readAllTables();
		} else {
			TableFilter filter = new TableFilter();
			filter.setInclude(global.getInclude());
			filter.setExclude(global.getExclude());
			filter.setMatchMode(global.isMatchMode());
			filter.setTablePrefix(global.getTablePrefix());
			return metadataReader.readTables(filter);
		}
	}

public List<String> getAllTableNames() {
		logger.debug("获取所有表名");
		List<String> list = new ArrayList<>();
		try {
			List<Table> tables = getTables(true);
			for (Table table : tables) {
				list.add(table.getName());
			}
			logger.debug("获取所有表名完成，共 {} 个表", list.size());
		} catch (Exception e) {
			logger.error("获取所有表名失败", e);
			e.printStackTrace();
		}
		return list;
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
		if (global.getNaming() == Naming.TOPASCAL) {
			// 删除前缀、下划线转帕斯卡（首字母大写的驼峰）
			return StringUtils.removePrefixAndPascal(name, tablePrefix, global.getSeparators());
		}
		return name;
	}

	public void mkDirs() {
		logger.debug("开始创建目录结构，输出目录: {}", global.getOutputDir());
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
		logger.debug("目录结构创建完成");
	}

	public void delDir() {
		File outputDir = new File(global.getOutputDir());
		if (!global.isDelOutputDir() || !outputDir.exists()) {
			return;
		}
		try {
			logger.info("删除目录: {}", outputDir);
			FileUtils.deleteDirectory(outputDir);
			logger.info("目录删除完成: {}", outputDir);
		} catch (IOException e) {
			logger.error("删除目录失败: {}", outputDir, e);
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
			ProcessBuilder pb;
			if (osName.contains(Consts.OS_NAME_WINDOWS)) {
				// cmd /c start "" "<dir>" — 第一个空字符串是窗口标题占位，避免路径含空格被当作标题
				pb = new ProcessBuilder("cmd", "/c", "start", "", dir);
			} else {
				pb = new ProcessBuilder("open", dir);
			}
			pb.start();
		} catch (IOException e) {
			logger.error("打开目录:{},发生异常:{}", dir, e.getLocalizedMessage());
		}
	}

	public static boolean isWin() {
		return getOsName().toLowerCase().startsWith(Consts.OS_NAME_PREFIX_WIN);
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

	public Map<Component, Map<String, Object>> getComponents() {
		return components;
	}

	public void setComponents(Map<Component, Map<String, Object>> components) {
		this.components = components;
	}

	public Map<Component, Map<String, Object>> getVersionOverrides() {
		return versionOverrides;
	}

	public void setVersionOverrides(Map<Component, Map<String, Object>> versionOverrides) {
		this.versionOverrides = versionOverrides;
	}
}
