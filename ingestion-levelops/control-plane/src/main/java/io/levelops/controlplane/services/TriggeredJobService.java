package io.levelops.controlplane.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.web.exceptions.NotFoundException;

import java.util.List;
import java.util.stream.Stream;

public interface TriggeredJobService {

    String createTriggeredJob(DbTrigger trigger, boolean isPartial, CreateJobRequest createJobRequest) throws NotFoundException, JsonProcessingException;

    Stream<DbTriggeredJob> retrieveLatestTriggeredJobs(String triggerId, boolean partial, List<JobStatus> statuses);

    Stream<DbTriggeredJob> retrieveLatestTriggeredJobs(String triggerId, boolean partial, List<JobStatus> statuses, Long beforeTimestamp, Long afterTimeStamp);

    Stream<DbTriggeredJob> retrieveLatestSuccessfulTriggeredJobs(String triggerId, boolean partial, boolean onlySuccessfulResults);

    Stream<DbTriggeredJob> retrieveSuccessfulTriggeredJobsBeforeIteration(String triggerId, String iterationId, boolean partial, boolean onlySuccessfulResults);

    void cleanUpTriggeredJobs(String triggerId);
}
