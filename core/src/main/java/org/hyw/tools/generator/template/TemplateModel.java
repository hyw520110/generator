package org.hyw.tools.generator.template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.Component;

import lombok.Builder;
import lombok.Data;

/**
 * 模板数据模型
 * <p>
 * 封装模板渲染所需的常用变量，提供强类型支持。
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
@Builder
public class TemplateModel {

    /**
     * 全局配置
     */
    private GlobalConf config;

    /**
     * 当前处理的表信息
     */
    private Table table;

    /**
     * 所有表信息（用于全局引用）
     */
    private java.util.List<Table> allTables;

    /**
     * 作者名称
     */
    private String author;

    /**
     * 当前日期（格式化字符串）
     */
    @Builder.Default
    private String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    /**
     * 版权信息
     */
    private String copyright;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 根包名
     */
    private String rootPackage;

    /**
     * 当前模块名称
     */
    private String moduleName;

    /**
     * 实体类包名（计算值）
     */
    private String entityPackage;

    /**
     * 已启用的组件列表
     */
    private Set<Component> components;

    /**
     * 自定义扩展变量
     */
    private Map<String, Object> extra;

    /**
     * 获取指定名称的扩展变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        return extra != null ? (T) extra.get(key) : null;
    }

    /**
     * 判断是否启用指定组件
     */
    public boolean hasComponent(Component component) {
        return components != null && components.contains(component);
    }

    /**
     * 快速访问常用的组件开关（用于 FTL 简写）
     */
    public boolean isMybatis() { return hasComponent(Component.MYBATIS); }
    public boolean isJpa() { return hasComponent(Component.JPA); }
}
