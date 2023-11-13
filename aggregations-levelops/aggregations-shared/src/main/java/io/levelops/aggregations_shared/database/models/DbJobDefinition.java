package io.levelops.aggregations_shared.database.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.BooleanUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJobDefinition.DbJobDefinitionBuilder.class)
public class DbJobDefinition {
    @JsonProperty("id")
    UUID id;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("integration_type")
    String integrationType;

    @JsonProperty("ingestion_trigger_id")
    String ingestionTriggerId;

    @JsonProperty("is_active")
    Boolean isActive;

    @JsonProperty("default_priority")
    JobPriority defaultPriority;

    @JsonProperty("attempt_max")
    Integer attemptMax;

    @JsonProperty("retry_wait_time_minutes")
    Integer retryWaitTimeInMinutes;

    @JsonProperty("timeout_in_minutes")
    Long timeoutInMinutes;

    @JsonProperty("frequency_in_minutes")
    Integer frequencyInMinutes;

    @JsonProperty("full_frequency_in_minutes")
    Integer fullFrequencyInMinutes;

    @JsonProperty("agg_processor_name")
    String aggProcessorName;

    @JsonProperty("last_iteration_ts")
    Instant lastIterationTs;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @NonNull
    @JsonProperty("jobType")
    JobType jobType;

    public String getIntegrationId() {
        if (integrationId != null) {
            return integrationId;
        }
        return "GENERIC";
    }

    public String getIntegrationType() {
        if (integrationType != null) {
            return integrationType;
        }
        return "GENERIC";
    }

    @JsonIgnore
    public Boolean isDisabled() {
        return BooleanUtils.isFalse(getIsActive());
    }

    @JsonIgnore
    public boolean isSchedulable(Instant now) {
        if (isDisabled() || frequencyInMinutes == null || frequencyInMinutes <= 0) {
            return false;
        }
        if (lastIterationTs == null) {
            return true;
        }
        var elapsedDuration= Duration.between(lastIterationTs, now);
        return elapsedDuration.toMinutes() > frequencyInMinutes;
    }
}
