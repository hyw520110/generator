package org.hyw.tools.generator.template;

/**
 * 模板引擎类型枚举
 */
public enum EngineType {

    VELOCITY("velocity", ".vm", "org.apache.velocity.app.VelocityEngine"),
    FREEMARKER("freemarker", ".ftl", "freemarker.template.Configuration");

    private final String name;
    private final String extension;
    private final String engineClass;

    EngineType(String name, String extension, String engineClass) {
        this.name = name;
        this.extension = extension;
        this.engineClass = engineClass;
    }

    public String getName() { return name; }
    public String getExtension() { return extension; }
    public String getEngineClass() { return engineClass; }

    public static EngineType fromName(String name) {
        if (name == null) return VELOCITY;
        for (EngineType type : values()) {
            if (type.name.equalsIgnoreCase(name)) return type;
        }
        return VELOCITY;
    }

    public String removeExtension(String path) {
        if (path == null) return null;
        if (path.endsWith(extension)) {
            return path.substring(0, path.length() - extension.length());
        }
        return path;
    }
}
