package org.hyw.tools.generator.conf.converts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hyw.tools.generator.conf.converts.impl.YamlTypeConvertor;
import org.hyw.tools.generator.enums.db.DBType;
import org.hyw.tools.generator.exception.ConfigurationException;

/**
 * 类型转换策略工厂
 * 
 * @author heyiwu
 * @version 2.0
 */
public class TypeConvertStrategyFactory {
    
    /**
     * 策略缓存
     */
    private static final Map<String, TypeConvertor> STRATEGY_CACHE = new ConcurrentHashMap<>();
    
    static {
        // 统一使用基于 YAML 的动态转换器实现模块化配置
        for (DBType dbType : DBType.values()) {
            register(dbType.getName(), new YamlTypeConvertor(dbType));
        }
    }
    
    /**
     * 注册策略
     */
    public static void register(String dbTypeName, TypeConvertor strategy) {
        STRATEGY_CACHE.put(dbTypeName.toLowerCase(), strategy);
    }
    
    /**
     * 获取策略
     * 
     * @param dbType 数据库类型
     * @return 类型转换器
     * @throws ConfigurationException 如果未找到对应策略
     */
    public static TypeConvertor getStrategy(DBType dbType) {
        if (dbType == null) {
            throw new ConfigurationException("数据库类型不能为空");
        }
        
        TypeConvertor strategy = STRATEGY_CACHE.get(dbType.getName().toLowerCase());
        if (strategy == null) {
            throw new ConfigurationException("未找到数据库类型 [" + dbType.getName() + "] 的类型转换策略");
        }
        
        return strategy;
    }
    
    /**
     * 获取策略（带默认值）
     */
    public static TypeConvertor getStrategy(DBType dbType, TypeConvertor defaultStrategy) {
        TypeConvertor strategy = STRATEGY_CACHE.get(dbType != null ? dbType.getName().toLowerCase() : null);
        return strategy != null ? strategy : defaultStrategy;
    }
    
    /**
     * 清除所有注册策略（用于测试）
     */
    public static void clear() {
        STRATEGY_CACHE.clear();
    }
}
