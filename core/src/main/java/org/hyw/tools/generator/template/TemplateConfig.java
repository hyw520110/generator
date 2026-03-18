package org.hyw.tools.generator.template;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 模板配置
 * <p>
 * 管理模板引擎、模板组、渲染规则等配置
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
public class TemplateConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认模板引擎
     */
    private String defaultEngine;

    /**
     * 支持的引擎列表
     */
    private List<String> supportedEngines;

    /**
     * 模板组列表
     */
    private List<TemplateGroup> templateGroups;

    /**
     * 按二进制处理的资源扩展名
     */
    private List<String> resources;

    /**
     * 排除的目录
     */
    private List<String> excludeDirs;
}
