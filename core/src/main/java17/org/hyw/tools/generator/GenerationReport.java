package org.hyw.tools.generator;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 17+ MRJAR implementation of the generation report.
 *
 * <p>The public API matches the Java 11 baseline class, while failure entries use a record
 * to reduce boilerplate on newer runtimes.</p>
 */
public class GenerationReport {

    public record Failure(String tableName, String templatePath, String reason) {
        public Failure {
            tableName = tableName == null ? "-" : tableName;
            templatePath = templatePath == null ? "-" : templatePath;
            reason = reason == null ? "" : reason;
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
        return failures.stream().toList();
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}
