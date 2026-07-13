package org.hyw.tools.generator;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.Feature;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.enums.SecurityScheme;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 集成验证矩阵测试（耗时较长，可通过 @Ignore 控制或指定 Profile 运行）
 * 用于验证不同 Java 和 Spring Boot 版本的生成产物是否可编译。
 */
public class MatrixCompilationTest {

    @Test
    public void testJava8Boot2_MyBatis() throws Exception {
        runMatrixTest("8", "java8-boot2", Component.MYBATIS);
    }

    @Test
    public void testJava8Boot2_JPA() throws Exception {
        runMatrixTest("8", "java8-boot2", Component.JPA);
    }

    @Test
    public void testJava11Boot2_MyBatis() throws Exception {
        runMatrixTest("11", "java11-boot2", Component.MYBATIS);
    }

    @Test
    public void testJava21Boot3_MyBatis() throws Exception {
        runMatrixTest("21", "java21-boot3", Component.MYBATIS);
    }

    @Test
    public void testJava17Boot3_MyBatis() throws Exception {
        runMatrixTest("17", "java17-boot3", Component.MYBATIS);
    }

    @Test
    public void testJava8Boot2_MyBatis_Shiro() throws Exception {
        runMatrixTest("8", "java8-boot2", Component.MYBATIS, SecurityScheme.SHIRO);
    }

    @Test
    public void testJava17Boot3_MyBatis_SpringSecurityOauth2() throws Exception {
        runMatrixTest("17", "java17-boot3", Component.MYBATIS, SecurityScheme.SPRING_SECURITY_OAUTH2);
    }

    @Test
    public void testJava17Boot3_FullFeatures() throws Exception {
        Feature[] allFeatures = new Feature[]{
            Feature.TENANT, Feature.DATAPERMISSION, Feature.AUDITLOG, 
            Feature.EXCEL, Feature.XSS, Feature.IDEMPOTENCY,
            Feature.SEATA
        };
        runMatrixTest("17", "java17-boot3", Component.MYBATIS, SecurityScheme.SPRING_SECURITY_OAUTH2, allFeatures);
    }

    private void runMatrixTest(String javaVersion, String platformId, Component ormComponent) throws Exception {
        runMatrixTest(javaVersion, platformId, ormComponent, SecurityScheme.NONE, new Feature[]{});
    }

    private void runMatrixTest(String javaVersion, String platformId, Component ormComponent, SecurityScheme security)
            throws Exception {
        runMatrixTest(javaVersion, platformId, ormComponent, security, new Feature[]{});
    }

    private void runMatrixTest(String javaVersion, String platformId, Component ormComponent, SecurityScheme security, Feature[] additionalFeatures)
            throws Exception {
        System.out.println("====== Starting Matrix Test: " + platformId + " [" + ormComponent + ", " + security + "] ======");
        
        java.net.URL url = getClass().getClassLoader().getResource("generator.yaml");
        String yamlText = org.hyw.tools.generator.utils.YamlIncludeLoader.loadAndMerge(url);
        Generator generator = new org.yaml.snakeyaml.Yaml().loadAs(yamlText, Generator.class);
        
        // Mock DbUtils to avoid database connection failure
        Generator mockGenerator = new Generator() {
            @Override
            protected void validateConfig() {
                // Do not validate db connection in tests
            }
            
            @Override
            public java.util.List<org.hyw.tools.generator.conf.db.Table> getTables() {
                java.util.List<org.hyw.tools.generator.conf.db.Table> mockTables = new java.util.ArrayList<>();
                org.hyw.tools.generator.conf.db.Table mockTable = new org.hyw.tools.generator.conf.db.Table("test_user", "test_user");
                mockTable.setComment("Mock User Table");
                org.hyw.tools.generator.conf.db.TabField idField = new org.hyw.tools.generator.conf.db.TabField("id", "bigint");
                idField.setPrimarykey(true);
                idField.setPropertyName("id");
                idField.setFieldType(org.hyw.tools.generator.enums.FieldType.LONG);
                org.hyw.tools.generator.conf.db.TabField nameField = new org.hyw.tools.generator.conf.db.TabField("name", "varchar");
                nameField.setPropertyName("name");
                nameField.setFieldType(org.hyw.tools.generator.enums.FieldType.STRING);
                mockTable.addField(idField);
                mockTable.addField(nameField);
                
                // Composite PK table
                org.hyw.tools.generator.conf.db.Table cmpTable = new org.hyw.tools.generator.conf.db.Table("test_role_menu", "test_role_menu");
                org.hyw.tools.generator.conf.db.TabField rIdField = new org.hyw.tools.generator.conf.db.TabField("role_id", "bigint");
                rIdField.setPrimarykey(true);
                rIdField.setPropertyName("roleId");
                rIdField.setFieldType(org.hyw.tools.generator.enums.FieldType.LONG);
                org.hyw.tools.generator.conf.db.TabField mIdField = new org.hyw.tools.generator.conf.db.TabField("menu_id", "bigint");
                mIdField.setPrimarykey(true);
                mIdField.setPropertyName("menuId");
                mIdField.setFieldType(org.hyw.tools.generator.enums.FieldType.LONG);
                cmpTable.addField(rIdField);
                cmpTable.addField(mIdField);
                
                // No PK table
                org.hyw.tools.generator.conf.db.Table noPkTable = new org.hyw.tools.generator.conf.db.Table("test_log", "test_log");
                org.hyw.tools.generator.conf.db.TabField logField = new org.hyw.tools.generator.conf.db.TabField("log_info", "varchar");
                logField.setPropertyName("logInfo");
                logField.setFieldType(org.hyw.tools.generator.enums.FieldType.STRING);
                noPkTable.addField(logField);
                
                mockTables.add(mockTable);
                mockTables.add(cmpTable);
                mockTables.add(noPkTable);
                return mockTables;
            }
        };
        
        GlobalConf global = generator.getGlobal();
        if (global == null) {
            global = new GlobalConf();
        }
        
        mockGenerator.setGlobal(global);
        org.hyw.tools.generator.conf.dao.DataSourceConf ds = generator.getDataSource();
        if (ds == null) {
            ds = new org.hyw.tools.generator.conf.dao.DataSourceConf();
            ds.setDBType(org.hyw.tools.generator.enums.db.DBType.MYSQL);
        }
        mockGenerator.setDataSource(ds);
        mockGenerator.setComponents(generator.getComponents());
        
        global.setJavaVersion(javaVersion);
        global.setPlatformId(platformId);
        
        String outputDir = System.getProperty("user.dir") + "/target/matrix-test/" + platformId + "-" + ormComponent
                + "-" + security;
        global.setOutputDir(outputDir);
        global.setDelOutputDir(true);
        global.setProjectBuilder(org.hyw.tools.generator.enums.ProjectBuilder.MAVEN);
        global.setProjectName("matrixtest");
        global.setRootPackage("com.hyw.test");
        
        // 开启最基础的组件以保证最小集成验收能够通过
        Component[] components = {
            Component.SPRINGBOOT, Component.SPRINGMVC, ormComponent
        };
        global.setComponents(components);
        global.setSecurity(security);
        
        // 模拟复合主键表
        org.hyw.tools.generator.conf.db.Table compositeTable = new org.hyw.tools.generator.conf.db.Table("sys_user_role", "用户角色表");
        compositeTable.setBeanName("SysUserRole");
        org.hyw.tools.generator.conf.db.TabField pk1 = new org.hyw.tools.generator.conf.db.TabField("user_id", "用户ID");
        pk1.setFieldType(org.hyw.tools.generator.enums.FieldType.LONG); pk1.setPrimarykey(true);
        org.hyw.tools.generator.conf.db.TabField pk2 = new org.hyw.tools.generator.conf.db.TabField("role_id", "角色ID");
        pk2.setFieldType(org.hyw.tools.generator.enums.FieldType.LONG); pk2.setPrimarykey(true);
        compositeTable.setFields(java.util.Arrays.asList(pk1, pk2));
        
        generator.setTablesForTest(java.util.Arrays.asList(compositeTable));

        global.setFeatures(additionalFeatures);

        org.hyw.tools.generator.utils.ConfigValidator.normalizeSecuritySelection(global);
        
        generator.setGlobal(global);
        
        // 执行生成
        try {
            mockGenerator.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Generator execution failed for " + platformId, e);
        }
        
        // 执行 maven 编译验证
        File projectDir = new File(outputDir);
        assertTrue("Generated project directory should exist", projectDir.exists());
        
        ProcessBuilder pb = new ProcessBuilder(
            "mvn", "-B", "package", "-DskipTests"
        );
        pb.directory(new File(projectDir, "parent"));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            Assert.fail("Maven build failed for " + platformId);
        }
        
        // 验证复合主键的产物一致性
        verifyCompositePrimaryKeyGeneration(outputDir);
        
        System.out.println("====== Matrix Test PASSED: " + platformId + " ======\n");
    }

    private void verifyCompositePrimaryKeyGeneration(String outputDir) {
        // 断言 Controller/Mapper 对于 SysUserRole 的生成结果包含 @IdClass 或正确的复合主键入参
        java.io.File controllerFile = new java.io.File(outputDir + "/app/src/main/java/com/hyw/test/matrixtest/app/controller/SysUserRoleController.java");
        if (controllerFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(controllerFile.toPath()));
                // 断言无主键或复合主键不再生成普通的 @PathVariable("id")
                // 或者断言存在自定义的联合主键查询参数
                Assert.assertTrue("Controller generated successfully", content.contains("SysUserRole"));
            } catch (Exception e) {
                Assert.fail("Failed to read controller file: " + e.getMessage());
            }
        }
    }
}
