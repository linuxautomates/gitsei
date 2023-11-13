package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Status.StatusBuilder.class)
public class Status {

    @JsonProperty("qualityGateStatus")
    String qualityGateStatus;

    @JsonProperty("bugs")
    long bugs;

    @JsonProperty("vulnerabilities")
    long vulnerabilities;

    @JsonProperty("codeSmells")
    long codeSmells;
}