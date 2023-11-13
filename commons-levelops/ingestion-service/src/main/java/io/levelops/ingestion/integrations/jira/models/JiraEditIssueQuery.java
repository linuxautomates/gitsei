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
@JsonDeserialize(builder = JiraEditIssueQuery.JiraEditIssueQueryBuilder.class)
public class JiraEditIssueQuery implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty("summary")
    String summary;

    @JsonProperty("description")
    String description;

    @JsonProperty("assignee_search_string")
    String assigneeSearchString;

    @JsonProperty("labels_to_add")
    List<String> labelsToAdd;

    @JsonProperty("labels_to_remove")
    List<String> labelsToRemove;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;

    @JsonProperty("status")
    String status;

    @JsonProperty("versions")
    List<String> versions;

    @JsonProperty("fix_versions")
    List<String> fixVersions;

    @JsonProperty("due_date")
    String dueDate; // (format "2021-03-16")

    @JsonProperty("watchers_to_add")
    List<String> watchersToAdd; // format: accountId (JIRA CLOUD) or  name (JIRA SERVER)

    @JsonProperty("priority")
    String priority;
}
