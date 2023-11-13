package io.levelops.logging;

import org.junit.Test;
import org.springframework.cloud.gcp.logging.TraceIdLoggingEnhancer;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class TraceEnabledForkJoinPoolTest {
    public static class TestTraceRunnable implements Runnable {
        private final String expectedContext;
        TestTraceRunnable(String expectedContext) {
            this.expectedContext = expectedContext;
        }
        public void run() {
            assertThat(TraceIdLoggingEnhancer.getCurrentTraceId()).isEqualTo(expectedContext);
        }
    }
    @Test
    public void testForkJoinPool() throws InterruptedException {
        TraceEnabledForkJoinPool pool = new TraceEnabledForkJoinPool(2);
        String traceId1 = "traceId1";
        TraceIdLoggingEnhancer.setCurrentTraceId(traceId1);
        pool.execute(new TestTraceRunnable(traceId1));
        pool.execute(new TestTraceRunnable(traceId1));
        pool.shutdown();
        pool.awaitTermination(2L, TimeUnit.SECONDS);
    }
}
