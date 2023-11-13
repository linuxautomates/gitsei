package io.levelops.aggregations_shared.utils;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: move this to commoons because this is a duplicate from ingestion control-plane code
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

    public static Timestamp toTimestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant);
    }

    public static boolean doesColumnExist(String columnName, ResultSet rs) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

}
