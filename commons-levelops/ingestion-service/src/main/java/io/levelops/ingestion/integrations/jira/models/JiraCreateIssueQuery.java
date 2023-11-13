package io.levelops.ingestion.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraCreateIssueQuery.JiraCreateIssueQueryBuilder.class)
public class JiraCreateIssueQuery implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("summary")
    String summary; // REQUIRED

    @JsonProperty("description")
    String description;

    @JsonProperty("project_key")
    String projectKey; // REQUIRED

    @JsonProperty("issue_type_name")
    String issueTypeName; // REQUIRED

    @JsonProperty("assignee_search_string")
    String assigneeSearchString;

    @JsonProperty("labels")
    List<String> labels;

    @JsonProperty("component_names")
    List<String> componentNames;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;

    @JsonProperty("versions")
    List<String> versions;

    @JsonProperty("fix_versions")
    List<String> fixVersions;

    @JsonProperty("priority")
    String priority;
}