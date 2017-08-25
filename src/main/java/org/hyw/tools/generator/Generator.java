package org.hyw.tools.generator;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * 
 * Filename:    Generator.java  
 * Description: 生成器  
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     org.hyw.cn Inc.
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
    	delDir();
    	mkDirs();
        VelocityEngine engine = getVelocityEngine();
        //根据组件配置过滤所需模板文件
        final File dir = global.getTemplateFile();
        if (dir == null || !dir.exists()) {
            System.err.println(dir + "模板目录不存在!");
            return;
        }

        Map<String, Map<String, String>> map = global.getComponentsMap();
        VelocityContext context = createVelocityContext(map.values());
        Collection<File> files = getFiles(dir, map.keySet());
        if (null == files) {
            System.err.println(dir + "目录下没有所需的模板文件！");
            return;
        }
        createScriptFile(engine, context);
        String encoding = global.getEncoding();
        String projectName = global.getProjectName();
        List<Table> tables = getTables();
        for (Table table : tables) {
            for (File file : files) {
                //获取模板对象
                Template template = engine.getTemplate(file.getPath().replace(dir.getParent(), ""), encoding);
                context.put("date", getDate());
                context.put("table", table);
                //模板文件名 
                String name = String.format(file.getName(), table.getBeanName());
                //模板文件名前缀
                String prefix = StringUtils.substringBeforeLast(name, ".");
                boolean isJava = name.endsWith(".java");
                String subDir = getSubDir(dir, file);
                if (isJava) {
                	String pName = file.getParentFile().getName();
                	if(!StringUtils.startsWith(name, "Base")){
                		//类名
                        context.put("className", prefix);
                        context.put(pName+"Name", prefix);	
                	}
                    //类的包名
                    context.put(pName+"Package" , StringUtils.toPackage(strategy.getRootPackage(), projectName, StringUtils.toPackage(subDir)));
                }
                String moduleName = StringUtils.substringBefore(subDir, File.separator);
                context.put("moduleName", moduleName);
                File f = new File(getDir(projectName, isJava, subDir, moduleName), name);
                if (!global.isFileOverride() && f.exists()) {
                    continue;
                }
                writer(context, encoding, template, f);
            }
        }
        openDir();
    }

    public void createScriptFile(VelocityEngine engine, VelocityContext context) {
        ProjectBuilder builder = strategy.getProjectBuilder();
        if (null == builder) {
            return;
        }
        File dir = global.getTemplateFile();
        String encoding = global.getEncoding();
        //TODO 单模块/无模块单工程 支持
        Collection<File> modules = FileUtils.listFiles(new File(dir, "modules"), FileFilterUtils.nameFileFilter(builder.getFileName()), FileFilterUtils.trueFileFilter());
        context.put("version", strategy.getVersion());
        for (File file : modules) {
            String tPath = file.getPath().replace(dir.getParent(), "");
            Template t = engine.getTemplate(tPath, encoding);
            String path = MessageFormat.format(StringUtils.substringAfter(tPath, "modules"), global.getModules());
            context.put("moduleName", StringUtils.substring(path, 1, StringUtils.indexOf(path, File.separator, 1)));
            writer(context, encoding, t, new File(global.getOutputDir(), path));
        }
    }

    private String getDir(String projectName, boolean isJava, String subDir, String moduleName) {
        if (isJava) {
            return StringUtils.toPath(global.getOutputDir(), moduleName, global.getSourceDirectory(), strategy.getRootPackage() + File.separator + projectName, subDir);
        }
        return StringUtils.toPath(global.getOutputDir(), moduleName, global.getResource(), StringUtils.substringAfter(subDir, moduleName));
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

    private void writer(VelocityContext context, String encoding, Template t, File f) {
		try {
            logger.info("generator file:{}", f);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            StringWriter writer = new StringWriter();
            t.merge(context, writer);
            //模板渲染为空则不生成文件,适用场景如:配置了第三方BaseEntity类时,默认BaseEntity类不生成
            if(StringUtils.isEmpty(writer.toString())){
            	return ;
            }
            FileUtils.write(f, writer.toString(), encoding);
        } catch (Exception e) {
            logger.error("write file:{} {}", f, e.getClass(), e);
        }  
    }

    private VelocityContext createVelocityContext(Collection<Map<String, String>> collection) {
        VelocityContext context = new VelocityContext();
        for (Map<String, String> pars : collection) {
            for (Entry<String, String> entry : pars.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        context.put("dataSource", dataSource);
        context.put("author", global.getAuthor());
        context.put("copyright",global.getCopyright());
        context.put("StringUtils", StringUtils.class);
        context.put("projectName", global.getProjectName());
        context.put("javaVersion", strategy.getJavaVersion());
        context.put("rootPackage", strategy.getRootPackage());
        context.put("dbType", dataSource.getDBType().getValue());
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
