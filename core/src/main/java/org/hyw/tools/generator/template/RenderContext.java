package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.Component;

public class RenderContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Object> variables = new HashMap<>();
    private TemplateModel model = new TemplateModel();

    public RenderContext put(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public RenderContext putAll(Map<String, ?> map) {
        if (map != null) {
            variables.putAll(map);
        }
        return this;
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public boolean containsKey(String key) {
        return variables.containsKey(key);
    }

    public Set<String> keySet() {
        return variables.keySet();
    }

    public RenderContext table(Table table) {
        this.model.setTable(table);
        this.variables.put(Consts.CTX_TABLE, table);
        return this;
    }

    public RenderContext components(Set<Component> components) {
        this.model.setComponents(components);
        this.variables.put("components", components);
        return this;
    }

    public Map<String, Object> toVelocityContext() {
        Map<String, Object> context = new HashMap<>(variables);
        
        // 显式将 model 中的属性平铺到 context 中，确保模板可以直接访问 ${entityPackage} 等
        if (model != null) {
            if (model.getTable() != null) context.put(Consts.CTX_TABLE, model.getTable());
            if (model.getConfig() != null) context.put(Consts.CTX_CONFIG, model.getConfig());
            if (model.getProjectName() != null) context.put("projectName", model.getProjectName());
            if (model.getRootPackage() != null) context.put("rootPackage", model.getRootPackage());
            if (model.getModuleName() != null) context.put(Consts.CTX_MODULE_NAME, model.getModuleName());
            if (model.getAuthor() != null) context.put("author", model.getAuthor());
            if (model.getDate() != null) context.put("date", model.getDate());
            if (model.getCopyright() != null) context.put("copyright", model.getCopyright());
            if (model.getExtra() != null) context.putAll(model.getExtra());
        }
        return context;
    }

    public Map<String, Object> toFreeMarkerContext() {
        return toVelocityContext();
    }

    public RenderContext createChildContext() {
        RenderContext child = new RenderContext();
        child.variables.putAll(this.variables);
        // 深度复制 model，确保子上下文的修改不影响父上下文
        if (this.model != null) {
            child.model = this.model.copy();
        }
        return child;
    }

    /**
     * 创建当前上下文的隔离快照，用于并行渲染。
     * 等价于 {@link #createChildContext()}，但语义聚焦于"并行隔离"场景。
     */
    public RenderContext snapshot() {
        return createChildContext();
    }

    public TemplateModel getModel() {
        return model;
    }

    public void setModel(TemplateModel model) {
        this.model = model;
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
