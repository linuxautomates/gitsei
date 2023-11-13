package io.levelops.ingestion.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.ingestion.components.IngestionComponent;
import io.levelops.ingestion.engine.EngineEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Entity {
    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("component_type")
    String componentType;

    @JsonProperty("component_class")
    String componentClass;

    public static Entity fromEngineEntity(EngineEntity e) {
        return Entity.builder()
                .id(e.getId())
                .name(e.getName())
                .componentClass(e.getIngestionComponent().getComponentClass())
                .componentType(e.getIngestionComponent().getComponentType())
                .build();
    }
}
