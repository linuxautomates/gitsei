package io.levelops.controlplane.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.controlplane.database.TriggeredJobDatabaseService;
import io.levelops.controlplane.database.TriggeredJobDatabaseService.TriggeredJobFilter;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class DefaultTriggeredJobService implements TriggeredJobService {

    private static final int NB_OF_ONBOARDING_JOBS_TO_INCLUDE = 2; // if 1, return jobs up to the latest onboarding, if 2, return jobs up to the second to onboarding, etc.
    private final JobTrackingService jobTrackingService;
    private final TriggeredJobDatabaseService triggeredJobDatabaseService;

    @Autowired
    public DefaultTriggeredJobService(JobTrackingService jobTrackingService,
                                      TriggeredJobDatabaseService triggeredJobDatabaseService) {
        this.jobTrackingService = jobTrackingService;
        this.triggeredJobDatabaseService = triggeredJobDatabaseService;
    }

    public String createTriggeredJob(DbTrigger trigger, boolean isPartial, CreateJobRequest createJobRequest) throws NotFoundException, JsonProcessingException {
        String jobId = jobTrackingService.createJob(createJobRequest.toBuilder()
                .tenantId(trigger.getTenantId())
                .integrationId(trigger.getIntegrationId())
                .reserved(trigger.getReserved())
                .build(), true);
        boolean success = triggeredJobDatabaseService.createTriggeredJob(jobId, trigger.getId(), trigger.getIterationId(), trigger.getIterationTs(), isPartial);
        log.debug("Creating triggered job for jobId={}: success={}", jobId, success);
        return jobId;
    }

    @Override
    public Stream<DbTriggeredJob> retrieveLatestTriggeredJobs(String triggerId, boolean partial, List<JobStatus> statuses, Long beforeTimestamp, Long afterTimeStamp) {
        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .statuses(statuses)
                .partial(partial)
                .afterExclusive(afterTimeStamp)
                .beforeInclusive(beforeTimestamp)
                .build();
        return triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null);
    }

    @Override
    public Stream<DbTriggeredJob> retrieveLatestTriggeredJobs(String triggerId, boolean partial, List<JobStatus> statuses) {
        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .statuses(statuses)
                .partial(partial)
                .build();
        return triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null);
    }

    public Stream<DbTriggeredJob> retrieveLatestSuccessfulTriggeredJobs(String triggerId, boolean partial, boolean onlySuccessfulResults) {

        var filterBuilder = TriggeredJobFilter.builder();
        if (onlySuccessfulResults) {
            filterBuilder.statuses(List.of(JobStatus.SUCCESS));
        }

        if (partial) {
            // latest successful job (partial or not)
            TriggeredJobFilter filter = filterBuilder.build();
            return triggeredJobDatabaseService.getTriggeredJob(triggerId, filter).stream();
        }

        // find last successful 'full' jobs
        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(false)
                .build();
        List<DbTriggeredJob> latestFullTriggeredJobs = triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, NB_OF_ONBOARDING_JOBS_TO_INCLUDE)
                .collect(Collectors.toList());
        if (latestFullTriggeredJobs.isEmpty()) {
            // If there have been no successful partial=false jobs then we should just get everything
            // from the last partial=false job
            filter = TriggeredJobFilter.builder()
                    .partial(false)
                    .build();
            latestFullTriggeredJobs = triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, NB_OF_ONBOARDING_JOBS_TO_INCLUDE)
                    .collect(Collectors.toList());
        }
        Optional<DbTriggeredJob> latestFullTriggeredJob = latestFullTriggeredJobs.stream()
                .min(Comparator.comparingLong(DbTriggeredJob::getIterationTs));

        // find all partial jobs that ran since the last full job
        Long lastFullIterationTs = latestFullTriggeredJob.map(DbTriggeredJob::getIterationTs).orElse(null);

        var partialFilterBuilder = TriggeredJobFilter.builder();
        if (onlySuccessfulResults) {
            partialFilterBuilder.statuses(List.of(JobStatus.SUCCESS));
        }

        filter = partialFilterBuilder
                .afterInclusive(lastFullIterationTs)
                .build();
        return triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null);
    }

    public Stream<DbTriggeredJob> retrieveSuccessfulTriggeredJobsBeforeIteration(String triggerId, String iterationId, boolean partial, boolean onlySuccessfulResults) {

        List<DbTriggeredJob> triggeredJobs = triggeredJobDatabaseService.getTriggeredJobsByIterationId(iterationId);
        // since we are expecting iterationId to be an old iteration; we return early if empty
        if (partial || triggeredJobs.isEmpty()) {
            return triggeredJobs.stream();
        }
        Long iterationTs = triggeredJobs.get(0).getIterationTs();

        // find last 'full' job before iteration
        TriggeredJobFilter filter = TriggeredJobFilter.builder()
                .partial(false)
                .beforeInclusive(iterationTs)
                .build();

        if (onlySuccessfulResults) {
            filter.toBuilder()
                    .statuses(List.of(JobStatus.SUCCESS))
                    .build();
        }

        Optional<DbTriggeredJob> fullJob = triggeredJobDatabaseService.getTriggeredJob(triggerId, filter);
        if (fullJob.isEmpty()) {
            return Stream.empty();
        }

        // find all partial jobs that ran between that full job and the given iteration
        Long lastFullIterationTs = fullJob.get().getIterationTs();
        filter = TriggeredJobFilter.builder()
                .statuses(List.of(JobStatus.SUCCESS))
                .partial(true)
                .afterExclusive(lastFullIterationTs)
                .beforeInclusive(iterationTs)
                .build();
        Stream<DbTriggeredJob> partialJobs = triggeredJobDatabaseService.streamTriggeredJobs(triggerId, filter, null);

        return Stream.concat(partialJobs, fullJob.stream());
    }

    public void cleanUpTriggeredJobs(String triggerId) {
        // TODO add canceled job flow
//        triggeredJobDatabaseService.streamTriggeredJobsByTriggerId(triggerId, PAGE_SIZE)
//                .forEach(triggeredJob -> {
//                    jobTrackingService.updateJobStatus(triggeredJob.getJobId(), JobStatus.ABORTED);
//                });
        triggeredJobDatabaseService.deleteTriggeredJobs(triggerId);
    }
}
