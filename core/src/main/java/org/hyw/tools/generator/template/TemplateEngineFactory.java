package org.hyw.tools.generator.template;

import org.hyw.tools.generator.template.impl.FreeMarkerEngineImpl;
import org.hyw.tools.generator.template.impl.VelocityEngineImpl;

import java.util.EnumMap;
import java.util.Map;

/**
 * 模板引擎工厂
 * 
 * @author heyiwu
 */
public class TemplateEngineFactory {
    
    /**
     * 引擎实例缓存
     */
    private static final Map<EngineType, TemplateEngine> engines = new EnumMap<>(EngineType.class);
    
    static {
        // 初始化引擎
        engines.put(EngineType.VELOCITY, new VelocityEngineImpl());
        engines.put(EngineType.FREEMARKER, new FreeMarkerEngineImpl());
    }
    
    /**
     * 获取指定类型的引擎
     */
    public static TemplateEngine getEngine(EngineType type) {
        TemplateEngine engine = engines.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的模板引擎类型：" + type);
        }
        return engine;
    }
    
    /**
     * 获取 Velocity 引擎
     */
    public static VelocityEngineImpl getVelocityEngine() {
        return (VelocityEngineImpl) engines.get(EngineType.VELOCITY);
    }
    
    /**
     * 获取 FreeMarker 引擎
     */
    public static FreeMarkerEngineImpl getFreeMarkerEngine() {
        return (FreeMarkerEngineImpl) engines.get(EngineType.FREEMARKER);
    }
    
    /**
     * 根据模板路径自动选择引擎
     */
    public static TemplateEngine getEngineForTemplate(String templatePath) {
        if (templatePath == null) {
            return getEngine(EngineType.VELOCITY); // 默认 Velocity
        }
        
        for (TemplateEngine engine : engines.values()) {
            if (engine.supports(templatePath)) {
                return engine;
            }
        }
        
        // 默认返回 Velocity
        return getEngine(EngineType.VELOCITY);
    }
    
    /**
     * 关闭所有引擎
     */
    public static void shutdown() {
        engines.clear();
    }
}
