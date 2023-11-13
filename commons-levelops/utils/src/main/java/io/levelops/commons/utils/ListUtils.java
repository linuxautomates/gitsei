package io.levelops.commons.utils;

import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ListUtils {

    public static <T> boolean isEmpty(@Nullable List<T> list) {
        return (list == null) || list.isEmpty();
    }

    @Nonnull
    public static <T> List<T> emptyIfNull(@Nullable List<T> list) {
        return (list != null) ? list : List.of();
    }

    @Nonnull
    public static <T> List<T> defaultIfEmpty(@Nullable List<T> list, @Nonnull List<T> defaultValue) {
        return isEmpty(list) ? emptyIfNull(defaultValue) : list;
    }

    @Nonnull
    public static <T> Stream<T> stream(@Nullable List<T> list) {
        return isEmpty(list) ? Stream.empty() : list.stream();
    }

    public static <T> List<T> addIfNotPresent(@Nullable List<T> list, @Nullable T element) {
        if (element == null) {
            return emptyIfNull(list);
        }
        if (list == null) {
            return List.of(element);
        }
        if (list.contains(element)) {
            return list;
        }
        list = new ArrayList<>(list);
        list.add(element);
        return Collections.unmodifiableList(list);
    }

    @Nonnull
    public static <E> List<E> intersection(@Nullable List<? extends E> list1, @Nullable List<? extends E> list2) {
        return org.apache.commons.collections4.ListUtils.intersection(emptyIfNull(list1), emptyIfNull(list2));
    }

    public static <E> List<E> intersectionIgnoringEmpty(@Nullable List<E> list1, @Nullable List<E> list2) {
        if (CollectionUtils.isEmpty(list1)) {
            return emptyIfNull(list2);
        }
        if (CollectionUtils.isEmpty(list2)) {
            return emptyIfNull(list1);
        }
        return ListUtils.intersection(list1, list2);
    }

    @Nonnull
    public static <E> List<E> union(@Nullable List<? extends E> list1, @Nullable List<? extends E> list2) {
        return org.apache.commons.collections4.ListUtils.union(emptyIfNull(list1), emptyIfNull(list2));
    }

}
