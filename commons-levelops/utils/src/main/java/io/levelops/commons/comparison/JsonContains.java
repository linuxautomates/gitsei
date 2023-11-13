package io.levelops.commons.comparison;

import lombok.Builder;
import lombok.Value;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Utility to find out if a JSON node (string, list or object) contains another JSON node.
 * The rules are as follow:
 *
 *           column  |
 *          contains |
 *            row    | str | list | map
 *         -------------------------------
 *            str    |  V  |   V  |  X
 *           list    |  V  |   V  |  V
 *            map    |  X  |   V  |  V
 *
 *         str  contains str   V (equals ignore case)
 *         list contains str   V (any item of list contains str)
 *         map  contains str   X
 *
 *         str  contains list  V (for all items of search list, container contains item)
 *         list contains list  V (for all items of search list, container contains item)
 *         map  contains list  V (for all items of search list, container contains item)
 *
 *         str  contains map   X
 *         list contains map   V (any item of list contains map)
 *         map  contains map   V (for each entry of search map, container value contains respective search value)
 *
 *         ? contains null -> true
 *         null contains ? -> false
 *
 */
@SuppressWarnings("unchecked")
public class JsonContains {

    @Value
    @Builder(toBuilder = true)
    public static class SearchOptions {
        boolean ignoreEmptyStrings;
        boolean trimStrings;

        public static SearchOptions NONE = SearchOptions.builder().build();
        public static SearchOptions IGNORE_EMPTY_STRINGS = SearchOptions.builder()
                .ignoreEmptyStrings(true)
                .build();
        public static SearchOptions TRIM_AND_IGNORE_EMPTY_STRINGS = SearchOptions.builder()
                .ignoreEmptyStrings(true)
                .trimStrings(true)
                .build();
    }

    public static boolean contains(Object container, Object search) {
        return contains(container, search, SearchOptions.NONE);
    }

    public static boolean contains(Object container, Object search, SearchOptions options) {
        if (search == null) {
            return true;
        }
        if (container == null) {
            return false;
        }
        if (search instanceof List) {
            return containsList(container, (List<Object>) search, options);
        }
        if (search instanceof Map) {
            return containsMap(container, (Map<String, Object>) search, options);
        }
        return containsString(container, search.toString(), options); // consider objectMapper.writeValueAsString()
    }

    private static boolean containsString(Object container, String search, SearchOptions options) {
        if (search == null) {
            return true;
        }
        if (container == null) {
            return false;
        }
        final String sanitizedSearch = options.isTrimStrings() ? search.trim() : search;
        if (options.isIgnoreEmptyStrings() && StringUtils.isEmpty(sanitizedSearch)) {
            return true;
        }
        if (container instanceof List) {
            return ((List<Object>) container).stream().anyMatch(item -> contains(item, sanitizedSearch, options));
        }
        if (container instanceof Map) {
            // map not supported
            return false;
        }
        return sanitizedSearch.equalsIgnoreCase(container.toString()); // consider objectMapper.writeValueAsString()
    }


    private static boolean containsList(Object container, List<Object> search, SearchOptions options) {
        if (search == null) {
            return true;
        }
        if (container == null) {
            return false;
        }
        return search.stream().allMatch(item -> contains(container, item, options));
    }


    private static boolean containsMap(Object container, Map<String, Object> search, SearchOptions options) {
        if (search == null) {
            return true;
        }
        if (container == null) {
            return false;
        }
        // str not supported
        if (container instanceof List) {
            return ((List<Object>) container).stream().anyMatch(item -> contains(item, search, options));
        }
        if (container instanceof Map) {
            var map = (Map<String, Object>) container;
            return search.entrySet().stream().allMatch(entry -> contains(map.get(entry.getKey()), entry.getValue(), options));
        }
        return false;
    }


}
