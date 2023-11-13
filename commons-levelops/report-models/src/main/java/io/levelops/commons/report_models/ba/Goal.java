package io.levelops.commons.report_models.ba;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Goal.GoalBuilder.class)
@AllArgsConstructor
public class Goal {
    @JsonProperty("min")
    Integer min;
    @JsonProperty("max")
    Integer max;
}