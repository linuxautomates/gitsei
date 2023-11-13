package io.levelops.aggregations.config;

import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.jedis.JedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import redis.clients.jedis.JedisPool;

@Log4j2
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerConfig implements SchedulingConfigurer
{
    /**
     * The pool size.
     */
    @Value("${SPRING_TASK_SCHEDULER_MAX_THREADS:5}")
    private Integer poolSize;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar)
    {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

        log.info("Setting scheduledTaskRegistrar pool size to: " + poolSize);
        threadPoolTaskScheduler.setPoolSize(poolSize);
        threadPoolTaskScheduler.setThreadNamePrefix("scheduled-task-pool-");
        threadPoolTaskScheduler.initialize();

        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }

    @Bean
    public LockProvider lockProvider(JedisPool jedisPool) {
        return new JedisLockProvider(jedisPool, "aggregations");
    }
}