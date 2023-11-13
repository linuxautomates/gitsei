package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubIterativeScanQuery.GithubIterativeScanQueryBuilder.class)
public class GithubIterativeScanQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;
    
    @JsonProperty("should_fetch_repos")
    Boolean shouldFetchRepos;

    @JsonProperty("should_fetch_all_cards")
    Boolean shouldFetchAllCards;

    @JsonProperty("should_fetch_tags")
    Boolean shouldFetchTags;

    @JsonProperty("should_fetch_users")
    Boolean shouldFetchUsers;
}