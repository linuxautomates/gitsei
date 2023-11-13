package io.levelops.controlplane.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseUtils {

    public static <T> Stream<T> fromSqlArray(Array sqlArray, Class<T> baseClass) throws SQLException {
        if (sqlArray == null) {
            return Stream.empty();
        }
        return Arrays.stream((Object[])sqlArray.getArray()).map(baseClass::cast);
    }

    public static <T> String toSqlArray(Collection<T> collection) {
        if (CollectionUtils.isEmpty(collection)) {
            return "{}";
        }
        return "{" +
                collection.stream()
                        .map(T::toString)
                        .map(str -> StringUtils.wrap(str, "\""))
                        .collect(Collectors.joining(",")) +
                "}";
    }

}
