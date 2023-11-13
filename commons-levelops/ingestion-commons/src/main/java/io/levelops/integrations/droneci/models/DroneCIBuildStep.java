package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DroneCIBuildStep.DroneCIBuildStepBuilder.class)
public class DroneCIBuildStep {
    @JsonProperty("id")
    Long id;

    @JsonProperty("step_id")
    Long stepId;

    @JsonProperty("number")
    Long number;

    @JsonProperty("name")
    String name;

    @JsonProperty("status")
    String status;

    @JsonProperty("exit_code")
    Long exitCode;

    @JsonProperty("started")
    Long started;

    @JsonProperty("stopped")
    Long stopped;
    
    @JsonProperty("version")
    Long version;

    @JsonProperty("image")
    String image;

    @JsonProperty("depends_on")
    List<String> dependsOn;

    @JsonProperty
    List<DroneCIBuildStepLog> stepLogs;
}
