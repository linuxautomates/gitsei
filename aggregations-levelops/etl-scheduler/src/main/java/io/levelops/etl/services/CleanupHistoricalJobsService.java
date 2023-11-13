package io.levelops.etl.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceDelete;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deletes job instances that are older than a certain threshold. This ensures
 * that our db doesn't get too big
 */
@Log4j2
@Service
public class CleanupHistoricalJobsService {
    private static final long WARMUP_DELAY_SECS = 10;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final ScheduledExecutorService executorService;
    private Future<?> schedulingFuture;
    private final int oldJobCleanupThresholdInDays;

    public CleanupHistoricalJobsService(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            @Value("${CLEANUP_JOBS_SCHEDULING_SECONDS:86400}") int schedulingIntervalInSec,
            @Value("${OLD_JOBS_CLEANUP_THRESHOLD_DAYS:30}") int oldJobCleanupThresholdInDays) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.oldJobCleanupThresholdInDays = oldJobCleanupThresholdInDays;
        executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("cleanup-old-jobs-%d")
                .build());
        initScheduling(schedulingIntervalInSec);
    }

    private void initScheduling(long schedulingIntervalInSec) {
        if (schedulingIntervalInSec <= 0) {
            log.warn("Non-positive scheduling interval provided. Will not start " +
                    "schedule thread. Scheduling interval: {}", schedulingIntervalInSec);
            return;
        }
        schedulingFuture = executorService.scheduleAtFixedRate(
                new CleanupHistoricalJobsRunnable(),
                WARMUP_DELAY_SECS,
                schedulingIntervalInSec,
                TimeUnit.MINUTES);
    }

    @VisibleForTesting
    public void stopScheduling() {
        schedulingFuture.cancel(true);
    }

    @VisibleForTesting
    public int deleteOldJobInstances() {
        int deleteCount = jobInstanceDatabaseService.delete(DbJobInstanceDelete.builder()
                .createdAtBefore(Instant.now().minus(oldJobCleanupThresholdInDays, ChronoUnit.DAYS))
                .build());
        log.info("Deleted {} old job instances", deleteCount);
        return deleteCount;
    }

    public class CleanupHistoricalJobsRunnable implements Runnable {
        @Override
        public void run() {
            try {
                deleteOldJobInstances();
            } catch (Exception e) {
                log.error("Failed to delete old job instances", e);
            }
        }
    }
}
