package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubQuery.GithubQueryBuilder.class)
public class GithubQuery implements DataQuery  {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("repo_name")
    String repoName;

    @JsonProperty("repo_owner")
    String repoOwner;

    @JsonProperty("pr_number")
    String prNumber;
}
