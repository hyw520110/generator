package org.hyw.tools.generator.metadata;

import java.util.List;

import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.exception.DatabaseException;

/**
 * 元数据读取器接口
 * 
 * 定义从数据源读取表结构元数据的统一接口
 * 支持从数据库、文件、API 等不同来源读取元数据
 * 
 * @author heyiwu
 * @version 2.0
 */
public interface MetadataReader {
    
    /**
     * 读取所有表
     * 
     * @return 表列表
     * @throws DatabaseException 读取失败时抛出
     */
    List<Table> readAllTables();
    
    /**
     * 根据过滤条件读取表
     * 
     * @param filter 表过滤器
     * @return 表列表
     * @throws DatabaseException 读取失败时抛出
     */
    List<Table> readTables(TableFilter filter);
    
    /**
     * 读取指定表
     * 
     * @param tableName 表名
     * @return 表信息
     * @throws DatabaseException 读取失败时抛出
     */
    Table readTable(String tableName);
    
    /**
     * 获取所有表名
     * 
     * @return 表名列表
     * @throws DatabaseException 读取失败时抛出
     */
    List<String> getAllTableNames();
    
    /**
     * 检查表是否存在
     * 
     * @param tableName 表名
     * @return 是否存在
     * @throws DatabaseException 检查失败时抛出
     */
    boolean tableExists(String tableName);
    
    /**
     * 获取元数据来源描述
     * 
     * @return 来源描述（如："MySQL 数据库"、"Oracle 数据库"等）
     */
    String getSourceDescription();
}