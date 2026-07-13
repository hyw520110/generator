package org.hyw.tools.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Test;

public class GenerationReportTest {

    @Test
    public void counters_areAccurate() {
        GenerationReport r = new GenerationReport();
        r.recordSuccess();
        r.recordSuccess();
        r.recordSkipped();
        r.recordFailure("users", "User.java.ftl", "missing var");
        assertEquals(2, r.successFiles());
        assertEquals(1, r.skippedFiles());
        assertEquals(1, r.failures().size());
        assertTrue(r.hasFailures());
    }

    @Test
    public void incrementTableCounter_isMonotonic() {
        GenerationReport r = new GenerationReport();
        for (int i = 1; i <= 5; i++) {
            assertEquals(i, r.incrementTableCounter());
        }
        assertEquals(5, r.processedTables());
    }

    @Test
    public void counters_areThreadSafe() throws InterruptedException {
        GenerationReport r = new GenerationReport();
        int n = 1000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        IntStream.range(0, n).forEach(i -> new Thread(() -> {
            try {
                start.await();
                r.recordSuccess();
                r.incrementTableCounter();
                if (i % 7 == 0) {
                    r.recordFailure("t" + i, "p" + i, "boom");
                }
            } catch (InterruptedException ignored) {
            } finally {
                done.countDown();
            }
        }).start());
        start.countDown();
        assertTrue("threads did not finish in time", done.await(30, TimeUnit.SECONDS));
        assertEquals(n, r.successFiles());
        assertEquals(n, r.processedTables());
        // 每 7 个失败一次：n=1000 → 143 次（i=0,7,14,...,994）
        long expectedFailures = 0;
        for (int i = 0; i < n; i++) {
            if (i % 7 == 0) expectedFailures++;
        }
        assertEquals(expectedFailures, r.failures().size());
    }

    @Test
    public void empty_report_hasNoFailures() {
        GenerationReport r = new GenerationReport();
        assertFalse(r.hasFailures());
        assertEquals(0, r.successFiles());
    }
}
