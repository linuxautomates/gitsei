package io.levelops.api.config;

import io.levelops.logging.LoggingTaskDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadingConfig {
    @Bean(name = "slackInteractivityEventTaskExecutor")
    public Executor slackInteractivityEventTaskExecutor(
            @Value("${SLACK_INTERACTIVITY_EVENT_MAX_THREADS:5}") final Integer maxPoolSize, final
    @Value("${SLACK_INTERACTIVITY_EVENT_QUEUE_SIZE:50}") Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("slack-interactivity-event-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "velocityTaskExecutor")
    public Executor velocityTaskExecutor( @Value("${VELOCITY_MAX_THREADS:10}") final Integer maxPoolSize,
                                          final @Value("${VELOCITY_QUEUE_SIZE:50}") Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("velocity-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "dbValuesTaskExecutor")
    public Executor dbValuesTaskExecutor( @Value("${DB_VALUES_MAX_THREADS:20}") final Integer maxPoolSize,
                                          final @Value("${DB_VALUES_QUEUE_SIZE:50}") Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("db-values-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "devProductivityTaskExecutor")
    public Executor devProductivityTaskExecutor( @Value("${DEV_PRODUCTIVITY_MAX_THREADS:30}") final Integer maxPoolSize,
                                          final @Value("${DEV_PRODUCTIVITY_QUEUE_SIZE:100}") Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("dev-productivity-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "userDevProductivityReportTaskExecutor")
    public Executor userDevProductivityReportTaskExecutor( @Value("${USER_DEV_PRODUCTIVITY_REPORT_MAX_THREADS:30}") final Integer maxPoolSize,
                                                 final @Value("${USER_DEV_PRODUCTIVITY_REPORT_QUEUE_SIZE:100}") Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("user-dev-productivity-report-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "orgDevProductivityReportTaskExecutor")
    public Executor orgDevProductivityReportTaskExecutor( @Value("${ORG_DEV_PRODUCTIVITY_REPORT_MAX_THREADS:30}") final Integer maxPoolSize,
                                                           final @Value("${ORG_DEV_PRODUCTIVITY_REPORT_QUEUE_SIZE:100}") Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("org-dev-productivity-report-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();
        return executor;
    }
}
