package io.propelo.trellis_framework.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= Event.EventBuilder.class)
public class Event {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("tenant_id")
    private String tenantId; //Used only during read from DB

    @JsonProperty("tenant_config_id")
    private Long tenantConfigId;

    @JsonProperty("tenant_enabled")
    private Boolean tenantEnabled; //Used only during read from DB

    @JsonProperty("event_type")
    private final EventType eventType;

    @JsonProperty("event_sub_type")
    private final EventType eventSubType;

    @JsonProperty("frequency_in_mins")
    private final  Integer  frequencyInMins;

    @JsonProperty("stuck_task_retry_after_mins")
    private final  Integer  stuckTaskRetryAfterMins;

    @JsonProperty("status")
    private final JobStatus status;

    @JsonProperty("status_changed_at")
    private final Instant statusChangedAt;

    @JsonProperty("error")
    Map<String, Object> error; // critical error

    @JsonProperty("failed_attempts_count")
    private final Integer failedAttemptsCount;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    private final Instant updatedAt;

    @JsonProperty("data")
    private Map<String, Object> data;

}