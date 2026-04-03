package org.hyw.tools.generator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.constants.Consts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置持久化测试
 * 
 * 测试场景：
 * 1. 加载默认配置文件
 * 2. 修改数据库连接信息
 * 3. 保存到用户目录
 * 4. 验证保存的配置文件
 */
public class GeneratorConfigPersistenceTest {

	private static final Logger logger = LoggerFactory.getLogger(GeneratorConfigPersistenceTest.class);
	
	// 实际使用的配置文件路径（与 Generator.java 中定义一致）
	private static final String USER_HOME = System.getProperty("user.home");
	private static final String EXTERNAL_CONFIG_DIR = USER_HOME + File.separator + Consts.CONFIG_DIR_NAME;
	private static final String EXTERNAL_CONFIG_FILE = EXTERNAL_CONFIG_DIR + File.separator + Consts.CONFIG_FILE_NAME;
	
	// 原始配置文件备份路径
	private static final String ORIGINAL_CONFIG_BACKUP = EXTERNAL_CONFIG_FILE + ".backup_" + System.currentTimeMillis();
	
	private Generator generator;
	private File originalConfigBackup;

	@Before
	public void setup() throws Exception {
		logger.info("========== 配置持久化测试开始 ==========");
		
		// 1. 备份用户目录下已有的配置文件（如果有）
		File originalConfigFile = new File(EXTERNAL_CONFIG_FILE);
		if (originalConfigFile.exists()) {
			originalConfigBackup = new File(ORIGINAL_CONFIG_BACKUP);
			Files.copy(originalConfigFile.toPath(), originalConfigBackup.toPath());
			logger.info("已备份原始配置文件到: {}", originalConfigBackup.getAbsolutePath());
			originalConfigFile.delete();
		}
		
		// 2. 加载默认配置（因为外部配置文件已被删除）
		generator = Generator.getInstance();
		logger.info("已加载默认配置");
		
		// 验证配置已加载
		GlobalConf global = generator.getGlobal();
		DataSourceConf dataSource = generator.getDataSource();
		
		logger.info("当前配置信息:");
		logger.info("  - 输出目录: {}", global.getOutputDir());
		logger.info("  - 根包名: {}", global.getRootPackage());
		logger.info("  - 数据库URL: {}", dataSource.getUrl());
		logger.info("  - 用户名: {}", dataSource.getUsername());
		logger.info("  - 数据库: {}", dataSource.getDbName());
	}

	@Test
	public void testLoadAndSaveConfig() throws Exception {
		logger.info("========== 测试：加载配置 -> 修改 -> 保存 ==========");
		
		// 1. 修改数据库连接信息
		DataSourceConf dataSource = generator.getDataSource();
		
		String newDbHost = "192.168.1.100";
		int newDbPort = 3307;
		String newDbName = "test_database_20260327";
		String newUsername = "test_user";
		
		logger.info("修改数据库配置:");
		logger.info("  - 原数据库: {}@{}:{}/{}", 
			dataSource.getUsername(), dataSource.getIp(), dataSource.getPort(), dataSource.getDbName());
		logger.info("  - 新数据库: {}@{}:{}/{}", 
			newUsername, newDbHost, newDbPort, newDbName);
		
		// 修改 IP 和端口
		dataSource.setIpAndPort(newDbHost + ":" + newDbPort);
		
		// 修改数据库名
		dataSource.setDbName(newDbName);
		
		// 修改用户名和密码
		dataSource.setUsername(newUsername);
		dataSource.setPassword(newUsername + "_password");
		
		// 重新构建 URL
		String newUrl = String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false",
			newDbHost, newDbPort, newDbName);
		dataSource.setUrl(newUrl);
		
		// 2. 修改全局配置（可选）
		GlobalConf global = generator.getGlobal();
		String originalOutputDir = global.getOutputDir();
		String newOutputDir = System.getProperty("user.dir") + "/output_persistence_test";
		global.setOutputDir(newOutputDir);
		logger.info("修改输出目录: {} -> {}", originalOutputDir, newOutputDir);
		
		// 3. 保存配置到用户目录
		logger.info("保存配置到用户目录: {}", EXTERNAL_CONFIG_FILE);
		generator.save();
		
		// 4. 验证文件是否创建成功
		File savedConfigFile = new File(EXTERNAL_CONFIG_FILE);
		if (!savedConfigFile.exists()) {
			throw new AssertionError("配置文件保存失败，文件不存在: " + EXTERNAL_CONFIG_FILE);
		}
		logger.info("✓ 配置文件保存成功: {}", EXTERNAL_CONFIG_FILE);
		
		// 5. 读取并验证保存的配置内容
		String savedContent = new String(Files.readAllBytes(savedConfigFile.toPath()));
		logger.info("保存的配置文件内容（前500字符）:");
		logger.info(savedContent.length() > 500 ? savedContent.substring(0, 500) + "..." : savedContent);
		
		// 验证关键信息是否正确保存
		if (!savedContent.contains(newDbHost)) {
			throw new AssertionError("保存的配置文件中未包含新的数据库主机: " + newDbHost);
		}
		if (!savedContent.contains(newDbName)) {
			throw new AssertionError("保存的配置文件中未包含新的数据库名: " + newDbName);
		}
		if (!savedContent.contains(newUsername)) {
			throw new AssertionError("保存的配置文件中未包含新的用户名: " + newUsername);
		}
		
		logger.info("✓ 配置内容验证通过");
		
		// 6. 重新加载配置并验证
		logger.info("重新加载保存的配置...");
		Generator newGenerator = Generator.getInstance();
		DataSourceConf newDataSource = newGenerator.getDataSource();
		
		if (!newDataSource.getDbName().equals(newDbName)) {
			throw new AssertionError("重新加载的数据库名不匹配: 期望=" + newDbName + ", 实际=" + newDataSource.getDbName());
		}
		if (!newDataSource.getUsername().equals(newUsername)) {
			throw new AssertionError("重新加载的用户名不匹配: 期望=" + newUsername + ", 实际=" + newDataSource.getUsername());
		}
		
		logger.info("✓ 重新加载的配置验证通过");
		logger.info("========== 测试通过 ==========");
	}

	@After
	public void cleanup() throws Exception {
		logger.info("========== 清理测试环境 ==========");
		
		// 1. 删除测试生成的配置文件
		File configDir = new File(EXTERNAL_CONFIG_DIR);
		File configFile = new File(EXTERNAL_CONFIG_FILE);
		if (configFile.exists()) {
			// 不删除文件，保留供用户查看
			logger.info("测试配置文件已保留: {}", EXTERNAL_CONFIG_FILE);
			logger.info("你可以使用以下命令查看文件内容:");
			logger.info("  cat {}", EXTERNAL_CONFIG_FILE);
		}
		
		// 2. 删除配置目录（如果为空）
		if (configDir.exists() && configDir.isDirectory()) {
			File[] files = configDir.listFiles();
			if (files == null || files.length == 0) {
				configDir.delete();
				logger.info("已删除配置目录: {}", EXTERNAL_CONFIG_DIR);
			}
		}
		
		// 3. 恢复原始配置文件（如果有备份）
		if (originalConfigBackup != null && originalConfigBackup.exists()) {
			if (configFile.exists()) {
				configFile.delete();
			}
			Files.copy(originalConfigBackup.toPath(), Paths.get(EXTERNAL_CONFIG_FILE));
			originalConfigBackup.delete();
			logger.info("已恢复原始配置文件");
		}
		
		logger.info("========== 清理完成 ==========");
	}
}