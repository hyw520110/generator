package org.hyw.tools.generator.template;

import java.io.Serializable;

import lombok.Data;

/**
 * 模板定义
 * <p>
 * 描述单个模板的元数据信息
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
public class TemplateDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板路径
     */
    private String path;

    /**
     * 目标路径（生成后的路径）
     */
    private String target;
}
