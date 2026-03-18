package org.hyw.tools.generator.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.hyw.tools.generator.exception.FileOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 静态资源管理器
 * <p>
 * 管理不需要模板渲染的静态资源文件，支持跨引擎复用
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class ResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    /**
     * 默认二进制文件扩展名（不渲染，直接复制）
     */
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
        // 图片
        "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp",
        // 字体
        "woff", "woff2", "ttf", "eot", "otf",
        // 压缩包
        "zip", "jar", "rar", "7z",
        // 媒体文件
        "mp3", "mp4", "avi", "mov", "wmv",
        // 其他二进制
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        // 前端资源
        "map"  // source map
    );

    /**
     * 模板引擎无关的资源目录
     */
    private static final String SHARED_RESOURCES_DIR = "/resources/shared";

    /**
     * Velocity 专属资源目录
     */
    private static final String VELOCITY_RESOURCES_DIR = "/resources/velocity";

    /**
     * FreeMarker 专属资源目录
     */
    private static final String FREEMARKER_RESOURCES_DIR = "/resources/freemarker";

    /**
     * 资源缓存（避免重复扫描）
     */
    private final ConcurrentHashMap<String, List<ResourceInfo>> resourceCache = new ConcurrentHashMap<>();

    /**
     * 资源根目录
     */
    private final String resourceRoot;

    /**
     * 构造函数
     */
    public ResourceManager() {
        this.resourceRoot = "classpath:/resources";
    }

    /**
     * 构造函数（指定资源根目录）
     *
     * @param resourceRoot 资源根目录
     */
    public ResourceManager(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    /**
     * 获取所有共享资源（引擎无关）
     *
     * @param baseDir 基础目录
     * @return 资源列表
     */
    public List<ResourceInfo> getSharedResources(String baseDir) {
        return scanResources(SHARED_RESOURCES_DIR + "/" + baseDir);
    }

    /**
     * 获取指定引擎的资源
     *
     * @param baseDir 基础目录
     * @param engineType 引擎类型（velocity/freemarker）
     * @return 资源列表
     */
    public List<ResourceInfo> getEngineResources(String baseDir, String engineType) {
        String engineDir = engineType.equalsIgnoreCase("velocity") 
            ? VELOCITY_RESOURCES_DIR 
            : FREEMARKER_RESOURCES_DIR;
        return scanResources(engineDir + "/" + baseDir);
    }

    /**
     * 获取所有资源（共享 + 引擎专属）
     *
     * @param baseDir 基础目录
     * @param engineType 引擎类型
     * @return 资源列表
     */
    public List<ResourceInfo> getAllResources(String baseDir, String engineType) {
        List<ResourceInfo> all = new ArrayList<>();
        all.addAll(getSharedResources(baseDir));
        all.addAll(getEngineResources(baseDir, engineType));
        return all;
    }

    /**
     * 扫描资源目录
     *
     * @param dirPath 目录路径
     * @return 资源列表
     */
    private List<ResourceInfo> scanResources(String dirPath) {
        // 检查缓存
        if (resourceCache.containsKey(dirPath)) {
            logger.debug("命中资源缓存：{}", dirPath);
            return resourceCache.get(dirPath);
        }

        List<ResourceInfo> resources = new ArrayList<>();
        
        try {
            URL url = getClass().getResource(dirPath);
            if (url == null) {
                logger.debug("资源目录不存在：{}", dirPath);
                return Collections.emptyList();
            }

            // 从文件系统或 JAR 中扫描资源
            if ("file".equals(url.getProtocol())) {
                scanFileSystem(new File(url.getPath()), dirPath, resources);
            } else if ("jar".equals(url.getProtocol())) {
                scanJar(url, dirPath, resources);
            }

            // 缓存结果
            resourceCache.put(dirPath, resources);
            logger.debug("扫描到 {} 个资源文件：{}", resources.size(), dirPath);

        } catch (Exception e) {
            logger.error("扫描资源失败：{}", dirPath, e);
        }

        return resources;
    }

    /**
     * 从文件系统扫描资源
     */
    private void scanFileSystem(File dir, String basePath, List<ResourceInfo> resources) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录
                scanFileSystem(file, basePath + "/" + file.getName(), resources);
            } else {
                // 添加文件
                String relativePath = basePath + "/" + file.getName();
                resources.add(new ResourceInfo(relativePath, file, isBinaryFile(file.getName())));
            }
        }
    }

    /**
     * 使用 JarFile 直接扫描 JAR 中的资源（避免 URL 解码问题）
     * </p>
     */
    private void scanJar(URL url, String basePath, List<ResourceInfo> resources) {
        try {
            // 从 URL 中提取 JAR 文件路径
            String jarPath = url.getPath();
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring(5);
            }
            // 移除 JAR URL 中的条目部分
            int exclamationIndex = jarPath.indexOf('!');
            if (exclamationIndex > 0) {
                jarPath = jarPath.substring(0, exclamationIndex);
            }

            // 直接打开 JAR 文件
            java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath);

            // 枚举 JAR 条目
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            String entryPrefix = basePath.startsWith("/") ? basePath.substring(1) : basePath;

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // 匹配路径
                if (!entry.isDirectory() && entryName.startsWith(entryPrefix)) {
                    // 计算相对路径
                    String relativePath = entryName;
                    if (entryName.startsWith("/")) {
                        relativePath = entryName.substring(1);
                    }

                    // 添加资源
                    resources.add(new ResourceInfo(relativePath, null, isBinaryFile(entryName)));
                }
            }

            jarFile.close();
        } catch (IOException e) {
            logger.error("扫描 JAR 资源失败：{}", basePath, e);
        }
    }

    /**
     * 判断是否为二进制文件
     */
    private boolean isBinaryFile(String fileName) {
        String extension = getFileExtension(fileName);
        return BINARY_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    /**
     * 复制资源文件到目标目录
     *
     * @param resource 资源信息
     * @param targetDir 目标目录
     * @param targetBasePath 目标基础路径（用于路径转换）
     */
    public void copyResource(ResourceInfo resource, File targetDir, String targetBasePath) {
        if (resource == null || targetDir == null) {
            return;
        }

        try {
            // 计算目标路径
            String relativePath = resource.getSourcePath();
            if (relativePath.startsWith(SHARED_RESOURCES_DIR)) {
                relativePath = relativePath.substring(SHARED_RESOURCES_DIR.length());
            } else if (relativePath.startsWith(VELOCITY_RESOURCES_DIR)) {
                relativePath = relativePath.substring(VELOCITY_RESOURCES_DIR.length());
            } else if (relativePath.startsWith(FREEMARKER_RESOURCES_DIR)) {
                relativePath = relativePath.substring(FREEMARKER_RESOURCES_DIR.length());
            }

            // 移除开头的斜杠
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            // 构建目标文件
            File targetFile = new File(targetDir, targetBasePath + "/" + relativePath);

            // 确保父目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 复制文件
            if (resource.getFile() != null) {
                // 从文件系统复制
                FileUtils.copyFile(resource.getFile(), targetFile);
            } else {
                // 从 JAR 复制
                String resourcePath = resource.getSourcePath();
                if (!resourcePath.startsWith("/")) {
                    resourcePath = "/" + resourcePath;
                }
                
                try (java.io.InputStream is = getClass().getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        logger.error("无法读取 JAR 资源：{}", resourcePath);
                        return;
                    }
                    FileUtils.copyInputStreamToFile(is, targetFile);
                }
            }

            logger.debug("复制资源：{} -> {}", resource.getSourcePath(), targetFile);

        } catch (IOException e) {
            logger.error("复制资源失败：{}", resource.getSourcePath(), e);
            throw new FileOperationException(resource.getFile(), "复制资源失败：" + e.getMessage());
        }
    }

    /**
     * 批量复制资源
     *
     * @param resources 资源列表
     * @param targetDir 目标目录
     * @param targetBasePath 目标基础路径
     * @return 复制的文件数量
     */
    public int copyResources(List<ResourceInfo> resources, File targetDir, String targetBasePath) {
        if (resources == null || resources.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ResourceInfo resource : resources) {
            copyResource(resource, targetDir, targetBasePath);
            count++;
        }
        return count;
    }

    /**
     * 清空资源缓存
     */
    public void clearCache() {
        resourceCache.clear();
        logger.info("清空资源缓存");
    }

    /**
     * 获取资源统计信息
     *
     * @return 统计信息
     */
    public ResourceStatistics getStatistics() {
        ResourceStatistics stats = new ResourceStatistics();
        
        for (List<ResourceInfo> resources : resourceCache.values()) {
            for (ResourceInfo resource : resources) {
                stats.totalCount++;
                if (resource.isBinary()) {
                    stats.binaryCount++;
                } else {
                    stats.textCount++;
                }
            }
        }
        
        return stats;
    }

    /**
     * 资源信息
     */
    public static class ResourceInfo {
        
        /**
         * 源文件路径
         */
        private final String sourcePath;
        
        /**
         * 文件对象（文件系统资源）
         */
        private final File file;
        
        /**
         * 是否为二进制文件
         */
        private final boolean binary;

        public ResourceInfo(String sourcePath, File file, boolean binary) {
            this.sourcePath = sourcePath;
            this.file = file;
            this.binary = binary;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public File getFile() {
            return file;
        }

        public boolean isBinary() {
            return binary;
        }

        @Override
        public String toString() {
            return "ResourceInfo{" +
                   "sourcePath='" + sourcePath + '\'' +
                   ", binary=" + binary +
                   '}';
        }
    }

    /**
     * 资源统计信息
     */
    public static class ResourceStatistics {
        public int totalCount = 0;
        public int binaryCount = 0;
        public int textCount = 0;

        @Override
        public String toString() {
            return "ResourceStatistics{total=" + totalCount + 
                   ", binary=" + binaryCount + 
                   ", text=" + textCount + '}';
        }
    }
}
