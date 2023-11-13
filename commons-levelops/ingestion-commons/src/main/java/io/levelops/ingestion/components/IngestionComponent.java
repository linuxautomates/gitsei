package io.levelops.ingestion.components;

public interface IngestionComponent {

    String getComponentType();

    default String getComponentClass() {
        return this.getClass().getSimpleName();
    }

}
