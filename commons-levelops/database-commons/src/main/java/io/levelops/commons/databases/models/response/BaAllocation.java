package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BaAllocation.BaAllocationBuilder.class)
public class BaAllocation {

    @JsonProperty("alignment_score")
    Integer alignmentScore; // score based on category goals
    @JsonProperty("percentage_score")
    Float percentageScore; // score based on category goals
    @JsonProperty("allocation")
    Float allocation; // percentage (effort / totalEffort)
    @JsonProperty("effort")
    Integer effort;
    @JsonProperty("total_effort")
    Integer totalEffort;

}
