package com.tzg.tools.generator;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.tzg.tools.generator.conf.db.Table;

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

    private VelocityEngine engine;

    private Generator() {
    }

    public static void main(String[] args) throws Exception {
        loadConf().execute();
    }

    public static Generator loadConf() {
        return loadConf("/generator.yaml");
    }
    /**
     * 生成代码
     * @throws Exception 
     */
    public void execute() throws Exception {
        initConf();
        mkdirs();
        List<Table> tables = getTables();
        for (Table table : tables) {
            System.out.println(table);
        }
    }

    

    /**
     * 加载配置、初始化
     * @author:  heyiwu 
     * @param yamlConf
     * @return
     */
    public static Generator loadConf(String yamlConf) {
        return new Yaml().loadAs(Generator.class.getResourceAsStream(yamlConf), Generator.class);
    }

    /**
     * 设置模版引擎，主要指向获取模版路径
     */
    private VelocityEngine getVelocityEngine() {
        if (engine == null) {
            Properties p = new Properties();
            String conf = "/conf/velocity.properties";
            try {
                p.load(Generator.class.getResourceAsStream(conf));
            } catch (IOException e) {
                logger.error("load {} {}", conf, e.getClass(), e);
            }
            engine = new VelocityEngine(p);
        }
        return engine;
    }
}
