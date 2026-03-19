package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.List;

public class TemplateOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String engine;
    private List<TemplateDefinition> templates;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }
    public List<TemplateDefinition> getTemplates() { return templates; }
    public void setTemplates(List<TemplateDefinition> templates) { this.templates = templates; }
}
