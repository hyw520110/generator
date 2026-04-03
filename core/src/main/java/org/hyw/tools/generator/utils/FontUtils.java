package org.hyw.tools.generator.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 字体处理工具类
 */
public class FontUtils {
    private static final Logger logger = LoggerFactory.getLogger(FontUtils.class);

    /**
     * 获取当前操作系统的 PDF 字体路径列表
     * @param fontConfigs 字体配置字符串（格式：目录路径:文件名模式，逗号分隔多项）
     * @return 找到的字体文件绝对路径列表
     */
    public static List<String> getPdfFontPaths(String fontConfigs) {
        List<String> fontPaths = new ArrayList<>();
        if (StringUtils.isBlank(fontConfigs)) {
            return fontPaths;
        }

        // 解析配置项列表（逗号分隔）
        for (String config : fontConfigs.split(",")) {
            String trimmed = config.trim();
            if (StringUtils.isBlank(trimmed)) {
                continue;
            }

            // 解析 目录:文件名模式
            String[] parts = trimmed.split(":", 2);
            if (parts.length != 2) {
                // 如果没有冒号，尝试直接作为文件路径
                File file = new File(trimmed);
                if (file.exists() && file.isFile()) {
                    fontPaths.add(file.getAbsolutePath());
                }
                continue;
            }

            String dirPath = parts[0].trim();
            String pattern = parts[1].trim();

            File fontFile = findFontFile(new File(dirPath), pattern);
            if (fontFile != null) {
                String path = fontFile.getAbsolutePath();
                if (!fontPaths.contains(path)) {
                    fontPaths.add(path);
                    logger.debug("找到字体: {}", path);
                }
            }
        }

        return fontPaths;
    }

    /**
     * 获取适合当前系统的字体配置字符串
     */
    public static String getOSFontConfig(String macConfig, String winConfig, String linuxConfig) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            return macConfig;
        } else if (osName.contains("windows")) {
            return winConfig;
        } else if (osName.contains("linux") || osName.contains("nix")) {
            return linuxConfig;
        }
        return "";
    }

    /**
     * 在目录中查找匹配模式的字体文件
     * @param dir 字体目录
     * @param pattern 文件名匹配模式（支持 * 通配符）
     * @return 匹配的字体文件，未找到返回 null
     */
    public static File findFontFile(File dir, String pattern) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        // 先尝试精确匹配
        File exactMatch = new File(dir, pattern);
        if (exactMatch.exists()) {
            return exactMatch;
        }

        // 将通配符模式转换为正则表达式
        String regex = pattern.replace(".", "\\.").replace("*", ".*");

        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        // 尝试通配符匹配
        for (File file : files) {
            if (file.isFile() && file.getName().matches(regex)) {
                return file;
            }
        }

        return null;
    }
}
