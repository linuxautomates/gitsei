package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraIssueSprintMappingAggResult.JiraIssueSprintMappingAggResultBuilder.class)
public class JiraIssueSprintMappingAggResult {
    @JsonProperty("sprint_mapping")
    DbJiraIssueSprintMapping sprintMapping;
    @JsonProperty("issue_type")
    String issueType;
}
