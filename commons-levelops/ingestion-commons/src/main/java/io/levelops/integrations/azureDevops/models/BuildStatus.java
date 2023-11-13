package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildStatus.BuildStatusBuilder.class)
public class BuildStatus {

    @JsonProperty("all")
    String all;

    @JsonProperty("cancelling")
    String cancelling;

    @JsonProperty("completed")
    String completed;

    @JsonProperty("inProgress")
    String inProgress;

    @JsonProperty("none")
    String none;

    @JsonProperty("notStarted")
    String notStarted;

    @JsonProperty("postponed")
    String postponed;
}
