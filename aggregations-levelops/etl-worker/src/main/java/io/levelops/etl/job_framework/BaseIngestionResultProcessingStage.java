package io.levelops.etl.job_framework;

import com.google.common.reflect.TypeToken;

public abstract class BaseIngestionResultProcessingStage<T, S> implements IngestionResultProcessingStage<T, S> {
    private final TypeToken<T> typeToken = new TypeToken<T>(getClass()) { };
    private final Class<T> gcsDataType = (Class<T>) typeToken.getRawType();


    @Override
    public Class<T> getGcsDataType() {
        return this.gcsDataType;
    }
}
