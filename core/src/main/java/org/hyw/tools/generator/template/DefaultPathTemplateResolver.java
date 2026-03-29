package org.hyw.tools.generator.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.PathAnchor;
import org.hyw.tools.generator.utils.FileUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认路径解析实现 (逻辑对齐最终修复版)
 * 1. 自动剥离组件名：递归剔除路径中的技术组件名 (如 /mybatis/, /springboot/)。
 * 2. 精准包名注入：支持模块名动态注入到 Java 包路径。
 * 3. 彻底解析锚点：# 替换为 /。
 */
@Slf4j
public class DefaultPathTemplateResolver implements PathTemplateResolver {


    private static final String SEPARATOR = Consts.PATH_SEPARATOR;

    @Override
    public String resolve(String path, TemplateModel model) {
        if (StringUtils.isBlank(path)) return path;

        // 1. 预处理：归一化并剥离分类前缀 (assets/, modules/, components/)
        String resolvedPath = FileUtils.normalizePath(path);

        // 智能剥离：剥离 modules/ 和 components/ 前缀
        if (resolvedPath.startsWith(Consts.DIR_MODULES + SEPARATOR) || resolvedPath.startsWith(Consts.DIR_COMPONENTS + SEPARATOR)) {
            resolvedPath = StringUtils.substringAfter(resolvedPath, SEPARATOR);
        }

        // 2. 占位符替换 (处理 {0}, {1}, ${beanName} 等) - 在剥离assets前缀之前先替换占位符
        resolvedPath = replacePlaceholders(resolvedPath, model);

        // 3. 智能剥离：剥离公共静态资源目录的 assets/ 前缀（在占位符替换后）
        if (resolvedPath.startsWith(Consts.ASSETS_DIR + SEPARATOR)) {
            resolvedPath = StringUtils.substringAfter(resolvedPath, SEPARATOR);
        }

        // 4. 标准化处理：移除模板后缀（保留 ## 标记）
        String cleanPath = FileUtils.normalizePath(resolvedPath.replaceAll(Consts.TEMPLATE_EXT_REGEX, ""));

        // 5. 过滤逻辑：跳过模板片段文件（以_开头）和不应生成的文件
        if (shouldSkip(cleanPath, model)) {
//            log.debug("路径解析 - [跳过]: {}", cleanPath);
            return null;
        }

        // 6. 过滤模板片段文件：文件名以_开头的不应生成
        if (isTemplateFragment(cleanPath)) {
            log.debug("路径解析 - [跳过模板片段]: {}", cleanPath);
            return null;
        }

        // 5. 递归剥离路径中的技术组件名 (例如：app/springboot/src/main -> app/src/main)
        String strippedPath = stripAllComponentNames(cleanPath, model);
        if (!cleanPath.equals(strippedPath)) {
            log.debug("路径解析 - [剥离组件]: {} -> {}", cleanPath, strippedPath);
        }
        cleanPath = strippedPath;

        // 6. Java 源码路由 (通用逻辑：识别 java/ 路径即注入包名)
        if (cleanPath.endsWith(Consts.EXT_JAVA) && cleanPath.contains(Consts.DIR_JAVA + SEPARATOR)) {
            String javaPath = handleJavaSourcePath(cleanPath, model);
            log.debug("路径解析 - [Java源码]: {} -> {}", cleanPath, javaPath);
            return javaPath;
        }

        // 7. 模块路由逻辑
        String firstPart = StringUtils.substringBefore(cleanPath, SEPARATOR);
        String[] modules = model.getConfig().getModules();
        
        // A. 脚本或根文件直通
        if (isModule(firstPart, modules) || Consts.DIR_PARENT.equalsIgnoreCase(firstPart)) {
            if (StringUtils.countMatches(cleanPath, SEPARATOR) == 1 && !cleanPath.endsWith(Consts.EXT_JAVA)) {
                return cleanPath;
            }
        }

        // B. 显式结构直通
        if (isModule(firstPart, modules) || Consts.DIR_PARENT.equalsIgnoreCase(firstPart) || cleanPath.contains(Consts.DIR_SRC + "/")) {
            return cleanPath;
        }
        
        // C. 兜底逻辑：返回处理后的路径
        String finalPath = FileUtils.normalizePath(cleanPath);
        log.debug("路径解析 - [完成]: {} -> {}", path, finalPath);
        return finalPath;
    }

    private String handleJavaSourcePath(String path, TemplateModel model) {
        String packagePath = model.getRootPackage() != null ? model.getRootPackage().replace(".", SEPARATOR) : "";
        String projectName = model.getProjectName();
        String moduleName = model.getModuleName();

        // 定位 java/ 所在位置
        String prefix = StringUtils.substringBeforeLast(path, Consts.DIR_JAVA + SEPARATOR) + Consts.DIR_JAVA + SEPARATOR;
        String suffix = StringUtils.substringAfterLast(path, Consts.DIR_JAVA + SEPARATOR);

        // 检查 suffix 中是否已经包含包名路径
        // 改进的逻辑：检查suffix的各个部分是否与包名、项目名、模块名匹配
        boolean hasPackageName = false;
        if (StringUtils.isNotBlank(packagePath)) {
            String[] packageParts = packagePath.split(SEPARATOR);
            String[] suffixParts = suffix.split(SEPARATOR);
            
            // 检查suffix是否以包名开头
            int matchCount = 0;
            for (int i = 0; i < packageParts.length && i < suffixParts.length; i++) {
                if (packageParts[i].equals(suffixParts[i])) {
                    matchCount++;
                } else {
                    break;
                }
            }
            
            // 如果匹配了完整的包名，则认为已经包含了包路径
            if (matchCount == packageParts.length) {
                hasPackageName = true;
            }
        }

        StringBuilder sb = new StringBuilder(prefix);
        if (!hasPackageName) {
            // 如果 suffix 中没有包含包名，则添加标准包名结构
            if (StringUtils.isNotBlank(packagePath)) sb.append(packagePath).append(SEPARATOR);
            if (StringUtils.isNotBlank(projectName)) sb.append(projectName).append(SEPARATOR);
            if (StringUtils.isNotBlank(moduleName)) {
                sb.append(moduleName).append(SEPARATOR);
            }
        }
        sb.append(suffix);

        return FileUtils.normalizePath(sb.toString());
    }
    private String stripAllComponentNames(String path, TemplateModel model) {
        String[] parts = path.split(SEPARATOR);
        String[] modules = model.getConfig().getModules();
        List<String> resultParts = new ArrayList<>();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            // 第一级如果是模块名或 parent，则必须保留
            if (i == 0 && (isModule(part.toLowerCase(), modules) || Consts.DIR_PARENT.equalsIgnoreCase(part))) {
                resultParts.add(part);
                continue;
            }
            
            // 如果路径部分以 # 开头和结尾，则去掉 ## 标记
            if (part.startsWith("#") && part.endsWith("#")) {
                String componentName = part.substring(1, part.length() - 1);
                log.debug("剥离组件: {} ({})", part, componentName);
                // 组件名被剥离，跳过该部分
                continue;
            }
            
            resultParts.add(part);
        }
        
        return String.join(SEPARATOR, resultParts);
    }

    private boolean shouldSkip(String path, TemplateModel model) {
        String[] modules = model.getConfig().getModules();
        // parent 是聚合模块结构：单模块工程跳过
        if (path.startsWith(Consts.DIR_PARENT + SEPARATOR) && (modules == null || modules.length <= 1)) {
            return true;
        }

        String[] parts = path.split(SEPARATOR);
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            // 忽略模块名、聚合模块名及标准目录的过滤干扰
            if (i == 0 && (isModule(p.toLowerCase(), modules) || Consts.DIR_PARENT.equalsIgnoreCase(p))) continue;
            if (p.equalsIgnoreCase(Consts.DIR_SRC) || p.equalsIgnoreCase(Consts.DIR_MAIN) || p.equalsIgnoreCase(Consts.DIR_TEST) || p.equalsIgnoreCase(Consts.DIR_JAVA) || p.equalsIgnoreCase(Consts.DIR_RESOURCES)) continue;

            // 处理 ## 标记的组件名（如 #jpa# -> jpa）
            String componentName = p;
            if (p.startsWith("#") && p.endsWith("#")) {
                componentName = p.substring(1, p.length() - 1);
            }

            Component comp = Component.getComponent(componentName.toLowerCase());
            if (comp != null && !hasComponentOrImplicit(model, comp)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasComponentOrImplicit(TemplateModel model, Component comp) {
        if (model.hasComponent(comp)) return true;
        // JWT 隐式启用条件
        if (comp == Component.JWT) {
            return model.hasComponent(Component.VUE) || model.hasComponent(Component.SPRINGMVC);
        }
        return false;
    }

    private String replacePlaceholders(String path, TemplateModel model) {
        if (path == null) return null;
        GlobalConf config = model.getConfig();
        String[] modules = config.getModules();
        
        // 1. 处理模块占位符 {0}, {1}...
        if (modules != null && modules.length > 0) {
            for (int i = 0; i < modules.length; i++) {
                path = path.replace(Consts.PATH_PLACEHOLDER_START + i + Consts.PATH_PLACEHOLDER_END, modules[i]);
                path = path.replace(Consts.MODULE_PLACEHOLDER_PREFIX + i + Consts.MODULE_PLACEHOLDER_SUFFIX, modules[i]);
            }
        } else {
            path = path.replaceAll(Consts.NUMBER_PLACEHOLDER_REGEX, "");
            path = path.replaceAll(Consts.MODULE_PLACEHOLDER_REGEX, "");
        }

        // 2. 处理目录占位符
        for (PathAnchor anchor : PathAnchor.values()) {
            String dirValue = getDirValueByAnchor(anchor, config);
            path = replaceDirectoryPlaceholder(path, anchor, dirValue);
        }

        // 3. 处理业务占位符
        if (model.getNaming() != null) {
            Map<String, String> naming = model.getNaming();
            path = path.replace("${beanName}", naming.get("entity"))
                       .replace("${EntityName}", naming.get("entity"))
                       .replace("${table.beanName}", naming.get("entity"))
                       .replace("${entityName}", naming.get("entityLower"))
                       .replace("${mapperName}", naming.get("mapper"))
                       .replace("${serviceName}", naming.get("service"))
                       .replace("${controllerName}", naming.get("controller"))
                       .replace("%s", naming.get("entity"));
        }
        
        if (model.getProjectName() != null) path = path.replace("${projectName}", model.getProjectName());
        if (model.getRootPackage() != null) path = path.replace("${packagePath}", model.getRootPackage().replace(".", SEPARATOR));
        
        // 如果依然包含 ${ 占位符，说明缺少必要的上下文（如渲染全局资源时遇到了业务占位符），抛出异常以静默跳过
        if (path.contains(Consts.PATH_PLACEHOLDER_START + "$") || path.contains("${")) {
            throw new RuntimeException("Unresolved placeholders in path: " + path);
        }

        return FileUtils.normalizePath(path);
    }

    private String getDirValueByAnchor(PathAnchor anchor, GlobalConf config) {
        switch (anchor) {
            case SOURCE: return config.getSourceDirectory();
            case RESOURCE: return config.getResourceDirectory();
            case TEST_SOURCE: return config.getTestSourceDirectory();
            case TEST_RESOURCE: return config.getTestResourceDirectory();
            default: return null;
        }
    }

    private String replaceDirectoryPlaceholder(String path, PathAnchor anchor, String dirValue) {
        if (dirValue == null) return path;
        // 将包名点号规约为路径分隔符，并统一格式
        String normalizedDir = dirValue.replace(".", SEPARATOR).replace("\\", SEPARATOR).replace("/", SEPARATOR);
        path = path.replace(anchor.getBracketAnchor(), normalizedDir);
        path = path.replace(anchor.getPlaceholder(), normalizedDir);
        return path;
    }

    private boolean isModule(String name, String[] modules) {
        if (modules == null || StringUtils.isBlank(name)) return false;
        for (String m : modules) if (m.equalsIgnoreCase(name)) return true;
        return false;
    }

    /**
     * 检查路径是否是模板片段文件（文件名以_开头）
     * 模板片段文件不应被生成，因为它们是被其他模板引用的片段
     */
    private boolean isTemplateFragment(String path) {
        if (StringUtils.isBlank(path)) return false;
        
        String[] parts = path.split(SEPARATOR);
        if (parts.length == 0) return false;
        
        // 获取文件名（最后一个部分）
        String fileName = parts[parts.length - 1];
        
        // 检查文件名是否以_开头
        if (fileName.startsWith("_")) {
            return true;
        }
        
        // 检查是否是_开头的目录
        for (String part : parts) {
            if (part.startsWith("_")) {
                return true;
            }
        }
        
        return false;
    }
}
