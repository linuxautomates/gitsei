package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemTypeState.WorkItemTypeStateBuilder.class)
public class WorkItemTypeState {

    @JsonProperty("category")
    String category;

    @JsonProperty("color")
    String color;

    @JsonProperty("name")
    String name;
}
