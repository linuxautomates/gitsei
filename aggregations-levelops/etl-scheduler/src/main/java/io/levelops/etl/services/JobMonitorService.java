package io.levelops.etl.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.commons.etl.models.JobStatus;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Looks for jobs that might be in inconsistent states and fixes them. Some of
 * these states include:
 * 1. Jobs waiting in queue for a long time. i.e jobs that are eligible to be run but haven't been
 * 2. Disabled job definitions that have active job instances
 * picked up by any worker nodes yet
 */
@Log4j2
@Service
public class JobMonitorService {
    private static final int WARMUP_DELAY_IN_SECONDS = 10;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final int schedulingIntervalInSeconds;
    private final ScheduledExecutorService jobMonitorExecutor;
    private final int maxQueuedThresholdInMinutes;
    private final MeterRegistry meterRegistry;

    @Autowired
    public JobMonitorService(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            MeterRegistry meterRegistry,
            @Value("${JOB_MONITOR_INTERVAL_IN_SECONDS:300}") Integer schedulingIntervalInSeconds,
            @Value("${MAX_QUEUED_THRESHOLD_IN_MINUTES:10}") Integer maxQueuedThresholdInMinutes) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.schedulingIntervalInSeconds = schedulingIntervalInSeconds;
        this.maxQueuedThresholdInMinutes = maxQueuedThresholdInMinutes;
        this.meterRegistry = meterRegistry;

        jobMonitorExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("job-monitor-loop-%d")
                .build());

        initScheduling();
    }

    @VisibleForTesting
    protected void monitorJobStuckInQueue(Instant now) {
        // TODO: Potentially bump up the priorities of these jobs if needed
        // For now, just logging this should be fine
        MutableInt count = new MutableInt(0);
        jobInstanceDatabaseService.stream(
                DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.SCHEDULED, JobStatus.ACCEPTED))
                        .lastHeartbeatBefore(now.minus(maxQueuedThresholdInMinutes, ChronoUnit.MINUTES))
                        .build()
        ).forEach(jobInstance -> {
                    count.increment();
                    Instant lastStatusChangedAt = jobInstance.getStatusChangedAt();
                    Duration queuedDuration = Duration.between(lastStatusChangedAt, now);
                    log.info("Job instance {} has been in scheduled/accepted state for {} minutes", jobInstance.getJobInstanceId(), queuedDuration.toMinutes());
                }
        );
        meterRegistry.gauge("scheduler.jobs.queued.stuck", count);
    }

    @VisibleForTesting
    protected void monitorDisabledJobDefinitions() {
        MutableInt count = new MutableInt(0);
        jobInstanceDatabaseService.stream(
                DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.SCHEDULED))
                        .build()
        ).forEach(jobInstance -> {
            try {
                DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobInstance.getJobDefinitionId()).get();
                if (jobDefinition.isDisabled()) {
                    count.increment();
                    log.info(
                            "Job instance {} is in scheduled state but its job definition {} is disabled. Marking job instance as canceled",
                            jobInstance.getJobInstanceId(),
                            jobDefinition.getId());
                    jobInstanceDatabaseService.update(jobInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                            .jobStatus(JobStatus.CANCELED)
                            .build());
                }
            } catch (Exception e) {
                log.error("Error while checking for disabled job definitions", e);
            }
        });
    }

    private void initScheduling() {
        if (schedulingIntervalInSeconds <= 0) {
            log.info("Scheduling interval {} is non-positive. Not starting job retry loop thread", schedulingIntervalInSeconds);
            return;
        }
        jobMonitorExecutor.scheduleAtFixedRate(
                new JobMonitorTask(), WARMUP_DELAY_IN_SECONDS, schedulingIntervalInSeconds, TimeUnit.SECONDS);
    }

    public class JobMonitorTask implements Runnable {
        @Override
        public void run() {
            try {
                Instant now = Instant.now();
                monitorJobStuckInQueue(now);
                monitorDisabledJobDefinitions();
            } catch (Throwable e) {
                log.error("Error in job retrying task", e);
            }
        }
    }
}
