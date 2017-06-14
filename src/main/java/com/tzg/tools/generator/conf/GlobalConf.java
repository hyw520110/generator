package com.tzg.tools.generator.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.tzg.tools.generator.enums.Component;

/**
 * 
 * Filename:    GlobalConf.java  
 * Description: 全局配置  
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     tzg.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年6月13日 上午10:03:10  
 *
 */
public class GlobalConf extends BaseBean {
     
    private static final long serialVersionUID = -2678498334219769129L;

    private static final Logger logger = LoggerFactory.getLogger(GlobalConf.class.getName());

    /**
     * 生成文件的输出目录,默认根目录
     */
    private String outputDir = "/";

    /**
     * 是否覆盖已有文件
     */
    private boolean fileOverride = false;

    /**
     * 是否打开输出目录
     */
    private boolean dirOpen = true;

    /**
     * 开发人员
     */
    private String                        author;
    /**
     * 组件配置 
     */
    private Map<Component, ComponentConf> components;
    private String                        serviceName;
    private String                        serviceImplName;

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isFileOverride() {
        return fileOverride;
    }

    public void setFileOverride(boolean fileOverride) {
        this.fileOverride = fileOverride;
    }

    public boolean isDirOpen() {
        return dirOpen;
    }

    public void setDirOpen(boolean dirOpen) {
        this.dirOpen = dirOpen;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setComponents(List<String> components) {
        Map<Component, ComponentConf> map = getComponentConfs();
        for (String item : components) {
            try {
                Component c = Component.valueOf(item.toUpperCase());
                if (null == c) {
                    logger.warn("unknown type of component:{}", item);
                    continue;
                }
                map.put(c, new Yaml().loadAs(getClass().getResourceAsStream(String.format("/conf/%s.yaml", item)), ComponentConf.class));
            } catch (Throwable e) {
                logger.error("load {} config {}:{}", item, e.getClass(), e.getLocalizedMessage());
            }
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceImplName() {
        return serviceImplName;
    }

    public void setServiceImplName(String serviceImplName) {
        this.serviceImplName = serviceImplName;
    }

    public Map<Component, ComponentConf> getComponentConfs() {
        if (null == components) {
            components = new HashMap<Component, ComponentConf>();
        }
        return components;
    }
}
