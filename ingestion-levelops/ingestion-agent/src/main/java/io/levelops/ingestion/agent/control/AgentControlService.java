package io.levelops.ingestion.agent.control;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.exceptions.RuntimeInterruptedException;
import io.levelops.commons.io.RollingOutputStream;
import io.levelops.commons.models.AgentResponse;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.agent.controllers.JobController;
import io.levelops.ingestion.agent.model.jobs.JobView;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.engine.EngineEntity;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.models.AgentHandle;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.Job;
import io.levelops.services.IngestionAgentControlClient;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.TooManyRequestsException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.http.HttpStatus;

import javax.annotation.Nullable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible of pulling job requests from control-plane and reporting jobs status back,
 * as well as sending a heartbeat for agent registration purposes.
 * Uses a scheduled thread with a fixed interval.
 */
@Slf4j
@Getter
public class AgentControlService {

    private static final int WARMUP_DELAY_SECS = 5;
    private static final long WAIT_IN_S_WHEN_RATE_LIMITED = 5;
    private final IngestionAgentControlClient controlClient;
    private final String agentId;
    private final String agentType;
    private final String agentVersion;
    @Nullable
    private final RollingOutputStream rollingLog;
    @Nullable
    private final String tenantId;
    @Nullable
    private final List<String> integrationIds;
    private final IngestionEngine ingestionEngine;
    private final JobController jobController;
    private final ScheduledExecutorService scheduler;

    /**
     * Jobs marked as reserved belong to integrations that must be ingested by a dedicated agent,
     * and therefore should be not be pulled by generic agents.
     * To ingest reserved jobs, dedicated agents will have to explicitly specify which tenant and integration ids they want.
     * - If null, will pull *all* jobs.
     * - If true, will only pull reserved jobs for a given tenant and integration ids.
     * - If false, will only pull non-reserved jobs.
     */
    private Boolean reservedJobsFilter;
    private boolean autoClearJobs;
    private boolean enableScheduling;
    private long schedulingIntervalInSec;

    private boolean firstRegistrationDone = false;

    @Slf4j
    private static class ControlPlaneSchedulerTask implements Runnable {

        private final AgentControlService agentControlService;

        public ControlPlaneSchedulerTask(AgentControlService agentControlService) {
            this.agentControlService = agentControlService;
        }

        @Override
        public void run() {
            log.debug("Scheduling...");
            try {
                agentControlService.doRegistrationAndHeartbeat();
                agentControlService.syncJobs();
            } catch (IngestionAgentControlClient.ControlException e) {
                log.warn("Could not communicate with control-plane", e);
            } catch (Throwable e) {
                log.error("Error while scheduling agent control task", e);
            }
            log.debug("Scheduling done.");
        }

    }

    @Builder
    public AgentControlService(IngestionAgentControlClient controlClient,
                               String agentId,
                               String agentType,
                               String agentVersion,
                               @Nullable RollingOutputStream rollingLog,
                               @Nullable String tenantId,
                               @Nullable List<String> integrationIds,
                               IngestionEngine ingestionEngine,
                               JobController jobController,
                               Boolean autoClearJobs,
                               Boolean enableScheduling,
                               Long schedulingIntervalInSec,
                               Boolean reservedJobsFilter) {
        // TODO add state validation, e.g. disable if controlClient is not initialized?
        this.controlClient = controlClient;
        this.agentId = agentId;
        this.agentType = agentType;
        this.agentVersion = agentVersion;
        this.rollingLog = rollingLog;
        this.tenantId = tenantId;
        this.integrationIds = integrationIds;
        this.ingestionEngine = ingestionEngine;
        this.jobController = jobController;
        this.autoClearJobs = Boolean.TRUE.equals(autoClearJobs);
        this.enableScheduling = Boolean.TRUE.equals(enableScheduling);
        this.schedulingIntervalInSec = ObjectUtils.defaultIfNull(schedulingIntervalInSec, TimeUnit.MINUTES.toSeconds(1));
        this.reservedJobsFilter = reservedJobsFilter;
        log.info("control_plane_scheduling={}, scheduling_interval_sec={}, auto_clear_jobs={}", enableScheduling, schedulingIntervalInSec, autoClearJobs);
        this.scheduler = this.enableScheduling ? initScheduling(this, this.schedulingIntervalInSec) : null;
    }

    private static ScheduledExecutorService initScheduling(AgentControlService agentControlService, long schedulingIntervalInSec) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("scheduler-%d")
                .build());
        executor.scheduleAtFixedRate(new ControlPlaneSchedulerTask(agentControlService), WARMUP_DELAY_SECS, schedulingIntervalInSec, TimeUnit.SECONDS);
        return executor;
    }

    private AgentHandle generateMyHandle(boolean includeTelemetry) {
        var builder = AgentHandle.builder()
                .agentId(agentId)
                .agentType(agentType)
                .controllerNames(getControllers())
                .tenantId(tenantId)
                .integrationIds(integrationIds);
        if (includeTelemetry) {
            builder.telemetry(generateTelemetryData());
        }
        return builder.build();
    }

    private Map<String, Object> generateTelemetryData() {
        // TODO move this to its own service if it becomes too big

        Map<String, Object> telemetry = new HashMap<>();
        // version
        telemetry.put("version", agentVersion);

        // uptime
        telemetry.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());

        // log
        if (rollingLog != null) {
            telemetry.put("log", rollingLog.toString());
        }

        // memory
        telemetry.put("memory", Map.of(
                "free", Runtime.getRuntime().freeMemory() / 1024 / 1024,
                "total", Runtime.getRuntime().totalMemory() / 1024 / 1024,
                "max", Runtime.getRuntime().maxMemory() / 1024 / 1024));

        // cpu
        telemetry.put("cpu", Map.of(
                "cores", Runtime.getRuntime().availableProcessors()));

        // jobs
        telemetry.putAll(ingestionEngine.generateTelemetryData());

        return telemetry;
    }

    private Set<String> getControllers() {
        // TODO cache?
        return ingestionEngine.getEntitiesByComponentType(DataController.COMPONENT_TYPE).stream()
                .map(EngineEntity::getName)
                .collect(Collectors.toSet());
    }

    public void doRegistrationAndHeartbeat() throws IngestionAgentControlClient.ControlException {
        AgentHandle agentHandle = generateMyHandle(true);
        log.debug("My handle: {}", agentHandle);
        if (!firstRegistrationDone) {
            controlClient.registerAgent(agentHandle);
            firstRegistrationDone = true;
            log.info("Registered agent_id={}", agentId);
        } else {
            try {
                controlClient.sendHeartbeat(agentHandle);
            } catch (IngestionAgentControlClient.ControlException e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (!(rootCause instanceof HttpException)) {
                    throw e;
                }
                Integer httpCode = ((HttpException) rootCause).getCode();
                if (Objects.equals(HttpStatus.CONFLICT.value(), httpCode)) {
                    log.warn("Control-plane: agent not yet registered!");
                    controlClient.registerAgent(agentHandle);
                    log.info("Re-registered agent_id={}", agentId);
                } else {
                    throw e;
                }
            }
            log.info("♥");
        }
    }

    public void syncJobs() {
        findJobs();
        reportJobs();
    }

    public void findJobs() {
        if (!ingestionEngine.canAcceptJobs()) {
            log.debug("Too busy, will not look for jobs");
            return;
        }

        Collection<CreateJobRequest> createJobRequestList = pullJobRequests();
        if (createJobRequestList.size() > 0) {
            log.info("Found {} scheduled job request(s)", createJobRequestList.size());
        }

        MutableInt createdJobs = new MutableInt(0);
        for (CreateJobRequest createJobRequest : createJobRequestList) {
            // NB: only id and controller name are available
            String jobId = createJobRequest.getJobId();

            acceptJobRequest(jobId)
                    .ifPresent(jobRequest -> {
                        if (submitJobRequestToEngine(jobRequest)) {
                            createdJobs.increment();
                        }
                    });

            if (!ingestionEngine.canAcceptJobs()) {
                log.debug("Too busy, will not look for any more jobs");
                return;
            }
        }

        if (createdJobs.getValue() > 0) {
            log.info("Successfully created {} jobs", createdJobs);
        }

    }

    // NB: only id and controller name are available!
    private Collection<CreateJobRequest> pullJobRequests() {
        try {
            return CollectionUtils.emptyIfNull(controlClient.listJobRequests(generateMyHandle(false), reservedJobsFilter));
        } catch (IngestionAgentControlClient.ControlException e) {
            log.warn("Failed to pull job requests from control-plane", e);
            return Collections.emptyList();
        }
    }


    private Optional<CreateJobRequest> acceptJobRequest(String jobId) {
        try {
            CreateJobRequest acceptedJobRequest = controlClient.acceptJobRequest(jobId, agentId, tenantId);

            log.info("Accepted request from control-plane for job={}", jobId);

            return Optional.ofNullable(acceptedJobRequest);
        } catch (IngestionAgentControlClient.ControlException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException httpException) {
                Integer code = MoreObjects.firstNonNull(httpException.getCode(), 0);
                if (code == 404) {
                    log.info("Could not accept job request for job={} (not found or already accepted). Ignoring...", jobId);
                    return Optional.empty();
                }
            }
            log.warn("Failed to accept request for job={}", jobId, e);
        }
        return Optional.empty();
    }

    private boolean submitJobRequestToEngine(CreateJobRequest createJobRequest) {
        try {
            try {
                jobController.submitJob(createJobRequest);
                return true;
            } catch (TooManyRequestsException e) {
                log.error("Failed to submit job to engine: too busy - {}", createJobRequest);
                // TODO use proper enum or use another parameter type (current reusing job status)
                TimeUnit.SECONDS.sleep(WAIT_IN_S_WHEN_RATE_LIMITED);
                controlClient.rejectJobRequest(createJobRequest.getJobId(), agentId, "scheduled", tenantId);
            } catch (BadRequestException e) {
                log.error("Failed to submit job to engine: invalid - {}", createJobRequest);
                controlClient.rejectJobRequest(createJobRequest.getJobId(), agentId, "invalid", tenantId);
            } catch (Exception e) {
                log.error("Failed to submit job to engine: invalid (internal error) - {}", createJobRequest, e);
                controlClient.rejectJobRequest(createJobRequest.getJobId(), agentId, "invalid", tenantId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeInterruptedException(e);
        } catch (IngestionAgentControlClient.ControlException e) {
            log.warn("Failed to reject job request from control-plane! id=" + createJobRequest.getJobId(), e);
        }
        return false;
    }

    public void reportJobs() {
        try {
            AgentResponse<ListResponse<JobView>> jobsViews = jobController.getJobs(Optional.empty());
            if (CollectionUtils.isEmpty(jobsViews.getResponse().getRecords())) {
                return;
            }

            // send report and retrieve acknowledgments (jobs for the which the report successfully went through)
            Map<String, Job> jobsById = jobsViews.getResponse().getRecords().stream()
                    .map(JobView::getJob)
                    .collect(Collectors.toMap(Job::getId, Function.identity()));
            Map<String, Boolean> acknowledgments = controlClient.sendJobReport(new ArrayList<>(jobsById.values()), tenantId);

            int ackCount = 0;
            int autoClearedCount = 0;
            for (Map.Entry<String, Boolean> ack : acknowledgments.entrySet()) {
                String jobId = ack.getKey();
                boolean acknowledged = Boolean.TRUE.equals(ack.getValue());
                if (acknowledged) {
                    ackCount++;
                    log.debug("Got ack for job_id={}", jobId);

                    // if autoClearJobs flag is set, then remove jobs that are both done and acknowledged
                    // (make sure to only clear jobs that were done *before* sending the report to prevent race conditions)
                    if (autoClearJobs && jobsById.get(jobId).isDone() && ingestionEngine.clearJobIfDone(jobId)) {
                        autoClearedCount++;
                        log.debug("Cleared job_id={}", jobId);
                    }
                }
            }

            if (ackCount > 0 || autoClearedCount > 0) {
                log.info("⇄ Sync-ed {} job(s) with control-plane ({} acknowledged, {} auto-cleared)", acknowledgments.size(), ackCount, autoClearedCount);
            }

        } catch (IngestionAgentControlClient.ControlException e) {
            log.warn("Failed to report jobs to control-plane", e);
        }
    }

}

