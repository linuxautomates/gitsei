package io.levelops.logging;

import org.springframework.cloud.gcp.logging.TraceIdLoggingEnhancer;
import org.springframework.core.task.TaskDecorator;

public class LoggingTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        String traceId = TraceIdLoggingEnhancer.getCurrentTraceId();
        return () -> {
            try {
                TraceIdLoggingEnhancer.setCurrentTraceId(traceId);
                task.run();
            } finally {
                TraceIdLoggingEnhancer.setCurrentTraceId(null);
            }
        };
    }
}

