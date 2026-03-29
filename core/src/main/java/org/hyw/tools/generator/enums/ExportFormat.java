package org.hyw.tools.generator.enums;

/**
 * 导出格式枚举
 */
public enum ExportFormat {

    PDF("pdf", "PDF 文档"),
    WORD("docx", "Word 文档"),
    EXCEL("xlsx", "Excel 表格"),
    HTML("html", "HTML 页面"),
    MARKDOWN("md", "Markdown 文本");

    private final String extension;
    private final String description;

    ExportFormat(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    public String getExtension() { return extension; }
    public String getDescription() { return description; }

    public static ExportFormat fromExtension(String ext) {
        if (ext == null) return null;
        for (ExportFormat format : values()) {
            if (format.extension.equalsIgnoreCase(ext)) return format;
        }
        return null;
    }
}
