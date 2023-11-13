package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;


@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = QualityGateResponse.QualityGateResponseBuilder.class)
public class QualityGateResponse {

    @JsonProperty("qualitygates")
    List<QualityGate> qualitygates;

    @JsonProperty("default")
    String defaults;

    @JsonProperty("actions")
    Action actions;
}
