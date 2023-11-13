package io.levelops.etl.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.job_framework.EtlJobRunner;
import io.levelops.etl.job_framework.EtlProcessor;
import io.levelops.etl.job_framework.EtlProcessorRegistry;
import io.levelops.etl.services.JobTrackingUtilsService;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

@Log4j2
@Service
public class EtlEngine {
    public static int JOB_RETENTION_MAX_COUNT = 10000;
    private final ExecutorService threadPoolExecutor;
    private final int nbOfThreads;
    private final EtlJobRunner jobRunner;
    private final Map<String, EngineJob> jobs;
    private final JobTrackingUtilsService jobTrackingUtils;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final EtlProcessorRegistry ETLProcessorRegistry;
    private final MeterRegistry meterRegistry;
    private final IngestionResultPayloadUtils ingestionResultPayloadUtils;

    @Autowired
    public EtlEngine(
            @Value("${ETL_WORKER_THREAD_COUNT:40}") int nbOfThreads,
            @Value("${ETL_WORKER_MONITOR_FREQUENCY_SECONDS:300}") int threadMonitorFrequencyInSeconds,
            EtlJobRunner jobRunner,
            JobTrackingUtilsService jobTrackingUtils,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            EtlProcessorRegistry ETLProcessorRegistry,
            MeterRegistry meterRegistry,
            IngestionResultPayloadUtils ingestionResultPayloadUtils) {
        this.nbOfThreads = nbOfThreads;
        this.jobTrackingUtils = jobTrackingUtils;
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.meterRegistry = meterRegistry;
        this.jobs = new ConcurrentHashMap<>();
        this.jobRunner = jobRunner;
        this.ETLProcessorRegistry = ETLProcessorRegistry;
        this.ingestionResultPayloadUtils = ingestionResultPayloadUtils;
        this.threadPoolExecutor = Executors.newFixedThreadPool(nbOfThreads, new ThreadFactoryBuilder()
                .setNameFormat("worker-%d")
                .build());
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
                .setNameFormat("worker-monitor")
                .build());
        scheduledExecutorService.scheduleAtFixedRate(this::monitorWorkerThreads, 10, threadMonitorFrequencyInSeconds, TimeUnit.SECONDS);
    }

    public boolean canRunJob(JobContext context) {
        try {
            ETLProcessorRegistry.getAggProcessor(context.getEtlProcessorName());
            return true;
        } catch (NotImplementedException e) {
            return false;
        }
    }

    public void clearJobs() {
        jobs.values().removeIf(engineJob -> engineJob.f.isDone());
    }

    public boolean canAcceptJobs() {
        if (jobs.size() > JOB_RETENTION_MAX_COUNT) {
            log.warn("Exceeded maximum job retention count... Clearing out jobs marked as done!");
            clearJobs();
        }
        return jobs.values().stream()
                .filter(job -> !job.f.isDone())
                .count() < nbOfThreads;
    }

    public synchronized void recordHeartbeatForAllThreads() {
        jobs.values().stream()
                .filter(j -> !j.f.isDone())
                .forEach(engineJob -> {
                    try {
                        jobInstanceDatabaseService.update(engineJob.jobContext.getJobInstanceId(), DbJobInstanceUpdate.builder()
                                .heartbeat(Instant.now())
                                .build());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        log.debug("Recorded heartbeat for all threads");
    }

    public Optional<EngineJob> submitJob(JobContext ctx) {
        if (!canAcceptJobs()) {
            log.warn("Engine can not accept more jobs");
            return Optional.empty();
        }
        if (jobs.containsKey(ctx.getJobInstanceId().toString())) {
            log.warn("jobId {} has already been submitted to the engine", ctx.getJobInstanceId());
            return Optional.empty();
        }
        try {
            EtlProcessor<?> ETLProcessor = ETLProcessorRegistry.getAggProcessor(ctx.getEtlProcessorName());
            JobRunnable jobRunnable = new JobRunnable(
                    ctx,
                    ETLProcessor,
                    jobRunner,
                    meterRegistry,
                    jobInstanceDatabaseService,
                    ingestionResultPayloadUtils);
            var f = threadPoolExecutor.submit(jobRunnable);
            EngineJob engineJob = new EngineJob(ctx, f);
            jobs.put(ctx.getJobInstanceId().toString(), engineJob);
            return Optional.of(engineJob);
        } catch (RejectedExecutionException e) {
            log.error("Could not submit job {} to threadpool", ctx.getJobInstanceId(), e);
            return Optional.empty();
        } catch (NotImplementedException e) {
            log.error("Engine could not handle job {} of type {}", ctx.getJobInstanceId(), ctx.getEtlProcessorName(), e);
            return Optional.empty();
        } catch (IOException | IngestionServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void detectTimedoutJobs() {
        try {
            jobs.values().stream()
                    .filter(j -> !j.f.isDone())
                    .forEach(
                            job -> {
                                long elapsedSeconds = job.stopwatch.elapsed(MINUTES);
                                if (elapsedSeconds > job.jobContext.getTimeoutInMinutes()) {
                                    log.warn("Job id {} for tenant {} has timed out. Elapsed time {}s, timeout: {}s",
                                            job.jobContext.getJobInstanceId(), job.jobContext.getTenantId(), elapsedSeconds, job.jobContext.getTimeoutInMinutes());
                                    job.f.cancel(true);
                                    job.stopwatch.stop();
                                    try {
                                        Boolean updated = jobTrackingUtils.updateJobInstanceStatus(job.jobContext.getJobInstanceId(), JobStatus.FAILURE);
                                        if (!updated) {
                                            log.warn("Unable to update timed out job instance id {} to FAILURE state", job.jobContext.getJobInstanceId());
                                        }
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                    );
        } catch (Throwable e) {
            log.error("Failed to detect timedout worker threads", e);
        }
    }

    // Ensure that all the futures that are done have terminal statuses. If they
    // do not then mark the job as failed. Also removes all done jobs from the
    // map.
    private void ensureDoneJobsMarkedAsTerminated() {
        var doneJobs = jobs.values().stream()
                .filter(j -> j.f.isDone()).toList();
        var dbJobInstanceMap = jobInstanceDatabaseService.stream(DbJobInstanceFilter.builder()
                        .jobInstanceIds(doneJobs.stream()
                                .map(j -> j.jobContext.getJobInstanceId())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toMap(DbJobInstance::getJobInstanceId, Function.identity()));
        for (EngineJob doneJob : doneJobs) {
            try {
                DbJobInstance jobInstance = dbJobInstanceMap.get(doneJob.jobContext.getJobInstanceId());
                if (Set.of(JobStatus.ACCEPTED, JobStatus.PENDING).contains(jobInstance.getStatus())) {
                    log.info("Found terminated job future with non-terminal job instance state: {}", jobInstance.getJobInstanceId());
                    jobTrackingUtils.updateJobInstanceStatus(jobInstance.getJobInstanceId(), JobStatus.FAILURE);
                }
                jobs.remove(doneJob.jobContext.getJobInstanceId().toString());
            } catch (Exception e) {
                log.error("Failed to check terminal status of job: {}. Removing from list of futures",
                        doneJob.jobContext.getJobInstanceId(), e);
                jobs.remove(doneJob.jobContext.getJobInstanceId().toString());
            }
        }
    }

    public void monitorWorkerThreads() {
        try {
            detectTimedoutJobs();
            ensureDoneJobsMarkedAsTerminated();
            meterRegistry.gaugeMapSize("etl.worker.running.jobs.gauge", List.of(), jobs);
            log.debug("Completed monitor worker threads");
        } catch (Throwable e) {
            log.error("Error occurred while monitoring engine threads", e);
        }
    }

    public static class JobRunnable implements Runnable {
        private final JobContext jobContext;
        private final EtlProcessor<?> ETLProcessor;
        private final EtlJobRunner ETLJobRunner;
        private final MeterRegistry meterRegistry;
        private final JobInstanceDatabaseService jobInstanceDatabaseService;
        private final IngestionResultPayloadUtils ingestionResultPayloadUtils;

        JobRunnable(
                JobContext jobContext,
                EtlProcessor<?> ETLProcessor,
                EtlJobRunner ETLJobRunner,
                MeterRegistry meterRegistry,
                JobInstanceDatabaseService jobInstanceDatabaseService,
                IngestionResultPayloadUtils ingestionResultPayloadUtils) throws IOException, IngestionServiceException {
            this.jobContext = jobContext.toBuilder()
                    .jobInstanceDatabaseService(jobInstanceDatabaseService)
                    .build();
            this.ETLProcessor = ETLProcessor;
            this.ETLJobRunner = ETLJobRunner;
            this.meterRegistry = meterRegistry;
            this.jobInstanceDatabaseService = jobInstanceDatabaseService;
            this.ingestionResultPayloadUtils = ingestionResultPayloadUtils;
        }

        /**
         * This method is used to rehydrate the job context with the payload. This can come from 3 sources:
         * 1. GCS - if the payload_gcs_filename is set
         * 2. From the job_instance_payload table - if payload_gcs_filename is not set (Deprecated)
         * 3. Calculated in realtime if neither payload_gcs_filename nor the payload is set in the DB. This is then stored in GCS
         * and referenced in the DB using payload_gcs_filename
         *
         * @return the rehydrated job context
         */
        private JobContext rehydrateJobContext() throws IOException, IngestionServiceException {
            return jobContext
                    .withRehydratedPayload(jobInstanceDatabaseService, ingestionResultPayloadUtils)
                    .withMetadataAccessors(jobInstanceDatabaseService);
        }

        @Override
        public void run() {
            Stopwatch stopwatch = Stopwatch.createStarted();
            JobStatus status = null;
            JobContext rehydratedJobContext;

            if (jobContext.getJobType().equals(JobType.INGESTION_RESULT_PROCESSING_JOB)) {
                try {
                    rehydratedJobContext = rehydrateJobContext();
                } catch (Throwable e) {
                    log.error("Unable to rehydrate job context for {}", jobContext.getJobInstanceId(), e);
                    throw new RuntimeException(e);
                }
            } else {
                rehydratedJobContext = jobContext;
            }

            try {
                var timerSample = LongTaskTimer
                        .builder("etl.worker.job.run.timer")
                        .tags("tenant_id", rehydratedJobContext.getTenantId(),
                                "integration_id", rehydratedJobContext.getIntegrationId(),
                                "processor", rehydratedJobContext.getEtlProcessorName())
                        .register(meterRegistry).start();
                status = ETLJobRunner.run(rehydratedJobContext, ETLProcessor);
                timerSample.stop();
            } catch (Throwable e) {
                log.info("Exception occurred during job run {}, tenant {}, integration {} {}",
                        rehydratedJobContext.getJobInstanceId(),
                        rehydratedJobContext.getTenantId(),
                        rehydratedJobContext.getIntegrationType(),
                        rehydratedJobContext.getIntegrationId(), e);
            }
            stopwatch.stop();
            long elapsedSeconds = stopwatch.elapsed(SECONDS);
            log.info("Job {}, tenant {}, integration {} {}, Status: {} ran in {} seconds",
                    rehydratedJobContext.getJobInstanceId(),
                    rehydratedJobContext.getTenantId(),
                    rehydratedJobContext.getIntegrationType(),
                    rehydratedJobContext.getIntegrationId(),
                    status,
                    elapsedSeconds);
        }
    }

    public static class EngineJob {
        final JobContext jobContext;
        public final Future<?> f;
        final Stopwatch stopwatch;

        EngineJob(JobContext ctx, Future<?> f) {
            this.jobContext = ctx;
            this.f = f;
            this.stopwatch = Stopwatch.createStarted();
        }
    }
}
