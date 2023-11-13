package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabQuery.GitlabQueryBuilder.class)
public class GitlabQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;
    @JsonProperty("from")
    Date from;
    @JsonProperty("to")
    Date to;
    @JsonProperty("limit")
    Integer limit;
}