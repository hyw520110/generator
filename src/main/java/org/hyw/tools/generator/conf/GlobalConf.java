package org.hyw.tools.generator.conf;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyw.tools.generator.enums.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * 
 * Filename:    GlobalConf.java  
 * Description: 全局配置  
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     org.hyw.cn Inc.
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
     * 生成文件前是否删除输出目录 
     */
    private boolean delOutputDir = false;
    /**
     * 模板目录
     */
    private String  template     = "/templates";
    /**
     * 生成文件的编码
     */
    private String  encoding     = "UTF-8";

    /**
     * 是否覆盖已有文件
     */
    private boolean fileOverride = false;

    /**
     * 是否打开输出目录
     */
    private boolean openDir = true;

    /**
     * 开发人员
     */
    private String author;

    /**
     * 版权信息
     */
    private String copyright="";
    /**
     * 组件配置 
     */
    private Map<String, Map<String, String>> components;

    /**
     * 模块信息
     */
    private String[] modules;

    /**
     * 工程目录，默认遵循maven目录结构
     */
    private String sourceDirectory     = "src/main/java";
    private String resource            = "src/main/resources";
    private String testSourceDirectory = "src/test/java";
    private String testResource        = "src/test/resources";

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
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

    public boolean isDelOutputDir() {
        return delOutputDir;
    }

    public void setDelOutputDir(boolean delOutputDir) {
        this.delOutputDir = delOutputDir;
    }

    public boolean isFileOverride() {
        return fileOverride;
    }

    public void setFileOverride(boolean fileOverride) {
        this.fileOverride = fileOverride;
    }

    public boolean isOpenDir() {
        return openDir;
    }

    public void setOpenDir(boolean openDir) {
        this.openDir = openDir;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, Map<String, String>> getComponentsMap() {
        return components;
    }

    @SuppressWarnings("unchecked")
    public void setComponents(Set<String> components) {
        this.components = new HashMap<>();
        for (String s : components) {
        	Map<String,String> pars=null;
            try {
                Component c = Component.getComonent(s);
                if (null == c) {
                    logger.warn("unknow of component:", s);
                    continue;
                }
                pars=new Yaml().loadAs(getResourceAsStream(String.format("/conf/%s.yml", c)), HashMap.class);
            } catch (Throwable e) {
                logger.error("load {} config {}:{}", s, e.getClass(), e.getLocalizedMessage());
                pars=new HashMap<String,String>();
            }
            this.components.put(s.toLowerCase(),pars);
        }
    }
    public String[] getModules() {
        return modules;
    }

    public void setModules(String[] modules) {
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

    public String getTemplate() {
        return template;
    }

    public File getTemplateFile() {
        URL url = getClass().getResource(getTemplate());
        if (null == url) {
            return null;
        }
        return new File(url.getFile());
    }

    public void setTemplate(String template) {
        this.template = template;
    }

	public String getCopyright() {
		return copyright;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

}
