package io.levelops.ingestion.sinks;

import io.levelops.ingestion.components.IngestionComponent;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.PushException;

import java.util.stream.Stream;

public interface DataSink<T, R extends SinkIngestionResult> extends IngestionComponent {

    R pushOne(Data<T> data) throws PushException;

    R pushMany(Stream<Data<T>> dataStream) throws PushException;

    default String getComponentType() {
        return "DataSink";
    }

}
