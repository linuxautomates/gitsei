package io.levelops.commons.functional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IterableUtils {

    public static <A, B> List<B> parseIterable(Iterable<A> iterable, Function<A, B> mapper) {
        if (iterable == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static <T> Optional<T> getFirst(Iterable<T> it) {
        if (it == null || !it.iterator().hasNext()) {
            return Optional.empty();
        }
        return Optional.ofNullable(it.iterator().next());
    }

    public static <T> Optional<T> getLast(List<T> list) {
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(list.size() - 1));
    }
}
