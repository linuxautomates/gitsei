package io.levelops.internal_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class ThreadingConfig extends AsyncConfigurerSupport {
    @Bean(name = "jenkinsPluginResultsPreProcessTaskExecutor")
    public Executor jenkinsMonitoringPluginTaskExecutor(
            @Value("${JENKINS_PRE_PROCESS_MAX_THREADS:10}") final Integer maxPoolSize,
            @Value("${JENKINS_PRE_PROCESS_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("jenkins-pre-process");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jenkinsPluginJobRunCompleteTaskExecutor")
    public Executor jenkinsPluginJobRunCompleteTaskExecutor(
            @Value("${JENKINS_JOB_RUN_COMPLETE_MAX_THREADS:10}") final Integer maxPoolSize, 
            @Value("${JENKINS_JOB_RUN_COMPLETE_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("jenkins-job-run-complete");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jenkinsPluginJobConfigChangeTaskExecutor")
    public Executor jenkinsPluginJobConfigChangeTaskExecutor(
            @Value("${JENKINS_JOB_CONFIG_CHANGE_MAX_THREADS:10}") final Integer maxPoolSize,
            @Value("${JENKINS_JOB_CONFIG_CHANGE_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("jenkins-job-config-change");
        executor.initialize();
        return executor;
    }

    @Bean(name = "asyncLowerPriorityTaskExecutor")
    public Executor asyncLowerPriorityTaskExecutor(
            @Value("${ASYNC_LOWER_PRIORITY_MAX_THREADS:5}") final Integer maxPoolSize, 
            @Value("${ASYNC_LOWER_PRIORITY_QUEUE_SIZE:50}") final Integer queueSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("async-lower-priority-");
        executor.initialize();
        return executor;
    }
}