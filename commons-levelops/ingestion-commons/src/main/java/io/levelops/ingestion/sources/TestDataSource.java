package io.levelops.ingestion.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.VoidQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.stream.Stream;

@Builder
@AllArgsConstructor
public class TestDataSource<T> implements DataSource<T, VoidQuery> {

    @Singular
    private final List<Data<T>> dataEntries;
    private final Class<T> dataClass;

    @Builder.Default
    private int index = 0;

    @Override
    public Data<T> fetchOne(VoidQuery x) {
        if (dataEntries.isEmpty()) {
            return BasicData.empty(dataClass);
        }
        Data<T> data = dataEntries.get(index);
        index = (index + 1) % dataEntries.size();
        return data;
    }

    @Override
    public Stream<Data<T>> fetchMany(VoidQuery x) {
        return dataEntries.stream();
    }

    @SafeVarargs
    public static <T> TestDataSource<T> of(Class<T> dataClass, T... values) {
        return TestDataSource.<T>builder()
                .dataClass(dataClass)
                .dataEntries(BasicData.ofMany(dataClass, values))
                .build();
    }

    public static class TestDataSourceBuilder<T> {

    }
}
