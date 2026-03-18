package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 模板组
 * <p>
 * 一组相关的模板定义，可以包含多个选项（互斥组）
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
public class TemplateGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组 ID（唯一标识）
     */
    private String id;

    /**
     * 组名称
     */
    private String name;

    /**
     * 是否必需
     */
    private boolean required;

    /**
     * 是否互斥（只能选择一个选项）
     */
    private boolean exclusive;

    /**
     * 模板列表
     */
    private List<TemplateDefinition> templates;

    /**
     * 选项列表（用于互斥组）
     */
    private List<TemplateOption> options;
}
