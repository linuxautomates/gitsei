package io.levelops.logging;

import org.junit.Test;
import org.springframework.cloud.gcp.logging.TraceIdLoggingEnhancer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutionException;

public class LoggingTaskDecoratorTest {
    @Test
    public void testLoggingTaskDecorator() throws ExecutionException, InterruptedException {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setThreadNamePrefix("slack-interactivity-event-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();

        String traceId1 = "traceId1";

        TraceIdLoggingEnhancer.setCurrentTraceId(traceId1);
        var f = executor.submit(new TraceEnabledForkJoinPoolTest.TestTraceRunnable(traceId1));
        f.get();
    }
}
