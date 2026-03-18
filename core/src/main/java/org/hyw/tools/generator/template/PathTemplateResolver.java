package org.hyw.tools.generator.template;

/**
 * 模板路径解析器
 * <p>
 * 负责根据模板路径、模块信息和全局配置计算输出文件的物理路径。
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public interface PathTemplateResolver {

    /**
     * 解析输出路径
     *
     * @param templatePath 模板相对路径
     * @param model 渲染数据模型
     * @return 最终生成文件的绝对或相对输出路径
     */
    String resolve(String templatePath, TemplateModel model);
}
