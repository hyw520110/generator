package org.hyw.tools.generator.template;

import java.io.Serializable;

public class TemplateDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String path;
    private String target;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}
