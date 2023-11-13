package io.levelops.commons.utils;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommaListSplitter {

    private static final Splitter COMMA_LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    public static List<String> split(String str) {
        return COMMA_LIST_SPLITTER.splitToList(StringUtils.defaultString(str));
    }

    public static Set<String> splitToSet(String str) {
        return splitToStream(str).collect(Collectors.toSet());
    }

    public static Stream<String> splitToStream(String str) {
        return split(str).stream();
    }

}
