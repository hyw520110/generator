package org.hyw.tools.generator;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.hyw.tools.generator.platform.PlatformAdapters;

/**
 * 生成结果累加器：聚合每张表/每个模板的成功与失败信息，
 * 在 {@code execute()} 末尾输出统一摘要，便于用户在百表场景下快速定位问题。
 *
 * <p>所有方法线程安全，与并行渲染兼容。</p>
 */
public class GenerationReport {

    public static class Failure {
        public final String tableName;
        public final String templatePath;
        public final String reason;

        public Failure(String tableName, String templatePath, String reason) {
            this.tableName = tableName == null ? "-" : tableName;
            this.templatePath = templatePath == null ? "-" : templatePath;
            this.reason = reason == null ? "" : reason;
        }

        public String tableName() {
            return tableName;
        }

        public String templatePath() {
            return templatePath;
        }

        public String reason() {
            return reason;
        }
    }

    private final AtomicInteger successFiles = new AtomicInteger();
    private final AtomicInteger skippedFiles = new AtomicInteger();
    private final AtomicInteger processedTables = new AtomicInteger();
    private final ConcurrentLinkedQueue<Failure> failures = new ConcurrentLinkedQueue<>();

    public void recordSuccess() {
        successFiles.incrementAndGet();
    }

    public void recordSkipped() {
        skippedFiles.incrementAndGet();
    }

    public int incrementTableCounter() {
        return processedTables.incrementAndGet();
    }

    public void recordFailure(String tableName, String templatePath, String reason) {
        failures.add(new Failure(tableName, templatePath, reason));
    }

    public int successFiles() {
        return successFiles.get();
    }

    public int skippedFiles() {
        return skippedFiles.get();
    }

    public int processedTables() {
        return processedTables.get();
    }

    public List<Failure> failures() {
        return PlatformAdapters.current().toList(failures.stream());
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}
