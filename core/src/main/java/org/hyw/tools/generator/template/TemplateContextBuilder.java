package org.hyw.tools.generator.template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;

/**
 * 模板上下文构建器
 * <p>
 * 集中管理模板上下文变量的初始化和配置
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class TemplateContextBuilder {

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 全局配置
     */
    private final GlobalConf global;

    /**
     * 数据源配置
     */
    private final DataSourceConf dataSource;

    /**
     * 组件配置
     */
    private final Map<Component, Map<String, String>> components;

    /**
     * 构造函数
     *
     * @param global 全局配置
     * @param dataSource 数据源配置
     * @param components 组件配置
     */
    public TemplateContextBuilder(GlobalConf global, DataSourceConf dataSource, 
                                  Map<Component, Map<String, String>> components) {
        this.global = global;
        this.dataSource = dataSource;
        this.components = components;
    }

    /**
     * 构建全局上下文（所有模板共享的变量）
     *
     * @return 渲染上下文
     */
    public RenderContext buildGlobalContext() {
        // 获取模块名
        String moduleName = null;
        if (global.getModules() != null && global.getModules().length > 0) {
            moduleName = global.getModules()[0];
        }
        
        // 计算各种包名
        String rootPackage = global.getRootPackage();
        String projectPackage = rootPackage + "." + global.getProjectName();
        if (moduleName != null && !moduleName.isEmpty()) {
            projectPackage = projectPackage + "." + moduleName;
        }
        
        RenderContext.Builder builder = RenderContext.builder()
            // 基础信息
            .variable("author", global.getAuthor())
            .variable("encoding", global.getEncoding())
            .variable("copyright", global.getCopyright())
            .variable("description", global.getDescription())
            .variable("projectName", global.getProjectName())
            .variable("version", global.getVersion())
            .variable("javaVersion", global.getJavaVersion())
            .variable("moduleName", moduleName)
            
            // 包配置
            .variable("rootPackage", rootPackage)
            .variable("modules", global.getModules())
            .variable("projectPackage", projectPackage)
            .variable("entityPackage", projectPackage + ".entity")
            .variable("mapperPackage", projectPackage + ".mapper")
            .variable("servicePackage", projectPackage + ".service")
            .variable("controllerPackage", projectPackage + ".controller")
            .variable("aspectsPackage", projectPackage + ".aspects")
            .variable("commonsPackage", projectPackage + ".commons")
            .variable("filterPackage", projectPackage + ".filter")
            .variable("utilsPackage", projectPackage + ".utils")
            .variable("configPackage", projectPackage + ".config")
            
            // 数据库信息
            .variable("dataSource", dataSource)
            .variable("dbType", dataSource.getDBType().getName())
            .variable("dbName", dataSource.getDbName())
            
            // 全局配置对象（用于访问未提取的属性）
            .variable("global", global)
            
            // 工具类
            .variable("StringUtils", new StringUtilsBean())
            .variable("LocalDateTime", LocalDateTime.class)
            .variable("date", getCurrentDate())
            
            // Lombok 支持
            .variable("lombok", true)
            
            // 常用配置项（从组件配置中提取）
            .variable("server_port", 8080)
            .variable("enableCache", false)
            .variable("enableSwagger", false)
            .variable("enableRedis", false)
            .variable("enableEhcache", false)
            .variable("enableMybatisPlus", false)
            .variable("mapperType", "xml")
            .variable("json_type", "fastjson")
            .variable("jsonp", false)
            .variable("ssl", false);
            
        // 添加 server 对象
        Map<String, Object> serverMap = new HashMap<>();
        serverMap.put("port", 8080);
        serverMap.put("servlet", new HashMap<String, Object>() {{
            put("context-path", "/");
        }});
        builder.variable("server", serverMap);
        
        // 添加 spring 对象
        Map<String, Object> springMap = new HashMap<>();
        springMap.put("version", "5.3.21");
        builder.variable("spring", springMap);
        
        // 添加 maven 对象
        Map<String, Object> mavenMap = new HashMap<>();
        mavenMap.put("compiler", new HashMap<String, Object>() {{
            put("source", "1.8");
            put("target", "1.8");
        }});
        builder.variable("maven", mavenMap);

        // 注意：Spring Boot 运行时表达式（如 ${AnsiColor.BLUE}、${application.version} 等）
        // 应该原样输出到生成的文件中，由 Spring Boot 在运行时解析
        // 不需要在这里设置这些变量
        
        // 添加 project 对象（用于访问 Maven 项目属性）
        Map<String, Object> projectMap = new HashMap<>();
        projectMap.put("name", global.getProjectName());
        projectMap.put("version", global.getVersion());
        projectMap.put("build", new HashMap<String, Object>() {{
            put("outputDirectory", "target/classes");
        }});
        builder.variable("project", projectMap);
        
        // 添加组件标志（大写和小写都支持）
        Map<String, Object> componentMap = buildComponentMap();
        componentMap.forEach((key, value) -> {
            // 添加小写键（如 thymeleaf）
            builder.variable(key, value);
            // 添加大写键（如 THYMELEAF）
            builder.variable(key.toUpperCase(), value);
        });

        // 添加额外的包名变量（组件包名）
        builder.variable("voPackage", projectPackage + ".vo");
        builder.variable("mvcPackage", projectPackage + ".mvc");
        builder.variable("implPackage", projectPackage + ".service.impl");
        builder.variable("redisPackage", projectPackage + ".redis");
        builder.variable("shiroPackage", projectPackage + ".shiro");
        builder.variable("zookeeperPackage", projectPackage + ".zookeeper");
        builder.variable("swagger2Package", projectPackage + ".swagger2");
        builder.variable("factoryPackage", projectPackage + ".beans.factory");
        builder.variable("runnerPackage", projectPackage + ".runner");
        builder.variable("dubboPackage", projectPackage + ".dubbo");  // 添加dubboPackage
        builder.variable("sentinelPackage", projectPackage + ".sentinel");  // 添加sentinelPackage
        builder.variable("skywalkingPackage", projectPackage + ".skywalking");  // 添加skywalkingPackage
        builder.variable("thymeleafPackage", projectPackage + ".thymeleaf");  // 添加thymeleafPackage
        builder.variable("vuePackage", projectPackage + ".vue");  // 添加vuePackage

        // 添加额外的配置变量
        builder.variable("BASE_DIR", "");
        builder.variable("connect", new HashMap<String, Object>() {{
            put("timeout", 3000);
        }});
        builder.variable("cachePath", "");
        builder.variable("validatedValue", "");
        builder.variable("superControllerClass", "");
        builder.variable("superEntityClass", "");
        builder.variable("superMapperClass", "");
        builder.variable("superServiceClass", "");
        builder.variable("superServiceImplClass", "");
        builder.variable("KEYWORD", "");
        builder.variable("dubbo_registry", "");
        builder.variable("dubbo_version", "");
        builder.variable("jdbc", "");
        builder.variable("spring_redis_password", "");
        builder.variable("tables", new ArrayList<>());

        // 添加 enumsPackage 和 interceptorPackage
        builder.variable("enumsPackage", projectPackage + ".enums");
        builder.variable("interceptorPackage", projectPackage + ".interceptor");

        // 添加额外的 project 属性
        builder.variable("project", new HashMap<String, Object>() {{
            put("artifactId", global.getProjectName());
            put("build", new HashMap<String, Object>() {{
                put("directory", "target");
                put("sourceEncoding", "UTF-8");
            }});
        }});

        // 加载组件配置
        if (components != null && !components.isEmpty()) {
            Component[] enabledComponents = global.getComponents();
            for (Map.Entry<Component, Map<String, String>> entry : components.entrySet()) {
                Component component = entry.getKey();
                Map<String, String> config = entry.getValue();
                
                // 检查组件是否启用
                boolean isEnabled = enabledComponents != null && 
                    Arrays.asList(enabledComponents).contains(component);
                
                if (isEnabled && config != null) {
                    // 加载组件的所有配置参数
                    for (Map.Entry<String, String> configEntry : config.entrySet()) {
                        builder.variable(configEntry.getKey(), configEntry.getValue());
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * 构建表级别的上下文（针对特定表的模板）
     *
     * @param table 表信息
     * @return 渲染上下文
     */
    public RenderContext buildTableContext(Table table) {
        RenderContext globalContext = buildGlobalContext();
        RenderContext tableContext = globalContext.createChildContext();
        
        // 设置当前表 - 这允许模板通过${table.xxx}访问表的所有属性
        tableContext.table(table);
        
        // 添加表特定的变量 - 这些允许模板直接通过${xxx}访问
        tableContext
            .put("entityName", table.getBeanName())
            .put("entityNameLower", table.getLowercaseBeanName())
            .put("tableName", table.getName())
            .put("tableComment", table.getComment())
            .put("fields", table.getFields())  // 允许模板通过${fields}直接访问
            .put("fieldCount", table.getFieldsSize())
            .put("primaryKeyField", table.getPrimaryKeyField())
            .put("primaryKeyClass", table.getPrimaryKeyClass())
            .put("hasPrimaryKey", table.hasPrimarykeys())
            .put("isCompositePrimaryKey", table.isCompositePrimaryKey())
            .put("primaryKeyInfo", table.getPrimaryKeyInfo())
            .put("importPackages", table.getImportPackages())
            .put("className", table.getBeanName() + "Controller")  // 添加className变量
            .put("mapperName", table.getBeanName() + "Mapper")     // 添加mapperName变量
            .put("serviceName", table.getBeanName() + "Service")    // 添加serviceName变量
            .put("serviceImplName", table.getBeanName() + "ServiceImpl") // 添加serviceImplName变量
            .put("controllerName", table.getBeanName() + "Controller"); // 添加controllerName变量
        
        // 添加字段相关的快捷变量
        addFieldShortcuts(tableContext, table);
        
        return tableContext;
    }

    /**
     * 构建组件上下文
     *
     * @param component 组件
     * @param componentParams 组件参数
     * @return 渲染上下文
     */
    public RenderContext buildComponentContext(Component component, 
                                                Map<String, String> componentParams) {
        RenderContext globalContext = buildGlobalContext();
        RenderContext componentContext = globalContext.createChildContext();
        
        // 设置当前组件
        Set<Component> components = new HashSet<>();
        components.add(component);
        componentContext.components(components);
        
        // 添加组件参数
        if (componentParams != null && !componentParams.isEmpty()) {
            componentParams.forEach(componentContext::put);
        }
        
        return componentContext;
    }

    /**
     * 添加字段快捷变量
     */
    private void addFieldShortcuts(RenderContext context, Table table) {
        // 主键字段名（复合主键时为逗号分隔）
        context.put("primaryKeyFields", table.getPrimarykeyFieldsNames());
        
        // 所有字段名（逗号分隔）
        context.put("fieldNames", table.getFieldNames());
        
        // 非主键字段列表
        context.put("nonPrimaryKeyFields", table.getFields().stream()
            .filter(f -> !f.isPrimarykey())
            .collect(java.util.stream.Collectors.toList()));
        
        // 普通字段（排除创建时间、更新时间等）
        context.put("commonFields", table.getFields().stream()
            .filter(f -> !f.isPrimarykey() && !isCommonField(f.getName()))
            .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 判断是否为公共字段
     */
    private boolean isCommonField(String fieldName) {
        String lowerName = fieldName.toLowerCase();
        return Arrays.asList(
            "create_time", "createTime",
            "update_time", "updateTime",
            "create_by", "createBy",
            "update_by", "updateBy",
            "deleted", "del_flag"
        ).contains(lowerName);
    }

    /**
     * 构建组件映射
     */
    private Map<String, Object> buildComponentMap() {
        Map<String, Object> componentMap = new HashMap<>();
        
        // 为所有组件添加默认值 false
        for (Component component : Component.values()) {
            componentMap.put(component.name().toLowerCase(), false);
        }
        
        // 如果组件被启用，设置为 true
        if (global.getComponents() != null) {
            for (Component component : global.getComponents()) {
                componentMap.put(component.name().toLowerCase(), true);
            }
        }
        
        return componentMap;
    }

    /**
     * 获取当前日期
     *
     * @return 格式化的当前日期
     */
    private String getCurrentDate() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

    /**
     * 获取全局配置
     *
     * @return 全局配置
     */
    public GlobalConf getGlobal() {
        return global;
    }

    /**
     * 获取数据源配置
     *
     * @return 数据源配置
     */
    public DataSourceConf getDataSource() {
        return dataSource;
    }

    /**
     * StringUtils 包装器
     * <p>
     * 用于在 FreeMarker 模板中调用 StringUtils 的静态方法
     * </p>
     */
    public static class StringUtilsBean {
        /**
         * 查找字符在字符串中的位置
         */
        public int indexOf(String str, int searchChar) {
            return org.apache.commons.lang.StringUtils.indexOf(str, (char) searchChar);
        }

        /**
         * 查找子字符串在字符串中的位置
         */
        public int indexOf(String str, String searchStr) {
            return org.apache.commons.lang.StringUtils.indexOf(str, searchStr);
        }

        /**
         * 检查字符串是否为空
         */
        public boolean isEmpty(String str) {
            return org.hyw.tools.generator.utils.StringUtils.isEmpty(str);
        }

        /**
         * 检查字符串是否不为空
         */
        public boolean isNotEmpty(String str) {
            return org.hyw.tools.generator.utils.StringUtils.isNotEmpty(str);
        }

        /**
         * 检查字符串是否为空白
         */
        public boolean isBlank(String str) {
            return org.hyw.tools.generator.utils.StringUtils.isBlank(str);
        }

        /**
         * 检查字符串是否不为空白
         */
        public boolean isNotBlank(String str) {
            return org.hyw.tools.generator.utils.StringUtils.isNotBlank(str);
        }

        /**
         * 去掉字符串两端的空格
         */
        public String trim(String str) {
            return org.hyw.tools.generator.utils.StringUtils.trim(str);
        }

        /**
         * 字符串相等
         */
        public boolean equals(String str1, String str2) {
            return org.hyw.tools.generator.utils.StringUtils.equals(str1, str2);
        }

        /**
             * 转换为驼峰命名
             */
            public String toCamelCase(String str, char separator) {
                return org.hyw.tools.generator.utils.StringUtils.toCamelCase(str, separator);
            }
        
            /**
             * 转换为驼峰命名（使用下划线分隔）
             */
            public String toCamelCase(String str) {
                return org.hyw.tools.generator.utils.StringUtils.toCamelCase(str, '_');
            }
        
            /**
        
                 * 首字母大写
        
                 */
        
                public String capitalFirst(String str) {
        
                    return org.hyw.tools.generator.utils.StringUtils.capitalFirst(str);
        
                }
        
            
        
                /**
        
                 * 首字母小写
        
                 */
        
                public String lowercaseFirst(String str) {
        
                    return org.hyw.tools.generator.utils.StringUtils.lowercaseFirst(str);
        
                }    }
}
