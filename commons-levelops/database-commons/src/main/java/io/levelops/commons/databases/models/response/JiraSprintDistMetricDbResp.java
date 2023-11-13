package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraSprintDistMetricDbResp.JiraSprintDistMetricDbRespBuilder.class)
public class JiraSprintDistMetricDbResp {
    @JsonProperty("key") // sprint percentile value
    String key;
    @JsonProperty("percentile")
    float percentile;
    @JsonProperty("planned")
    Boolean planned;

    @JsonProperty("sprint")
    String sprint;

    @JsonProperty("delivered_story_points")
    Integer deliveredStoryPoints;

    @JsonProperty("total_time_taken")
    Integer totalTimeTaken;

}
