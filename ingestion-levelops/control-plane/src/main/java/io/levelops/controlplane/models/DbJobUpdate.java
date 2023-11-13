package io.levelops.controlplane.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJobUpdate.DbJobUpdateBuilder.class)
public class DbJobUpdate {
    JobStatus status;
    String agentId;
    Integer attemptCount;
    Boolean incrementAttemptCount; // only if attemptCount is not specified
    Map<String, Object> result;
    Map<String, Object> error;
    Map<String, Object> intermediateState;
    List<IngestionFailure> ingestionFailures;

    JobStatus statusCondition; // only update if the current status is equal to this
}