package io.levelops.ingestion.integrations.splunk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SplunkQuery.SplunkQueryBuilder.class)
public class SplunkQuery implements DataQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("query")
    String query;

    @JsonProperty("limit")
    Integer limit;
}
