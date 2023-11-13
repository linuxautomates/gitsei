package io.levelops.ingestion.sinks;

public interface DataPipe<T, U, R extends SinkIngestionResult> extends DataSink<T, R> {

    DataSink<U, R> getOutputSink();

}
