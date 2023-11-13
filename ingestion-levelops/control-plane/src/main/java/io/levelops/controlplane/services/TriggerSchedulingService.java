package io.levelops.controlplane.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.commons.dates.DateUtils;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService.TriggeredJobFilter;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.TriggerRunnableRegistry;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Log4j2
@Service
public class TriggerSchedulingService {

    private static final int PAGE_SIZE = 100;
    private static final int TRIGGER_EXECUTOR_THREADS = 10;
    private static final int FORCED_LOGGING_PERIOD_SECS = (int) TimeUnit.MINUTES.toSeconds(30);
    private final ScheduledExecutorService scheduler;
    private final ExecutorService triggerExecutor;
    private final TriggerDatabaseService triggerDatabaseService;
    private final TriggeredJobDatabaseService triggeredJobDatabaseService;
    private final TriggerRunnableRegistry triggerRunnableRegistry;
    private final int defaultBackPressureThreshold;
    private final Map<String, Integer> backPressureThresholdByTriggerType;
    private boolean schedulingEnabled = true;
    private Instant lastLogging = Instant.MIN;

    @Autowired
    public TriggerSchedulingService(@Value("${TRIGGER_SCHEDULING_INTERVAL:60}") Long triggerSchedulingIntervalSec,
                                    @Value("${TRIGGER_SCHEDULING_BACK_PRESSURE_THRESHOLD_DEFAULT:5}") int defaultBackPressureThreshold,
                                    @Value("#{${TRIGGER_SCHEDULING_BACK_PRESSURE_THRESHOLD:{github:1,azure_devops:1,testrails:1}}}") Map<String, Integer> backPressureThreshold,
                                    TriggerDatabaseService triggerDatabaseService,
                                    TriggeredJobDatabaseService triggeredJobDatabaseService,
                                    TriggerRunnableRegistry triggerRunnableRegistry,
                                    final ExpirableLockRegistry lockRegistry) {
        this.defaultBackPressureThreshold = defaultBackPressureThreshold;
        this.backPressureThresholdByTriggerType = backPressureThreshold;
        this.triggerDatabaseService = triggerDatabaseService;
        this.triggeredJobDatabaseService = triggeredJobDatabaseService;
        this.triggerRunnableRegistry = triggerRunnableRegistry;
        scheduler = initScheduling(this, triggerSchedulingIntervalSec, lockRegistry);
        triggerExecutor = initTriggerExecutor(TRIGGER_EXECUTOR_THREADS);
    }

    private static ScheduledExecutorService initScheduling(TriggerSchedulingService triggerSchedulingService, long schedulingIntervalInSec, final ExpirableLockRegistry lockRegistry) {
        if (schedulingIntervalInSec <= 0) {
            log.info("Trigger scheduling is DISABLED");
            return null;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("scheduler-%d")
                .build());
        executor.scheduleAtFixedRate(new TriggerSchedulerTask(triggerSchedulingService, lockRegistry), 5, schedulingIntervalInSec, TimeUnit.SECONDS);
        log.info("Trigger scheduling is ENABLED (interval={}sec)", schedulingIntervalInSec);
        return executor;
    }

    private static ExecutorService initTriggerExecutor(int nbThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads, new ThreadFactoryBuilder()
                .setNameFormat("triggers-%d")
                .build());
        return executor;
    }

    private static class TriggerSchedulerTask implements Runnable {

        private static final Object SCHEDULING_LOCK_KEY = "trigger_scheduler_task";
        private final TriggerSchedulingService triggerSchedulingService;
        private final ExpirableLockRegistry lockRegistry;

        public TriggerSchedulerTask(TriggerSchedulingService triggerSchedulingService, final ExpirableLockRegistry lockRegistry) {
            this.triggerSchedulingService = triggerSchedulingService;
            this.lockRegistry = lockRegistry;
        }

        @Override
        public void run() {
            Lock lock = null;
            try {
                lock = lockRegistry.obtain(SCHEDULING_LOCK_KEY);
                if(!lock.tryLock()){
                    log.info("Lock not acquired, skipping execution...");
                    lock = null;
                    return;
                }
                if (!triggerSchedulingService.schedulingEnabled) {
                    log.debug("Scheduling is disabled");
                    return;
                }
                log.debug("Scheduling triggers...");
                triggerSchedulingService.scheduleAllTriggers(Instant.now());
                log.debug("Scheduling done");
            } catch (Throwable e) {
                log.warn("Failed to run Trigger Scheduling task", e);
            }
            finally{
                if (lock != null){
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

    public void enableScheduling(boolean enableScheduling) {
        this.schedulingEnabled = enableScheduling;
        if (schedulingEnabled) {
            log.info("Trigger scheduling has been ENABLED!");
        } else {
            log.info("Trigger scheduling has been DISABLED!");
        }
    }

    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }

    public void scheduleAllTriggers(Instant current) {
        MutableLong total = new MutableLong(0);
        MutableLong disabled = new MutableLong(0);
        MutableLong schedulableCount = new MutableLong(0);
        MutableLong backPressureCount = new MutableLong(0);
        long triggered = triggerDatabaseService.streamTriggers(PAGE_SIZE)
                .peek(trigger -> {
                    total.increment();
                    if (trigger.isDisabled()) {
                        disabled.increment();
                    }
                })
                .filter(trigger -> {
                    boolean schedulable = trigger.isSchedulable(current);
                    if (schedulable) {
                        schedulableCount.increment();
                    }
                    return schedulable;
                })
                .filter(trigger -> {
                    if (this.isTriggerBackPressured(trigger)) {
                        int backPressureThreshold = getBackPressureThreshold(trigger);
                        log.debug("⊝ Back pressure (more than {} jobs) for trigger_type={}, tenant={}, integration={}, trigger_id={}", backPressureThreshold, trigger.getType(), trigger.getTenantId(), trigger.getIntegrationId(), trigger.getId());
                        backPressureCount.increment();
                        return false;
                    }
                    return true;
                })
                .map(trigger -> scheduleTrigger(trigger, current))
                .count();
        if (triggered > 0 || lastLogging.isBefore(Instant.now().minusSeconds(FORCED_LOGGING_PERIOD_SECS))) {
            log.info("Scheduled {} trigger(s) (schedulable={}, back_pressured={}, disabled={}, total={})",
                    triggered, schedulableCount.longValue(), backPressureCount.longValue(), disabled.longValue(), total.longValue());
            lastLogging = Instant.now();
        }
    }

    public boolean isTriggerBackPressured(DbTrigger trigger) {
        int backPressureThreshold = getBackPressureThreshold(trigger);
        TriggerBackPressureStrategy strategy = TriggerBackPressureStrategy.forTriggerType(trigger.getType());
        int jobCount = 0;

        // -- count in progress jobs (or soon to be)
        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SCHEDULED, JobStatus.UNASSIGNED, JobStatus.PENDING))
                .build();
        jobCount += triggeredJobDatabaseService.filterTriggeredJobs(0, backPressureThreshold, trigger.getId(), filter, null).getRecords().size();
        if (jobCount >= backPressureThreshold) {
            return true;
        }

        // -- count retryable jobs
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.FAILURE))
                .belowMaxAttemptsOrDefaultValue(strategy.getFailedJobsMaxAttempt())
                .build();
        jobCount += triggeredJobDatabaseService.filterTriggeredJobs(0, backPressureThreshold, trigger.getId(), filter, null).getRecords().size();
        // TODO remove debug log
        if (jobCount >= backPressureThreshold) {
            log.debug("(Back-pressured because of failed jobs for triggerType={}, triggerId={}, total={}/{})", trigger.getType(), trigger.getId(), jobCount, backPressureThreshold);
        }

        return jobCount >= backPressureThreshold;
    }

    public int getBackPressureThreshold(DbTrigger trigger) {
        if (trigger.getSettings() != null && trigger.getSettings().getBackpressureThreshold() != null) {
            return trigger.getSettings().getBackpressureThreshold();
        }
        String triggerType = trigger.getType();
        if (StringUtils.isBlank(triggerType)) {
            return defaultBackPressureThreshold;
        }
        return backPressureThresholdByTriggerType.getOrDefault(triggerType.trim().toLowerCase(), defaultBackPressureThreshold);
    }

    public UUID scheduleTrigger(DbTrigger trigger, Instant current) {
        UUID iterationId = UUID.randomUUID();

        triggerDatabaseService.updateTriggerWithIteration(trigger.getId(), iterationId.toString(), current);
        DbTrigger updatedTrigger = trigger.toBuilder()
                .iterationId(iterationId.toString())
                .iterationTs(DateUtils.toEpochSecond(current))
                .build();

        Optional<TriggerRunnable> triggerRunnable = triggerRunnableRegistry.get(trigger.getType());
        if (triggerRunnable.isEmpty()) {
            log.warn("Could not find trigger runnable for type: {} - {}", trigger.getType(), trigger);
            return iterationId;
        }

        triggerExecutor.submit(() -> {
            try {
                triggerRunnable.get().run(updatedTrigger);
            } catch (Exception e) {
                log.warn("Failed to execute trigger: {}", updatedTrigger, e);
            }
        });
        log.info("⚑ Triggered iteration={} for trigger={} (type={}, elapsed={}sec, freq={}min) ", iterationId, trigger.getId(), trigger.getType(), trigger.getElapsedInSeconds(current), trigger.getFrequency());
        return iterationId;
    }

}
