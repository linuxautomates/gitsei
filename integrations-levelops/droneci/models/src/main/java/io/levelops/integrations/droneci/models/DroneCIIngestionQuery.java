package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DroneCIIngestionQuery.DroneCIIngestionQueryBuilder.class)
public class DroneCIIngestionQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;

    @JsonProperty("repos")
    List<String> repositories;

    @JsonProperty("exclude_repos")
    List<String> excludeRepositories;

    @JsonProperty("fetch_steplogs")
    Boolean shouldFetchStepLogs;
}
