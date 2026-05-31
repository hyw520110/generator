package org.hyw.tools.generator.platform;

import java.util.List;
import java.util.stream.Stream;

/**
 * Java 17+ 平台适配实现。
 *
 * <p>本类仅在 Java 17+ JVM 上可用，编译/部署到 {@code META-INF/versions/17/} 路径下。
 * 当前实现承载 Java 17 专属 API，例如 {@link Stream#toList()}。</p>
 *
 * <p>通过 {@code META-INF/services/...PlatformAdapter} 注册，
 * {@link PlatformAdapters#current()} 在 Java 17+ 上会优先选中本实现（priority=17）。</p>
 */
public class Java17PlatformAdapter implements PlatformAdapter {

    @Override
    public int runtimeMajor() {
        return Runtime.version().feature();
    }

    @Override
    public int minSupportedMajor() {
        return 17;
    }

    @Override
    public String describe() {
        return "Java 17+ adapter with Stream.toList() (runtime=" + runtimeMajor() + ")";
    }

    @Override
    public <T> List<T> toList(Stream<T> stream) {
        return stream.toList();
    }
}
