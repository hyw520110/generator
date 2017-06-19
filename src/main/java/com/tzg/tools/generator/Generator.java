package com.tzg.tools.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.tzg.tools.generator.conf.db.Table;
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
        final File dir = global.getTemplateFile();
        if (dir == null || !dir.exists()) {
            System.err.println(dir + "模板目录不存在!");
            return;
        }
        Map<String, Map<String, String>> map = global.getComponentsMap();
        Collection<File> files = getFiles(dir, map.keySet());
        if (null == files) {
            System.err.println(dir + "目录下没有所需的模板文件！");
            return;
        }
        VelocityContext context = getVelocityContext(map.values());
        if (StringUtils.isNotBlank(strategy.getSuperServiceClass())) {
            context.put("superServiceClass", strategy.getSuperServiceClass());
            context.put("superServiceClassName", StringUtils.substringAfterLast(strategy.getSuperServiceClass(), "."));
        }

        String encoding = global.getEncoding();
        String projectName = global.getProjectName();
        for (Table table : tables) {
            for (File file : files) {
                //获取模板对象
                Template template = engine.getTemplate(file.getPath().replace(dir.getParent(), ""), encoding);
                context.put("date", getDate());
                context.put("table", table);
                //模板文件名 
                String name = file.getName();
                //模板文件名前缀
                String prefix = StringUtils.substringBeforeLast(name, ".");
                boolean isJava = name.endsWith(".java");
                String subDir = getSubDir(dir, file);
                if (isJava) {
                    //类名
                    context.put(prefix + "Name", table.getBeanName() + prefix);
                    //类的包名
                    context.put("package" + prefix, StringUtils.toPackage(strategy.getRootPackage(), projectName, StringUtils.toPackage(subDir)));
                }
                boolean isPom = name.equals("pom.xml");
                String fileName = name.startsWith("Base") || isPom ? name : table.getBeanName() + name;
                String moduleName = StringUtils.substringBefore(subDir, File.separator);
                context.put("moduleName",moduleName);
                String parent = StringUtils.toPath(global.getOutputDir(), moduleName);
                if (!isPom) {
                    parent = StringUtils.toPath(parent, isJava ? global.getSourceDirectory() : global.getResource(), isJava ? strategy.getRootPackage()+File.separator+projectName : "", subDir);
                }
                File f = new File(parent, fileName);
                if (!global.isFileOverride() && f.exists()) {
                    continue;
                }
                writer(context, encoding, template, f);
            }
        }
        openDir();
    }

    private String getSubDir(final File dir, File file) {
        //子路径（以组件开头）
        String subPath = file.getParent().replace(dir.getPath(), "").substring(1);
        //子目录(去除组件路径)
        return MessageFormat.format(StringUtils.substringAfter(subPath, File.separator), global.getModules());
    }

    private Collection<File> getFiles(final File dir, final Set<String> components) {
        Collection<File> files = FileUtils.listFiles(dir, new DirectoryFileFilter() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean accept(File file) {
                String path = file.getPath().replace(dir.getAbsolutePath(), "").substring(1);
                int index = StringUtils.indexOf(path, File.separator);
                if (index != -1) {
                    path = StringUtils.substring(path, 0, index);
                }
                return components.contains(path);
            }
        }, FileFilterUtils.trueFileFilter());
        return files;
    }

    private void writer(VelocityContext context, String encoding, Template t, File f) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        Writer writer = null;
        try {
            logger.info("generator file:{}", f);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
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
        context.put("projectName", global.getProjectName());
        context.put("rootPackage", strategy.getRootPackage());
        return context;
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
