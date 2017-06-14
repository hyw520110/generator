package com.tzg.tools.generator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
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
    private static final Logger logger = LoggerFactory.getLogger(Generator.class.getName());

    /**
     * 生成器默认配置文件
     */
    private static String conf = "/generator.yaml";

    private Generator() {
    }

    public static void main(String[] args) throws Exception {
        Generator generator = getInstance();
        System.out.println(generator);
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
        initConf();
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
        
        StringWriter writer = new StringWriter();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Table table : tables) {
            System.out.println(table);
            for (File file : files) {
                Template t = engine.getTemplate(file.getPath().replace(dir.getParent(), ""), "UTF-8");
                context.put("date", format.format(new Date()));
                context.put("table", table);
                t.merge(context, writer);
//                System.out.println(writer.toString());
            }
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
