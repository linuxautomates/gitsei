package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= GlobalTracker.GlobalTrackerBuilder.class)
public class GlobalTracker {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("type")
    private final String type;
    @JsonProperty("frequency")
    private final  Integer  frequency;
    @JsonProperty("status")
    private final String status;
    @JsonProperty("status_changed_at")
    private final Instant statusChangedAt;
    @JsonProperty("created_at")
    private final  Instant  createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
