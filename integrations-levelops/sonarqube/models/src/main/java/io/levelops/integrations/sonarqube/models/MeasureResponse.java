package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MeasureResponse.MeasureResponseBuilder.class)
public class MeasureResponse {

    @JsonProperty("component")
    ComponentWithMeasures component;
}
