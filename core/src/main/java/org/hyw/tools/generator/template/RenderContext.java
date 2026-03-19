package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;

public class RenderContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Object> variables = new HashMap<>();
    private TemplateModel model = new TemplateModel();

    public RenderContext put(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public RenderContext table(Table table) {
        this.model.setTable(table);
        this.variables.put("table", table);
        return this;
    }

    public RenderContext components(Set<Component> components) {
        this.model.setComponents(components);
        this.variables.put("components", components);
        return this;
    }

    public Map<String, Object> toVelocityContext() {
        Map<String, Object> context = new HashMap<>(variables);
        // 只在 model 中的值不为 null 时才覆盖 variables 中的值
        if (model.getTable() != null) {
            context.put("table", model.getTable());
        }
        if (model.getConfig() != null) {
            context.put("config", model.getConfig());
        }
        if (model.getProjectName() != null) {
            context.put("projectName", model.getProjectName());
        }
        return context;
    }

    public Map<String, Object> toFreeMarkerContext() {
        return toVelocityContext();
    }

    public RenderContext createChildContext() {
        RenderContext child = new RenderContext();
        child.variables.putAll(this.variables);
        child.model = this.model;
        return child;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RenderContext context = new RenderContext();
        public Builder variable(String key, Object value) {
            context.put(key, value);
            return this;
        }
        public RenderContext build() {
            return context;
        }
    }
}
