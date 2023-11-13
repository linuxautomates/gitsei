package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Priority.PriorityBuilder.class)
public class Priority {

    @JsonProperty("aboveNormal")
    String aboveNormal;

    @JsonProperty("belowNormal")
    String belowNormal;

    @JsonProperty("high")
    String high;

    @JsonProperty("low")
    String low;

    @JsonProperty("normal")
    String normal;
}
