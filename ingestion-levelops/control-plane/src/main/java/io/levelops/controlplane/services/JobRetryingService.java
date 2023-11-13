package io.levelops.controlplane.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.controlplane.database.JobDatabaseService;
import io.levelops.controlplane.database.JobDatabaseService.JobFilter;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.models.DbJob;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Log4j2
@Service
public class JobRetryingService {

    private static final int WARMUP_DELAY_SECS = 180; // give time for agents to re-register
    private static final int PAGE_SIZE = 50;
    private final ScheduledExecutorService scheduler;
    private final AgentRegistryService agentRegistryService;
    private final boolean enableJobTimeout;
    private final Long jobTimeoutMin;
    private final boolean enableAssignedJobTimeout;
    private final Long assignedJobTimeoutMin;
    private final JobTrackingService jobTrackingService;
    private final JobDatabaseService jobDatabaseService;
    private final ExpirableLockRegistry lockRegistry;

    @Autowired
    public JobRetryingService(@Value("${RETRYING_SCHEDULING_INTERVAL:60}") Long retryingSchedulingIntervalSec,
                              @Value("${ENABLE_JOB_TIMEOUT:true}") boolean enableJobTimeout,
                              @Value("${JOB_TIMEOUT_MIN:10}") Long jobTimeoutMin,
                              @Value("${ENABLE_ASSIGNED_JOB_TIMEOUT:false}") boolean enableAssignedJobTimeout,
                              @Value("${ASSIGNED_JOB_TIMEOUT_MIN:1000000}") Long assignedJobTimeoutMin,
                              JobTrackingService jobTrackingService,
                              JobDatabaseService jobDatabaseService,
                              AgentRegistryService agentRegistryService,
                              final ExpirableLockRegistry lockRegistry) {
        this.enableJobTimeout = enableJobTimeout;
        this.jobTimeoutMin = jobTimeoutMin;
        this.enableAssignedJobTimeout = enableAssignedJobTimeout;
        this.assignedJobTimeoutMin = assignedJobTimeoutMin;
        this.jobTrackingService = jobTrackingService;
        this.jobDatabaseService = jobDatabaseService;
        this.agentRegistryService = agentRegistryService;
        this.lockRegistry = lockRegistry;
        scheduler = initScheduling(this, lockRegistry, retryingSchedulingIntervalSec);
        if (scheduler != null) {
            log.info("Job timeout is {} (job_timeout={}min, assigned_job_timeout={}min)",
                    enableJobTimeout ? "ENABLED" : "DISABLED", jobTimeoutMin, assignedJobTimeoutMin);
        }
    }

    private static ScheduledExecutorService initScheduling(JobRetryingService jobRetryingService, final ExpirableLockRegistry lockRegistry, long schedulingIntervalInSec) {
        if (schedulingIntervalInSec <= 0) {
            log.info("Job retrying is DISABLED");
            return null;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("retrying-%d")
                .build());
        executor.scheduleAtFixedRate(new JobRetryingService.JobRetryingTask(jobRetryingService, lockRegistry), WARMUP_DELAY_SECS, schedulingIntervalInSec, TimeUnit.SECONDS);
        log.info("Job retrying is ENABLED (interval={}sec)", schedulingIntervalInSec);
        return executor;
    }

    public static class JobRetryingTask implements Runnable {
        private static final Object RETRY_LOCK_KEY = "job_retrying_service_lock";
        private final JobRetryingService jobRetryingService;
        private final ExpirableLockRegistry lockRegistry;

        public JobRetryingTask(JobRetryingService jobRetryingService, final ExpirableLockRegistry lockRegistry) {
            this.jobRetryingService = jobRetryingService;
            this.lockRegistry = lockRegistry;
        }

        @Override
        public void run() {
            Lock lock = null;
            try{
                // Get concurrent lock
                lock = lockRegistry.obtain(RETRY_LOCK_KEY);
                if(!lock.tryLock()){
                    log.info("Lock not acquired, skipping execution...");
                    lock = null;
                    return;
                }
                jobRetryingService.retryFailedJobs();
                if (jobRetryingService.enableJobTimeout) {
                    jobRetryingService.retryTimedOutJobs();
                }
            } catch (Throwable e) {
                log.warn("Failed to run Job Retrying task", e);
            }
            finally{
                if (lock != null) {
                    try{
                        lock.unlock();
                    }
                    catch(IllegalStateException e1){
                        log.error("Trying to release the lock...", e1);
                    }
                }
            }
        }
    }

    public void retryFailedJobs() {
        long count = 0;
        for (JobRetryingStrategy strategy : JobRetryingStrategy.values()) {
            count += retryFailedJobsByStrategy(strategy);
        }
        if (count > 0) {
            log.info("Processed {} jobs for retry", count);
        }
    }

    public long retryFailedJobsByStrategy(JobRetryingStrategy strategy) {
        JobFilter.JobFilterBuilder jobFilter = JobFilter.builder()
                .statuses(List.of(JobStatus.FAILURE))
                .belowMaxAttemptsOrDefaultValue(strategy.getAttemptMax());
        if (StringUtils.isNotEmpty(strategy.getControllerName())) {
            jobFilter.controllerNames(List.of(strategy.getControllerName()));
        } else {
            // default strategy
            jobFilter.excludeControllerNames(JobRetryingStrategy.getAllControllerNames());
        }
        return jobDatabaseService.streamJobs(PAGE_SIZE, jobFilter.build())
                .filter(JobRetryingService::isJobRetryable)
                .peek(job -> log.info("⟲ Retrying job: ctrl='{}' (strategy={}), attempt={}/{}, status='{}' (at {}), id={}", job.getControllerName(), strategy.toString(),
                        ObjectUtils.defaultIfNull(job.getAttemptCount(), 0), getAttemptMax(job, strategy.getAttemptMax()), job.getStatus(), job.getStatusChangedAt(), job.getId()))
                .peek(jobTrackingService::scheduleJob)
                .count();
    }

    public static boolean isJobRetryable(@Nullable DbJob job) {
        if (job == null || job.getStatus() != JobStatus.FAILURE) {
            return false;
        }

        JobRetryingStrategy strategy = JobRetryingStrategy.forJob(job);

        int attemptCount = getAttemptCount(job);
        int attemptMax = getAttemptMax(job, strategy.getAttemptMax());
        if (hasReachedMaxAttempts(attemptCount, attemptMax)) {
            return false;
        }
        Instant statusChangedAt = DateUtils.fromEpochSecond(job.getStatusChangedAt());
        if (statusChangedAt == null) {
            return true;
        }

        Integer waitInSeconds = strategy.getWaitStrategy().apply(attemptCount);
        return statusChangedAt.isBefore(Instant.now()
                .minus(waitInSeconds, ChronoUnit.SECONDS));
    }

    public void retryTimedOutJobs() {
        Instant timeoutCutoff = Instant.now().minus(jobTimeoutMin, ChronoUnit.MINUTES);
        MutableInt rescheduled = new MutableInt(0);
        MutableInt failed = new MutableInt(0);
        JobFilter jobFilter = JobFilter.builder()
                .statuses(List.of(JobStatus.ACCEPTED, JobStatus.PENDING))
                .before(timeoutCutoff)
                .build();
        jobDatabaseService.streamJobs(PAGE_SIZE, jobFilter)
                .forEach(job -> {
                    // check if max attempts have been reached
                    JobRetryingStrategy strategy = JobRetryingStrategy.forJob(job);
                    int attemptCount = getAttemptCount(job);
                    int attemptMax = getAttemptMax(job, strategy.getAttemptMax());
                    if (hasReachedMaxAttempts(attemptCount, attemptMax)) {
                        log.info("✘ Timed-out job has reached max attempts: status={}, attempts={}/{}, job_id={}",
                                job.getStatus(), attemptCount, attemptMax, job.getId());
                        jobTrackingService.markJobAsFailed(job.getId());
                        failed.increment();
                        return;
                    }

                    // check if job is orphaned, or if it timed out and assigned job timeout is enabled
                    boolean orphaned = agentRegistryService.getAgentById(job.getAgentId()).isEmpty();
                    long elapsedMin = Duration.between(
                            DateUtils.fromEpochSecond(job.getStatusChangedAt(), DateUtils.fromEpochSecond(job.getCreatedAt())),
                            Instant.now()
                    ).toMinutes();
                    boolean retry = orphaned || (enableAssignedJobTimeout && elapsedMin > assignedJobTimeoutMin);
                    if (!retry) {
                        return;
                    }

                    log.info("⧗ Rescheduled timed-out job: status={}, elapsed={}min, orphaned={}, attempt={}, job_id={}",
                            job.getStatus(), elapsedMin, orphaned, job.getAttemptCount(), job.getId());
                    if (!MapUtils.isEmpty(job.getIntermediateState())) {
                        // DEBUG remove this
                        log.info(">>> tenant={}, integrationId={}, jobId={}, intermediate_state={}", job.getTenantId(), job.getIntegrationId(), job.getId(), job.getIntermediateState());
                    }
                    jobTrackingService.scheduleJob(job);
                    rescheduled.increment();
                });
        if (rescheduled.getValue() > 0 || failed.getValue() > 0) {
            log.info("Rescheduled {} timed-out jobs; marked {} jobs as failed", rescheduled.getValue(), failed.getValue());
        }
    }

    // region utils

    private static int getAttemptCount(DbJob job) {
        return Math.min(0, ObjectUtils.defaultIfNull(job.getAttemptCount(), 0));
    }

    private static int getAttemptMax(DbJob job, int defaultAttemptMax) {
        // if max value is null or 0, use default_max - only explicitly disable retrying if max is negative
        int attemptMax = ObjectUtils.defaultIfNull(job.getAttemptMax(), defaultAttemptMax);
        attemptMax = (attemptMax == 0) ? defaultAttemptMax : attemptMax;
        return attemptMax;
    }

    public static boolean hasReachedMaxAttempts(int attemptCount, int attemptMax) {
        return attemptMax < 0 || attemptCount >= attemptMax;
    }

    // end region
}
