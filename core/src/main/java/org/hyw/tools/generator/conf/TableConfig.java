package org.hyw.tools.generator.conf;

import java.io.Serializable;

import org.hyw.tools.generator.enums.Naming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表配置
 * <p>
 * 管理数据库表相关的配置（命名策略、前缀等）
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 需要包含的表名
     */
    private String[] include;

    /**
     * 需要排除的表名
     */
    private String[] exclude;

    /**
     * 表名是否匹配模式
     */
    @Builder.Default
    private boolean matchMode = true;

    /**
     * 表前缀
     */
    private String[] tablePrefix;

    /**
     * 数据库表映射到实体的命名策略
     */
    @Builder.Default
    private Naming naming = Naming.TOCAMEL;

    /**
     * 单词分隔符
     */
    private char[] separators;

    /**
     * 是否大写命名
     */
    @Builder.Default
    private boolean isCapitalMode = false;

    /**
     * 是否生成实体字段常量
     */
    @Builder.Default
    private boolean columnConstant = false;

    /**
     * 设置包含的表名（单个）
     */
    public void setInclude(String include) {
        this.include = new String[]{include};
    }
}
