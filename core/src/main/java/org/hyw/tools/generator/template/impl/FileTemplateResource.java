package org.hyw.tools.generator.template.impl;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.hyw.tools.generator.template.TemplateResource;

public class FileTemplateResource implements TemplateResource {
    private final File file;
    private final String path;

    public FileTemplateResource(File file, String path) {
        this.file = file;
        this.path = path;
    }

    @Override
    public String getPath() { return path; }

    @Override
    public String getName() { return file.getName(); }

    @Override
    public String getContent() throws IOException {
        return FileUtils.readFileToString(file, "UTF-8");
    }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public byte[] getBytes() throws IOException {
        return FileUtils.readFileToByteArray(file);
    }
}
