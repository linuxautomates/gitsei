package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraSprintDistMetric.JiraSprintDistMetricBuilder.class)
public class JiraSprintDistMetric {
    @JsonProperty("key") // sprint percentile value
    String key;
    @JsonProperty("additional_key") // sprint name
    String additionalKey;

    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("sprint")
    String sprint;

    @JsonProperty("delivered_story_points")
    Integer deliveredStoryPoints;
    @JsonProperty("delivered_keys")
    Set<String> deliveredKeys;

    @JsonProperty("total_keys")
    Integer totalKeys;
    @JsonProperty("total_time_taken")
    Integer totalTimeTaken;
    @JsonProperty("planned")
    Integer planned;
    @JsonProperty("unplanned")
    Integer unplanned;

}
