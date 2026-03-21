package org.hyw.tools.generator.conf.db;

import lombok.Data;

/**
 * 表关系（外键关系）
 * 
 * @author heyw
 */
@Data
public class TableRelation {
    
    /** 源表名（包含外键的表） */
    private String sourceTable;
    
    /** 源表注释 */
    private String sourceComment;
    
    /** 目标表名（被引用的表） */
    private String targetTable;
    
    /** 目标表注释 */
    private String targetComment;
    
    /** 外键列名 */
    private String fkColumn;
    
    /** 被引用的列名 */
    private String pkColumn;
    
    /** 外键约束名 */
    private String fkName;
    
    /** 外键列是否可为空（true=可选关系，用虚线；false=必选关系，用实线） */
    private boolean nullable = true;
    
    public TableRelation() {
    }
    
    public TableRelation(String sourceTable, String targetTable) {
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
    }
    
    /**
     * 获取关系描述
     */
    public String getRelationDesc() {
        return sourceTable + "." + fkColumn + " -> " + targetTable + "." + pkColumn;
    }
}
