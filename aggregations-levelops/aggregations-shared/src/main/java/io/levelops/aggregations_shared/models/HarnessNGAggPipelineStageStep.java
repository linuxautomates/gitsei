package io.levelops.aggregations_shared.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGAggPipelineStageStep.HarnessNGAggPipelineStageStepBuilder.class)
public class HarnessNGAggPipelineStageStep {
    @JsonProperty("stageInfo")
    HarnessNGPipelineStageStep stageInfo;

    @JsonProperty("steps")
    List<HarnessNGPipelineStageStep> steps;
}
