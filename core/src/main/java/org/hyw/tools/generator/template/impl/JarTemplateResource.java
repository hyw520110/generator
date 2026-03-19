package org.hyw.tools.generator.template.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;
import org.hyw.tools.generator.template.TemplateResource;

public class JarTemplateResource implements TemplateResource {
    private final JarFile jarFile;
    private final JarEntry jarEntry;
    private final String path;

    public JarTemplateResource(JarFile jarFile, JarEntry jarEntry, String path) {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
        this.path = path;
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
        try (InputStream is = jarFile.getInputStream(jarEntry)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Override
    public boolean isBinary() { return false; }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream is = jarFile.getInputStream(jarEntry)) {
            return IOUtils.toByteArray(is);
        }
    }
}
