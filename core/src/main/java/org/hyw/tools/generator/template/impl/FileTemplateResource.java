package org.hyw.tools.generator.template.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.template.TemplateResource;

public class FileTemplateResource implements TemplateResource {
    private final File file;
    private final String path;
    private final boolean binary;

    public FileTemplateResource(File file, String path, boolean binary) {
        this.file = file;
        this.path = path;
        this.binary = binary;
    }

    @Override
    public String getPath() { return path; }

    @Override
    public String getName() { return file.getName(); }

    @Override
    public String getContent() throws IOException {
        return FileUtils.readFileToString(file, Consts.DEFAULT_ENCODING);
    }

    @Override
    public boolean isBinary() { return binary; }

    @Override
    public byte[] getBytes() throws IOException {
        return FileUtils.readFileToByteArray(file);
    }

    @Override
    public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }
}
