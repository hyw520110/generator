package org.hyw.tools.generator.enums;

/**
 * 路径语义化锚点枚举
 * 用于在模板路径中定义语义化的占位符，如 [sourceDirectory] 或 ${sourceDirectory}
 * 
 * @author heyiwu
 */
public enum PathAnchor {
    /** Java 源码目录 */
    SOURCE("sourceDirectory"),
    /** 资源文件目录 */
    RESOURCE("resourceDirectory"),
    /** 测试源码目录 */
    TEST_SOURCE("testSourceDirectory"),
    /** 测试资源目录 */
    TEST_RESOURCE("testResourceDirectory");

    private final String key;

    PathAnchor(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * 获取方括号锚点格式，如 [sourceDirectory]
     */
    public String getBracketAnchor() {
        return "[" + key + "]";
    }

    /**
     * 获取占位符格式，如 ${sourceDirectory}
     */
    public String getPlaceholder() {
        return "${" + key + "}";
    }

    /**
     * 根据 key 获取枚举实例
     */
    public static PathAnchor fromKey(String key) {
        for (PathAnchor anchor : values()) {
            if (anchor.key.equals(key)) {
                return anchor;
            }
        }
        return null;
    }
}
