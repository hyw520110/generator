package org.hyw.tools.generator.template;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;

/**
 * 默认路径解析实现 (修复版：完善占位符支持与模块路由)
 *
 * @author heyiwu
 */
public class DefaultPathTemplateResolver implements PathTemplateResolver {

    private static final String SEPARATOR = "/";

    @Override
    public String resolve(String path, TemplateModel model) {
        if (StringUtils.isBlank(path)) return path;

        // 0. 预处理：精准剥离模板分类前缀
        String firstDir = StringUtils.substringBefore(path, SEPARATOR);
        boolean isVirtualDir = "modules".equals(firstDir) || "assets".equals(firstDir) 
                || "commons".equals(firstDir) || "components".equals(firstDir);
        
        if (isVirtualDir) {
            path = StringUtils.substringAfter(path, SEPARATOR);
        }

        // 1. Vue 路径特判
        if (path.startsWith(Component.VUE.name().toLowerCase() + SEPARATOR)) {
            return path;
        }

        // 2. 替换占位符 (含单模块架构自适应)
        path = replacePlaceholders(path, model);
        
        // 3. 标准化：处理 # 锚点并移除模板后缀
        String cleanPath = path.replace("#", SEPARATOR).replaceAll("\\.(ftl|vm)$", "");

        // 4. 核心路由判定
        
        // 4.1 全局根文件直通
        if (!cleanPath.contains(SEPARATOR)) {
            return cleanPath;
        }

        if (cleanPath.endsWith(".java")) {
            // 4.2 Java 智能路由 (优先识别 src/ 锚点)
            if (cleanPath.contains("src/")) {
                String modulePart = StringUtils.substringBefore(cleanPath, "src/");
                String afterSrcPart = cleanPath.substring(modulePart.length());
                if (afterSrcPart.contains("java/")) {
                    String baseDir = StringUtils.substringBefore(afterSrcPart, "java/") + "java";
                    String subPath = StringUtils.substringAfter(afterSrcPart, "java" + SEPARATOR);
                    return buildFullJavaPath(modulePart, baseDir, subPath, model);
                }
            }
            // 4.3 Java Legacy 路由
            return buildJavaPath(cleanPath, model, "commons".equals(firstDir));
        } else {
            // 4.4 非 Java 路由
            if (cleanPath.contains("src/")) {
                return cleanPath;
            }
            
            String firstPart = StringUtils.substringBefore(cleanPath, SEPARATOR);
            boolean isRootModule = isModule(firstPart, model.getConfig().getModules()) 
                    || "parent".equalsIgnoreCase(firstPart) 
                    || "commons".equalsIgnoreCase(firstPart)
                    || Component.VUE.name().equalsIgnoreCase(firstPart);

            if (isRootModule) {
                return cleanPath;
            }
            
            // 否则视为资源，补全 src/main/resources
            return buildResourcePath(cleanPath, model, true);
        }
    }

    private String buildFullJavaPath(String module, String baseDir, String subPath, TemplateModel model) {
        String rootPackage = model.getRootPackage() != null ? model.getRootPackage().replace(".", SEPARATOR) : "";
        String projectName = model.getProjectName() != null ? model.getProjectName() : "";
        
        String moduleClean = module.endsWith(SEPARATOR) ? module.substring(0, module.length() - 1) : module;
        String currentModuleName = StringUtils.substringAfterLast(moduleClean, SEPARATOR);
        if (StringUtils.isBlank(currentModuleName)) currentModuleName = moduleClean;

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

        path = replaceSemanticPlaceholders(path, model);
        path = replaceEntityPlaceholder(path, model);

        path = path.replace("\\", SEPARATOR).replace("//", SEPARATOR);
        if (path.startsWith(SEPARATOR)) path = path.substring(1);
        return path;
    }

    private String replaceSemanticPlaceholders(String path, TemplateModel model) {
        if (model.getModuleName() != null) path = path.replace("${moduleName}", model.getModuleName());
        if (model.getTable() != null) {
            String beanName = model.getTable().getBeanName();
            path = path.replace("${beanName}", beanName);
            path = path.replace("${EntityName}", beanName);
            path = path.replace("${table.beanName}", beanName); // 修复：支持 ${table.beanName}
            path = path.replace("${entityName}", StringUtils.uncapitalize(beanName));
        }
        if (model.getProjectName() != null) path = path.replace("${projectName}", model.getProjectName());
        if (model.getRootPackage() != null) path = path.replace("${packagePath}", model.getRootPackage().replace(".", SEPARATOR));
        return path;
    }

    private String buildJavaPath(String path, TemplateModel model, boolean isCommons) {
        GlobalConf global = model.getConfig();
        String cleanPath = path.replaceAll("\\.(ftl|vm)$", "");
        String subPath = StringUtils.substringAfter(cleanPath, SEPARATOR);
        String firstModule = extractFirstModule(subPath, global.getModules());
        if (firstModule != null) subPath = StringUtils.substringAfter(subPath, firstModule + SEPARATOR);
        boolean isTest = subPath.startsWith("test/");
        if (isTest) subPath = StringUtils.substringAfter(subPath, "test/");

        String moduleName = firstModule != null ? firstModule : (isCommons ? "commons" : "api");
        return toPath(moduleName, isTest ? global.getTestSourceDirectory() : global.getSourceDirectory(), 
                global.getRootPackage().replace(".", SEPARATOR), global.getProjectName(), moduleName, subPath);
    }
    
    private String buildResourcePath(String path, TemplateModel model, boolean isCommons) {
        GlobalConf global = model.getConfig();
        String cleanPath = path.replaceAll("\\.(ftl|vm)$", "");
        String subPath = StringUtils.substringAfter(cleanPath, SEPARATOR);
        String firstModule = extractFirstModule(subPath, global.getModules());
        if (firstModule != null) subPath = StringUtils.substringAfter(subPath, firstModule + SEPARATOR);
        boolean isTest = subPath.startsWith("test/");
        if (isTest) subPath = StringUtils.substringAfter(subPath, "test/");

        String moduleName = firstModule != null ? firstModule : (isCommons ? "commons" : "api");
        return toPath(moduleName, isTest ? global.getTestResourceDirectory() : global.getResourceDirectory(), subPath);
    }

    private String replaceEntityPlaceholder(String path, TemplateModel model) {
        if (path == null || model == null || model.getTable() == null) return path;
        String beanName = model.getTable().getBeanName();
        int count = StringUtils.countMatches(path, "%s");
        if (count == 0) return path;
        try {
            Object[] args = new Object[count];
            for (int i = 0; i < count; i++) args[i] = (i == 0 && count > 1) ? StringUtils.uncapitalize(beanName) : beanName;
            return String.format(path, args);
        } catch (Exception ignore) { return path; }
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
