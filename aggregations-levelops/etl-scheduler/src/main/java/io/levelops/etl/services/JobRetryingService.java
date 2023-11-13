package io.levelops.etl.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.utils.MetricUtils;
import io.levelops.commons.etl.models.JobStatus;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class JobRetryingService {
    private static final int WARMUP_DELAY_IN_SECONDS = 10;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final int schedulingIntervalInSeconds;
    private final int heartbeatTimeoutInMinutes;
    private final ScheduledExecutorService jobRetryingExecutor;
    private final MeterRegistry meterRegistry;

    @Autowired
    public JobRetryingService(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            MeterRegistry meterRegistry,
            @Value("${HEARTBEAT_TIMEOUT_IN_MINUTES:20}") Integer heartbeatTimeoutInMinutes,
            @Value("${RETRY_LOOP_INTERVAL_IN_SECONDS:600}") Integer schedulingIntervalInSeconds) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.schedulingIntervalInSeconds = schedulingIntervalInSeconds;
        this.heartbeatTimeoutInMinutes = heartbeatTimeoutInMinutes;
        this.meterRegistry = meterRegistry;

        jobRetryingExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("job-retry-loop-%d")
                .build());

        initScheduling();
    }

    private void initScheduling() {
        if (schedulingIntervalInSeconds <= 0) {
            log.info("Scheduling interval {} is non-positive. Not starting job retry loop thread", schedulingIntervalInSeconds);
        }
        jobRetryingExecutor.scheduleAtFixedRate(
                new JobRetryingTask(), WARMUP_DELAY_IN_SECONDS, schedulingIntervalInSeconds, TimeUnit.SECONDS);
    }

    private void retryFailedJobs(Instant now) {
        MutableLong retryCount = new MutableLong(0);
        MutableLong retryEliglibleCount = new MutableLong(0);
        jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.FAILURE))
                        .belowMaxAttempts(true)
                        .build())
                .peek(jobInstance -> retryEliglibleCount.increment())
                .filter(jobInstance -> {
                    // Ensure that we are waiting for an appropriate amount of time before retrying
                    try {
                        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobInstance.getJobDefinitionId()).orElseThrow();
                        var statusChangedAt = jobInstance.getStatusChangedAt();
                        Duration elapsedTime = Duration.between(statusChangedAt, now);
                        log.info("Elapsed retry time for failed job {}: {}. Wait time threshold: {} minutes",
                                jobInstance.getJobInstanceId(), elapsedTime, jobDefinition.getRetryWaitTimeInMinutes());
                        return elapsedTime.toMinutes() > jobDefinition.getRetryWaitTimeInMinutes();
                    } catch (Exception e) {
                        log.error("Error in filtering retryable job: " + jobInstance.getJobInstanceId().toString(), e);
                        return false;
                    }
                })
                .forEach(jobInstance -> {
                    DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobInstance.getJobDefinitionId()).orElseThrow();
                    MetricUtils.getTenantCounter(
                                    meterRegistry,
                                    "etl.scheduler.jobs.retried.count",
                                    jobDefinition.getTenantId(),
                                    jobDefinition.getIntegrationId(),
                                    jobDefinition.getIntegrationType(),
                                    jobDefinition.getAggProcessorName())
                            .increment();
                    retryCount.increment();
                    try {
                        jobInstanceDatabaseService.update(jobInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                                .jobStatus(JobStatus.SCHEDULED)
                                .workerId("")
                                .statusCondition(jobInstance.getStatus())
                                .build());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        if (retryEliglibleCount.getValue() > 0) {
            log.info("Found {} failed jobs eligible for retrying, and {} jobs that have crossed the retry wait time threshold",
                    retryEliglibleCount, retryCount);
        }
    }

    private void retryAbandonedHeartbeatJobs(Instant now) {
        MutableInt count = new MutableInt(0);
        jobInstanceDatabaseService.stream(
                DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.ACCEPTED, JobStatus.PENDING))
                        .lastHeartbeatBefore(now.minus(heartbeatTimeoutInMinutes, ChronoUnit.MINUTES))
                        .build()
        ).forEach(jobInstance -> {
                    count.increment();
                    // TODO: need to ensure that this job is not running on a worker node even with a stale heartbeat.
                    //  The job framework should also bail out on a job if it's seeing that the worker has changed.
                    log.info("Rescheduling job instance with stale heartbeat {}, last heartbeat received at {}, now: {} - time out threshold: {} minutes",
                            jobInstance.getJobInstanceId().toString(), jobInstance.getLastHeartbeat(), now, heartbeatTimeoutInMinutes);
                    try {
                        jobInstanceDatabaseService.update(jobInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                                .jobStatus(JobStatus.SCHEDULED)
                                .workerId("")
                                .statusCondition(jobInstance.getStatus())
                                .workerIdCondition(jobInstance.getWorkerId())
                                .build());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        log.debug("Found {} jobs instances with stale heartbeats", count);
    }

    // Currently we're not retrying jobs here so the function name is a misnomer
    // We need to change this to automatically retry timed out jobs but that requires
    // the ability to cancel jobs
    // Currently this function only retries jobs if
    private void retryTimedoutJobs(Instant now) {
        jobInstanceDatabaseService.stream(
                DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.ACCEPTED, JobStatus.PENDING))
                        .timedOut(true)
                        .build()
        ).forEach(jobInstance -> {
            Instant lastHeartbeat = jobInstance.getLastHeartbeat();
            Duration durationSinceLastHeartbeat = Duration.ZERO;
            if (Objects.nonNull(lastHeartbeat)) {
                durationSinceLastHeartbeat = Duration.between(lastHeartbeat, now);
            }

            // If the job is in accepted state or if the lastheartbeat is null,
            // then we can safely reschedule the job because it means
            // that it either never really started working on the worker
            // Or it died before the heartbeat could be updated on the workers
            if (jobInstance.getStatus() == JobStatus.ACCEPTED || lastHeartbeat == null) {
                try {
                    log.info("Retrying timedout job instance {} in '{}' state. Last status updated at: {}, Last heartbeat: {}",
                            jobInstance.getJobInstanceId(), jobInstance.getStatus(), jobInstance.getStatusChangedAt(), lastHeartbeat);
                    jobInstanceDatabaseService.update(jobInstance.getJobInstanceId(), DbJobInstanceUpdate.builder()
                            .jobStatus(JobStatus.SCHEDULED)
                            .workerId("")
                            .statusCondition(jobInstance.getStatus())
                            .workerIdCondition(jobInstance.getWorkerId())
                            .build());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else if (durationSinceLastHeartbeat.toMinutes() < heartbeatTimeoutInMinutes) {
                // Job is in pending state, but we do have a recent heart beat. In this case we just log and move on for now
                // TODO: Revisit this - because we may potentially want to cancel this job on the worker and restart it
                log.info("Job instance {} has exceeded it's timeout of {} minutes but still found recent heartbeat {}. Continuing to monitor..",
                        jobInstance.getJobInstanceId(), jobInstance.getTimeoutInMinutes(), lastHeartbeat);
            }
        });
    }

    public class JobRetryingTask implements Runnable {
        @Override
        public void run() {
            try {
                Instant now = Instant.now();
                retryFailedJobs(now);
                retryAbandonedHeartbeatJobs(now);
                retryTimedoutJobs(now);
            } catch (Throwable e) {
                log.error("Error in job retrying task", e);
            }
        }
    }
}
