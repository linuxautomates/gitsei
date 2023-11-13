package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTrigger.DbTriggerBuilder.class)
public final class DbTrigger {

    // TODO move back to control plane

    @JsonProperty("id")
    String id;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("reserved")
    Boolean reserved;

    @JsonProperty("type")
    String type;

    @JsonProperty("frequency")
    Integer frequency; // in minutes

    @JsonProperty("metadata")
    Object metadata; // use for storing cursors, iterative scan data, etc.

    @JsonProperty("iteration_id")
    String iterationId;

    @JsonProperty("iteration_ts")
    Long iterationTs; // epoch in seconds

    @JsonProperty("callback_url")
    String callbackUrl;

    @JsonProperty("created_at")
    Long createdAt; // epoch in seconds

    @JsonIgnore
    public IntegrationKey getIntegrationKey() {
        return IntegrationKey.builder()
                .integrationId(integrationId)
                .tenantId(tenantId)
                .build();
    }

    @JsonIgnore
    public Long getElapsedInSeconds(Instant current) {
        Instant iterationInstant = DateUtils.fromEpochSecond(iterationTs);
        if (iterationInstant == null) {
            return null;
        }
        return Duration.between(iterationInstant, current).toSeconds();
    }

    @JsonIgnore
    public boolean isDisabled() {
        return frequency == null || frequency <= 0;
    }

    @JsonIgnore
    public boolean isSchedulable(Instant current) {
        if (frequency == null || frequency <= 0) {
            return false;
        }
        Long elapsedInSeconds = getElapsedInSeconds(current);
        if (elapsedInSeconds == null) {
            return true;
        }
        return elapsedInSeconds >= TimeUnit.MINUTES.toSeconds(frequency);
    }

}
