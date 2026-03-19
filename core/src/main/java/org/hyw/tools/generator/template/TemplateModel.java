package org.hyw.tools.generator.template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.List;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;

public class TemplateModel {
    private GlobalConf config;
    private Table table;
    private List<Table> allTables;
    private String author;
    private String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private String copyright;
    private String projectName;
    private String rootPackage;
    private String moduleName;
    private String entityPackage;
    private Set<Component> components;
    private Map<String, Object> extra;

    public GlobalConf getConfig() { return config; }
    public void setConfig(GlobalConf config) { this.config = config; }
    public Table getTable() { return table; }
    public void setTable(Table table) { this.table = table; }
    public List<Table> getAllTables() { return allTables; }
    public void setAllTables(List<Table> allTables) { this.allTables = allTables; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getRootPackage() { return rootPackage; }
    public void setRootPackage(String rootPackage) { this.rootPackage = rootPackage; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getEntityPackage() { return entityPackage; }
    public void setEntityPackage(String entityPackage) { this.entityPackage = entityPackage; }
    public Set<Component> getComponents() { return components; }
    public void setComponents(Set<Component> components) { this.components = components; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }

    public <T> T getExtra(String key) {
        return extra != null ? (T) extra.get(key) : null;
    }

    public boolean hasComponent(Component component) {
        return components != null && components.contains(component);
    }

    public boolean isMybatis() { return hasComponent(Component.MYBATIS); }
    public boolean isJpa() { return hasComponent(Component.JPA); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private TemplateModel model = new TemplateModel();
        public Builder config(GlobalConf config) { model.setConfig(config); return this; }
        public Builder table(Table table) { model.setTable(table); return this; }
        public Builder allTables(List<Table> allTables) { model.setAllTables(allTables); return this; }
        public Builder author(String author) { model.setAuthor(author); return this; }
        public Builder date(String date) { model.setDate(date); return this; }
        public Builder copyright(String copyright) { model.setCopyright(copyright); return this; }
        public Builder projectName(String projectName) { model.setProjectName(projectName); return this; }
        public Builder rootPackage(String rootPackage) { model.setRootPackage(rootPackage); return this; }
        public Builder moduleName(String moduleName) { model.setModuleName(moduleName); return this; }
        public Builder entityPackage(String entityPackage) { model.setEntityPackage(entityPackage); return this; }
        public Builder components(Set<Component> components) { model.setComponents(components); return this; }
        public Builder extra(Map<String, Object> extra) { model.setExtra(extra); return this; }
        public TemplateModel build() { return model; }
    }
}
