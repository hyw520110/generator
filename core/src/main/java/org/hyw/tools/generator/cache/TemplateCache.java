package org.hyw.tools.generator.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * 模板缓存工具类
 * <p>
 * 使用 Caffeine 实现模板内容缓存，避免重复加载
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class TemplateCache {

    /**
     * 模板内容缓存 (10 分钟过期)
     */
    private static final Cache<String, String> TEMPLATE_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats()
            .build();

    /**
     * 模板路径缓存 (永久有效)
     */
    private static final Cache<String, String> PATH_CACHE = Caffeine.newBuilder()
            .maximumSize(5000)
            .build();

    /**
     * 获取缓存的模板内容
     *
     * @param key 缓存键 (模板路径)
     * @return 模板内容，不存在返回 null
     */
    public static String getTemplate(String key) {
        return TEMPLATE_CACHE.getIfPresent(key);
    }

    /**
     * 缓存模板内容
     *
     * @param key     缓存键
     * @param content 模板内容
     */
    public static void putTemplate(String key, String content) {
        if (key != null && content != null) {
            TEMPLATE_CACHE.put(key, content);
        }
    }

    /**
     * 获取或加载模板内容
     *
     * @param key     缓存键
     * @param loader  加载器
     * @return 模板内容
     */
    public static String getTemplate(String key, java.util.function.Supplier<String> loader) {
        return TEMPLATE_CACHE.get(key, k -> loader.get());
    }

    /**
     * 移除模板缓存
     *
     * @param key 缓存键
     */
    public static void invalidateTemplate(String key) {
        TEMPLATE_CACHE.invalidate(key);
    }

    /**
     * 清空所有模板缓存
     */
    public static void invalidateAllTemplates() {
        TEMPLATE_CACHE.invalidateAll();
    }

    /**
     * 获取缓存的模板路径
     *
     * @param key 原始路径
     * @return 处理后的路径
     */
    public static String getPath(String key) {
        return PATH_CACHE.getIfPresent(key);
    }

    /**
     * 缓存路径
     *
     * @param key   原始路径
     * @param value 处理后的路径
     */
    public static void putPath(String key, String value) {
        if (key != null && value != null) {
            PATH_CACHE.put(key, value);
        }
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息字符串
     */
    public static String getStats() {
        return String.format(
            "Template Cache - hits: %d, misses: %d, evictions: %d, size: %d",
            TEMPLATE_CACHE.stats().hitCount(),
            TEMPLATE_CACHE.stats().missCount(),
            TEMPLATE_CACHE.stats().evictionCount(),
            TEMPLATE_CACHE.estimatedSize()
        );
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率 (0.0-1.0)
     */
    public static double getHitRate() {
        long hits = TEMPLATE_CACHE.stats().hitCount();
        long misses = TEMPLATE_CACHE.stats().missCount();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
}
