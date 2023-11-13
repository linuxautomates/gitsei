package io.levelops.integrations.confluence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfluenceSearchQuery.ConfluenceSearchQueryBuilder.class)
public class ConfluenceSearchQuery implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("keywords")
    List<String> keywords;

    @JsonProperty("since_days")
    Integer sinceDays;

    @JsonProperty("limit")
    Integer limit;

    @JsonProperty("skip")
    Integer skip;
}