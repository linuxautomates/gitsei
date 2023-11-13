package io.levelops.commons.caching;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.hash.Hashing;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CacheHashUtils {

    private static ObjectMapper MAPPER = DefaultObjectMapper.get().copy()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public static String generateCacheHash(Object... objects) {
        return generateCacheHashUsingSerialization(objects);
    }

    /**
     * @deprecated Use {@code generateCacheHashUsingSerialization()}. This method doesn't hash objects with same keys in different order.
     */
    @Deprecated
    public static String generateCacheHashUsingToString(Object... objects) {
        return StreamUtils.toStream(objects)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(CacheHashUtils::generateCacheHash)
                .collect(Collectors.joining("_"));
    }

    public static String generateCacheHashUsingSerialization(Object... objects) {
        return StreamUtils.toStream(objects)
                .filter(Objects::nonNull)
                .map(RuntimeStreamException.wrap(MAPPER::writeValueAsString))
                .map(CacheHashUtils::generateCacheHash)
                .collect(Collectors.joining("_"));
    }

    public static String generateCacheHash(String string) {
        return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
    }

    public static String combineCacheHashes(String... hashes) {
        return StreamUtils.toStream(hashes)
                .collect(Collectors.joining("_"));
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, String str) {
        if (StringUtils.isNotEmpty(str)) {
            dataToHash.append(",").append(fieldName).append("=").append(str);
        }
    }

    public static void hashDataUsingToString(StringBuilder dataToHash, String fieldName, Object o) {
        if (o != null) {
            hashData(dataToHash, fieldName, o.toString());
        }
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, Boolean o) {
        hashDataUsingToString(dataToHash, fieldName, o);
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, Integer o) {
        hashDataUsingToString(dataToHash, fieldName, o);
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, Long o) {
        hashDataUsingToString(dataToHash, fieldName, o);
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, Enum<?> o) {
        hashDataUsingToString(dataToHash, fieldName, o);
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        ArrayList<String> tempList = new ArrayList<>(list);
        Collections.sort(tempList);
        dataToHash.append(",").append(fieldName).append("=").append(String.join(",", tempList));
    }

    public static void hashData(StringBuilder dataToHash, String fieldName, ImmutablePair<Long, Long> dateRange) {
        if (dateRange == null) {
            return;
        }
        dataToHash.append(",").append(fieldName).append("=");
        if (dateRange.getLeft() != null) {
            dataToHash.append(dateRange.getLeft()).append("-");
        }
        if (dateRange.getRight() != null) {
            dataToHash.append(dateRange.getRight());
        }
    }

    public static void hashDataTimestamp(StringBuilder dataToHash, String fieldName,
                                         ImmutablePair<Long, Long> dateRange) {
        hashDataNumericRange(dataToHash, fieldName, dateRange);
    }

    public static void hashDataNumericRange(StringBuilder dataToHash, String fieldName,
                                         ImmutablePair<? extends Number, ? extends Number> numericRange) {
        if (numericRange == null) {
            return;
        }
        dataToHash.append(",").append(fieldName).append("=");
        if (numericRange.getLeft() != null) {
            dataToHash.append(numericRange.getLeft()).append("-");
        }
        if (numericRange.getRight() != null) {
            dataToHash.append(numericRange.getRight());
        }
    }

    public static void hashDataMapOfLists(StringBuilder dataToHash, String fieldName, Map<String, List<String>> map) {
        if (!MapUtils.isNotEmpty(map)) {
            return;
        }
        TreeSet<String> fields = new TreeSet<>(map.keySet());
        dataToHash.append(",").append(fieldName).append("=(");
        for (String field : fields) {
            List<String> data = new ArrayList<>(map.get(field));
            Collections.sort(data);
            dataToHash.append(field).append("=").append(String.join(",", data)).append(",");
        }
        dataToHash.append(")");
    }

    public static void hashDataMapOfStrings(StringBuilder dataToHash, String fieldName, Map<String, ?> map) {
        if (!MapUtils.isNotEmpty(map)) {
            return;
        }
        TreeSet<String> fields = new TreeSet<>(map.keySet());
        dataToHash.append(",").append(fieldName).append("=(");
        for (String field : fields) {
            String data = String.valueOf(map.get(field));
            dataToHash.append(field).append("=").append(data).append(",");
        }
        dataToHash.append(")");
    }

    public static void hashDataMapOfMaps(StringBuilder dataToHash, String fieldName, Map<String, Map<String,String>> map) {
        if (MapUtils.isNotEmpty(map)) {
            TreeSet<String> fields = new TreeSet<>(map.keySet());
            dataToHash.append(",").append(fieldName).append("=(");
            for (String field : fields) {
                Map<String, String> innerMap = map.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
        }
    }

}
