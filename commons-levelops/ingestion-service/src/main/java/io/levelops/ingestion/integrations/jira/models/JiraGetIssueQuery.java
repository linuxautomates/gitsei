package io.levelops.ingestion.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraGetIssueQuery.JiraGetIssueQueryBuilder.class)
public class JiraGetIssueQuery implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("issue_key")
    String issueKey;

}
