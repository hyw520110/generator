package org.hyw.tools.generator.platform;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认平台适配实现：Java 11 基线，使用稳定 API。
 *
 * <p>放在主源码目录（{@code src/main/java}），编译目标为 Java 11——
 * 任何 JVM &gt;= 11 都能加载，作为兜底实现。</p>
 *
 * <p>更高版本的优化（virtual threads、Stream.toList、records 等）应放在
 * MRJAR 的 {@code META-INF/versions/17/} 或 {@code 21/} 中，通过同名类覆盖
 * 或新增 {@code Java17PlatformAdapter} / {@code Java21PlatformAdapter} 实现并提高 priority。</p>
 */
public class DefaultPlatformAdapter implements PlatformAdapter {

    @Override
    public int runtimeMajor() {
        return Runtime.version().feature();
    }

    @Override
    public int minSupportedMajor() {
        return 11;
    }

    @Override
    public String describe() {
        return "Default Java 11 adapter (runtime=" + runtimeMajor() + ")";
    }

    @Override
    public <T> List<T> toList(Stream<T> stream) {
        return stream.collect(Collectors.toList());
    }
}
