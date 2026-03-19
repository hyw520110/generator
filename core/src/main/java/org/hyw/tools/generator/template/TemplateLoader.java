package org.hyw.tools.generator.template;

import java.net.URL;
import java.util.Map;

/**
 * 模板加载器接口
 * 
 * 定义模板加载的统一接口
 * 支持从文件系统、JAR 包、内存等不同来源加载模板
 * 
 * @author heyiwu
 * @version 2.0
 */
public interface TemplateLoader {
    
    /**
     * 加载所有模板
     * 
     * @param baseDir 基础目录
     * @return 模板映射（路径 -> 内容）
     */
    Map<String, String> loadTemplates(String baseDir);
    
    /**
     * 根据组件名称加载模板
     * 
     * @param baseDir 基础目录
     * @param componentNames 组件名称数组
     * @return 模板映射
     */
    Map<String, String> loadTemplates(String baseDir, String... componentNames);
    
    /**
     * 根据组件名称加载模板（指定是否构建路径）
     * 
     * @param baseDir 基础目录
     * @param buildPath 是否构建完整路径
     * @param componentNames 组件名称数组
     * @return 模板映射
     */
    Map<String, String> loadTemplates(String baseDir, boolean buildPath, String... componentNames);
    
    /**
     * 加载单个模板
     * 
     * @param templatePath 模板路径
     * @return 模板内容
     */
    String loadTemplate(String templatePath);
    
    /**
     * 获取模板目录 URL
     * 
     * @param templateDir 模板目录
     * @return 模板目录 URL
     */
    URL getTemplateDirUrl(String templateDir);
    
    /**
     * 检查模板是否存在
     * 
     * @param templatePath 模板路径
     * @return 是否存在
     */
    boolean templateExists(String templatePath);
}