package com.tzg.tools.generator.conf;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 生成文件的编码
     */
    private String encoding = "UTF-8";

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
    /**
     * 模块配置
     */
    private Map<String, String>           modules;
    private String                        sourceDirectory     = "src/main/java";
    private String                        testSourceDirectory = "src/test/java";
    private String                        resource            = "src/main/resources";
    private String                        testResource        = "src/test/resources";

    public String getOutputDir() {
        return outputDir;
    }

    /**
     * 获取项目名获取输出路径的子目录名
     * @author:  heyiwu 
     * @return
     */
    public String getProjectName() {
        File file = new File(outputDir);
        return file.getName();
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
                map.put(c, new Yaml().loadAs(getResourceAsStream(String.format("/conf/%s.yaml", item)), ComponentConf.class));
            } catch (Throwable e) {
                logger.error("load {} config {}:{}", item, e.getClass(), e.getLocalizedMessage());
            }
        }
    }

    public Map<Component, ComponentConf> getComponentConfs() {
        if (null == components) {
            components = new HashMap<Component, ComponentConf>();
        }
        return components;
    }

    public Map<String, String> getModules() {
        return modules;
    }

    public void setModules(Map<String, String> modules) {
        this.modules = modules;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getTestSourceDirectory() {
        return testSourceDirectory;
    }

    public void setTestSourceDirectory(String testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getTestResource() {
        return testResource;
    }

    public void setTestResource(String testResource) {
        this.testResource = testResource;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public static Logger getLogger() {
        return logger;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
