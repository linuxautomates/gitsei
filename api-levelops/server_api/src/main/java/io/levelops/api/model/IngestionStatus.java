package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IngestionStatus.IngestionStatusBuilder.class)
public class IngestionStatus {
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("status")
    String status;
    @JsonProperty("last_ingested_activity_from")
    Long lastIngestedActivityFrom;
    @JsonProperty("last_ingested_activity_to")
    Long lastIngestedActivityTo;
}
