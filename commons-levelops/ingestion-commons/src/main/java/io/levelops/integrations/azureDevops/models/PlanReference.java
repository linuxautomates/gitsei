package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PlanReference.PlanReferenceBuilder.class)
public class PlanReference {

    @JsonProperty("planId")
    String planId;

    @JsonProperty("orchestrationType")
    int orchestrationType;
}
