package org.hyw.tools.generator.template;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.db.DBType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TemplateContextBuilderTest {

    @Test
    public void testBuildGlobalContextWithComponents() {
        // 创建配置对象
        GlobalConf global = new GlobalConf();
        global.setRootPackage("com.example");
        global.setOutputDir("./demo");  // projectName 会从 outputDir 中提取
        global.setModules(new String[]{"api", "app"});
        global.setComponents(new Component[]{Component.DUBBO, Component.SENTINEL});
        
        DataSourceConf dataSource = new DataSourceConf();
        dataSource.setDbName("testdb");
        dataSource.setDBType(DBType.MYSQL);
        
        // 创建组件配置
        Map<Component, Map<String, String>> components = new HashMap<>();
        
        Map<String, String> dubboConfig = new HashMap<>();
        dubboConfig.put("dubbo_version", "2.7.8");
        dubboConfig.put("dubbo_registry", "zookeeper://localhost:2181");
        dubboConfig.put("dubbo_port", "20800");
        components.put(Component.DUBBO, dubboConfig);
        
        Map<String, String> sentinelConfig = new HashMap<>();
        sentinelConfig.put("sentinel_version", "1.8.0");
        components.put(Component.SENTINEL, sentinelConfig);
        
        // 创建上下文构建器
        TemplateContextBuilder builder = new TemplateContextBuilder(global, dataSource, components);
        RenderContext context = builder.buildGlobalContext();
        
        // 验证组件标志
        assertEquals("DUBBO should be true", "true", context.get("DUBBO").toString());
        assertEquals("dubbo should be true", "true", context.get("dubbo").toString());
        assertEquals("SENTINEL should be true", "true", context.get("SENTINEL").toString());
        assertEquals("sentinel should be true", "true", context.get("sentinel").toString());
        assertEquals("SKYWALKING should be false", "false", context.get("SKYWALKING").toString());
        assertEquals("skywalking should be false", "false", context.get("skywalking").toString());
        
        // 验证组件配置参数
        assertEquals("dubbo_version should be 2.7.8", "2.7.8", context.get("dubbo_version"));
        assertEquals("dubbo_registry should be set", "zookeeper://localhost:2181", context.get("dubbo_registry"));
        assertEquals("dubbo_port should be 20800", "20800", context.get("dubbo_port"));
        assertEquals("sentinel_version should be 1.8.0", "1.8.0", context.get("sentinel_version"));
        
        // 验证包名变量
        assertNotNull("dubboPackage should not be null", context.get("dubboPackage"));
        assertNotNull("sentinelPackage should not be null", context.get("sentinelPackage"));
        assertNotNull("skywalkingPackage should not be null", context.get("skywalkingPackage"));
        System.out.println("dubboPackage = " + context.get("dubboPackage"));
        System.out.println("sentinelPackage = " + context.get("sentinelPackage"));
        System.out.println("skywalkingPackage = " + context.get("skywalkingPackage"));
    }
}
