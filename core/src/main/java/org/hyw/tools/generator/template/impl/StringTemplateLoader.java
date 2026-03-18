package org.hyw.tools.generator.template.impl;

import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * 字符串模板加载器
 * 支持从字符串加载模板内容
 */
public class StringTemplateLoader implements TemplateLoader {

    private final java.util.Map<String, String> templates = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 注册模板
     */
    public void putTemplate(String name, String content) {
        templates.put(name, content);
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return templates.containsKey(name) ? name : null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return System.currentTimeMillis();
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        String name = (String) templateSource;
        String content = templates.get(name);
        return content != null ? new StringReader(content) : null;
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        // 无需关闭
    }
}
