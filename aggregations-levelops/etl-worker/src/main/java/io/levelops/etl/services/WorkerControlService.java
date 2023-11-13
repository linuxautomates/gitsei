package io.levelops.etl.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.clients.EtlSchedulerClient;
import io.levelops.etl.engine.EtlEngine;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This service is responsible for pulling jobs from the scheduler, claiming the
 * jobs and running them on the agent node through an instance of the AggsEngine
 */
@Log4j2
@Service
public class WorkerControlService {
    private static final int DEFAULT_WARMUP_DELAY_SECS = 10;
    private final EtlEngine engine;
    private final EtlSchedulerClient etlSchedulerClient;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final String workerId;
    private final int schedulingIntervalInSec;
    private final int heartbeatIntervalInSec;
    private final int warmupDelaySecs;
    private final int maxNewJobsInOneCycle;
    private final ScheduledExecutorService runJobExecutor;
    private final ScheduledExecutorService heartbeatExecutor;
    private Future<?> jobFetcherLoopFuture;
    private Future<?> heartbeatLoopFuture;

    @Autowired
    public WorkerControlService(
            EtlEngine engine,
            EtlSchedulerClient etlSchedulerClient,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            @Qualifier("workerId") String workerId,
            @Value("${WORKER_CONTROL_INTERVAL_SECONDS:60}") int schedulingIntervalInSec,
            @Value("${HEARTBEAT_INTERVAL_SECONDS:120}") int heartbeatIntervalInSec,
            @Value("${MAX_NEW_JOBS_IN_ONE_CYCLE:10}") int maxNewJobsInOneCycle) {
        this.engine = engine;
        this.etlSchedulerClient = etlSchedulerClient;
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.workerId = workerId;
        this.schedulingIntervalInSec = schedulingIntervalInSec;
        this.heartbeatIntervalInSec = heartbeatIntervalInSec;
        this.warmupDelaySecs = DEFAULT_WARMUP_DELAY_SECS;
        this.maxNewJobsInOneCycle = maxNewJobsInOneCycle;
        runJobExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("worker-loop-%d")
                .build());
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("heart-beat-%d")
                .build());
        initScheduling();
    }

    private void initScheduling() {
        log.info("Initializing scheduler threads");
        if (schedulingIntervalInSec > 0) {
            jobFetcherLoopFuture = runJobExecutor.scheduleAtFixedRate(
                    new JobFetcherLoop(), warmupDelaySecs, schedulingIntervalInSec, TimeUnit.SECONDS);
        } else {
            log.warn("Worker control service job fetcher loop is disabled");
        }

        if (heartbeatIntervalInSec > 0) {
            heartbeatLoopFuture = heartbeatExecutor.scheduleAtFixedRate(
                    new HeartbeatLoop(), warmupDelaySecs, heartbeatIntervalInSec, TimeUnit.SECONDS);
        } else {
            log.warn("Worker control service heart beat loop is disabled");
        }
    }

    @VisibleForTesting
    public void stopScheduling() {
        log.warn("Stopping scheduling on worker {}", workerId);
        jobFetcherLoopFuture.cancel(true);
        heartbeatLoopFuture.cancel(true);
    }

    private List<JobContext> getJobsToRun() throws EtlSchedulerClient.SchedulerClientException {
        var jobContextList = etlSchedulerClient.getJobsToRun();
        var filteredJobContextList = jobContextList.stream().filter(engine::canRunJob).collect(Collectors.toList());
        if (jobContextList.size() > 0 || filteredJobContextList.size() > 0) {
            log.info("Total jobs received from scheduler: {}. Filtered job count: {}", jobContextList.size(), filteredJobContextList.size());
        }
        return filteredJobContextList;
    }

    public synchronized void fetchAndRunJobs() throws EtlSchedulerClient.SchedulerClientException {
        List<JobContext> jobContextList = getJobsToRun();
        int newJobsRunCount = 0;
        for (JobContext jobContext : jobContextList) {
            // This prevents the current worker from hogging up all the queued
            // jobs leading to imbalanced worker nodes
            if (newJobsRunCount >= maxNewJobsInOneCycle) {
                return;
            }
            if (engine.canAcceptJobs() && etlSchedulerClient.claimJob(jobContext.getJobInstanceId(), workerId)) {
                log.info("Claimed job {} on worker {}", jobContext.getJobInstanceId(), workerId);
                Optional<EtlEngine.EngineJob> engineJob = engine.submitJob(jobContext);
                if (engineJob.isEmpty()) {
                    etlSchedulerClient.unclaimJob(jobContext.getJobInstanceId(), workerId);
                    log.info("Unclaimed job {} on worker {}", jobContext.getJobInstanceId(), workerId);
                } else {
                    newJobsRunCount++;
                }
            }
        }
    }

    private class HeartbeatLoop implements Runnable {
        @Override
        public void run() {
            try {
                engine.recordHeartbeatForAllThreads();
            } catch (Throwable e) {
                log.error("Exception occurred while running heart beat loop", e);
            }
        }
    }

    private class JobFetcherLoop implements Runnable {
        @Override
        public void run() {
            try {
                fetchAndRunJobs();
            } catch (Throwable e) {
                log.error("Exception occurred while running worker loop", e);
            }
        }
    }
}
