package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ComponentWithMeasures.ComponentWithMeasuresBuilder.class)
public class ComponentWithMeasures {

    @JsonProperty("id")
    String id;

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("qualifier")
    String qualifier;

    @JsonProperty("measures")
    List<Measure> measures;

}
