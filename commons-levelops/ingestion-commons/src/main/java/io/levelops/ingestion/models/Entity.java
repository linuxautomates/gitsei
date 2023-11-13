package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Entity.EntityBuilder.class)
public class Entity {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("component_type")
    String componentType;

    @JsonProperty("component_class")
    String componentClass;

}
