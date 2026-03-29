package org.hyw.tools.generator.template;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.EngineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * 模板配置加载器
 * <p>
 * 从 template-config.yaml 加载模板配置信息
 * 用于驱动模板选择和渲染
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class TemplateConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(TemplateConfigLoader.class);

    private static final String CONFIG_PATH = "/templates/template-config.yaml";

    private final TemplateConfig templateConfig;

    /**
     * 私有构造函数
     */
    private TemplateConfigLoader() {
        this.templateConfig = loadConfig();
    }

    /**
     * 获取单例实例
     *
     * @return 模板配置加载器实例
     */
    public static TemplateConfigLoader getInstance() {
        return TemplateConfigLoaderHolder.INSTANCE;
    }

    /**
     * 静态内部类单例
     */
    private static class TemplateConfigLoaderHolder {
        private static final TemplateConfigLoader INSTANCE = new TemplateConfigLoader();
    }

    /**
     * 加载配置文件
     *
     * @return 模板配置
     */
    private TemplateConfig loadConfig() {
        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            if (is == null) {
                logger.warn("模板配置文件 {} 未找到，将使用默认配置", CONFIG_PATH);
                return createDefaultConfig();
            }

            Yaml yaml = new Yaml();
            Map<String, Object> configMap = (Map<String, Object>) yaml.load(is);
            TemplateConfig config = convertToConfig(configMap);
            logger.info("模板配置文件加载成功");
            return config;

        } catch (Exception e) {
            logger.error("加载模板配置文件失败", e);
            return createDefaultConfig();
        }
    }

    /**
     * 转换为配置对象
     */
    private TemplateConfig convertToConfig(Map<String, Object> configMap) {
        TemplateConfig config = new TemplateConfig();

        if (configMap.containsKey("engine")) {
            Map<String, Object> engineMap = getMap(configMap, "engine");
            config.setDefaultEngine(getString(engineMap, "default"));
            config.setSupportedEngines(getStringList(engineMap, "support"));
        }

        if (configMap.containsKey("groups")) {
            List<Map<String, Object>> groupsMap = getList(configMap, "groups");
            List<TemplateGroup> groups = groupsMap.stream()
                .map(this::convertToGroup)
                .collect(java.util.stream.Collectors.toList());
            config.setTemplateGroups(groups);
        }

        if (configMap.containsKey("render")) {
            Map<String, Object> renderMap = getMap(configMap, "render");
            config.setResources(getStringList(renderMap, "resources"));
            config.setExcludeDirs(getStringList(renderMap, "excludeDirs"));
        }

        return config;
    }

    /**
     * 转换为模板组
     */
    private TemplateGroup convertToGroup(Map<String, Object> groupMap) {
        TemplateGroup group = new TemplateGroup();
        group.setId(getString(groupMap, "id"));
        group.setName(getString(groupMap, "name"));
        group.setRequired(getBoolean(groupMap, "required", false));
        group.setExclusive(getBoolean(groupMap, "exclusive", false));

        if (groupMap.containsKey("templates")) {
            List<Map<String, Object>> templatesMap = getList(groupMap, "templates");
            List<TemplateDefinition> templates = templatesMap.stream()
                .map(this::convertToTemplate)
                .collect(java.util.stream.Collectors.toList());
            group.setTemplates(templates);
        }

        if (groupMap.containsKey("options")) {
            List<Map<String, Object>> optionsMap = getList(groupMap, "options");
            List<TemplateOption> options = optionsMap.stream()
                .map(this::convertToOption)
                .collect(java.util.stream.Collectors.toList());
            group.setOptions(options);
        }

        return group;
    }

    /**
     * 转换为模板定义
     */
    private TemplateDefinition convertToTemplate(Map<String, Object> templateMap) {
        TemplateDefinition template = new TemplateDefinition();
        template.setName(getString(templateMap, "name"));
        template.setPath(getString(templateMap, "path"));
        template.setTarget(getString(templateMap, "target"));
        return template;
    }

    /**
     * 转换为模板选项
     */
    private TemplateOption convertToOption(Map<String, Object> optionMap) {
        TemplateOption option = new TemplateOption();
        option.setId(getString(optionMap, "id"));
        option.setEngine(getString(optionMap, "engine"));

        if (optionMap.containsKey("templates")) {
            List<Map<String, Object>> templatesMap = getList(optionMap, "templates");
            List<TemplateDefinition> templates = templatesMap.stream()
                .map(this::convertToTemplate)
                .collect(java.util.stream.Collectors.toList());
            option.setTemplates(templates);
        }

        return option;
    }

    /**
     * 安全获取 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object obj = source.get(key);
        return (obj instanceof Map) ? (Map<String, Object>) obj : Collections.emptyMap();
    }

    /**
     * 安全获取 List
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getList(Map<String, Object> source, String key) {
        Object obj = source.get(key);
        return (obj instanceof List) ? (List<T>) obj : Collections.emptyList();
    }

    /**
     * 安全获取 String
     */
    private String getString(Map<String, Object> source, String key) {
        Object obj = source.get(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * 安全获取 String List
     */
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> source, String key) {
        Object obj = source.get(key);
        return (obj instanceof List) ? (List<String>) obj : Collections.emptyList();
    }

    /**
     * 安全获取 Boolean
     */
    private boolean getBoolean(Map<String, Object> source, String key, boolean defaultValue) {
        Object obj = source.get(key);
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return Boolean.parseBoolean(obj.toString());
    }

    /**
     * 创建默认配置
     */
    private TemplateConfig createDefaultConfig() {
        TemplateConfig config = new TemplateConfig();
        config.setDefaultEngine(EngineType.VELOCITY.getName());
        List<String> support = new ArrayList<>();
        for (EngineType type : EngineType.values()) {
            support.add(type.getName());
        }
        config.setSupportedEngines(support);
        return config;
    }

    /**
     * 获取模板配置
     *
     * @return 模板配置
     */
    public TemplateConfig getTemplateConfig() {
        return templateConfig;
    }

    /**
     * 根据组件获取模板组
     *
     * @param component 组件
     * @return 模板组
     */
    public TemplateGroup getGroupByComponent(Component component) {
        if (templateConfig == null || templateConfig.getTemplateGroups() == null) {
            return null;
        }

        for (TemplateGroup group : templateConfig.getTemplateGroups()) {
            if (isGroupForComponent(group, component)) {
                return group;
            }
        }
        return null;
    }

    /**
     * 判断模板组是否对应指定组件
     */
    private boolean isGroupForComponent(TemplateGroup group, Component component) {
        if (group == null || component == null) {
            return false;
        }

        String groupId = group.getId() != null ? group.getId().toLowerCase() : "";
        String groupName = group.getName() != null ? group.getName().toLowerCase() : "";
        String componentName = component.name().toLowerCase();

        return groupId.contains(componentName) || groupName.contains(componentName);
    }

    /**
     * 获取指定组件的模板列表
     *
     * @param component 组件
     * @return 模板列表
     */
    public List<TemplateDefinition> getTemplatesForComponent(Component component) {
        TemplateGroup group = getGroupByComponent(component);
        if (group == null) {
            return Collections.emptyList();
        }

        // 如果有选项，根据组件选择对应选项
        if (group.getOptions() != null && !group.getOptions().isEmpty()) {
            for (TemplateOption option : group.getOptions()) {
                if (option.getId() != null && option.getId().equalsIgnoreCase(component.name())) {
                    return option.getTemplates() != null ? option.getTemplates() : Collections.emptyList();
                }
            }
        }

        return group.getTemplates() != null ? group.getTemplates() : Collections.emptyList();
    }
}
