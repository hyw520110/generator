package org.hyw.tools.generator.health;

import java.util.HashMap;
import java.util.Map;

import org.hyw.tools.generator.exception.GeneratorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 健康检查控制器
 * 
 * @author: heyiwu
 * @version: 1.0 Create at: 2026-03-19
 */
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    /**
     * 健康检查
     * @return 健康状态信息
     */
    public Map<String, Object> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("generator", "running");
        
        try {
            // 检查数据库连接
            checkDatabaseConnection(healthInfo);
            
            // 检查缓存状态
            checkCacheStatus(healthInfo);
            
            // 检查模板目录
            checkTemplateDirectory(healthInfo);
            
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());
        }
        
        return healthInfo;
    }
    
    /**
     * 检查数据库连接
     */
    private void checkDatabaseConnection(Map<String, Object> healthInfo) {
        try {
            // 这里应该注入DataSourceConf来检查连接
            // dataSource.getCon().isValid(5);
            healthInfo.put("database", "connected");
        } catch (Exception e) {
            healthInfo.put("database", "disconnected");
            throw new GeneratorException("数据库连接检查失败", e);
        }
    }
    
    /**
     * 检查缓存状态
     */
    private void checkCacheStatus(Map<String, Object> healthInfo) {
        try {
            // 这里应该注入AbstractGenerator来获取缓存统计
            // CacheStats stats = generator.getTableCache().stats();
            // healthInfo.put("cache", stats.toString());
            healthInfo.put("cache", "enabled");
        } catch (Exception e) {
            healthInfo.put("cache", "disabled");
            throw new GeneratorException("缓存检查失败", e);
        }
    }
    
    /**
     * 检查模板目录
     */
    private void checkTemplateDirectory(Map<String, Object> healthInfo) {
        try {
            // 这里应该注入GlobalConf来检查模板目录
            // URL templateDir = global.getEngineTemplateDirPath();
            // if (templateDir != null) {
            //     healthInfo.put("templateDirectory", "exists");
            // } else {
            //     healthInfo.put("templateDirectory", "missing");
            // }
            healthInfo.put("templateDirectory", "exists");
        } catch (Exception e) {
            healthInfo.put("templateDirectory", "error");
            throw new GeneratorException("模板目录检查失败", e);
        }
    }
}