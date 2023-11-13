package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyIterativeScanQuery.PagerDutyIterativeScanQueryBuilder.class)
public class PagerDutyIterativeScanQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    private IntegrationKey integrationKey;
    @JsonProperty("from")
    private Long from;
    @JsonProperty("to")
    private Long to;
}