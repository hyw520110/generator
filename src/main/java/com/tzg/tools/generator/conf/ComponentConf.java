package com.tzg.tools.generator.conf;

import java.util.HashMap;
import java.util.Map;

public class ComponentConf extends BaseBean {
    private static final long serialVersionUID = 1L;
     

    /**
     * 组件配置信息 
     */
    private Map<String, String> conf = new HashMap<>();

   
    public Map<String, String> getConf() {
        return conf;
    }

    public void setConf(Map<String, String> conf) {
        this.conf = conf;
    }
}
