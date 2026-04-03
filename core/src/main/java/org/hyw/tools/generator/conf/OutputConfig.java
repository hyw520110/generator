package org.hyw.tools.generator.conf;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyw.tools.generator.constants.Consts;

/**
 * 输出配置
 * <p>
 * 管理文件输出相关的配置
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutputConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 生成文件的输出目录
     */
    @Builder.Default
    private String outputDir = "/";

    /**
     * 生成文件前是否删除输出目录
     */
    @Builder.Default
    private boolean delOutputDir = false;

    /**
     * 是否覆盖已有文件
     */
    @Builder.Default
    private boolean fileOverride = false;

    /**
     * 是否打开输出目录
     */
    @Builder.Default
    private boolean openDir = true;

    /**
     * 生成文件的编码
     */
    @Builder.Default
    private String encoding = Consts.DEFAULT_ENCODING;
}
