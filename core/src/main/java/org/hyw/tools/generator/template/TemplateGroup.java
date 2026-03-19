package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.List;

public class TemplateGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private boolean required;
    private boolean exclusive;
    private List<TemplateDefinition> templates;
    private List<TemplateOption> options;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isExclusive() { return exclusive; }
    public void setExclusive(boolean exclusive) { this.exclusive = exclusive; }
    public List<TemplateDefinition> getTemplates() { return templates; }
    public void setTemplates(List<TemplateDefinition> templates) { this.templates = templates; }
    public List<TemplateOption> getOptions() { return options; }
    public void setOptions(List<TemplateOption> options) { this.options = options; }
}
