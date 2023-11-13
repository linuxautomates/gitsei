package io.levelops.ingestion.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraIterativeScanQuery.JiraIterativeScanQueryBuilder.class)
public class JiraIterativeScanQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;

    @JsonProperty("fetch_projects")
    Boolean fetchProjects;

    @JsonProperty("fetch_sprints")
    Boolean fetchSprints;

    /**
     * Override default page size when pulling issues. (Optional)
     */
    @JsonProperty("issues_page_size")
    Integer issuesPageSize;
}