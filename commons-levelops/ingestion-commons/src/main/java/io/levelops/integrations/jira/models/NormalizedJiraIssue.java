package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = NormalizedJiraIssue.NormalizedJiraIssueBuilder.class)
public class NormalizedJiraIssue {

    @JsonProperty("key")
    String key;
    @JsonProperty("project_key")
    String projectKey;
    @JsonProperty("type")
    String type;
    @JsonProperty("status")
    String status;
    @JsonProperty("priority")
    String priority;
    @JsonProperty("assignee")
    String assignee;
    @JsonProperty("assignee_email")
    String assigneeEmail;
    @JsonProperty("reporter")
    String reporter;
    @JsonProperty("reporter_email")
    String reporterEmail;
    @JsonProperty("title")
    String title;
    @JsonProperty("components")
    List<String> components;
    @JsonProperty("labels")
    List<String> labels;
    @JsonProperty("fix_versions")
    List<String> fixVersions;
    @JsonProperty("due_at")
    Date dueAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("custom_fields")
    Map<String, Object> customFields;
}
