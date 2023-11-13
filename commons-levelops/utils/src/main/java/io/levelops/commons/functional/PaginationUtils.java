package io.levelops.commons.functional;

import io.levelops.commons.exceptions.ExceptionSuppliers;
import io.levelops.commons.exceptions.FunctionWithException;
import io.levelops.commons.exceptions.RuntimeStreamException;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PaginationUtils {

    /**
     * Creates a stream from a page supplier.
     *
     * @param start        starting page
     * @param increment    increment
     * @param pageSupplier Given a page, returns a list of data. If null or empty, stops the pagination.
     * @param <T>          data type
     * @return stream
     */
    public static <T> Stream<T> stream(int start, int increment, Function<Integer, List<T>> pageSupplier) {
        return IntStream.iterate(start, i -> i + increment)
                .mapToObj(pageSupplier::apply)
                .takeWhile(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull);
    }

    public static <T> Stream<IngestionResult<T>> streamData(int start, int increment,
                                                            Function<Integer, IngestionResult<T>> pageSupplier) {
        return IntStream.iterate(start, i -> i + increment)
                .mapToObj(pageSupplier::apply)
                .takeWhile(tIngestionResult -> tIngestionResult != null && (CollectionUtils.isNotEmpty(tIngestionResult.getData())
                        || CollectionUtils.isNotEmpty(tIngestionResult.getIngestionFailures())));
    }

    public static <T> Stream<T> stream(String startingCursor, Function<String, CursorPageData<T>> pageSupplier,
                                       Predicate<List<T>> predicate) {
        return Stream.generate(new InternalCursorPageSupplier<>(startingCursor, pageSupplier))
                .takeWhile(predicate)
                .takeWhile(CollectionUtils::isNotEmpty)
                .flatMap(List::stream)
                .filter(Objects::nonNull);
    }

    public static <T> Stream<T> stream(int start, int increment, Function<Integer, List<T>> pageSupplier, long limit) {
        return IntStream.iterate(start, i -> i + increment)
                .mapToObj(pageSupplier::apply)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .limit(limit);
    }

    public static <T> Stream<T> streamThrowingRuntime(int start, int increment, FunctionWithException<Integer, List<T>, Exception> pageSupplier) throws RuntimeStreamException {
        return stream(start, increment, RuntimeStreamException.wrap(pageSupplier));
    }

    public static <T, E extends Exception> Stream<T> stream(int start, int increment,
                                                             FunctionWithException<Integer, List<T>, E> pageSupplier,
                                                             ExceptionSuppliers.ExceptionWithCauseAndMessageSupplier<E> exceptionWithCauseAndMessageSupplier) throws E {
        try {
            return stream(start, increment, RuntimeStreamException.wrap(pageSupplier));
        } catch (RuntimeStreamException e) {
            throw exceptionWithCauseAndMessageSupplier.build("Streamed pagination failed", e);
        }
    }

    // region cursor

    public static <T> Stream<T> stream(String startingCursor, Function<String, CursorPageData<T>> pageSupplier) {
        return Stream.generate(new InternalCursorPageSupplier<>(startingCursor, pageSupplier))
                .takeWhile(CollectionUtils::isNotEmpty)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull);
    }

    /**
     * Creates a stream from a page supplier until predicate returns true.
     * @param pageSupplier Returns data when invoked.
     * @param predicate Condition for data fetching. Supplier will be invoked till predicate returns true. 
     * @param <T> data type
     * @return stream
     */
    public static <T> Stream<T> stream(Supplier<T> pageSupplier, Predicate<T> predicate) {
        return Stream.generate(pageSupplier)
                .takeWhile(predicate)
                .filter(Objects::nonNull);
    }

    @Value
    @Builder(toBuilder = true)
    public static class CursorPageData<T> {
        List<T> data;
        String cursor; // nullable
    }

    private static class InternalCursorPageSupplier<T> implements Supplier<List<T>> {
        private final Function<String, CursorPageData<T>> pageSupplier;
        private boolean done = false;
        private String cursor;

        InternalCursorPageSupplier(String startingCursor, Function<String, CursorPageData<T>> pageSupplier) {
            this.cursor = startingCursor;
            this.pageSupplier = pageSupplier;
        }

        @Override
        public List<T> get() {
            if (done) {
                return null;
            }
            CursorPageData<T> pageData = pageSupplier.apply(cursor);
            if (pageData == null) {
                done = true;
                return Collections.emptyList();
            }
            String newCursor = pageData.getCursor();
            if (Strings.isEmpty(newCursor) || newCursor.equals(cursor)) {
                done = true;
            }
            cursor = newCursor;
            return pageData.getData();
        }
    }
    //endregion

}
