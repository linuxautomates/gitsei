package io.levelops.commons.tenant_management.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.GlobalTracker;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= TaskTracker.TaskTrackerBuilder.class)
public class TaskTracker {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("type")
    private final TaskType type;
    @JsonProperty("frequency")
    private final  Integer  frequency;
    @JsonProperty("status")
    private final TaskStatus status;
    @JsonProperty("status_changed_at")
    private final Instant statusChangedAt;
    @JsonProperty("created_at")
    private final  Instant  createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
