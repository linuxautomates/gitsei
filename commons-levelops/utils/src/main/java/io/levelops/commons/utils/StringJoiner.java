package io.levelops.commons.utils;

import io.levelops.commons.functional.StreamUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringJoiner {

    public static String join(CharSequence delimiter, CharSequence... elements) {
        return String.join(delimiter, elements);
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        return String.join(delimiter, elements);

    }

    public static String dedupeAndJoin(String delimiter, Iterable<String> elements) {
        return dedupeAndJoin(delimiter, StreamUtils.toStream(elements));
    }

    public static String dedupeAndJoin(String delimiter, String... elements) {
        return dedupeAndJoin(delimiter, StreamUtils.toStream(elements));
    }

    public static String dedupeAndJoin(String delimiter, Stream<String> elements) {
        return dedupeStream(elements).collect(Collectors.joining(delimiter));
    }

    public static Stream<String> dedupeStream(Stream<String> elements) {
        return elements
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .distinct();
    }

    public static String prefixIfNotBlank(String prefix, String str) {
        return prefixConditionally(prefix, str, StringUtils::isNotBlank);
    }

    public static String prefixConditionally(String prefix, String str, Predicate<String> predicate) {
        if (predicate.test(str)) {
            return prefix + str;
        } else {
            return str;
        }
    }
}
