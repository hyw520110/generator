package org.hyw.tools.generator.metadata;

import java.util.Arrays;

/**
 * 表过滤器
 * 
 * 用于过滤需要读取的表
 * 
 * @author heyiwu
 * @version 2.0
 */
public class TableFilter {
    
    /**
     * 包含的表名（null 表示包含所有）
     */
    private String[] include;
    
    /**
     * 排除的表名
     */
    private String[] exclude;
    
    /**
     * 表前缀
     */
    private String[] tablePrefix;
    
    /**
     * 是否使用匹配模式（前缀匹配）
     */
    private boolean matchMode = true;
    
    /**
     * 是否包含所有表
     */
    private boolean includeAll = false;
    
    public TableFilter() {
    }
    
    /**
     * 创建包含所有表的过滤器
     */
    public static TableFilter includeAll() {
        TableFilter filter = new TableFilter();
        filter.setIncludeAll(true);
        return filter;
    }
    
    /**
     * 创建包含指定表的过滤器
     */
    public static TableFilter include(String... tableNames) {
        TableFilter filter = new TableFilter();
        filter.setInclude(tableNames);
        return filter;
    }
    
    /**
     * 创建排除指定表的过滤器
     */
    public static TableFilter exclude(String... tableNames) {
        TableFilter filter = new TableFilter();
        filter.setExclude(tableNames);
        return filter;
    }
    
    /**
     * 判断表是否匹配过滤条件
     */
    public boolean matches(String tableName) {
        if (includeAll) {
            return true;
        }
        
        // 检查排除规则
        if (exclude != null && exclude.length > 0) {
            for (String pattern : exclude) {
                if (matchesPattern(tableName, pattern)) {
                    return false;
                }
            }
        }
        
        // 检查包含规则
        if (include == null || include.length == 0) {
            return true; // 未指定包含规则，默认包含
        }
        
        for (String pattern : include) {
            if (matchesPattern(tableName, pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断表名是否匹配模式
     */
    private boolean matchesPattern(String tableName, String pattern) {
        if (pattern == null || tableName == null) {
            return false;
        }
        
        if (matchMode) {
            // 前缀匹配模式
            return tableName.startsWith(pattern) || tableName.equalsIgnoreCase(pattern);
        } else {
            // 精确匹配模式
            return tableName.equalsIgnoreCase(pattern);
        }
    }
    
    // Getters and Setters
    
    public String[] getInclude() {
        return include;
    }
    
    public void setInclude(String[] include) {
        this.include = include;
    }
    
    public String[] getExclude() {
        return exclude;
    }
    
    public void setExclude(String[] exclude) {
        this.exclude = exclude;
    }
    
    public String[] getTablePrefix() {
        return tablePrefix;
    }
    
    public void setTablePrefix(String[] tablePrefix) {
        this.tablePrefix = tablePrefix;
    }
    
    public boolean isMatchMode() {
        return matchMode;
    }
    
    public void setMatchMode(boolean matchMode) {
        this.matchMode = matchMode;
    }
    
    public boolean isIncludeAll() {
        return includeAll;
    }
    
    public void setIncludeAll(boolean includeAll) {
        this.includeAll = includeAll;
    }
    
    @Override
    public String toString() {
        return "TableFilter{" +
                "include=" + Arrays.toString(include) +
                ", exclude=" + Arrays.toString(exclude) +
                ", matchMode=" + matchMode +
                ", includeAll=" + includeAll +
                '}';
    }
}