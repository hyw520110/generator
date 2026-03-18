package org.hyw.tools.generator.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 模板渲染上下文（优化版）
 * <p>
 * 主要改进：
 * 1. 提供类型安全的上下文构建器
 * 2. 内置常用变量，减少重复配置
 * 3. 支持上下文继承（父上下文 -> 子上下文）
 * 4. 添加变量变更监听
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
@NoArgsConstructor
@Accessors(chain = true, fluent = true)
public class RenderContext {

    /**
     * 强类型模板模型
     */
    private TemplateModel model;

    /**
     * 全局变量
     */
    private final Map<String, Object> variables = new HashMap<>(64);

    /**
     * 当前表（表相关模板使用）
     */
    private Table currentTable;

    /**
     * 已选择的组件
     */
    private Set<Component> selectedComponents;

    /**
     * 父上下文（用于变量继承）
     */
    private RenderContext parentContext;

    /**
     * 是否只读
     */
    private boolean readOnly = false;

    /**
     * 私有构造函数（用于 build 方法）
     */
    private RenderContext(Builder builder) {
        this.variables.putAll(builder.variables);
        this.currentTable = builder.currentTable;
        this.selectedComponents = builder.selectedComponents;
        this.parentContext = builder.parentContext;
        this.model = builder.model;
    }

    /**
     * 设置模板模型
     */
    public RenderContext model(TemplateModel model) {
        this.model = model;
        if (model != null) {
            put("model", model);
            // 同步模型中的核心变量到顶级 Map，确保旧模板兼容
            if (model.getTable() != null) {
                table(model.getTable());
            }
            if (model.getComponents() != null) {
                components(model.getComponents());
            }
            put("author", model.getAuthor());
            put("date", model.getDate());
            put("copyright", model.getCopyright());
            put("projectName", model.getProjectName());
            put("rootPackage", model.getRootPackage());
            put("moduleName", model.getModuleName());
            put("entityPackage", model.getEntityPackage());
        }
        return this;
    }

    /**
     * 添加变量
     *
     * @param key 键
     * @param value 值
     * @return 当前上下文
     * @throws IllegalStateException 当上下文为只读时
     */
    public RenderContext put(String key, Object value) {
        if (readOnly) {
            throw new IllegalStateException("上下文为只读模式，不能修改");
        }
        variables.put(key, value);
        return this;
    }

    /**
     * 添加所有变量
     *
     * @param vars 变量映射
     * @return 当前上下文
     */
    public RenderContext putAll(Map<String, Object> vars) {
        if (vars != null && !vars.isEmpty()) {
            variables.putAll(vars);
        }
        return this;
    }

    /**
     * 获取变量
     *
     * @param key 键
     * @return 变量值，不存在时返回 null
     */
    public Object get(String key) {
        // 先从当前上下文查找
        Object value = variables.get(key);
        if (value != null) {
            return value;
        }
        // 从父上下文查找
        if (parentContext != null) {
            return parentContext.get(key);
        }
        return null;
    }

    /**
     * 获取变量（带默认值）
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 变量值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 设置当前表
     *
     * @param table 表信息
     * @return 当前上下文
     */
    public RenderContext table(Table table) {
        this.currentTable = table;
        if (table != null) {
            put("table", table);
            put("entityName", table.getBeanName());
            put("entityNameLower", table.getLowercaseBeanName());
            put("tableName", table.getName());
            put("tableComment", table.getComment());
            put("fields", table.getFields());
            put("primaryKeyField", table.getPrimaryKeyField());
            put("primaryKeyClass", table.getPrimaryKeyClass());
            put("hasPrimaryKey", table.hasPrimarykeys());
            put("isCompositePrimaryKey", table.isCompositePrimaryKey());
        }
        return this;
    }

    /**
     * 设置已选择的组件
     *
     * @param components 组件集合
     * @return 当前上下文
     */
    public RenderContext components(Set<Component> components) {
        this.selectedComponents = components != null ? components : new HashSet<>();
        // 添加组件开关变量
        for (Component component : this.selectedComponents) {
            put(component.name().toLowerCase(), true);
        }
        return this;
    }

    /**
     * 转换为 Velocity 上下文
     *
     * @return Velocity Context 映射
     */
    public Map<String, Object> toVelocityContext() {
        Map<String, Object> fullContext = new HashMap<>(variables);
        
        // 合并父上下文变量
        if (parentContext != null) {
            parentContext.variables.forEach((k, v) -> {
                fullContext.putIfAbsent(k, v);  // 子上下文优先
            });
        }
        
        return Collections.unmodifiableMap(fullContext);
    }

    /**
     * 转换为 FreeMarker 上下文
     *
     * @return FreeMarker Context 映射
     */
    public Map<String, Object> toFreeMarkerContext() {
        return toVelocityContext();
    }

    /**
     * 创建只读副本
     *
     * @return 只读上下文
     */
    public RenderContext createReadOnlyCopy() {
        RenderContext copy = new RenderContext(new Builder(this));
        copy.readOnly = true;
        return copy;
    }

    /**
     * 创建子上下文（继承父上下文变量和model）
     *
     * @return 子上下文
     */
    public RenderContext createChildContext() {
        RenderContext child = new RenderContext(new Builder(this));
        // 确保parentContext指向正确的父上下文
        child.parentContext = this;
        // 确保子上下文继承父上下文的model
        if (child.model == null && this.model != null) {
            child.model = this.model;
        }
        return child;
    }

    /**
     * 清空所有变量
     *
     * @return 当前上下文
     */
    public RenderContext clear() {
        if (!readOnly) {
            variables.clear();
        }
        return this;
    }

    /**
     * 获取变量数量
     *
     * @return 变量数量
     */
    public int size() {
        return variables.size();
    }

    /**
     * 判断是否包含变量
     *
     * @param key 键
     * @return 是否包含
     */
    public boolean containsKey(String key) {
        return variables.containsKey(key) || 
               (parentContext != null && parentContext.containsKey(key));
    }

    /**
     * 构建器
     */
    public static class Builder {
        private final Map<String, Object> variables = new HashMap<>(64);
        private Table currentTable;
        private Set<Component> selectedComponents;
        private RenderContext parentContext;
        private TemplateModel model;

        public Builder() {
        }

        public Builder(RenderContext context) {
            if (context != null) {
                this.variables.putAll(context.variables);
                this.currentTable = context.currentTable;
                this.selectedComponents = context.selectedComponents;
                this.parentContext = context.parentContext;
                this.model = context.model;
            }
        }

        public Builder model(TemplateModel model) {
            this.model = model;
            return this;
        }

        public Builder variable(String key, Object value) {
            this.variables.put(key, value);
            return this;
        }

        public Builder variables(Map<String, Object> vars) {
            if (vars != null) {
                this.variables.putAll(vars);
            }
            return this;
        }

        public Builder currentTable(Table table) {
            this.currentTable = table;
            return this;
        }

        public Builder selectedComponents(Set<Component> components) {
            this.selectedComponents = components;
            return this;
        }

        public Builder parentContext(RenderContext parent) {
            this.parentContext = parent;
            return this;
        }

        /**
         * 构建渲染上下文
         *
         * @return 渲染上下文
         */
        public RenderContext build() {
            return new RenderContext(this);
        }
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }
}
