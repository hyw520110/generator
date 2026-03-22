package org.hyw.tools.generator.template;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;

/**
 * 默认路径解析实现 (精简最终版：修复 Booter.java 及组件过滤逻辑)
 * 
 * @author heyiwu
 */
public class DefaultPathTemplateResolver implements PathTemplateResolver {

    private static final String SEPARATOR = "/";

    @Override
    public String resolve(String path, TemplateModel model) {
        if (StringUtils.isBlank(path)) return path;

        // 预处理：剥离一级分类前缀 (如 modules/, components/, assets/ 等)
        if (path.contains(SEPARATOR)) {
            path = StringUtils.substringAfter(path, SEPARATOR);
        }

        // 过滤逻辑
        if (shouldSkip(path, model)) {
            return null;
        }

        // 替换占位符 (必须在 Vue 特判之前)
        path = replacePlaceholders(path, model);

        // Vue 路径：移除模板后缀直接返回
        if (path.startsWith(Component.VUE.name().toLowerCase() + SEPARATOR)) {
            return path.replaceAll("\\.(ftl|vm)$", "");
        }
        
        // 标准化路径：处理 # 锚点并移除模板后缀
        String cleanPath = path.replace("#", SEPARATOR).replaceAll("\\.(ftl|vm)$", "");

        if (!cleanPath.contains(SEPARATOR)) return cleanPath;

        // Java 源码：智能路由，注入包路径
        if (cleanPath.endsWith(".java")) {
            if (cleanPath.contains("src/") && cleanPath.contains("java/")) {
                String modulePart = StringUtils.substringBefore(cleanPath, "src/");
                String afterSrcPart = cleanPath.substring(modulePart.length());
                String baseDir = StringUtils.substringBefore(afterSrcPart, "java/") + "java";
                String subPath = StringUtils.substringAfter(afterSrcPart, "java" + SEPARATOR);
                
                subPath = skipKnownComponentInPackage(subPath);
                
                return buildFullJavaPath(modulePart, baseDir, subPath, model);
            }
        }

        // 显式包含 src/ 的路径，直通返回
        if (cleanPath.contains("src/")) {
            return cleanPath;
        }
        
        // 以模块名或 parent 开头的路径，直通返回
        String firstPart = StringUtils.substringBefore(cleanPath, SEPARATOR);
        if (isModule(firstPart, model.getConfig().getModules()) 
                || "parent".equalsIgnoreCase(firstPart)) {
            return cleanPath;
        }
        
        // 兜底：补全 src/main/resources
        return buildResourcePath(cleanPath, model);
    }

    private boolean shouldSkip(String path, TemplateModel model) {
        String[] modules = model.getConfig().getModules();
        boolean isMultiModule = modules != null && modules.length > 1;

        // 单模块模式下，跳过 parent 目录
        if (!isMultiModule && path.startsWith("parent" + SEPARATOR)) {
            return true;
        }

        // 检查技术组件：路径中包含未启用的技术关键字则跳过
        String[] parts = path.split(SEPARATOR);
        for (String part : parts) {
            String p = part.toLowerCase();
            if (isKnownComponent(p)) {
                Component comp = getComponentByName(p);
                if (comp != null && !hasComponentOrImplicit(model, comp)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查组件是否启用，包括隐式启用的组件
     * 当 VUE 或 SPRINGMVC 启用时，JWT 组件自动隐式启用
     */
    private boolean hasComponentOrImplicit(TemplateModel model, Component comp) {
        if (model.hasComponent(comp)) {
            return true;
        }
        // JWT 组件隐式启用条件：VUE 或 SPRINGMVC 启用
        if (comp == Component.JWT) {
            return model.hasComponent(Component.VUE) || model.hasComponent(Component.SPRINGMVC);
        }
        return false;
    }

    private Component getComponentByName(String name) {
        for (Component c : Component.values()) {
            if (c.name().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    private String skipKnownComponentInPackage(String subPath) {
        String firstPart = StringUtils.substringBefore(subPath, SEPARATOR);
        if (isKnownComponent(firstPart)) {
            return StringUtils.substringAfter(subPath, SEPARATOR);
        }
        return subPath;
    }

    private boolean isKnownComponent(String part) {
        String p = part.toLowerCase();
        // 动态遍历枚举，确保新增加的组件能被自动识别
        for (Component component : Component.values()) {
            if (component.name().equalsIgnoreCase(p)) {
                return true;
            }
        }
        // 兼容一些不是枚举但作为子路径存在的技术名
        return p.equals("springboot") || p.equals("springmvc") || p.equals("jwt") || p.equals("parent");
    }

    private String buildFullJavaPath(String module, String baseDir, String subPath, TemplateModel model) {
        if (subPath.contains("${packagePath}")) {
            return toPath(module, baseDir, subPath);
        }
        String rootPackage = model.getRootPackage() != null ? model.getRootPackage().replace(".", SEPARATOR) : "";
        String projectName = model.getProjectName() != null ? model.getProjectName() : "";
        String moduleClean = module.endsWith(SEPARATOR) ? module.substring(0, module.length() - 1) : module;
        String currentModuleName = StringUtils.substringAfterLast(moduleClean, SEPARATOR);
        if (StringUtils.isBlank(currentModuleName)) currentModuleName = moduleClean;
        if (StringUtils.isBlank(currentModuleName)) currentModuleName = "commons";

        return toPath(module, baseDir, rootPackage, projectName, currentModuleName, subPath);
    }

    private String replacePlaceholders(String path, TemplateModel model) {
        if (path == null) return null;
        GlobalConf global = model.getConfig();
        String[] modules = global.getModules();
        boolean isMultiModule = modules != null && modules.length > 1;

        if (isMultiModule) {
            for (int i = 0; i < modules.length; i++) {
                path = path.replace("{" + i + "}", modules[i]);
                path = path.replace("${module[" + i + "]}", modules[i]);
            }
        } else {
            path = path.replaceFirst("^\\{[0-9]+\\}/?", "");
            path = path.replaceFirst("^\\$\\{module\\[[0-9]+\\]\\}/?", "");
        }

        if (model.getTable() != null) {
            String beanName = model.getTable().getBeanName();
            path = path.replace("${beanName}", beanName)
                       .replace("${EntityName}", beanName)
                       .replace("${table.beanName}", beanName)
                       .replace("${entityName}", StringUtils.uncapitalize(beanName))
                       // 支持 %s 占位符格式 (如 %s.java.ftl, %sMapper.java.ftl)
                       .replace("%s", beanName);
        }
        
        if (model.getProjectName() != null) path = path.replace("${projectName}", model.getProjectName());
        if (model.getRootPackage() != null) path = path.replace("${packagePath}", model.getRootPackage().replace(".", SEPARATOR));

        path = path.replace("\\", SEPARATOR).replace("//", SEPARATOR);
        if (path.startsWith(SEPARATOR)) path = path.substring(1);
        return path;
    }

    private String buildJavaPath(String path, TemplateModel model) {
        GlobalConf global = model.getConfig();
        String subPath = path;
        String firstModule = extractFirstModule(path, global.getModules());
        String moduleName = firstModule != null ? firstModule : "api";
        if (firstModule != null) subPath = StringUtils.substringAfter(path, firstModule + SEPARATOR);

        return toPath(moduleName, global.getSourceDirectory(), 
                global.getRootPackage().replace(".", SEPARATOR), global.getProjectName(), moduleName, subPath);
    }
    
    private String buildResourcePath(String path, TemplateModel model) {
        GlobalConf global = model.getConfig();
        String firstModule = extractFirstModule(path, global.getModules());
        String moduleName = firstModule != null ? firstModule : "api";
        String subPath = firstModule != null ? StringUtils.substringAfter(path, firstModule + SEPARATOR) : path;

        return toPath(moduleName, global.getResourceDirectory(), subPath);
    }

    private boolean isModule(String name, String[] modules) {
        if (modules == null) return false;
        for (String m : modules) if (m.equalsIgnoreCase(name)) return true;
        return false;
    }

    private String extractFirstModule(String path, String[] modules) {
        if (modules == null) return null;
        for (String part : path.split(SEPARATOR)) if (isModule(part, modules)) return part;
        return null;
    }

    private String toPath(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (String s : segments) {
            if (StringUtils.isNotBlank(s)) {
                if (sb.length() > 0 && !sb.toString().endsWith(SEPARATOR)) sb.append(SEPARATOR);
                sb.append(s.startsWith(SEPARATOR) ? s.substring(1) : s);
            }
        }
        return sb.toString();
    }
}
