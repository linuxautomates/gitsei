package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraAssignee.DbJiraAssigneeBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbJiraAssignee {
    //unique key for this table will be: assignee,key,start_time
    @JsonProperty("assignee")
    String assignee;

    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("start_time")
    Long startTime;

    @JsonProperty("end_time")
    Long endTime;

    @JsonProperty("created_at")
    Long createdAt;
}
