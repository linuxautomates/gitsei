package io.levelops.commons.functional;

import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        if (iterable == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> Stream<T> toStream(T... array) {
        if (array == null) {
            return Stream.empty();
        }
        return Arrays.<T>stream(array);
    }

    public static <T> void forEachPage(Stream<T> stream, int pageSize, Consumer<List<T>> consumer) {
        ArrayList<T> page = new ArrayList<>();
        try (stream) {
            stream.forEach(t -> {
                page.add(t);
                if (page.size() >= pageSize) {
                    consumer.accept(page);
                    page.clear();
                }
            });
        }
        if (!page.isEmpty()) {
            consumer.accept(page);
        }
    }

    public static <T> void forEachPageTakingWhile(Stream<T> stream, int pageSize, Consumer<List<T>> consumer, Predicate<T> takeWhilePredicate, boolean takeWhileInclusive) {
        ArrayList<T> page = new ArrayList<>();
        MutableBoolean breakStream = new MutableBoolean(false);
        try (stream) {
            stream.peek(t -> {
                        if (!takeWhilePredicate.test(t)) {
                            if (takeWhileInclusive) {
                                page.add(t);
                            }
                            breakStream.setTrue();
                            return;
                        }
                        page.add(t);
                        if (page.size() >= pageSize) {
                            consumer.accept(page);
                            page.clear();
                        }
                    })
                    .takeWhile(o -> breakStream.isFalse())
                    .forEach(noOp->{});
        }
        if (!page.isEmpty()) {
            consumer.accept(page);
        }
    }

    public static <T> Stream<List<T>> partition(Stream<T> stream, int batchSize) {
        List<T> buffer = new ArrayList<>(batchSize);
        return Stream.concat(
                        // go through the stream and fill the buffer, return it when full then clear it
                        stream.sequential()
                                .map(t -> {
                                    buffer.add(t);
                                    if (buffer.size() >= batchSize) {
                                        List<T> fullBatch = new ArrayList<>(buffer);
                                        buffer.clear();
                                        return fullBatch;
                                    }
                                    return null;
                                }),
                        // if there are left over items but the last batch was not full yet, return them.
                        // (using generate() + limit(1) to make sure the code runs after the first part of the stream is done)
                        Stream.generate(() -> buffer.isEmpty() ? null : buffer).limit(1)
                )
                .filter(Objects::nonNull);
    }

}
