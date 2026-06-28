package org.hyw.tools.generator.platform;

import java.util.Comparator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 平台适配的静态门面：按 ServiceLoader 发现所有候选 {@link PlatformAdapter}，
 * 选当前 JVM 支持且 priority 最高的实例返回。
 *
 * <p>调用者只需 {@code PlatformAdapters.current()} 获取最佳实现，
 * 不感知具体 Java 版本分支——这是 OSGi 思想的轻量级标准库版本。</p>
 */
public final class PlatformAdapters {

    private static final Logger logger = LoggerFactory.getLogger(PlatformAdapters.class);

    private static volatile PlatformAdapter selected;

    private PlatformAdapters() {
    }

    public static PlatformAdapter current() {
        if (selected == null) {
            synchronized (PlatformAdapters.class) {
                if (selected == null) {
                    selected = pick();
                }
            }
        }
        return selected;
    }

    /** 测试用：清除缓存以便重新发现。 */
    public static void reset() {
        selected = null;
    }

    private static PlatformAdapter pick() {
        String version = System.getProperty("java.version");
        int currentMajor;
        if (version.startsWith("1.")) {
            currentMajor = Integer.parseInt(version.substring(2, 3));
        } else {
            int dot = version.indexOf(".");
            currentMajor = Integer.parseInt(dot != -1 ? version.substring(0, dot) : version);
        }
        PlatformAdapter best = null;
        for (PlatformAdapter candidate : ServiceLoader.load(PlatformAdapter.class)) {
            best = chooseBetter(best, candidate, currentMajor);
        }
        for (PlatformAdapter candidate : loadVersionedCandidates(currentMajor)) {
            best = chooseBetter(best, candidate, currentMajor);
        }
        if (best == null) {
            best = new DefaultPlatformAdapter();
            logger.warn("未发现已注册的 PlatformAdapter，使用兜底默认实现");
        }
        logger.info("PlatformAdapter 选定：{}", best.describe());
        return best;
    }

    private static PlatformAdapter chooseBetter(PlatformAdapter best, PlatformAdapter candidate, int currentMajor) {
        if (candidate == null || !candidate.supports(currentMajor)) {
            return best;
        }
        if (best == null || comparePriority(candidate, best) > 0) {
            return candidate;
        }
        return best;
    }

    private static java.util.List<PlatformAdapter> loadVersionedCandidates(int currentMajor) {
        java.util.List<PlatformAdapter> candidates = new java.util.ArrayList<>();
        if (currentMajor >= 17) {
            addIfPresent(candidates, "org.hyw.tools.generator.platform.Java17PlatformAdapter");
        }
        return candidates;
    }

    private static void addIfPresent(java.util.List<PlatformAdapter> candidates, String className) {
        try {
            Class<?> candidateClass = Class.forName(className);
            if (PlatformAdapter.class.isAssignableFrom(candidateClass)) {
                candidates.add((PlatformAdapter) candidateClass.getDeclaredConstructor().newInstance());
            }
        } catch (ClassNotFoundException ignored) {
            // MRJAR versioned classes are only visible when running from the packaged jar on a matching JVM.
        } catch (ReflectiveOperationException | LinkageError e) {
            logger.warn("加载版本化 PlatformAdapter 失败: {}", className, e);
        }
    }

    private static int comparePriority(PlatformAdapter a, PlatformAdapter b) {
        return Comparator.comparingInt(PlatformAdapter::priority)
                .thenComparingInt(PlatformAdapter::minSupportedMajor)
                .compare(a, b);
    }
}
