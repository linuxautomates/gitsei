package io.levelops.logging;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gcp.logging.TraceIdLoggingEnhancer;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class propagates the ThreadLocal traceId from {@link TraceIdLoggingEnhancer} into the thread pool. It does this
 * by providing a custom ForkJoinTask that handles setting and unsetting the traceId.
 *
 * This allows us to associate GCP load balancer trace ids with DeferredResponses that use a ForkJoinPool.
 *
 * NOTE: spring-sleuth and spring-cloud-gcp-trace-starter provide this ability out of the box, but because of version
 * mismatches (spring-cloud-gcp-trace-starter is not compatible with spring-boot 2.6.6) we are unable to use them.
 *
 * TODO: When spring-cloud-gcp-trace-starter starts supporting newer spring-boot versions, we should deprecate this
 * class.
 */
@Log4j2
public final class TraceEnabledForkJoinPool extends ForkJoinPool
{
    public TraceEnabledForkJoinPool(int parallelism) {
        super(parallelism);
    }

    @Override
    public void execute(ForkJoinTask<?> task) {
        // See http://stackoverflow.com/a/19329668/14731
        super.execute(wrap(task, TraceIdLoggingEnhancer.getCurrentTraceId()));
    }

    @Override
    public void execute(Runnable task) {
        // See http://stackoverflow.com/a/19329668/14731
        super.execute(wrap(task, TraceIdLoggingEnhancer.getCurrentTraceId()));
    }


    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return super.submit(wrap(task, TraceIdLoggingEnhancer.getCurrentTraceId()));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return super.submit(wrap(task, TraceIdLoggingEnhancer.getCurrentTraceId()), result);
    }

    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        return super.submit(wrap(task, TraceIdLoggingEnhancer.getCurrentTraceId()));
    }

    private <T> Callable<T> wrap(Callable<T> task,  String newTraceId) {
        return () ->
        {
            String oldTraceId = beforeExecution(newTraceId);
            try
            {
                return task.call();
            }
            finally
            {
                afterExecution(oldTraceId);
            }
        };
    }

    private <T> ForkJoinTask<T> wrap(ForkJoinTask<T> task, String newContext) {
        return new ForkJoinTask<>() {
            private static final long serialVersionUID = 1L;
            /**
             * If non-null, overrides the value returned by the underlying task.
             */
            private final AtomicReference<T> override = new AtomicReference<>();

            @Override
            public T getRawResult() {
                T result = override.get();
                if (result != null) {
                    return result;
                }
                return task.getRawResult();
            }

            @Override
            protected void setRawResult(T value) {
                override.set(value);
            }

            @Override
            protected boolean exec() {
                // According to ForkJoinTask.fork() "it is a usage error to fork a task more than once unless it has completed
                // and been reinitialized". We therefore assume that this method does not have to be thread-safe.
                String oldContext = beforeExecution(newContext);
                try {
                    task.invoke();
                    return true;
                } finally {
                    afterExecution(oldContext);
                }
            }
        };
    }

    private Runnable wrap(Runnable task, String newContext) {
        return () -> {
            String oldContext = beforeExecution(newContext);
            try {
                task.run();
            }
            finally {
                afterExecution(oldContext);
            }
        };
    }

    /**
     * Invoked before running a task.
     *
     * @param newTraceId the new traceId
     * @return the old traceId
     */
    private String beforeExecution(String newTraceId) {
        String previous = TraceIdLoggingEnhancer.getCurrentTraceId();
        TraceIdLoggingEnhancer.setCurrentTraceId(newTraceId);
        return previous;
    }

    /**
     * Invoked after running a task.
     *
     * @param oldValue the old trace context
     */
    private void afterExecution(String oldValue) {
        TraceIdLoggingEnhancer.setCurrentTraceId(oldValue);
    }
}