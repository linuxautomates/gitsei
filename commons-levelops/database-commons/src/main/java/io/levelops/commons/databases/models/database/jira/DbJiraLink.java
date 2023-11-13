package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraSalesforceCase.DbJiraSalesforceCaseBuilder.class)
public class DbJiraLink {
    @JsonProperty("integration_id")
    Integer integrationId;

    @JsonProperty("from_issue_key")
    String fromIssueKey;

    @JsonProperty("to_issue_key")
    String toIssueKey;

    @JsonProperty("relation")
    String relation;

    @JsonProperty("to_project")
    String toProject;
}
