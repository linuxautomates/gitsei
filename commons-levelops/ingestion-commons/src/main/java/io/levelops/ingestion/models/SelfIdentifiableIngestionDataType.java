package io.levelops.ingestion.models;

public interface SelfIdentifiableIngestionDataType<T, K> {

    public IngestionDataType<T, K> getIngestionDataType();

}
