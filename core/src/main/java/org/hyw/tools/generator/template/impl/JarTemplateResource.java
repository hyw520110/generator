package org.hyw.tools.generator.template.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import lombok.Getter;

import org.apache.commons.io.IOUtils;
import org.hyw.tools.generator.template.TemplateResource;

/**
 * JAR 模板资源
 */
@Getter
public class JarTemplateResource implements TemplateResource {
    private final String path; // 相对路径 (用于输出)
    private final URL url;
    private final boolean binary;
    private final String name;

    public JarTemplateResource(String path, URL url, boolean binary) {
        this.path = path;
        this.url = url;
        this.binary = binary;
        String urlStr = url.toString();
        // 简单提取文件名，可能不完全准确但通常足够
        int lastSlash = urlStr.lastIndexOf('/');
        if (lastSlash != -1) {
            this.name = urlStr.substring(lastSlash + 1);
        } else {
            this.name = urlStr;
        }
    }

    @Override
    public String getContent() throws IOException {
        try (InputStream is = url.openStream()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
    
    @Override
    public boolean isBinary() {
        return binary;
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream is = url.openStream()) {
            return IOUtils.toByteArray(is);
        }
    }
}
