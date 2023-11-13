package io.levelops.aggregations_shared.database.models;

import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class DbJobInstanceUpdate {
    JobStatus jobStatus;
    String workerId;
    Boolean incrementAttemptCount;
    Instant heartbeat;
    Map<String, Integer> progress;
    Instant startTime;
    String payloadGcsFileName;

    JobStatus statusCondition;
    String workerIdCondition;
    Map<String, StageProgressDetail> progressDetails;
    JobMetadata metadata;
}
