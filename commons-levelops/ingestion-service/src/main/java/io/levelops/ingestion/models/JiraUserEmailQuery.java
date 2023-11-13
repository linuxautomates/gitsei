package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonDeserialize(builder = JiraUserEmailQuery.JiraUserEmailQueryBuilder.class)
public class JiraUserEmailQuery implements DataQuery, IntegrationQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("account_ids")
    List<String> accountIds;
}
