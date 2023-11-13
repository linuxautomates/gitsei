package io.levelops.ingestion.engine;

import io.levelops.ingestion.components.IngestionComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class EngineEntity {

    private IngestionComponent ingestionComponent;
    private String id;
    private String name;

    @Override
    public String toString() {
        return String.format("%s-%s (%s)", ingestionComponent.getComponentType(), id, ingestionComponent.getClass().getSimpleName());
    }
}
