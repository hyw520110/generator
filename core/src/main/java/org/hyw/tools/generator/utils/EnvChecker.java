package org.hyw.tools.generator.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hyw.tools.generator.platform.PlatformAdapter;
import org.hyw.tools.generator.platform.PlatformAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 启动环境预检：在执行生成前快速校验运行环境，
 * 把"运行时崩溃"前移为"启动期可读错误"，便于诊断。
 *
 * <p>区分两种 JDK：</p>
 * <ul>
 *   <li><b>运行时 JDK</b>：当前跑生成器的 JVM。要求 &gt;= {@value #MIN_JDK_MAJOR}（生成器自身基线）。</li>
 *   <li><b>目标 JDK</b>：生成的工程要求的 Java 版本（{@code global.javaVersion}）。可独立选择 8/11/17/21，
 *       不要求和运行时 JDK 相同——例如 Java 21 上跑生成器、生成 Java 8 的工程是合法组合。</li>
 * </ul>
 */
public final class EnvChecker {

    private static final Logger logger = LoggerFactory.getLogger(EnvChecker.class);

    /** 生成器自身的最低运行时 JDK（编译目标基线）。 */
    private static final int MIN_JDK_MAJOR = 11;

    private EnvChecker() {
    }

    /**
     * 执行环境预检。
     *
     * @param outputDir 输出目录（用于写权限校验）
     * @return 预检发现的告警列表（非致命问题）；致命问题直接抛 IllegalStateException
     */
    public static List<String> check(String outputDir) {
        return check(outputDir, null);
    }

    /**
     * 执行环境预检（携带目标 JDK 版本）。
     *
     * @param outputDir   输出目录
     * @param targetJava  生成的工程要求的 Java 版本（如 "8" / "11" / "17" / "21"），可为 null
     */
    public static List<String> check(String outputDir, String targetJava) {
        List<String> warnings = new ArrayList<>();
        int runtimeMajor = checkJdk(warnings);
        checkTargetJava(runtimeMajor, targetJava, warnings);
        checkCharset(warnings);
        checkOutputDirWritable(outputDir);
        return warnings;
    }

    private static int checkJdk(List<String> warnings) {
        PlatformAdapter adapter = PlatformAdapters.current();
        int major = adapter.runtimeMajor();
        String specVersion = System.getProperty("java.specification.version", String.valueOf(major));
        if (major > 0 && major < MIN_JDK_MAJOR) {
            throw new IllegalStateException(
                    "运行时 JDK 版本过低: " + specVersion + "，生成器要求 >= " + MIN_JDK_MAJOR
                            + "（生成的工程仍可指定 Java 8/11/17/21 任意版本）");
        }
        if (major == 0) {
            warnings.add("无法识别 JDK 版本: java.specification.version=" + specVersion);
        }
        logger.info("环境预检：运行时 JDK={} ({}), 适配器={}", specVersion,
                System.getProperty("java.vendor", ""), adapter.describe());
        return major;
    }

    /**
     * 校验目标 JDK 版本：
     * <ul>
     *   <li>必须是支持的版本（8/11/17/21）</li>
     *   <li>目标版本可任意选择，不强制小于等于运行时 JDK——
     *       例如 Java 11 跑生成器、生成 Java 17 的工程是允许的，只是生成的工程需要在 Java 17+ 上构建运行。</li>
     * </ul>
     */
    private static void checkTargetJava(int runtimeMajor, String targetJava, List<String> warnings) {
        if (targetJava == null || targetJava.trim().isEmpty()) {
            return;
        }
        int target = parseMajor(targetJava);
        if (target == 0) {
            warnings.add("无法识别目标 Java 版本: " + targetJava);
            return;
        }
        if (target != 8 && target != 11 && target != 17 && target != 21) {
            warnings.add("目标 Java 版本 " + target + " 不在推荐列表（8/11/17/21）");
        }
        logger.info("环境预检：目标工程 JDK={}（运行时={}）", target, runtimeMajor);
    }

    private static int parseMajor(String specVersion) {
        if (specVersion == null || specVersion.isEmpty()) {
            return 0;
        }
        // "1.8" → 8；"17" / "21" → 17 / 21
        try {
            if (specVersion.startsWith("1.")) {
                return Integer.parseInt(specVersion.substring(2));
            }
            return Integer.parseInt(specVersion);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void checkCharset(List<String> warnings) {
        Charset defaultCharset = Charset.defaultCharset();
        String fileEncoding = System.getProperty("file.encoding", "");
        if (!StandardCharsets.UTF_8.equals(defaultCharset)) {
            warnings.add("默认字符集非 UTF-8: " + defaultCharset
                    + "，可能导致模板渲染中文乱码。建议设置 -Dfile.encoding=UTF-8");
        }
        logger.info("环境预检：默认字符集={} (file.encoding={})", defaultCharset, fileEncoding);
    }

    private static void checkOutputDirWritable(String outputDir) {
        if (outputDir == null || outputDir.trim().isEmpty()) {
            return;
        }
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("输出目录无法创建: " + outputDir);
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException("输出路径不是目录: " + outputDir);
        }
        Path probe = null;
        try {
            probe = Files.createTempFile(dir.toPath(), ".envcheck_", ".tmp");
        } catch (IOException e) {
            throw new IllegalStateException("输出目录无写权限: " + outputDir + "，原因: " + e.getMessage(), e);
        } finally {
            if (probe != null) {
                try {
                    Files.deleteIfExists(probe);
                } catch (IOException ignored) {
                }
            }
        }
        logger.info("环境预检：输出目录可写 {}", outputDir);
    }
}
