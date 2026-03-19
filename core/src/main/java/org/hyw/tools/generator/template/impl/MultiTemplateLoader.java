package org.hyw.tools.generator.template.impl;

import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;

/**
 * 复合模板加载器
 * 支持从多个模板加载器加载模板 (优先级从高到低)
 */
public class MultiTemplateLoader implements TemplateLoader {

    private final TemplateLoader[] loaders;

    /**
     * 构造函数
     *
     * @param loaders 模板加载器数组 (按优先级排序)
     */
    public MultiTemplateLoader(TemplateLoader... loaders) {
        this.loaders = loaders;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        for (TemplateLoader loader : loaders) {
            Object source = loader.findTemplateSource(name);
            if (source != null) {
                return new SourceWrapper(loader, source);
            }
        }
        return null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        if (templateSource instanceof SourceWrapper) {
            return ((SourceWrapper) templateSource).loader.getLastModified(
                ((SourceWrapper) templateSource).source);
        }
        return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if (templateSource instanceof SourceWrapper) {
            SourceWrapper wrapper = (SourceWrapper) templateSource;
            return wrapper.loader.getReader(wrapper.source, encoding);
        }
        return null;
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        if (templateSource instanceof SourceWrapper) {
            ((SourceWrapper) templateSource).loader.closeTemplateSource(
                ((SourceWrapper) templateSource).source);
        }
    }

    /**
     * 模板源包装类
     */
    private static class SourceWrapper {
        final TemplateLoader loader;
        final Object source;

        SourceWrapper(TemplateLoader loader, Object source) {
            this.loader = loader;
            this.source = source;
        }
    }
}
