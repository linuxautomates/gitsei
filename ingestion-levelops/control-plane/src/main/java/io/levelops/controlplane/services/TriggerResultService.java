package io.levelops.controlplane.services;

import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.controlplane.database.JobDatabaseService;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService;
import io.levelops.controlplane.models.DbJob;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.controlplane.models.JobDTOConverters;
import io.levelops.events.clients.EventsClient;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class TriggerResultService {

    private final TriggeredJobDatabaseService triggeredJobDatabaseService;
    private final TriggeredJobService triggeredJobService;
    private final TriggerDatabaseService triggerDatabaseService;
    private final JobDatabaseService jobDatabaseService;
    private final TriggerCallbackService triggerCallbackService;
    private final EventsClient eventsClient;
    private final InventoryService inventoryService;
    private final boolean lightTriggerCallback;

    @Autowired
    public TriggerResultService(TriggeredJobDatabaseService triggeredJobDatabaseService,
                                TriggeredJobService triggeredJobService, TriggerDatabaseService triggerDatabaseService,
                                JobDatabaseService jobDatabaseService, TriggerCallbackService triggerCallbackService,
                                EventsClient eventsClient, InventoryService inventoryService,
                                @Value("${LIGHT_TRIGGER_CALLBACK:true}") boolean lightTriggerCallback) {

        this.triggeredJobDatabaseService = triggeredJobDatabaseService;
        this.triggeredJobService = triggeredJobService;
        this.triggerDatabaseService = triggerDatabaseService;
        this.jobDatabaseService = jobDatabaseService;
        this.triggerCallbackService = triggerCallbackService;
        this.eventsClient = eventsClient;
        this.inventoryService = inventoryService;
        this.lightTriggerCallback = lightTriggerCallback;
    }

    /**
     * Reports an eventual update to a trigger's results; if the results where
     * actually changed, sends a callback. This will check if the job actually
     * belongs to a Trigger (i.e. if it's a triggeredJob or not) so this can be
     * called for any job.
     */
    public boolean reportTriggerResults(String jobId, JobStatus status) {
        if (status != JobStatus.SUCCESS) {
            // only consider successful jobs
            // TODO consider any 'done' job?
            return false;
        }

        Optional<DbTriggeredJob> triggeredJob = triggeredJobDatabaseService.getTriggeredJobByJobId(jobId);
        if (triggeredJob.isEmpty()) {
            // not a triggered job
            return false;
        }

        String triggerId = triggeredJob.get().getTriggerId();
        Optional<DbTrigger> triggerOpt = triggerDatabaseService.getTriggerById(triggerId);
        if (triggerOpt.isEmpty()) {
            log.warn("[DATA CONSISTENCY] TriggeredJob without Trigger");
            return false;
        }

        DbTrigger trigger = triggerOpt.get();
        var callbackStatus = triggerCallbackForLatestResults(trigger);
        sendEvent(trigger);
        return callbackStatus;
    }

    public boolean triggerCallbackForLatestResults(DbTrigger trigger) {
        if (Strings.isEmpty(trigger.getCallbackUrl())) {
            // no callback setup for this trigger
            return false;
        }

        TriggerResults results;
        if (lightTriggerCallback) {
            results = TriggerResults.builder()
                    .triggerId(trigger.getId())
                    .triggerType(trigger.getType())
                    .tenantId(trigger.getTenantId())
                    .integrationId(trigger.getIntegrationId())
                    .iterationId(trigger.getIterationId())
                    .build();
        } else {
            // For the callback, we want to return the whole data set but also optimize the
            // results:
            // - partial = false :
            // Returns all the results since last successful full scan (full scan ==
            // non-partial scan)
            //
            // - allowEmptyResults = false :
            // Omit results that are explicitly marked as empty (e.g. iterative scan found
            // nothing)
            //
            // - onlySuccessfulResults = true :
            // Only include successful results (i.e. filter out jobs that are pending,
            // scheduled, etc.)
            results = retrieveLatestTriggerResults(trigger, false, false, true, true);
            if (CollectionUtils.isEmpty(results.getJobs())) {
                log.info("Skipping empty trigger callback for trigger_id={}", trigger.getId());
                return true;
            }
        }
        triggerCallbackService.callback(trigger, results);
        return true;
    }

    public void sendEvent(DbTrigger trigger) {
        var results = retrieveLatestTriggerResults(trigger, true, false, true, true);

        if (Strings.isBlank(trigger.getTenantId()) || Strings.isBlank(trigger.getIntegrationId())) {
            log.warn("no integration or customer associated with the trigger. Skipping event for the trigger: {}",
                    trigger);
            return;
        }
        Integration integration;
        try {
            integration = inventoryService.getIntegration(trigger.getTenantId(), trigger.getIntegrationId());
        } catch (InventoryException e1) {
            log.error("Unable to retrieve the integration: id={}, customer={}", trigger.getIntegrationId(), trigger.getTenantId(), e1);
            return;
        }
        EventType eventType = EventType.fromString(integration.getApplication() + "_NEW_INGESTION");
        if (eventType == null || eventType == EventType.UNKNOWN) {
            log.warn("No known event type detected: Skipping event for the trigger id={}, application={}", trigger.getId(), integration.getApplication());
            return;
        }
        results.getJobs().forEach(job -> {
            try {
                eventsClient.emitEvent(trigger.getTenantId(), eventType,
                        Map.of(
                                "trigger_id", trigger.getId(),
                                "iteration_id", trigger.getIterationId(),
                                "integration_id", trigger.getIntegrationId(),
                                "job_id", job.getId(),
                                "partial", results.getPartial(),
                                "query", job.getQuery(),
                                "status", job.getStatus()
                        ));
            } catch (Exception e) {
                log.error("Error emitting new ingestion event... integration={}, job={}, company={}", job.getIntegrationId(), job.getId(), job.getTenantId(), e);
            }
        });
    }

    public boolean triggerCallback(DbTrigger trigger, String iterationId) {
        if (Strings.isEmpty(trigger.getCallbackUrl())) {
            // no callback setup for this trigger
            return false;
        }

        TriggerResults results;
        if (lightTriggerCallback) {
            results = TriggerResults.builder()
                    .triggerId(trigger.getId())
                    .triggerType(trigger.getType())
                    .integrationId(trigger.getIntegrationId())
                    .iterationId(iterationId)
                    .tenantId(trigger.getTenantId())
                    .jobs(List.of())
                    .build();
        } else {
            results = retrieveTriggerResults(trigger, iterationId, false, false, true, true);
        }
        triggerCallbackService.callback(trigger, results);
        return true;
    }


    /**
     * Retrieve the 'latest' results of a Trigger.
     * The results might cover one or more iterations depending on what data is requested (partial allowed, or full data set).
     *
     * @param trigger trigger
     * @param partial Boolean
     *                - If partial is True (i.e partial results are allowed),
     *                then only look for the results of latest successful iteration.
     *                <p>
     *                - If partial is False (i.e we want all the data since the last full scan),
     *                then retrieve the results of every iteration since the last successful full (non-partial) iteration.
     */
    public TriggerResults retrieveLatestTriggerResults(DbTrigger trigger, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, boolean includeJobResultField) {
        Stream<DbTriggeredJob> triggeredJobs = triggeredJobService.retrieveLatestSuccessfulTriggeredJobs(trigger.getId(), Boolean.TRUE.equals(partial), onlySuccessfulResults);
        return gatherResultsFromTriggeredJobs(trigger, partial, allowEmptyResults, onlySuccessfulResults, triggeredJobs, includeJobResultField);
    }

    public TriggerResults retrieveLatestTriggerResults(int page, int pageSize, DbTrigger trigger, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, boolean includeJobResultField) {
        long skip = (long) page * pageSize;
        MutableLong count = new MutableLong(0);
        Stream<DbTriggeredJob> triggeredJobs = triggeredJobService.retrieveLatestSuccessfulTriggeredJobs(trigger.getId(), Boolean.TRUE.equals(partial), onlySuccessfulResults)
                .skip(skip)
                .limit(pageSize + 1)
                .filter(x -> count.getAndIncrement() < pageSize);
        TriggerResults triggerResults = gatherResultsFromTriggeredJobs(trigger, partial, allowEmptyResults, onlySuccessfulResults, triggeredJobs, includeJobResultField);
        boolean hasNext = (count.longValue() > pageSize);
        return triggerResults.toBuilder()
                .hasNext(hasNext)
                .build();
    }

    /**
     * Retrieves the last N results for the trigger.
     *
     * @param trigger               the trigger to retrieve results for.
     * @param lastNJobs             the number of jobs to retrieve.
     * @param partial               whether the jobs can be partial or full
     * @param allowEmptyResults     allow jobs that have empty results
     * @param onlySuccessfulResults only use jobs with successfull status.
     */
    public TriggerResults retrieveTriggerResults(DbTrigger trigger, Integer lastNJobs, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, Long before, Long after, boolean includeJobResultField) {
        Stream<DbTriggeredJob> triggeredJobs = triggeredJobService.retrieveLatestTriggeredJobs(trigger.getId(), partial, List.of(JobStatus.SUCCESS), before, after);
        return gatherResultsFromTriggeredJobs(trigger, partial, allowEmptyResults, onlySuccessfulResults, triggeredJobs, lastNJobs, includeJobResultField);
    }

    public TriggerResults retrieveTriggerResults(DbTrigger trigger, String iterationId, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, boolean includeJobResultField) {
        Stream<DbTriggeredJob> triggeredJobs = triggeredJobService.retrieveSuccessfulTriggeredJobsBeforeIteration(trigger.getId(), iterationId, Boolean.TRUE.equals(partial), onlySuccessfulResults);
        return gatherResultsFromTriggeredJobs(trigger, partial, allowEmptyResults, onlySuccessfulResults, triggeredJobs, includeJobResultField);
    }

    private TriggerResults gatherResultsFromTriggeredJobs(DbTrigger trigger, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, Stream<DbTriggeredJob> triggeredJobs, boolean includeJobResultField) {
        return gatherResultsFromTriggeredJobs(trigger, partial, allowEmptyResults, onlySuccessfulResults, triggeredJobs, null, includeJobResultField);
    }

    private TriggerResults gatherResultsFromTriggeredJobs(DbTrigger trigger, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, Stream<DbTriggeredJob> triggeredJobs, Integer lastNJobs, boolean includeJobResultField) {
        List<JobDTO> results = retrieveJobResults(triggeredJobs, allowEmptyResults, onlySuccessfulResults, lastNJobs, includeJobResultField);
        return TriggerResults.builder()
                .triggerId(trigger.getId())
                .triggerType(trigger.getType())
                .integrationId(trigger.getIntegrationId())
                .iterationId(IterableUtils.getFirst(results).map(JobDTO::getIterationId).orElse(null)) // assumes results are sorted in descending order
                .tenantId(trigger.getTenantId())
                .partial(partial)
                .jobs(results)
                .build();
    }

    /**
     * Retrieve the Trigger results for a gi
     *
     * @param trigger
     * @param iterationId
     * @return
     */
    public TriggerResults retrieveTriggerResultsForIteration(DbTrigger trigger, String iterationId, boolean includeJobResultField) {
        List<JobDTO> results = getJobResultsForIteration(iterationId, includeJobResultField);
        return TriggerResults.builder()
                .triggerId(trigger.getId())
                .triggerType(trigger.getType())
                .integrationId(trigger.getIntegrationId())
                .iterationId(iterationId)
                .tenantId(trigger.getTenantId())
                .partial(results.stream()
                        .map(JobDTO::getPartial)
                        .allMatch(Boolean.TRUE::equals)) // the iteration is partial if all jobs are partial
                .jobs(results)
                .build();
    }

    private List<JobDTO> getJobResultsForIteration(String iterationId, boolean includeJobResultField) {
        Stream<DbTriggeredJob> triggeredJobStream = triggeredJobDatabaseService.getTriggeredJobsByIterationId(iterationId)
                .stream();
        // when examining the results of a specific iteration, we want to return everything
        return retrieveJobResults(triggeredJobStream, true, false, includeJobResultField);
    }

    /**
     * Given a stream of triggered jobs, retrieve the actual Jobs results and create DTOs (combining Job model + triggered job metadata).
     *
     * @param triggeredJobStream    stream of triggered jobs
     * @param allowEmptyResults     if false, jobs with explicit empty result (see {@link EmptyIngestionResult}) will be omitted
     * @param onlySuccessfulResults if true, only successful jobs will be included
     * @return list of Job DTOs
     */
    private List<JobDTO> retrieveJobResults(Stream<DbTriggeredJob> triggeredJobStream, boolean allowEmptyResults, boolean onlySuccessfulResults, boolean includeJobResultField) {
        return retrieveJobResults(triggeredJobStream, allowEmptyResults, onlySuccessfulResults, null, includeJobResultField);
    }

    /**
     * Given a stream of triggered jobs, retrieve the actual Jobs results and create DTOs (combining Job model + triggered job metadata).
     *
     * @param triggeredJobStream    stream of triggered jobs
     * @param allowEmptyResults     if false, jobs with explicit empty result (see {@link EmptyIngestionResult}) will be omitted
     * @param onlySuccessfulResults if true, only successful jobs will be included
     * @param lastNJobs             limit the results to only N jobs. the list of triggered jobs should come already sorted as the N elements will be the first N.
     * @return list of Job DTOs
     */
    private List<JobDTO> retrieveJobResults(Stream<DbTriggeredJob> triggeredJobStream, boolean allowEmptyResults, boolean onlySuccessfulResults, Integer lastNJobs, boolean includeJobResultField) {
        var stream = triggeredJobStream
                .map(triggeredJob -> {
                    DbJob dbJob = jobDatabaseService.getJobMetadataById(triggeredJob.getJobId(), true, includeJobResultField).orElse(null);
                    if (dbJob == null) {
                        return null;
                    }
                    if (onlySuccessfulResults && !JobStatus.SUCCESS.equals(dbJob.getStatus())) {
                        return null;
                    }
                    if (!allowEmptyResults) {
                        // filter out explicitly empty results (i.e. keep failures and not complete jobs)
                        if (dbJob.getResult() != null && EmptyIngestionResult.isEmpty(dbJob.getResult())) {
                            return null;
                        }
                    }
                    return JobDTOConverters.convertFromDbJobAndTriggeredJob(dbJob, triggeredJob);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(JobDTO::getIterationTs).reversed());
        if (lastNJobs != null && lastNJobs > 0) {
            stream = stream.limit(lastNJobs);
        }
        return stream.collect(Collectors.toList());
    }

}
