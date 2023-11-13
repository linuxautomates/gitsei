package io.levelops.commons.databases.models.database.combined;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SalesforceWithJira.SalesforceWithJiraBuilder.class)
public class SalesforceWithJira {
    @JsonProperty("case_id")
    String caseId;

    @JsonProperty("jira_issues")
    List<String> jiraIssues;

    @JsonProperty("subject")
    String subject;

    @JsonProperty("contact")
    String contact;

    @JsonProperty("creator")
    String creator;

    @JsonProperty("origin")
    String origin;

    @JsonProperty("status")
    String status;

    @JsonProperty("type")
    String type;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("reason")
    String reason;

    @JsonProperty("case_created_at")
    Date caseCreatedAt;

    @JsonProperty("case_modified_at")
    Date caseModifiedAt;

    @JsonProperty("escalation_time")
    Long escalationTime;

    @JsonProperty("jira_key")
    String jiraKey;
}
