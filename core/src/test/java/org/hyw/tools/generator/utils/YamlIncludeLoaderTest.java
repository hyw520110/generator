package org.hyw.tools.generator.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class YamlIncludeLoaderTest {

    @Test
    public void include_mergesNestedMaps_andMainOverridesIncluded() throws Exception {
        Path dir = Files.createTempDirectory("yaml-include-");
        dir.toFile().deleteOnExit();

        Path base = dir.resolve("base.yml");
        Files.write(base, ("global:\n  outputDir: /from-base\n  rootPackage: com.base\n").getBytes(StandardCharsets.UTF_8));
        Path main = dir.resolve("main.yml");
        Files.write(main, ("include:\n  - base.yml\nglobal:\n  rootPackage: com.main\n").getBytes(StandardCharsets.UTF_8));

        String merged = YamlIncludeLoader.loadAndMerge(main.toFile());
        Map<String, Object> data = new Yaml().load(merged);
        @SuppressWarnings("unchecked")
        Map<String, Object> global = (Map<String, Object>) data.get("global");
        // 主文件覆盖
        assertEquals("com.main", global.get("rootPackage"));
        // 被 include 的字段保留
        assertEquals("/from-base", global.get("outputDir"));
    }

    @Test
    public void include_handlesCircularReference_gracefully() throws Exception {
        Path dir = Files.createTempDirectory("yaml-circular-");
        dir.toFile().deleteOnExit();

        Path a = dir.resolve("a.yml");
        Path b = dir.resolve("b.yml");
        Files.write(a, ("include: [b.yml]\nfromA: 1\n").getBytes(StandardCharsets.UTF_8));
        Files.write(b, ("include: [a.yml]\nfromB: 2\n").getBytes(StandardCharsets.UTF_8));

        String merged = YamlIncludeLoader.loadAndMerge(a.toFile());
        Map<String, Object> data = new Yaml().load(merged);
        // 两个文件都应有，循环不应抛异常
        assertEquals(1, ((Number) data.get("fromA")).intValue());
        assertEquals(2, ((Number) data.get("fromB")).intValue());
    }

    @Test
    public void include_missingFile_logsButContinues() throws Exception {
        Path dir = Files.createTempDirectory("yaml-missing-");
        dir.toFile().deleteOnExit();

        Path main = dir.resolve("main.yml");
        Files.write(main, ("include: [does-not-exist.yml]\nkey: value\n").getBytes(StandardCharsets.UTF_8));

        String merged = YamlIncludeLoader.loadAndMerge(main.toFile());
        Map<String, Object> data = new Yaml().load(merged);
        assertEquals("value", data.get("key"));
    }

    @Test
    public void noInclude_loadsRawYaml() throws Exception {
        Path dir = Files.createTempDirectory("yaml-plain-");
        dir.toFile().deleteOnExit();

        Path main = dir.resolve("main.yml");
        Files.write(main, ("name: foo\nlevel: 3\n").getBytes(StandardCharsets.UTF_8));

        String merged = YamlIncludeLoader.loadAndMerge(main.toFile());
        Map<String, Object> data = new Yaml().load(merged);
        assertEquals("foo", data.get("name"));
        assertEquals(3, ((Number) data.get("level")).intValue());
    }
}
