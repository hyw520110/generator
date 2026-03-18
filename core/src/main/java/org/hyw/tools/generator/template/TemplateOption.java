package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 模板选项
 * <p>
 * 用于互斥组的选项定义，每个选项可以有不同的模板列表
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
public class TemplateOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选项 ID
     */
    private String id;

    /**
     * 使用的模板引擎
     */
    private String engine;

    /**
     * 模板列表
     */
    private List<TemplateDefinition> templates;
}
