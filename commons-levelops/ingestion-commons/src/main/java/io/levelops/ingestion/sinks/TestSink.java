package io.levelops.ingestion.sinks;

import io.levelops.ingestion.data.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
@NoArgsConstructor
public class TestSink<T> implements DataSink<T, TestSink.TestSinkResult> {

    private List<Data<T>> capturedData = new ArrayList<>();

    @Override
    public TestSinkResult pushOne(Data<T> data) {
        capturedData.add(data);
        return new TestSinkResult();
    }

    @Override
    public TestSinkResult pushMany(Stream<Data<T>> dataStream) {
        dataStream.forEach(capturedData::add);
        return new TestSinkResult();
    }

    public static <T> TestSink<T> forClass(@SuppressWarnings("unused") Class<T> dataClass) {
        return new TestSink<>();
    }

    public static class TestSinkResult implements SinkIngestionResult {

    }
}

