package io.levelops.ingestion.engine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.components.IngestionComponent;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.engine.exceptions.WorkerMonitorException;
import io.levelops.ingestion.engine.runnables.JobRunnable;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Log4j2
public class IngestionEngine {

    private final int THREAD_MONITOR_PERIOD_SECS = 300;
    private final int JOB_RETENTION_MAX_COUNT = 1 << 20; // must be less than HashMap.MAXIMUM_CAPACITY (1<<30)
    private final CallbackService callbackService;
    private final ExecutorService threadPoolExecutor;
    private final Set<EngineEntity> entities;
    private final Map<String, EngineJob> jobs;
    private final int nbOfThreads;

    public IngestionEngine(int nbOfThreads, CallbackService callbackService) {
        this.nbOfThreads = nbOfThreads;
        this.callbackService = callbackService;
        this.entities = new HashSet<>();
        this.jobs = new ConcurrentHashMap<>();
        this.threadPoolExecutor = Executors.newFixedThreadPool(nbOfThreads, new ThreadFactoryBuilder()
                .setNameFormat("worker-%d")
                .build());
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
                .setNameFormat("worker-monitor")
                .build());
        scheduledExecutorService.scheduleAtFixedRate(this::monitorWorkerThreads, 10, THREAD_MONITOR_PERIOD_SECS, TimeUnit.SECONDS);
    }

    public static class EngineJobIntermediateStateUpdater implements IntermediateStateUpdater {
        private final EngineJob job;

        public EngineJobIntermediateStateUpdater(EngineJob job) {
            this.job = job;
        }

        public void updateIntermediateState(Map<String, Object> intermediateState) {
            job.setIntermediateState(intermediateState);
        }

        @Override
        public Map<String, Object> getIntermediateState() {
            return job.getIntermediateState();
        }
    }


    // region Jobs

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class EngineJob {
        private String id;
        private String agentId;
        private String controllerId;
        private String controllerName;
        private DataController<?> controller;
        private Date createdAt;
        private DataQuery query;
        @VisibleForTesting
        protected boolean done;
        private Date doneAt;
        private String callbackUrl;
        @Setter
        private boolean canceled;
        private Map<String, Object> result;
        @Setter
        private List<IngestionFailure> ingestionFailures;
        @Setter
        private Map<String, Object> intermediateState;
        @Setter
        private Throwable exception;
        @Setter
        private Future<?> future;
        @Setter
        private Thread workerThread;

        public void markAsDone() {
            this.done = true;
            this.doneAt = new Date();
        }

        public void setResult(Object result) {
            this.result = ParsingUtils.toJsonObject(DefaultObjectMapper.get(), result);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("createdAt", createdAt)
                    .append("callbackUrl", callbackUrl)
                    .append("done", done)
                    .toString();
        }
    }

    public boolean canAcceptJobs() {
        if (jobs.size() > JOB_RETENTION_MAX_COUNT) {
            log.warn("Exceeded maximum job retention count... Clearing out jobs marked as done!");
            clearJobs();
        }
        return jobs.values().stream()
                .filter(job -> !job.isDone())
                .count() < nbOfThreads;
    }

    public synchronized <Q extends DataQuery> Optional<EngineJob> submitJob(DataController<Q> controller,
                                                                            DataQuery query,
                                                                            String agentId,
                                                                            @Nullable JobContext jobContext,
                                                                            @Nullable String callbackUrl,
                                                                            @Nullable String overrideJobId) {
        try {
            if (!canAcceptJobs()) {
                log.warn("Cannot accept more jobs at this time");
                return Optional.empty(); // TODO improve error handling
            }
            String jobId = StringUtils.defaultIfBlank(overrideJobId, UUID.randomUUID().toString());
            if (getJobById(jobId).isPresent()) {
                log.warn("Job already submitted: job_id={}", jobId);
                return Optional.empty();
            }

            if (jobContext == null) {
                jobContext = JobContext.builder()
                        .jobId(jobId)
                        .build();
            }
            final JobContext jobContextFinal = jobContext;

            Optional<EngineEntity> entity = getEntityByIngestionComponent(controller);
            EngineJob job = EngineJob.builder()
                    .id(jobId)
                    .controllerId(entity.map(EngineEntity::getId).orElse(null))
                    .controllerName(entity.map(EngineEntity::getName).orElse(null))
                    .agentId(agentId)
                    .controller(controller)
                    .query(query)
                    .callbackUrl(callbackUrl)
                    .createdAt(new Date())
                    .build();

            // run ingestion in JobRunnable to keep track of result or exception
            IntermediateStateUpdater intermediateStateUpdater = new EngineJobIntermediateStateUpdater(job);
            @SuppressWarnings("unchecked")
            JobRunnable<?> jobRunnable = JobRunnable.builder()
                    .jobContext(jobContextFinal)
                    .job(job)
                    .callable(() -> controller.ingest(jobContextFinal, (Q) query, intermediateStateUpdater))
                    .callbackService(callbackService)
                    .build();

            Future<?> future = threadPoolExecutor.submit(jobRunnable);
            job.setFuture(future);
            jobs.put(jobId, job);

            return Optional.of(job);

        } catch (RejectedExecutionException e) {
            // TODO improve error handling
            log.error("Could not submit job for controller={} and query={}", controller.getComponentClass(), query, e);
            return Optional.empty();
        }

    }

    public enum JobValidity {
        VALID_RUNNING,
        VALID_DONE,
        INVALID_DONE_BUT_STILL_RUNNING,
        INVALID_NOT_DONE_BUT_NOT_RUNNING
    }

    public JobValidity checkJobValidity(EngineJob job) {
        boolean workerRunning = job.getFuture() != null && !job.getFuture().isDone();
        boolean jobDone = job.isDone();
        if (jobDone) {
            if (workerRunning) {
                return JobValidity.INVALID_DONE_BUT_STILL_RUNNING;
            } else {
                return JobValidity.VALID_DONE;
            }
        } else {
            if (workerRunning) {
                return JobValidity.VALID_RUNNING;
            } else {
                return JobValidity.INVALID_NOT_DONE_BUT_NOT_RUNNING;
            }
        }
    }

    public void monitorWorkerThreads() {
        try {
            if (jobs.isEmpty()) {
                return;
            }
            int doneCount = 0;
            int workerRunningCount = 0;
            int errors = 0;
            for (EngineJob job : jobs.values()) {
                JobValidity jobValidity = checkJobValidity(job);
                switch (jobValidity) {
                    case VALID_DONE:
                        doneCount++;
                        break;
                    case VALID_RUNNING:
                        workerRunningCount++;
                        break;
                    case INVALID_DONE_BUT_STILL_RUNNING:
                        log.warn("[worker-monitor] Invalid State: Job was marked as done but worker is STILL running (will be interrupted) - job_id={}", job.getId());
                        job.getFuture().cancel(true);
                        errors++;
                        break;
                    case INVALID_NOT_DONE_BUT_NOT_RUNNING:
                        log.warn("[worker-monitor] Invalid State: Job is still pending but worker is NOT running (will be marked as done) - job_id={}", job.getId());
                        job.setException(new WorkerMonitorException("Job worker thread terminated early"));
                        job.markAsDone();
                        errors++;
                        break;
                }
            }
            long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            log.info("[worker-monitor] jobs={} (done={}, running={}, invalid_state={})", jobs.size(), doneCount, workerRunningCount, errors);
            log.info("[worker-monitor] memory: free={}/{} MB, max={} MB", freeMemory, totalMemory, maxMemory);
        } catch (Throwable e) {
            log.error("Failed to execute worker monitoring", e);
        }
    }

    public Map<String, Object> generateTelemetryData() {
        Map<JobValidity, Long> validityCounts = MapUtils.emptyIfNull(jobs).values().stream()
                .filter(Objects::nonNull)
                .map(this::checkJobValidity)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long done = validityCounts.getOrDefault(JobValidity.VALID_DONE, 0L);
        long running = validityCounts.getOrDefault(JobValidity.VALID_RUNNING, 0L);
        long doneButRunning = validityCounts.getOrDefault(JobValidity.INVALID_DONE_BUT_STILL_RUNNING, 0L);
        long notDoneButNotRunning = validityCounts.getOrDefault(JobValidity.INVALID_NOT_DONE_BUT_NOT_RUNNING, 0L);
        Map<String, Object> jobStats = new HashMap<>();
        jobStats.put("total", jobs.size());
        jobStats.put("done", done);
        jobStats.put("running", running);
        jobStats.put("invalid_done_but_running", doneButRunning);
        jobStats.put("invalid_not_done_but_not_running", notDoneButNotRunning);
        jobStats.put("invalid_total", doneButRunning + notDoneButNotRunning);
        return Map.of("jobs", jobStats);
    }

    public Collection<EngineJob> getJobs() {
        return jobs.values();
    }

    public Optional<EngineJob> getJobById(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public synchronized boolean cancelJob(EngineJob job) {
        if (job.isDone() || job.isCanceled()) {
            return true;
        }
        if (!job.getFuture().cancel(true)) {
            return false;
        }
        job.setCanceled(true);
        job.markAsDone();
        return true;
    }

    public boolean clearJobIfDone(String jobId) {
        return getJobById(jobId)
                .filter(EngineJob::isDone)
                .map(j -> jobs.remove(jobId))
                .isPresent();
    }

    public void clearJobs() {
        jobs.values().removeIf(EngineJob::isDone);
    }
    //endregion

    public EngineEntity registerIngestionComponent(IngestionComponent component) {
        return registerIngestionComponent(null, component);
    }

    /**
     * Register ingestion component as entity of the engine, so that it an be discovered.
     */
    public synchronized EngineEntity registerIngestionComponent(String name, IngestionComponent component) {
        String id = UUID.randomUUID().toString();
        String entityName = StringUtils.defaultString(name, component.getComponentClass() + "-" + id);

        // check if the name is already taken
        Optional<EngineEntity> alreadyTaken = entities.stream()
                .filter(e -> name.equals(e.getName()))
                .findAny();
        if (alreadyTaken.isPresent()) {
            throw new IllegalStateException(String.format("Entity name '%s' is already taken: %s", name, alreadyTaken.get()));
        }

        EngineEntity entity = EngineEntity.builder()
                .id(id)
                .name(entityName)
                .ingestionComponent(component)
                .build();
        entities.add(entity);
        return entity;
    }

    /**
     * Convenience method
     */
    public <T extends IngestionComponent> T add(String name, T component) {
        registerIngestionComponent(name, component);
        return component;
    }

    public Optional<EngineEntity> getEntityById(String id) {
        return entities.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    public Optional<EngineEntity> getEntityByName(String name) {
        return entities.stream()
                .filter(e -> e.getName().equals(name))
                .findFirst();
    }

    public Set<EngineEntity> getEntitiesByComponentType(String type) {
        if (StringUtils.isEmpty(type)) {
            return entities;
        }
        return entities.stream()
                .filter(e -> e.getIngestionComponent().getComponentType().equals(type))
                .collect(Collectors.toSet());
    }

    public Optional<EngineEntity> getEntityByIngestionComponent(IngestionComponent component) {
        return entities.stream()
                .filter(e -> e.getIngestionComponent().equals(component))
                .findAny();
    }
}
