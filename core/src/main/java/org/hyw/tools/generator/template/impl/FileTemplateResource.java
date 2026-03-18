package org.hyw.tools.generator.template.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.apache.commons.io.FileUtils;
import org.hyw.tools.generator.template.TemplateResource;

/**
 * 文件系统模板资源
 */
@Getter
@AllArgsConstructor
public class FileTemplateResource implements TemplateResource {
    private final String path; // 相对路径 (用于输出)
    private final File file;
    private final boolean binary;

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getContent() throws IOException {
        // TODO: 使用配置的编码，暂时硬编码 UTF-8
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }
    
    @Override
    public boolean isBinary() {
        return binary;
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        return FileUtils.readFileToByteArray(file);
    }
}
