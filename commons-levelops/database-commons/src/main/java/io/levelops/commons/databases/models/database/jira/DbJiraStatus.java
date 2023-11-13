package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraStatus.DbJiraStatusBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbJiraStatus {
    @JsonProperty("status")
    String status;

    @JsonProperty("status_id")
    String statusId;

    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("end_time")
    Long endTime;

    @JsonProperty("start_time")
    Long startTime;

    @JsonProperty("created_at")
    Long createdAt;
}
