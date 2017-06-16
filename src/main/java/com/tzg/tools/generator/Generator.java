package com.tzg.tools.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.tzg.tools.generator.conf.db.Table;
import com.tzg.tools.generator.enums.Component;
import com.tzg.tools.generator.utils.StringUtils;

/**
 * 
 * Filename:    Generator.java  
 * Description: 生成器  
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     tzg.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年6月12日 上午10:53:49  
 *
 */
public class Generator extends AbstractGenerator {

    private static final long serialVersionUID = 345083252519120430L;

    private static final Logger logger = LoggerFactory.getLogger(Generator.class.getName());

    /**
     * 生成器默认配置文件
     */
    private static String conf = "/generator.yaml";

    private Generator() {
    }

    public static void main(String[] args) throws Exception {
        Generator generator = getInstance();
        generator.execute();
    }

    public static Generator getInstance() {
        return getInstance(conf);
    }

    /**
     * 生成代码
     * @throws Exception 
     */
    public void execute() throws Exception {
        mkdirs();
        List<Table> tables = getTables();
        VelocityEngine engine = getVelocityEngine();
        //根据组件配置过滤所需模板文件
        File dir = global.getTemplateFile();
        if(dir==null||!dir.exists()){
            System.err.println(dir+"模板目录不存在!");
            return ;
        }
        Map<Component, Map<String, String>> map = global.getComponentsMap();
        List<File> files = listFiles(dir, new ArrayList<File>(), map.keySet());
        if (null == files) {
            System.err.println(dir+"目录下没有所需的模板文件！");
            return;
        }
        VelocityContext context = getVelocityContext(map.values());
        if (StringUtils.isNotBlank(strategy.getSuperServiceClass())) {
            context.put("superServiceClass", strategy.getSuperServiceClass());
            context.put("superServiceClassName", StringUtils.substringAfterLast(strategy.getSuperServiceClass(), "."));
        }

        String encoding = global.getEncoding();
        for (Table table : tables) {
            for (File file : files) {
            	String t = file.getPath().replace(dir.getParent(), "");
                Template template = engine.getTemplate(t, encoding);
                context.put("date", getDate());
                context.put("table", table);
                //模板文件名 
                String name = file.getName();
                //模板文件名前缀
                String prefix = StringUtils.substringBeforeLast(name, ".");
                boolean isJava = name.endsWith(".java");
                String subDir = StringUtils.substringAfter(file.getParent(), t);
                if (isJava) {
                    context.put(prefix + "Name", table.getBeanName() + prefix);
                    context.put("package" + prefix, StringUtils.toPackage(strategy.getRootPackage(), global.getProjectName(),StringUtils.toPackage(subDir)));
                }
                String fileName = table.getBeanName() + name;
                int index = StringUtils.indexOf(subDir, File.separatorChar, 1);
                String moduleName=StringUtils.substring(subDir, 1,index);
                subDir=StringUtils.substring(subDir, index);
				File f = new File(StringUtils.toPath(global.getOutputDir(), moduleName,
						isJava ? global.getSourceDirectory() : global.getResource(), subDir), fileName);
                if (!global.isFileOverride() && f.exists()) {
                    continue;
                }
                System.out.println(f);
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                writer(context, encoding, template, f);
            }
        }
        openDir();
    }

    private void writer(VelocityContext context, String encoding, Template t, File f) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        Writer writer = null;
        try {
            fos = new FileOutputStream(f);
            osw = new OutputStreamWriter(fos, encoding);
            writer = new BufferedWriter(osw);
            t.merge(context, writer);
            writer.flush();
        } catch (Exception e) {
            logger.error("write file:{} {}", f, e.getClass(), e);
        } finally {
            close(fos, osw, writer);
        }
    }

    private VelocityContext getVelocityContext(Collection<Map<String, String>> collection) {
        VelocityContext context = new VelocityContext();
        for (Map<String, String> pars : collection) {
            for (Entry<String, String> entry : pars.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        context.put("author", global.getAuthor());
        context.put("StringUtils", StringUtils.class);
        return context;
    }

    private List<File> listFiles(File file, final List<File> list, final Set<Component> set) {
        if (file.isFile()) {
            list.add(file);
            return list;
        }
        file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                if (f.isFile()) {
                    list.add(f);
                    return true;
                }
                Component component = Component.getComonent(name);
                if (component != null && set.contains(component)) {
                    listFiles(f, list, set);
                }
                return false;
            }
        });
        return list;
    }

    /**
     * 加载配置、初始化
     * @author:  heyiwu 
     * @param yamlConf
     * @return
     */
    public static Generator getInstance(String yamlConf) {
        return new Yaml().loadAs(getResourceAsStream(yamlConf), Generator.class);
    }

    /**
     * 设置模版引擎，主要指向获取模版路径
     */
    private VelocityEngine getVelocityEngine() {
        Properties p = new Properties();
        String conf = "/conf/velocity.properties";
        try {
            p.load(Generator.class.getResourceAsStream(conf));
        } catch (IOException e) {
            logger.error("load {} {}", conf, e.getClass(), e);
        }
        return new VelocityEngine(p);
    }
}
