package io.levelops.ingestion.integrations.custom.k8s.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = K8sExecResult.K8sExecResultBuilder.class)
public class K8sExecResult implements ControllerIngestionResult {

    @JsonProperty("output")
    String output;

    @JsonProperty("error")
    String error;
}
