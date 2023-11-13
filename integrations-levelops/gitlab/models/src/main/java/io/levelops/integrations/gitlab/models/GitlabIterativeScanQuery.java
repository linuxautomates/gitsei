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
@JsonDeserialize(builder = GitlabIterativeScanQuery.GitlabIterativeScanQueryBuilder.class)
public class GitlabIterativeScanQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;

    @JsonProperty("should_Fetch_groups")
    Boolean shouldFetchGroups;

    @JsonProperty("should_fetch_projects")
    Boolean shouldFetchProjects;

    @JsonProperty("should_fetch_all_users")
    Boolean shouldFetchAllUsers;
}
