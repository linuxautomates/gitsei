package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIBuildStep.CircleCIBuildStepBuilder.class)
public class CircleCIBuildStep {

    @JsonProperty("name")
    String name;

    @JsonProperty("actions")
    List<CircleCIStepAction> actions;
}
