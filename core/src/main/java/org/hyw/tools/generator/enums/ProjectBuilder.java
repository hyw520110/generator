package org.hyw.tools.generator.enums;

public enum ProjectBuilder {
    MAVEN("pom.xml"), GRADLE("build.gradle");
    private String fileName;

    private ProjectBuilder(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
