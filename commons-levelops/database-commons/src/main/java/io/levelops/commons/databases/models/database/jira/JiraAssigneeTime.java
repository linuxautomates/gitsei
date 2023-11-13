package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraAssigneeTime.JiraAssigneeTimeBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JiraAssigneeTime {
    @JsonProperty("assignee")
    String assignee;

    @JsonProperty("key")
    String key;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("summary")
    String summary;

    @JsonProperty("total_time")
    Long totalTime;
}
