package io.levelops.integrations.github.actions.models;

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
@JsonDeserialize(builder = GithubActionsIngestionQuery.GithubActionsIngestionQueryBuilder.class)
public class GithubActionsIngestionQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;

    @JsonProperty("onboarding")
    Boolean onboarding;

    @JsonProperty("repos")
    List<String> repos;

    @JsonProperty("github_app")
    Boolean githubApp;
}