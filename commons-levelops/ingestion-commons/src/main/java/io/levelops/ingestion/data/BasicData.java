package io.levelops.ingestion.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BasicData<T> implements Data<T> {

    Class<T> dataClass;
    T payload;

    @Override
    public boolean isPresent() {
        return payload != null;
    }

    public static <T> BasicData<T> of(Class<T> dataClass, T payload) {
        return new BasicData<>(dataClass, payload);
    }

    public static <T> Function<T, BasicData<T>> mapper(final Class<T> dataClass) {
        return (T payload) -> new BasicData<T>(dataClass, payload);
    }

    @SafeVarargs
    public static <T> List<BasicData<T>> ofMany(Class<T> dataClass, T... data) {
        return Stream.of(data)
                .map(payload -> new BasicData<>(dataClass, payload))
                .collect(Collectors.toList());
    }

    public static <T> BasicData<T> empty(Class<T> dataClass) {
        return of(dataClass, null);
    }

}
