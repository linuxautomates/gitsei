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
@JsonDeserialize(builder = QualityGate.QualityGateBuilder.class)
public class QualityGate {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("isDefault")
    Boolean isDefault;

    @JsonProperty("isBuiltIn")
    Boolean isBuiltIn;

    @JsonProperty("actions")
    Action actions;

    @JsonProperty("status")
    String status;

    @JsonProperty("stillFailing")
    Boolean stillFailing;

    @JsonProperty("failing")
    List<Fail> failing;


}