package org.hyw.tools.generator.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模板引擎类型枚举
 * <p>
 * 定义模板引擎的类型、文件后缀等信息
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Getter
@AllArgsConstructor
public enum EngineType {

    /**
     * Velocity 模板引擎
     * 后缀：.vm
     */
    VELOCITY("velocity", ".vm", "org.apache.velocity.app.VelocityEngine"),

    /**
     * FreeMarker 模板引擎
     * 后缀：.ftl
     */
    FREEMARKER("freemarker", ".ftl", "freemarker.template.Configuration");

    /**
     * 引擎名称
     */
    private final String name;

    /**
     * 模板文件后缀
     */
    private final String extension;

    /**
     * 引擎类名（用于检测是否可用）
     */
    private final String engineClass;

    /**
     * 根据名称获取引擎类型
     *
     * @param name 引擎名称
     * @return 引擎类型
     */
    public static EngineType fromName(String name) {
        if (name == null) {
            return VELOCITY; // 默认
        }
        for (EngineType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return VELOCITY;
    }

    /**
     * 根据文件后缀获取引擎类型
     *
     * @param extension 文件后缀
     * @return 引擎类型
     */
    public static EngineType fromExtension(String extension) {
        if (extension == null) {
            return VELOCITY;
        }
        for (EngineType type : values()) {
            if (type.extension.equalsIgnoreCase(extension)) {
                return type;
            }
        }
        return VELOCITY;
    }

    /**
     * 判断是否为支持的模板引擎
     *
     * @param name 引擎名称
     * @return 是否支持
     */
    public static boolean isSupported(String name) {
        if (name == null) {
            return false;
        }
        for (EngineType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取不带后缀的文件路径
     *
     * @param path 文件路径
     * @return 不带后缀的路径
     */
    public String removeExtension(String path) {
        if (path == null) {
            return null;
        }
        if (path.endsWith(extension)) {
            return path.substring(0, path.length() - extension.length());
        }
        return path;
    }

    /**
     * 添加后缀到文件路径
     *
     * @param path 文件路径
     * @return 带后缀的路径
     */
    public String addExtension(String path) {
        if (path == null) {
            return null;
        }
        if (!path.endsWith(extension)) {
            return path + extension;
        }
        return path;
    }
}
