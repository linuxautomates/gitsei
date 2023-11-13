package io.levelops.aggregations_shared.helpers.harnessng;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.StepArtifacts;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGGenericCiStageOutcome.HarnessNGGenericCiStageOutcomeBuilder.class)
public class HarnessNGGenericCiStageOutcome {
    @JsonProperty("stepArtifacts")
    StepArtifacts stepArtifacts;

}
