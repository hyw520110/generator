package com.tzg.tools.generator.conf;

import java.util.HashMap;
import java.util.Map;

public class ComponentConf extends BaseBean {
    private static final long serialVersionUID = 1L;
    /**
     * 包名(子包)
     */
    private String subPackage  = "";
    /**
     * 资源文件目录(子目录)
     */
    private String resourceDir = "";

    /**
     * 组件配置信息 
     */
    private Map<String, String> conf = new HashMap<>();

    public String getSubPackage() {
        return subPackage;
    }

    public void setSubPackage(String subPackage) {
        this.subPackage = subPackage;
    }

    public String getResourceDir() {
        return resourceDir;
    }

    public void setResourceDir(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    public Map<String, String> getConf() {
        return conf;
    }

    public void setConf(Map<String, String> conf) {
        this.conf = conf;
    }
}
