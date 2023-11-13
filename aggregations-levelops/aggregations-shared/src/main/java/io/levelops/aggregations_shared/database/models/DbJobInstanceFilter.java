package io.levelops.aggregations_shared.database.models;

import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class DbJobInstanceFilter {
    // Either jobIds or jobInstanceIds should be defined, but not both.
    // jobInstanceIds take precedence if both are defined
    @Nullable
    List<UUID> jobDefinitionIds;
    @Nullable
    List<JobInstanceId> jobInstanceIds;
    @Nullable
    List<JobStatus> jobStatuses;
    @Nullable
    List<String> tags;
    @Nullable
    Instant scheduledTimeAtOrBefore;
    @Nullable
    Instant lastHeartbeatBefore;
    @Nullable
    Instant lastStatusChangeBefore;
    @Nullable
    Boolean isFull;
    @Nullable
    Boolean belowMaxAttempts;
    @Nullable
    Boolean timedOut;
    @Nullable
    List<JobInstanceOrderByField> orderBy;
    @Nullable
    Boolean excludePayload;

    @AllArgsConstructor
    public enum JobInstanceOrderBy {
        CREATED_AT("created_at"),
        PRIORITY("priority"),
        SCHEDULED_START_TIME("scheduled_start_time"),
        INSTANCE_ID("instance_id");
        private final String fieldName;
    }

    @Value
    @Builder
    public static class JobInstanceOrderByField {
        @NonNull
        JobInstanceOrderBy orderByColumn;
        @NonNull
        Boolean isAscending;

        public String toSql() {
            return orderByColumn.fieldName + " " + (isAscending ? "ASC" : "DESC");
        }
    }
}
