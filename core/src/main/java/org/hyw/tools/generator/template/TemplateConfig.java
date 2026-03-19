package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.List;

/**
 * 模板配置
 * <p>
 * 管理模板引擎、模板组、渲染规则等配置
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class TemplateConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认模板引擎
     */
    private String defaultEngine;

    /**
     * 支持的引擎列表
     */
    private List<String> supportedEngines;

    /**
     * 模板组列表
     */
    private List<TemplateGroup> templateGroups;

    /**
     * 按二进制处理的资源扩展名
     */
    private List<String> resources;

    /**
     * 排除的目录
     */
    private List<String> excludeDirs;

    public String getDefaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(String defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public List<String> getSupportedEngines() {
        return supportedEngines;
    }

    public void setSupportedEngines(List<String> supportedEngines) {
        this.supportedEngines = supportedEngines;
    }

    public List<TemplateGroup> getTemplateGroups() {
        return templateGroups;
    }

    public void setTemplateGroups(List<TemplateGroup> templateGroups) {
        this.templateGroups = templateGroups;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public List<String> getExcludeDirs() {
        return excludeDirs;
    }

    public void setExcludeDirs(List<String> excludeDirs) {
        this.excludeDirs = excludeDirs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String defaultEngine;
        private List<String> supportedEngines;
        private List<TemplateGroup> templateGroups;
        private List<String> resources;
        private List<String> excludeDirs;

        public Builder defaultEngine(String defaultEngine) {
            this.defaultEngine = defaultEngine;
            return this;
        }

        public Builder supportedEngines(List<String> supportedEngines) {
            this.supportedEngines = supportedEngines;
            return this;
        }

        public Builder templateGroups(List<TemplateGroup> templateGroups) {
            this.templateGroups = templateGroups;
            return this;
        }

        public Builder resources(List<String> resources) {
            this.resources = resources;
            return this;
        }

        public Builder excludeDirs(List<String> excludeDirs) {
            this.excludeDirs = excludeDirs;
            return this;
        }

        public TemplateConfig build() {
            TemplateConfig templateConfig = new TemplateConfig();
            templateConfig.setDefaultEngine(defaultEngine);
            templateConfig.setSupportedEngines(supportedEngines);
            templateConfig.setTemplateGroups(templateGroups);
            templateConfig.setResources(resources);
            templateConfig.setExcludeDirs(excludeDirs);
            return templateConfig;
        }
    }
}
