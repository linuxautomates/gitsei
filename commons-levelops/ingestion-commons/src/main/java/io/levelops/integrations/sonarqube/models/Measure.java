package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Measure.MeasureBuilder.class)
public class Measure {

    @JsonProperty("metric")
    String metric;

    @JsonProperty("value")
    String value;

    @JsonProperty("data_type")
    String dataType;

    @JsonProperty("bestValue")
    Boolean bestValue;

}