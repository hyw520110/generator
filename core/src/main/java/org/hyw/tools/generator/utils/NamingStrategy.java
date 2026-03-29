package org.hyw.tools.generator.utils;

import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * 命名策略工具类
 * 提供表名到类名、包名、变量名的统一转换逻辑
 *
 * @author heyiwu
 */
public class NamingStrategy {

    /**
     * 生成结构化的命名 Map
     * @param beanName 经过处理后的基础类名（如 User）
     * @return 包含各种命名变体的 Map
     */
    public static Map<String, String> buildNamingMap(String beanName) {
        Map<String, String> naming = new HashMap<>();
        
        // 基础类名
        naming.put("entity", beanName);
        naming.put("entityLower", StringUtils.uncapitalize(beanName));
        
        // 常用组件名
        naming.put("mapper", beanName + "Mapper");
        naming.put("mapperLower", StringUtils.uncapitalize(beanName) + "Mapper");
        naming.put("dao", beanName + "Dao");
        naming.put("daoLower", StringUtils.uncapitalize(beanName) + "Dao");
        
        naming.put("service", beanName + "Service");
        naming.put("serviceLower", StringUtils.uncapitalize(beanName) + "Service");
        naming.put("serviceImpl", beanName + "ServiceImpl");
        
        naming.put("controller", beanName + "Controller");
        naming.put("controllerLower", StringUtils.uncapitalize(beanName) + "Controller");
        
        naming.put("vo", beanName + "VO");
        naming.put("voName", beanName + "Vo");
        naming.put("voNameLower", StringUtils.uncapitalize(beanName) + "Vo");
        naming.put("dto", beanName + "DTO");
        
        return naming;
    }
}
