package org.hyw.tools.generator.template.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.template.TemplateResource;
import org.hyw.tools.generator.utils.StringUtils;

public class JarTemplateResource implements TemplateResource {
    private final JarFile jarFile;
    private final JarEntry jarEntry;
    private final String path;
    private final boolean binary;

    public JarTemplateResource(JarFile jarFile, JarEntry jarEntry, String path, boolean binary) {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
        this.path = path;
        this.binary = binary;
    }

    @Override
    public String getPath() { return path; }

    @Override
    public String getName() {
        String name = jarEntry.getName();
        int index = name.lastIndexOf("/");
        return index != -1 ? name.substring(index + 1) : name;
    }

    @Override
    public String getContent() throws IOException {
        try (InputStream is = openStream()) {
            return IOUtils.toString(is, Consts.DEFAULT_ENCODING);
        }
    }

    @Override
    public boolean isBinary() { return binary; }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream is = openStream()) {
            return IOUtils.toByteArray(is);
        }
    }

    @Override
    public InputStream openStream() throws IOException {
        return jarFile.getInputStream(jarEntry);
    }
}
