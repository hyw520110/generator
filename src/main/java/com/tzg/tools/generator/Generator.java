package com.tzg.tools.generator;

import java.io.BufferedWriter;
import java.io.Closeable;
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

import com.tzg.tools.generator.conf.ComponentConf;
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
        //TODO 根据配置过滤模板文件
        File dir = new File(getClass().getResource("/templates").getPath()).getAbsoluteFile();
        Map<Component, ComponentConf> map = global.getComponentConfs();
        Set<Component> set = map.keySet();
        List<File> files = listFiles(dir, new ArrayList<File>(), set);
        if (null == files) {
            return;
        }
        VelocityContext context = getVelocityContext(map);
        context.put("rootPackage", strategy.getRootPackage());
        context.put("projectName", global.getProjectName());
        context.put("superServiceClass", strategy.getSuperServiceClass());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String encoding = global.getEncoding();
        for (Table table : tables) {
            for (File file : files) {
                Template t = engine.getTemplate(file.getPath().replace(dir.getParent(), ""), encoding);
                context.put("date", format.format(new Date()));
                context.put("table", table);
                String name = file.getName();
                String prefix = StringUtils.substringBeforeLast(name, ".");
                boolean isJava = name.endsWith(".java");
                String moduleName = global.getModules().get(isJava ? prefix : name);
                String subPackage = strategy.getPackages().get(isJava ? prefix : name);
                if (isJava) {
                    context.put(prefix + "Name", table.getEntityName() + prefix);
                    context.put("package" + prefix, moduleName + "." + subPackage);
                }
                String fileName = table.getEntityName() + name;
                String subPath = StringUtils.toPath(strategy.getRootPackage(), global.getProjectName(), moduleName, subPackage);
                File f = new File(paths.get(moduleName), (isJava ? global.getSourceDirectory() : global.getResource()) + File.separator
                                                         + (isJava ? subPath : StringUtils.toPath(subPackage)) + File.separator + fileName);
                if (!global.isFileOverride() && f.exists()) {
                    continue;
                }
                System.out.println(f);
                if(!f.getParentFile().exists()){
                    f.getParentFile().mkdirs();
                }
                writer(context, encoding, t, f);
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

    private VelocityContext getVelocityContext(Map<Component, ComponentConf> map) {
        VelocityContext context = new VelocityContext();
        for (ComponentConf conf : map.values()) {
            Map<String, String> confs = conf.getConf();
            for (Entry<String, String> entry : confs.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        context.put("author", global.getAuthor());
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
                if (f.isDirectory()) {
                    Component component = Component.getComonent(name);
                    if (component != null && set.contains(component)) {
                        listFiles(f, list, set);
                    }
                    return false;
                }
                list.add(f);
                return true;
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
