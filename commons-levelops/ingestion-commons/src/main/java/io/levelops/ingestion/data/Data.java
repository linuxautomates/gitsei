package io.levelops.ingestion.data;

import io.levelops.commons.functional.IngestionFailure;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Data<T> {

    // TODO  use oop instead?

    Class<T> getDataClass();

    T getPayload();

    boolean isPresent();

    /**
     * @deprecated replace with direct call to {@link io.levelops.ingestion.data.DataCollector#collect(java.util.stream.Stream)}
     */
    @Deprecated
    @Nonnull
    static <T> List<T> collect(@Nullable Stream<Data<T>> stream) {
        return DataCollector.collect(stream).getRecords();
    }

    /**
     * @deprecated replace with direct call to {@link io.levelops.ingestion.data.DataCollector#collect(java.util.stream.Stream)}
     */
    @Deprecated
    @Nonnull
    static <T> ImmutablePair<List<T>, List<IngestionFailure>> collectData(@Nullable Stream<Data<T>> stream) {
        DataCollector<T> collect = DataCollector.collect(stream);

        return ImmutablePair.of(collect.getRecords(), collect.getIngestionFailures());
    }
}
