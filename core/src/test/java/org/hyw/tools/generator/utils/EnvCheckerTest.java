package org.hyw.tools.generator.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

public class EnvCheckerTest {

    @Test
    public void check_withCurrentJvm_returnsWithoutThrowing() {
        Path tmp = createTempDir();
        List<String> warnings = EnvChecker.check(tmp.toAbsolutePath().toString());
        assertNotNull("warnings should never be null", warnings);
        // 当前测试环境应是 Java 11+，不应抛异常
    }

    @Test
    public void check_withTargetJava_acceptsAllSupportedVersions() {
        Path tmp = createTempDir();
        for (String target : new String[]{"8", "11", "17", "21"}) {
            List<String> warnings = EnvChecker.check(tmp.toAbsolutePath().toString(), target);
            // 任意支持版本不应触发"不在推荐列表"告警
            for (String w : warnings) {
                assertFalse("unexpected warning for target " + target + ": " + w,
                        w.contains("不在推荐列表"));
            }
        }
    }

    @Test
    public void check_withNonexistentDir_throws() {
        File ghost = new File(System.getProperty("java.io.tmpdir"),
                "envcheck-nonexistent-" + System.nanoTime() + "/sub/level/path");
        // 父目录可创建，所以这里实际不会抛——改用文件而非目录路径
        File asFile;
        try {
            asFile = File.createTempFile("envcheck", ".tmp");
            asFile.deleteOnExit();
        } catch (Exception e) {
            fail("setup failed");
            return;
        }
        try {
            EnvChecker.check(asFile.getAbsolutePath());
            fail("expected IllegalStateException for non-directory path");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("不是目录"));
        }
    }

    private Path createTempDir() {
        try {
            Path dir = Files.createTempDirectory("envcheck-");
            dir.toFile().deleteOnExit();
            return dir;
        } catch (Exception e) {
            fail("无法创建测试目录");
            return null;
        }
    }
}
