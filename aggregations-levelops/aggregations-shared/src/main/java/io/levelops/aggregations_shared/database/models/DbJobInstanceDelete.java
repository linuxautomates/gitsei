package io.levelops.aggregations_shared.database.models;

import io.levelops.commons.etl.models.JobInstanceId;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class DbJobInstanceDelete {
    List<JobInstanceId> jobInstanceIds;
    Instant createdAtBefore;
}
