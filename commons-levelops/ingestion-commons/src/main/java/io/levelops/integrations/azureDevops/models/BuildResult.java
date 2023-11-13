package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildResult.BuildResultBuilder.class)
public class BuildResult {

    @JsonProperty("canceled")
    String canceled;

    @JsonProperty("failed")
    String failed;

    @JsonProperty("none")
    String none;

    @JsonProperty("partiallySucceeded")
    String partiallySucceeded;

    @JsonProperty("succeeded")
    String succeeded;
}
