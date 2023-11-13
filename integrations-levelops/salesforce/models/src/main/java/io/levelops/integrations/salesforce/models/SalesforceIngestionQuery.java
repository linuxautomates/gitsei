package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SalesforceIngestionQuery.SalesforceIngestionQueryBuilder.class)
public class SalesforceIngestionQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty
    Long from;

    @JsonProperty
    Long to;

    @JsonProperty
    Boolean partial;
}
