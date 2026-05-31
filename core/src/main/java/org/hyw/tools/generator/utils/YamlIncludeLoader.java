package org.hyw.tools.generator.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * 支持 {@code include: [a.yml, b.yml]} 语法的 YAML 加载器。
 *
 * <p>合并策略（被 include 的文件先加载，主文件后加载并覆盖）：</p>
 * <ul>
 *   <li>Map 之间深合并（同 key 递归合并）</li>
 *   <li>非 Map 类型直接覆盖</li>
 *   <li>循环 include 自动检测并跳过</li>
 * </ul>
 *
 * <p>路径解析：相对路径相对于当前 YAML 文件所在目录；绝对路径直接使用。</p>
 */
public final class YamlIncludeLoader {

    private static final Logger logger = LoggerFactory.getLogger(YamlIncludeLoader.class);
    private static final String INCLUDE_KEY = "include";

    private YamlIncludeLoader() {
    }

    /** 从 URL 加载并展开 include。仅支持 file: 协议的相对 include；jar/classpath 不解析 include。 */
    public static String loadAndMerge(URL url) throws IOException {
        if (url == null) {
            return "";
        }
        String protocol = url.getProtocol();
        if (!"file".equalsIgnoreCase(protocol)) {
            // 非文件系统资源（jar/classpath）：原样读取，不展开 include
            try (InputStream is = url.openStream()) {
                return readAll(is);
            }
        }
        try {
            Path path = Paths.get(URI.create(url.toString()));
            return mergeFile(path).toString();
        } catch (Exception e) {
            throw new IOException("加载 YAML 失败: " + url, e);
        }
    }

    /** 从 File 加载并展开 include，返回合并后的 YAML 字符串。 */
    public static String loadAndMerge(File file) throws IOException {
        return mergeFile(file.toPath()).toString();
    }

    private static MergedYaml mergeFile(Path entry) throws IOException {
        Map<String, Object> merged = new LinkedHashMap<>();
        Set<Path> visited = new HashSet<>();
        loadInto(entry.toAbsolutePath().normalize(), merged, visited);
        return new MergedYaml(merged);
    }

    @SuppressWarnings("unchecked")
    private static void loadInto(Path file, Map<String, Object> target, Set<Path> visited) throws IOException {
        if (!visited.add(file)) {
            logger.warn("YAML include 循环引用，跳过: {}", file);
            return;
        }
        if (!Files.exists(file)) {
            logger.warn("YAML include 文件不存在，跳过: {}", file);
            return;
        }
        Map<String, Object> raw;
        try (InputStream is = Files.newInputStream(file)) {
            Object loaded = new Yaml().load(is);
            raw = loaded instanceof Map ? (Map<String, Object>) loaded : new LinkedHashMap<>();
        }

        // 先处理 include：被引用的内容作为底层，本文件覆盖之
        Object includes = raw.remove(INCLUDE_KEY);
        if (includes instanceof List) {
            for (Object inc : (List<Object>) includes) {
                if (inc == null) continue;
                Path resolved = file.getParent().resolve(inc.toString()).normalize();
                loadInto(resolved, target, visited);
            }
        } else if (includes instanceof String) {
            Path resolved = file.getParent().resolve((String) includes).normalize();
            loadInto(resolved, target, visited);
        }

        deepMerge(target, raw);
    }

    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> override) {
        if (override == null) return;
        for (Map.Entry<String, Object> e : override.entrySet()) {
            Object current = target.get(e.getKey());
            Object value = e.getValue();
            if (current instanceof Map && value instanceof Map) {
                Map<String, Object> merged = new LinkedHashMap<>((Map<String, Object>) current);
                deepMerge(merged, (Map<String, Object>) value);
                target.put(e.getKey(), merged);
            } else {
                target.put(e.getKey(), value);
            }
        }
    }

    private static String readAll(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** 合并结果包装：toString() 返回可重新 dump 的 YAML 文本。 */
    private static final class MergedYaml {
        private final Map<String, Object> data;

        MergedYaml(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public String toString() {
            org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
            options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
            return new Yaml(options).dump(data);
        }
    }
}
