package org.hyw.tools.generator.conf;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板配置
 * <p>
 * 管理模板相关的配置
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板目录
     */
    @Builder.Default
    private String templateDir = "/templates";

    /**
     * 非模板文件排除渲染
     */
    private String[] resources;

    /**
     * 排除的目录
     */
    private String[] excludeDir;
}
