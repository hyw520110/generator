package org.hyw.tools.generator.template.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.EngineType;
import org.hyw.tools.generator.template.TemplateEngine;
import org.hyw.tools.generator.template.TemplateUtils;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * FreeMarker 模板引擎实现
 * <p>
 * 主要改进:
 * 1. 配置 BeansWrapper 暴露静态工具类 (支持 T.method() 调用)
 * 2. 使用 ClassTemplateLoader 支持<#import>和<#include>
 * 3. 增强临时渲染配置的性能 (基于主配置克隆)
 * </p>
 *
 * @author heyiwu
 */
@Slf4j
public class FreeMarkerEngineImpl implements TemplateEngine {

    private static final String ENGINE_NAME = EngineType.FREEMARKER.getName();

    /**
     * FreeMarker 配置
     */
    private final Configuration configuration;

    public FreeMarkerEngineImpl() {
        this.configuration = createConfiguration();
    }

    /**
     * 创建并配置 FreeMarker
     */
    private Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        
        // 设置模板加载器 (支持 classpath 加载，支持<#import>和<#include>)
        cfg.setTemplateLoader(new ClassTemplateLoader(
            FreeMarkerEngineImpl.class.getClassLoader(), 
            Consts.TEMPLATE_DIR_FREEMARKER
        ));
        
        // 编码设置
        cfg.setDefaultEncoding(Consts.DEFAULT_ENCODING);
        cfg.setOutputEncoding(Consts.DEFAULT_ENCODING);
        
        // 异常处理
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        
        // 数字和布尔值格式
        cfg.setNumberFormat("computer");
        cfg.setBooleanFormat("c");
        
        // 安全设置 (允许访问指定类)
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        
        // 设置共享变量 (全局可用) - 支持 T.method() 调用静态方法
        try {
            freemarker.ext.beans.BeansWrapper wrapper = new freemarker.ext.beans.BeansWrapperBuilder(Configuration.VERSION_2_3_31).build();
            freemarker.template.TemplateHashModel staticModels = wrapper.getStaticModels();
            freemarker.template.TemplateHashModel fileStatics = (freemarker.template.TemplateHashModel) staticModels.get(TemplateUtils.class.getName());
            cfg.setSharedVariable("T", fileStatics);
        } catch (Exception e) {
            log.warn("设置共享变量失败：{}", e.getMessage());
        }
        
        log.info("{} 模板引擎初始化成功", ENGINE_NAME);
        return cfg;
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public void init() {
        // 已在构造函数中初始化
    }

    @Override
    public String render(String template, Map<String, Object> context) {
        try {
            // 生成模板名称 (避免冲突)
            String templateName = "template_" + System.nanoTime() + "_" + Math.abs(template.hashCode());

            // 使用 StringTemplateLoader 动态注册模板
            StringTemplateLoader stringLoader = new StringTemplateLoader();
            stringLoader.putTemplate(templateName, template);
            
            // 创建临时配置 (从主配置克隆，合并模板加载器，支持 import/include)
            Configuration tempConfig = (Configuration) configuration.clone();
            tempConfig.setTemplateLoader(new MultiTemplateLoader(
                stringLoader,
                configuration.getTemplateLoader()
            ));
            
            // 获取模板
            Template fmTemplate = tempConfig.getTemplate(templateName);

            // 渲染
            StringWriter writer = new StringWriter();
            fmTemplate.process(context, writer);
            return writer.toString();

        } catch (IOException | TemplateException e) {
            log.error("{} 模板渲染失败：{}", ENGINE_NAME, e.getMessage());
            throw new RuntimeException("FreeMarker 模板渲染失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String templatePath) {
        return templatePath != null && (
            templatePath.endsWith(EngineType.FREEMARKER.getExtension()) ||
            templatePath.contains("/" + EngineType.FREEMARKER.getName() + "/")
        );
    }

    /**
     * 获取 FreeMarker 配置
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}
