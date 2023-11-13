package io.levelops.integrations.k8s.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = K8sExecOutput.K8sExecOutputBuilder.class)
public class K8sExecOutput {

    @JsonProperty("output")
    String output;

    @JsonProperty("error")
    String error;

}
