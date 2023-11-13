package io.levelops.ingestion.sinks;

import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.DataFunctions;
import io.levelops.ingestion.exceptions.PushException;
import lombok.Builder;
import lombok.Getter;

import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Builder(toBuilder = true)
public class TransformationPipe<T, U, R extends SinkIngestionResult> implements DataPipe<T, U, R> {

    private final DataSink<U, R> outputSink;
    private final Function<Data<T>, Data<U>> transform;

    @Override
    public R pushOne(Data<T> data) throws PushException {
        return outputSink.pushOne(transform.apply(data));
    }

    @Override
    public R pushMany(Stream<Data<T>> dataStream) throws PushException {
        return outputSink.pushMany(dataStream.map(transform));
    }

    public static class TransformationPipeBuilder<T, U, R extends SinkIngestionResult> {
        public TransformationPipeBuilder<T, U, R> basicDataTransform(Class<U> outputDataClass, Function<T, U> transform) {
            return this.transform(DataFunctions.basicDataTransform(outputDataClass, transform));
        }
    }

}
