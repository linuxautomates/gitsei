package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraStatusTime.JiraStatusTimeBuilder.class)
public class JiraStatusTime {

    @JsonProperty("status")
    String status;

    @JsonProperty("key")
    String key;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("summary")
    String summary;

    @JsonProperty("total_time")
    Long totalTime;
}
