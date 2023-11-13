package io.levelops.ingestion.models.controlplane;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CreateTriggerRequest.CreateTriggerRequestBuilder.class)
public class CreateTriggerRequest {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    /**
     * Jobs marked as reserved belong to integrations that must be ingested by a dedicated agent,
     * and therefore should be not be pulled by generic agents.
     * To ingest reserved jobs, dedicated agents will have to explicitly specify which tenant and integration ids they want.
     */
    @JsonProperty("reserved")
    Boolean reserved;

    @JsonProperty("trigger_type")
    String triggerType;

    @JsonProperty("frequency")
    Integer frequency;

    @JsonProperty("metadata")
    Object metadata; // optional

    @JsonProperty("callback_url")
    String callbackUrl; // optional

}