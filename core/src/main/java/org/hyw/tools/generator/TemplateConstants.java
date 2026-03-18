package org.hyw.tools.generator;

/**
 * 模板常量类
 * <p>
 * 集中管理模板相关的路径和配置常量
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public final class TemplateConstants {

    /**
     * 模板根目录
     */
    public static final String TEMPLATE_ROOT = "templates";

    /**
     * Velocity 模板目录
     */
    public static final String VELOCITY_DIR = "velocity";

    /**
     * FreeMarker 模板目录
     */
    public static final String FREEMARKER_DIR = "freemarker";

    /**
     * 公共组件目录
     */
    public static final String COMMONS_DIR = "commons";

    /**
     * 特定组件目录
     */
    public static final String COMPONENTS_DIR = "components";

    /**
     * 模块目录
     */
    public static final String MODULES_DIR = "modules";

    /**
     * 注释模板目录
     */
    public static final String COMMENTS_DIR = "comments";

    /**
     * 宏模板文件
     */
    public static final String MACROS_FILE = "macros.vm";

    /**
     * 注释模板文件
     */
    public static final String COMMENT_FILE = "comment.vm";

    /**
     * 模板文件扩展名
     */
    public static final String VELOCITY_EXTENSION = ".vm";

    public static final String FREEMARKER_EXTENSION = ".ftl";

    public static final String FREEMARKER_HTML_EXTENSION = ".ftlh";

    /**
     * 路径分隔符
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * 私有构造函数，防止实例化
     */
    private TemplateConstants() {
        // 常量类
    }

    /**
     * 构建模板路径
     *
     * @param parts 路径片段
     * @return 完整路径
     */
    public static String buildTemplatePath(String... parts) {
        if (parts == null || parts.length == 0) {
            return TEMPLATE_ROOT;
        }

        StringBuilder path = new StringBuilder(TEMPLATE_ROOT);
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                if (!path.toString().endsWith(PATH_SEPARATOR) && !part.startsWith(PATH_SEPARATOR)) {
                    path.append(PATH_SEPARATOR);
                }
                path.append(part);
            }
        }
        return path.toString();
    }

    /**
     * 判断是否为 Velocity 模板
     *
     * @param path 模板路径
     * @return 是否为 Velocity 模板
     */
    public static boolean isVelocityTemplate(String path) {
        return path != null && (
            path.endsWith(VELOCITY_EXTENSION) ||
            path.contains(VELOCITY_DIR)
        );
    }

    /**
     * 判断是否为 FreeMarker 模板
     *
     * @param path 模板路径
     * @return 是否为 FreeMarker 模板
     */
    public static boolean isFreeMarkerTemplate(String path) {
        return path != null && (
            path.endsWith(FREEMARKER_EXTENSION) ||
            path.endsWith(FREEMARKER_HTML_EXTENSION) ||
            path.contains(FREEMARKER_DIR)
        );
    }
}
