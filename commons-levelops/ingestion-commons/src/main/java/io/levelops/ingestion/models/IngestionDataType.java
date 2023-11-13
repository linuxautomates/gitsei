package io.levelops.ingestion.models;

public interface IngestionDataType<Singular, Plural> {
    public String getIngestionDataType();
    public String getIngestionPluralDataType();
    public Class<? extends Singular> getIngestionDataTypeClass();
    public Class<? extends Plural> getIngestionPluralDataTypeClass();
}