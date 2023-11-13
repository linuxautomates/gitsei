package io.levelops.etl.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.utils.MetricUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.levelops.etl.utils.SchedulingUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.services.ControlPlaneService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically figures out which jobs are to be run next and stores it in a
 * local variable. This is then used to power the scheduling controllers
 * which talk to the worker nodes.
 * <p>
 * We do this asynchronously to decrease db load and ensure scalability when
 * there are a lot of worker nodes querying the controller
 */
@Log4j2
@Service
public class SchedulerService {
    private static final int JOB_SCHEDULER_EXECUTOR_THREADS = 10;

    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final ControlPlaneService controlPlaneService;
    private final SchedulingUtils schedulingUtils;
    private final ObjectMapper objectMapper;
    private final JobDefinitionParameterSupplierRegistry paramSupplierRegistry;
    private final int schedulingIntervalInSec;
    private final int warmupDelaySecs;
    // This runs the scheduling loop that runs periodically and scans all job definitions
    private final ScheduledExecutorService schedulingExecutor;
    // This runs the scheduling logic for an individual job definition. Since this can be time consuming
    // we run it in parallel
    private final ExecutorService scheduleJobExecutorService;
    private final MeterRegistry meterRegistry;
    private final Counter scheduledInstanceCounter;
    private final LongTaskTimer schedulingLoopTimer;
    private final String METRICS_PREFIX = "etl.scheduler";

    public SchedulerService(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            ControlPlaneService controlPlaneService,
            SchedulingUtils schedulingUtils,
            ObjectMapper objectMapper,
            JobDefinitionParameterSupplierRegistry paramSupplierRegistry,
            MeterRegistry meterRegistry,
            @Value("${SCHEDULER_INTERVAL_SECONDS:60}") Integer schedulingIntervalInSec,
            @Value("${SCHEDULER_WARMUP_DELAY_SECONDS:10}") Integer warmupDelaySecs) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.controlPlaneService = controlPlaneService;
        this.schedulingUtils = schedulingUtils;
        this.objectMapper = objectMapper;
        this.paramSupplierRegistry = paramSupplierRegistry;
        this.schedulingIntervalInSec = schedulingIntervalInSec;
        this.warmupDelaySecs = warmupDelaySecs;
        this.meterRegistry = meterRegistry;
        scheduledInstanceCounter = meterRegistry.counter(METRICS_PREFIX + ".scheduled.count");
        schedulingLoopTimer = LongTaskTimer.builder(METRICS_PREFIX + ".scheduler_loop.timer").register(meterRegistry);

        schedulingExecutor = initScheduling();
        scheduleJobExecutorService = initScheduleJobExecutor(JOB_SCHEDULER_EXECUTOR_THREADS);
    }

    private ScheduledExecutorService initScheduling() {
        if (schedulingIntervalInSec <= 0) {
            log.info("Scheduling interval {} is non-positive. Not starting scheduler thread", schedulingIntervalInSec);
            return null;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("scheduler-loop-%d")
                .build());
        executor.scheduleAtFixedRate(
                new SchedulerRunnable(), warmupDelaySecs, schedulingIntervalInSec, TimeUnit.SECONDS);
        return executor;
    }

    private ExecutorService initScheduleJobExecutor(int nbThreads) {
        return Executors.newFixedThreadPool(nbThreads, new ThreadFactoryBuilder()
                .setNameFormat("schedule-job-%d")
                .build());
    }

    @VisibleForTesting
    public synchronized void scheduleNextJobs(Instant now) throws InterruptedException {
        MutableLong total = new MutableLong(0);
        MutableLong schedulableCount = new MutableLong(0);
        MutableLong backPressureCount = new MutableLong(0);
        List<Future<JobInstanceId>> futures = jobDefinitionDatabaseService.stream(
                        DbJobDefinitionFilter.builder().isActive(true).build())
                .peek(jobDefinition -> {
                    total.increment();
                })
                .filter(jobDefinition -> {
                    boolean schedulable = jobDefinition.isSchedulable(now);
                    log.debug("jobDefinitionId={}, schedulable={}", jobDefinition.getId(), schedulable);
                    if (schedulable) {
                        schedulableCount.increment();
                    }
                    return schedulable;
                })
                .filter(jobDefinition -> {
                    try {
                        if (isJobDefinitionBackPressured(jobDefinition)) {
                            log.debug("Job Definition has backpressure, not scheduling more instances: {} {} {}",
                                    jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId());
                            backPressureCount.increment();
                            return false;
                        } else {
                            return true;
                        }
                    } catch (Throwable e) {
                        log.error("Failed to check backpressure for job definition: {}, tenant: {}, integration id: {}", jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId(), e);
                        return false;
                    }
                })
                .map(jobDefinition -> {
                    try {
                        return scheduleJobDefinitionInThreadpool(jobDefinition, now);
                    } catch (Throwable e) {
                        log.error("Failed to schedule job definition: {}, tenant: {}, integration id: {}", jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();

        List<JobInstanceId> scheduledInstanceIds =
                futures.stream()
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (InterruptedException |
                                     ExecutionException e) {
                                log.error("Exception when waiting for scheduling future", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull).toList();
        if (scheduledInstanceIds.size() > 0) {
            // TODO rate limit this log potentially
            log.info("Scheduled {} job definition(s) [schedulable={}, back-pressured={}, total={}]",
                    scheduledInstanceIds.size(), schedulableCount, backPressureCount, total);
        }
    }

    private Optional<DbJobInstance> getLastFullJobInstance(DbJobDefinition jobDefinition) {
        return Optional.ofNullable(
                Iterables.get(
                        jobInstanceDatabaseService.filter(0, 1, DbJobInstanceFilter.builder()
                                .jobDefinitionIds(List.of(jobDefinition.getId()))
                                .isFull(true)
                                .build()).getRecords(), 0, null));
    }

    private Future<JobInstanceId> scheduleJobDefinitionInThreadpool(DbJobDefinition jobDefinition, Instant now) {
        return scheduleJobExecutorService.submit(() -> {
            try {
                JobInstanceId instanceId = scheduleJobDefinition(jobDefinition, now);
                MetricUtils.getTenantCounter(
                                meterRegistry,
                                METRICS_PREFIX + ".scheduled.count",
                                jobDefinition.getTenantId(),
                                jobDefinition.getIntegrationId(),
                                jobDefinition.getIntegrationType(),
                                jobDefinition.getAggProcessorName())
                        .increment();
                return instanceId;
            } catch (IngestionServiceException | IOException e) {
                log.error("Failed to schedule for job definition " + jobDefinition.getId(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private JobInstanceId scheduleJobDefinition(DbJobDefinition jobDefinition, Instant now) throws IngestionServiceException, IOException {
        return schedulingUtils.scheduleJobDefinition(jobDefinition, now, true, null, false);
    }

    /**
     * The backpressure threshold is always 1 here, this is because we don't expect
     * to run jobs in parallel for aggs at the moment.
     */
    private boolean isJobDefinitionBackPressured(DbJobDefinition jobDefinition) {
        var currentlyRunningCount = jobInstanceDatabaseService.filter(0, 1, DbJobInstanceFilter.builder()
                        .jobStatuses(List.of(JobStatus.SCHEDULED, JobStatus.ACCEPTED, JobStatus.PENDING)) // Not including unassigned jobs here may have to revisit
                        .jobDefinitionIds(List.of(jobDefinition.getId()))
                        .build())
                .getCount();
        if (currentlyRunningCount > 0) {
            log.debug("Backpressured because running/scheduled jobs exist in the system definition id: {}, tenant: {}, integration id: {}",
                    jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId());
            return true;
        }

        var retryableCount = jobInstanceDatabaseService.filter(0, 1, DbJobInstanceFilter.builder()
                        .jobDefinitionIds(List.of(jobDefinition.getId()))
                        .jobStatuses(List.of(JobStatus.FAILURE))
                        .belowMaxAttempts(true)
                        .build())
                .getCount();
        if (retryableCount > 0) {
            log.info("Backpressured because retryable jobs exist in the system definition id: {}, tenant: {}, integration id: {}",
                    jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId());
            return true;
        }
        return false;
    }

    public class SchedulerRunnable implements Runnable {
        private long runCount;

        public SchedulerRunnable() {
            runCount = 0;
        }

        @Override
        public void run() {
            var sample = schedulingLoopTimer.start();
            runCount++;
            try {
                Instant now = Instant.now();
                scheduleNextJobs(now);
            } catch (Throwable e) {
                log.error("Error occurred in scheduler loop", e);
            }
            long elapsedNanoSeconds = sample.stop();
            double elapsedSeconds = (double)elapsedNanoSeconds/ 1_000_000_000.0;
            if (elapsedSeconds > 1) {
                log.info("Scheduler loop took {} seconds to run. Run count: {}", elapsedSeconds, runCount);
            }
        }
    }
}
