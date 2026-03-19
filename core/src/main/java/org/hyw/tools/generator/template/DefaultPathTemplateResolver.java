package org.hyw.tools.generator.template;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;

/**
 * 默认路径解析实现
 *
 * @author heyiwu
 */
public class DefaultPathTemplateResolver implements PathTemplateResolver {

    private static final String SEPARATOR = "/";

    @Override
    public String resolve(String path, TemplateModel model) {
        GlobalConf global = model.getConfig();
        
        // 先替换 %s 占位符（实体名称）
        path = replaceEntityPlaceholder(path, model);
        
        // Vue 文件：不构建路径，直接返回
        if (path.startsWith(Component.VUE.name().toLowerCase() + SEPARATOR)) {
            return path;
        }
        
        // 检查是否为模块根目录的特殊文件（build.gradle, pom.xml, readme.md）
        String fileName = StringUtils.substringAfterLast(path, SEPARATOR);
        if (fileName.endsWith(".ftl")) {
            fileName = StringUtils.substringBeforeLast(fileName, ".ftl");
        }
        boolean isModuleFile = "build.gradle".equals(fileName) || "pom.xml".equals(fileName) || "readme.md".equals(fileName);
        
        // 如果是模块根目录的特殊文件，直接返回原路径
        if (isModuleFile) {
            // 移除 .ftl 后缀（如果有的话）
            String cleanPath = path;
            if (cleanPath.endsWith(".ftl")) {
                cleanPath = cleanPath.substring(0, cleanPath.length() - 4);
            }
            return cleanPath;
        }
        
        // 判断是否为 commons 或 components 路径
        boolean isCommons = path.startsWith("commons" + SEPARATOR);
        boolean isComponents = path.startsWith("components" + SEPARATOR);
        
        // 格式化路径中的模块变量 {0}, {1}（使用 MessageFormat）
        // 注意：对于 commons、components 路径，也要格式化其中的占位符
        path = formatPath(path, global.getModules());
        path = path.replace("\\", "/");
        
        // 根据文件类型构建路径（注意：此时 path 可能还包含 .ftl 后缀）
        boolean isJava = path.contains(".java") || path.contains(".java.");
        
        if (isJava) {
            // Java 文件：构建 src/main/java/... 路径
            return buildJavaPath(path, model, isCommons || isComponents);
        } else {
            // 资源文件：构建 src/main/resources/... 路径
            return buildResourcePath(path, model, isCommons || isComponents);
        }
    }

    /**
     * 格式化路径中的模块变量 {0}, {1}
     * 使用 MessageFormat 替换占位符
     */
    private String formatPath(String path, String[] modules) {
        if (modules == null || modules.length == 0) return path;
        try {
            return MessageFormat.format(path, (Object[]) modules);
        } catch (Exception e) {
            // 格式化失败，返回原始路径
            return path;
        }
    }
    
    /**
     * 构建 Java 文件路径
     * 格式: moduleName/src/main/java/rootPackage/subPath
     */
    private String buildJavaPath(String path, TemplateModel model, boolean isCommonsOrComponents) {
        GlobalConf global = model.getConfig();

        // 移除模板后缀
        String cleanPath = path.replaceAll("\\.(ftl|vm)$", "");

        if (isCommonsOrComponents) {
            // commons 或 components 路径：提取模块名后，将剩余部分作为包路径
            // 例如：commons/mybatis/api/entity/BaseEntity.java
            // 提取：mybatis/api/entity/BaseEntity.java
            String subPath = StringUtils.substringAfter(cleanPath, SEPARATOR);
            
            // 处理模块占位符：提取第一个模块名
            String firstModule = extractFirstModule(subPath, global.getModules());
            if (firstModule != null) {
                // 移除模块名，保留剩余路径
                subPath = StringUtils.substringAfter(subPath, firstModule + SEPARATOR);
            }

            // 判断是否为测试文件
            boolean isTest = subPath.startsWith("test/");
            if (isTest) {
                subPath = StringUtils.substringAfter(subPath, "test/");
            }

            // 构建完整路径：moduleName/src/main/java/rootPackage/projectName/moduleName/subPath
            String sourceDir = isTest ? global.getTestSourceDirectory() : global.getSourceDirectory();
            String rootPackage = global.getRootPackage().replace(".", SEPARATOR);
            String projectName = global.getProjectName();
            String moduleName = firstModule != null ? firstModule : "api";

            return toPath(moduleName, sourceDir, rootPackage, projectName, moduleName, subPath);
        } else {
            // 模块路径：正常处理
            // 提取第一个目录作为模块名
            String moduleName = StringUtils.substringBefore(cleanPath, SEPARATOR);

            // 提取子路径
            String subPath = StringUtils.substringAfter(cleanPath, SEPARATOR);
            String pathModuleName = StringUtils.substringBefore(subPath, SEPARATOR);
            subPath = StringUtils.substringAfter(subPath, pathModuleName + SEPARATOR);

            // 如果第二个目录也是模块名，则使用它
            if (isModule(pathModuleName, global.getModules())) {
                moduleName = pathModuleName;
            }

            // 判断是否为测试文件
            boolean isTest = subPath.startsWith("test/");
            if (isTest) {
                subPath = StringUtils.substringAfter(subPath, "test/");
            }

            // 构建完整路径：moduleName/src/main/java/rootPackage/projectName/moduleName/subPath
            String sourceDir = isTest ? global.getTestSourceDirectory() : global.getSourceDirectory();
            String rootPackage = global.getRootPackage().replace(".", SEPARATOR);
            String projectName = global.getProjectName();

            return toPath(moduleName, sourceDir, rootPackage, projectName, moduleName, subPath);
        }
    }
    
    /**
     * 构建资源文件路径
     * 格式: moduleName/src/main/resources/subPath
     */
    private String buildResourcePath(String path, TemplateModel model, boolean isCommonsOrComponents) {
        GlobalConf global = model.getConfig();

        // 移除模板后缀
        String cleanPath = path.replaceAll("\\.(ftl|vm)$", "");

        if (isCommonsOrComponents) {
            // commons 或 components 路径：提取模块名后，将剩余部分作为资源路径
            String subPath = StringUtils.substringAfter(cleanPath, SEPARATOR);
            
            // 处理模块占位符：提取第一个模块名
            String firstModule = extractFirstModule(subPath, global.getModules());
            if (firstModule != null) {
                // 移除模块名，保留剩余路径
                subPath = StringUtils.substringAfter(subPath, firstModule + SEPARATOR);
            }

            // 判断是否为测试资源
            boolean isTest = subPath.startsWith("test/");
            if (isTest) {
                subPath = StringUtils.substringAfter(subPath, "test/");
            }

            // 构建完整路径：moduleName/src/main/resources/subPath
            String resourceDir = isTest ? global.getTestResourceDirectory() : global.getResourceDirectory();

            return toPath(firstModule != null ? firstModule : "api", resourceDir, subPath);
        } else {
            // 模块路径：正常处理
            // 提取第一个目录作为模块名
            String moduleName = StringUtils.substringBefore(cleanPath, SEPARATOR);

            // 提取子路径
            String subPath = StringUtils.substringAfter(cleanPath, SEPARATOR);
            String pathModuleName = StringUtils.substringBefore(subPath, SEPARATOR);
            subPath = StringUtils.substringAfter(subPath, pathModuleName + SEPARATOR);

            // 如果第二个目录也是模块名，则使用它
            if (isModule(pathModuleName, global.getModules())) {
                moduleName = pathModuleName;
            }

            // 判断是否为测试资源
            boolean isTest = subPath.startsWith("test/");
            if (isTest) {
                subPath = StringUtils.substringAfter(subPath, "test/");
            }

            // 构建完整路径
            String resourceDir = isTest ? global.getTestResourceDirectory() : global.getResourceDirectory();

            return toPath(moduleName, resourceDir, subPath);
        }
    }

    /**
     * 替换路径中的 %s 占位符
     */
    private String replaceEntityPlaceholder(String path, TemplateModel model) {
        if (path == null || model == null) {
            return path;
        }

        String beanName = model.getTable() != null ? model.getTable().getBeanName() : null;
        String className = beanName; // 在代码生成器中，className 就是 beanName

        if (beanName == null && className == null) {
            return path;
        }

        // 统计 %s 占位符数量
        int placeholderCount = StringUtils.countMatches(path, "%s");
        
        if (placeholderCount == 0) {
            return path;
        }

        try {
            Object[] args;
            if (placeholderCount == 1) {
                // 单个 %s，使用 className（首字母大写）
                args = new Object[]{className != null ? className : beanName};
            } else if (placeholderCount == 2) {
                // 两个 %s，第一个用小写，第二个用大写
                String lower = (className != null ? StringUtils.uncapitalize(className) : beanName);
                String upper = (className != null ? className : StringUtils.capitalize(beanName));
                args = new Object[]{lower, upper};
            } else {
                // 多个 %s，使用实体名称填充
                args = new Object[placeholderCount];
                for (int i = 0; i < placeholderCount; i++) {
                    args[i] = className != null ? className : beanName;
                }
            }
            path = String.format(path, args);
        } catch (Exception e) {
            // 替换失败，记录日志但继续处理
        }

        return path;
    }

    private boolean isModule(String name, String[] modules) {
        if (modules == null) return false;
        for (String m : modules) {
            if (m.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * 从路径中提取第一个模块名
     */
    private String extractFirstModule(String path, String[] modules) {
        if (modules == null || modules.length == 0) {
            return null;
        }

        // 按分隔符分割路径
        String[] parts = path.split(SEPARATOR);

        // 查找第一个匹配的模块名
        for (String part : parts) {
            if (isModule(part, modules)) {
                return part;
            }
        }

        return null;
    }

    private String toPath(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            if (StringUtils.isNotBlank(segment)) {
                if (sb.length() > 0 && !sb.toString().endsWith(SEPARATOR)) {
                    sb.append(SEPARATOR);
                }
                sb.append(segment.startsWith(SEPARATOR) ? segment.substring(1) : segment);
            }
        }
        return sb.toString();
    }
}
