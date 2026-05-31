package org.hyw.tools.generator.platform;

import java.util.List;
import java.util.stream.Stream;

/**
 * 运行时平台适配策略：按当前 JVM 版本切换不同实现。
 *
 * <p>设计意图：</p>
 * <ul>
 *   <li>主线代码用 Java 11 稳定 API 即可（{@link DefaultPlatformAdapter}）。</li>
 *   <li>Java 17/21 上想启用更优实现（如 virtual threads、records、Stream.toList）时，
 *       新增子类并通过 {@code META-INF/services/org.hyw.tools.generator.platform.PlatformAdapter}
 *       注册即可——配合 MRJAR 可把高版本字节码隔离到 {@code META-INF/versions/N/}。</li>
 *   <li>{@link PlatformAdapters#current()} 在多个候选中按 {@link #priority()} + {@link #supports(int)} 选最优。</li>
 * </ul>
 */
public interface PlatformAdapter {

    /** 当前 JVM 主版本号（如 11 / 17 / 21）。 */
    int runtimeMajor();

    /** 实现可用的最低 JVM 主版本号。 */
    int minSupportedMajor();

    /**
     * 候选实现的选择优先级，数值大者优先。
     * 同一 JVM 上多个实现 supports() 返回 true 时，priority 高的胜出。
     */
    default int priority() {
        return minSupportedMajor();
    }

    /**
     * 当前 JVM 是否能加载/运行该实现。
     * 默认按 {@link #minSupportedMajor()} 比较，子类可加额外能力检测。
     */
    default boolean supports(int jvmMajor) {
        return jvmMajor >= minSupportedMajor();
    }

    /** 一句话描述，用于诊断日志。 */
    String describe();

    /**
     * 将 Stream 收集为 List。
     * <p>Java 11 基线实现使用 Collectors.toList()；MRJAR 高版本实现可替换为 Stream.toList()。</p>
     */
    <T> List<T> toList(Stream<T> stream);
}
