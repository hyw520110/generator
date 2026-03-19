package org.hyw.tools.generator;

import java.io.File;
import java.lang.reflect.Field;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.enums.Component;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代码生成器集成测试
 * 
 * 运行此测试前请确保数据库已准备好
 */
public class GeneratorTest {

	private static final Logger logger = LoggerFactory.getLogger(GeneratorTest.class);
	private Generator generator;

	@Before
	public void setup() throws Exception {
		// 获取单例
		generator = Generator.getInstance();
		
		// 1. 配置数据源 (根据您的 startup.sh 参数配置)
		DataSourceConf dataSource = new DataSourceConf();
		String dbName = "db_20260317202850";
		dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/" + dbName + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC");
		dataSource.setUsername("root");
		dataSource.setPassword("123456");
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		
		// 2. 配置全局参数
		GlobalConf global = new GlobalConf();
		String projectDir = System.getProperty("user.dir");
		// 如果在 core 目录下运行，user.dir 可能是 .../generator/core
		if (projectDir.endsWith("core")) {
			projectDir = new File(projectDir).getParent();
		}
		
		String outputDir = projectDir + "/output_test";
		global.setOutputDir(outputDir);
		global.setAuthor("heyiwu");
		global.setRootPackage("org.hyw.test");
		global.setModules(new String[]{"demo"});
		global.setComponents(new Component[]{Component.MYBATIS, Component.SPRINGMVC});
		global.setFileOverride(true);
		global.setDelOutputDir(true);
		
		// 3. 通过反射注入配置 (因为 Generator.load 是私有的且由静态块调用)
		injectField(generator, "dataSource", dataSource);
		injectField(generator, "global", global);
		
		logger.info("测试环境初始化完成，输出目录: {}", outputDir);
	}

	@Test
	public void testExecute() {
		try {
			generator.execute();
			logger.info("代码生成测试成功！");
		} catch (Exception e) {
			logger.error("代码生成测试失败", e);
			throw e;
		}
	}

	private void injectField(Object target, String fieldName, Object value) throws Exception {
		Field field = AbstractGenerator.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
