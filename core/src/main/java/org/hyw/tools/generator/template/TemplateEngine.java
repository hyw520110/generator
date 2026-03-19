package org.hyw.tools.generator.template;

import java.util.Map;

/**
 * 模板引擎接口
 * 
 * @author heyiwu
 * @version 2.0
 */
public interface TemplateEngine {
    
    /**
     * 获取引擎名称
     */
    String getName();
    
    /**
     * 渲染模板
     * 
     * @param template 模板内容
     * @param context 上下文变量
     * @return 渲染后的内容
     */
    String render(String template, Map<String, Object> context);
    
    /**
     * 初始化引擎
     */
    void init();
    
    /**
     * 是否支持该模板文件
     */
    boolean supports(String templatePath);
}
