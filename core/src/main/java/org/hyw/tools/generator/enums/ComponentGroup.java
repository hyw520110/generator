package org.hyw.tools.generator.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hyw.tools.generator.utils.ConfigValidator.ValidationResult;

/**
 * 组件互斥组定义
 * 
 * 用于管理组件之间的互斥关系和依赖关系
 * 
 * @author heyiwu
 * @version 2.0
 */
public enum ComponentGroup {
    
    
    /**
     * ORM 框架组 - 持久层框架，必选且只能选一个
     */
    ORM("ORM 框架", true, Component.MYBATIS, Component.JPA),
    
    
    /**
     * 视图技术组 - 前端方案，可选但最多选一个
     */
    VIEW("视图技术", false, Component.VUE, Component.THYMELEAF),
    
    /**
     * 认证授权组 - 安全框架，可选但最多选一个
     */
    AUTH("认证授权", false, Component.SHIRO, Component.JWT),
    
    /**
     * 链路追踪组 - APM 系统，可选但最多选一个
     */
    TRACE("链路追踪", false, Component.ZIPKIN, Component.SKYWALKING),
    
    /**
     * 注册中心组 - 微服务注册中心，可选但最多选一个
     */
    REGISTRY("注册中心", false, Component.ZOOKEEPER, Component.NACOS),
    
    /**
     * 微服务框架组 - 可选但最多选一个
     */
    MICROSERVICE("微服务框架", false, Component.DUBBO, Component.SPRINGCLOUD);
    
    /**
     * 组名称
     */
    private final String groupName;
    
    /**
     * 是否必选
     */
    private final boolean required;
    
    /**
     * 组内组件列表（互斥）
     */
    private final List<Component> components;
    
    /**
    	 * 构造函数
    	 */
    	ComponentGroup(String groupName, boolean required, Component... components) {
    		this.groupName = groupName;
    		this.required = required;
    		this.components = Collections.unmodifiableList(Arrays.asList(components));
    	}

    public String getGroupName() {
        return groupName;
    }

    public boolean isRequired() {
        return required;
    }

    public List<Component> getComponents() {
        return components;
    }

    /**
     * 检查组件是否属于该组
     */
    public boolean contains(Component component) {
        return components.contains(component);
    }
    
    /**
     * 获取所有互斥组
     */
    public static List<ComponentGroup> getAllGroups() {
        return Collections.unmodifiableList(Arrays.asList(values()));
    }
    
    /**
     * 获取所有必选组
     */
    public static List<ComponentGroup> getRequiredGroups() {
        return Arrays.stream(values())
            .filter(ComponentGroup::isRequired)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有可选组
     */
    public static List<ComponentGroup> getOptionalGroups() {
        return Arrays.stream(values())
            .filter(g -> !g.isRequired())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据组件查找所属的组
     */
    public static ComponentGroup findByComponent(Component component) {
        for (ComponentGroup group : values()) {
            if (group.contains(component)) {
                return group;
            }
        }
        return null;
    }
    
    /**
     * 验证组件配置
     * 
     * @param selected 已选择的组件数组
     * @return 验证结果
     */
    public static ValidationResult validate(Component[] selected) {
        ValidationResult result = new ValidationResult();
        
        if (selected == null || selected.length == 0) {
            // 检查是否有必选组未选择
            for (ComponentGroup group : values()) {
                if (group.required) {
                    result.addError(String.format(
                        "【%s】必须选择一个组件，可选：%s",
                        group.groupName, group.components
                    ));
                }
            }
            return result;
        }
        
        Set<Component> selectedSet = Arrays.stream(selected)
            .collect(Collectors.toSet());
        
        for (ComponentGroup group : values()) {
            // 找出该组中被选中的组件
            List<Component> groupSelected = group.components.stream()
                .filter(selectedSet::contains)
                .collect(Collectors.toList());
            
            // 检查必选组
            if (group.required && groupSelected.isEmpty()) {
                result.addError(String.format(
                    "【%s】必须选择一个组件，可选：%s",
                    group.groupName, group.components
                ));
            }
            
            // 检查互斥冲突
            if (groupSelected.size() > 1) {
                result.addError(String.format(
                    "【%s】存在冲突，只能选择一个，当前选择了：%s",
                    group.groupName, groupSelected
                ));
            }
        }
        
        // 检查依赖关系
        validateDependencies(selectedSet, result);
        
        return result;
    }
    
    /**
     * 验证依赖关系
     */
    private static void validateDependencies(Set<Component> selected, ValidationResult result) {
        // 依赖规则集中定义：组件 -> 依赖的组件列表
        Map<Component, List<Component>> dependencies = Map.ofEntries(
            // Dubbo 依赖 Zookeeper 或 Nacos
            Map.entry(Component.DUBBO, List.of(Component.ZOOKEEPER, Component.NACOS)),
            
            // Vue 前端需要认证（Shiro 或 JWT）
            Map.entry(Component.VUE, List.of(Component.SHIRO, Component.JWT)),
            
            // Spring Cloud 需要 Spring Boot
            Map.entry(Component.SPRINGCLOUD, List.of(Component.SPRINGBOOT)),
            
            // Sentinel 需要 Spring Cloud 或 Dubbo
            Map.entry(Component.SENTINEL, List.of(Component.SPRINGCLOUD, Component.DUBBO)),
            
            // RocketMQ 需要微服务框架（Spring Cloud 或 Dubbo）
            Map.entry(Component.ROCKETMQ, List.of(Component.SPRINGCLOUD, Component.DUBBO)),
            
            // Redis 建议 Spring Boot 使用
            Map.entry(Component.REDIS, List.of(Component.SPRINGBOOT)),
            
            // Skywalking 需要 Spring Boot 或 Spring Cloud
            Map.entry(Component.SKYWALKING, List.of(Component.SPRINGBOOT, Component.SPRINGCLOUD)),
            
            // Zipkin 需要 Spring Boot 或 Spring Cloud
            Map.entry(Component.ZIPKIN, List.of(Component.SPRINGBOOT, Component.SPRINGCLOUD)),
            
            // Thymeleaf 需要 Spring MVC 或 Spring Boot
            Map.entry(Component.THYMELEAF, List.of(Component.SPRINGMVC, Component.SPRINGBOOT))
        );
        
        for (Map.Entry<Component, List<Component>> entry : dependencies.entrySet()) {
            Component component = entry.getKey();
            List<Component> requiredDeps = entry.getValue();
            
            if (selected.contains(component)) {
                // 检查是否至少有一个依赖被选中
                boolean hasDependency = requiredDeps.stream()
                    .anyMatch(selected::contains);
                
                if (!hasDependency) {
                    if (requiredDeps.size() == 1) {
                        result.addWarn(String.format(
                            "%s 需要配合 %s 使用",
                            component, requiredDeps.get(0)
                        ));
                    } else {
                        result.addWarn(String.format(
                            "%s 建议配合以下组件之一使用：%s",
                            component, requiredDeps
                        ));
                    }
                }
            }
        }
        
        // 检查已废弃或不推荐的组合
        checkDeprecatedCombinations(selected, result);
    }
    
    /**
     * 检查已废弃或不推荐的组合
     */
    private static void checkDeprecatedCombinations(Set<Component> selected, ValidationResult result) {
        // 检查是否同时选择了 Shiro 和 JWT（虽然不互斥，但通常二选一）
        if (selected.contains(Component.SHIRO) && selected.contains(Component.JWT)) {
            result.addInfo("Shiro 和 JWT 都提供了认证功能，建议根据实际需求选择其一");
        }
    }
    
    /**
     * 验证组件配置（带版本兼容性检查）
     * 
     * @param selected 已选择的组件数组
     * @param globalConf 全局配置对象（用于获取版本信息）
     * @return 验证结果
     */
    public static ValidationResult validate(Component[] selected, Object globalConf) {
        ValidationResult result = validate(selected);

        // 执行版本兼容性检查
        if (globalConf != null) {
            Set<Component> selectedSet = selected == null ? java.util.Collections.emptySet() :
                java.util.Arrays.stream(selected).collect(java.util.stream.Collectors.toSet());
            validateVersionCompatibility(selectedSet, globalConf, result);
        }

        return result;
    }
    
    /**
     * 验证版本兼容性
     */
    private static void validateVersionCompatibility(Set<Component> selected, Object globalConf, 
                                                     ValidationResult result) {
        try {
            // 使用反射获取版本信息
            var clazz = globalConf.getClass();
            
            // 获取 Spring Boot 版本
            String springBootVersion = null;
            String springCloudVersion = null;
            String springCloudAlibabaVersion = null;
            
            try {
                var componentsMethod = clazz.getMethod("getComponents");
                var components = (Map<?, Map<String, Object>>) componentsMethod.invoke(globalConf);
                
                // 获取 Spring Boot 版本
                if (components.containsKey(Component.SPRINGBOOT)) {
                    var bootConfig = components.get(Component.SPRINGBOOT);
                    springBootVersion = (String) bootConfig.get("springboot_version");
                }
                
                // 获取 Spring Cloud 版本
                if (components.containsKey(Component.SPRINGCLOUD)) {
                    var cloudConfig = components.get(Component.SPRINGCLOUD);
                    springCloudVersion = (String) cloudConfig.get("springcloud_version");
                    springCloudAlibabaVersion = (String) cloudConfig.get("springcloud_alibaba_version");
                }
            } catch (Exception e) {
                // 无法获取版本信息，跳过版本兼容性检查
                return;
            }
            
            // Spring Cloud Alibaba 版本兼容性检查
            if (selected.contains(Component.NACOS) && 
                selected.contains(Component.SPRINGCLOUD) &&
                springCloudAlibabaVersion != null) {
                
                // Spring Cloud Alibaba 2021.x 需要 Spring Boot 2.6.x+
                if (isVersionLessThan(springBootVersion, "2.6.0") && 
                    springCloudAlibabaVersion.startsWith("2021")) {
                    result.addError(String.format(
                        "Spring Cloud Alibaba %s 需要 Spring Boot 2.6.0 或更高版本，当前版本：%s",
                        springCloudAlibabaVersion, springBootVersion
                    ));
                }
            }
            
            // Sentinel 版本兼容性
            if (selected.contains(Component.SENTINEL) && 
                selected.contains(Component.SPRINGCLOUD)) {
                
                if (isVersionLessThan(springCloudVersion, "2020.0.0")) {
                    result.addWarn(String.format(
                        "Sentinel 与 Spring Cloud %s 的兼容性可能存在问题，建议使用 Spring Cloud 2020.0.0 或更高版本",
                        springCloudVersion
                    ));
                }
            }
            
            // Nacos 版本兼容性
            if (selected.contains(Component.NACOS) && 
                selected.contains(Component.SPRINGCLOUD)) {
                
                if (isVersionLessThan(springCloudVersion, "2020.0.0")) {
                    result.addWarn(String.format(
                        "Nacos 与 Spring Cloud %s 的兼容性可能存在问题，建议使用 Spring Cloud 2020.0.0 或更高版本",
                        springCloudVersion
                    ));
                }
            }
            
            // Skywalking 版本兼容性
            if (selected.contains(Component.SKYWALKING) && 
                selected.contains(Component.SPRINGBOOT)) {
                
                if (isVersionLessThan(springBootVersion, "2.3.0")) {
                    result.addWarn(String.format(
                        "Skywalking 与 Spring Boot %s 的兼容性可能存在问题，建议使用 Spring Boot 2.3.0 或更高版本",
                        springBootVersion
                    ));
                }
            }
            
        } catch (Exception e) {
            // 版本兼容性检查失败，不影响主要功能
            result.addInfo("版本兼容性检查失败：" + e.getMessage());
        }
    }
    
    /**
     * 比较版本号
     * 
     * @param version1 版本1
     * @param version2 版本2
     * @return version1 < version2 返回 true
     */
    private static boolean isVersionLessThan(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return false;
        }
        
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int length = Math.min(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int v1 = Integer.parseInt(parts1[i].replaceAll("[^0-9]", ""));
            int v2 = Integer.parseInt(parts2[i].replaceAll("[^0-9]", ""));
            
            if (v1 < v2) {
                return true;
            } else if (v1 > v2) {
                return false;
            }
        }
        
        return parts1.length < parts2.length;
    }
}
