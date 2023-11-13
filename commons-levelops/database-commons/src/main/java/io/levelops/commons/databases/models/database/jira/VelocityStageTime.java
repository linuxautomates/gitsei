package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.response.VelocityStageResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityStageTime.VelocityStageTimeBuilder.class)
public class VelocityStageTime {

    @JsonProperty("stage")
    String stage;

    @JsonProperty("time_spent")
    Long timeSpent;

    @JsonProperty("velocity_stage_result")
    VelocityStageResult velocityStageResult;

}
