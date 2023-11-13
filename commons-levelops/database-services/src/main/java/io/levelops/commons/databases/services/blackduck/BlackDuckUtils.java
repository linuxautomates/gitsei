package io.levelops.commons.databases.services.blackduck;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class BlackDuckUtils {

    public static String getWhereClause(List<String> conditions) {
        if (!conditions.isEmpty())
            return " WHERE " + String.join(" AND ", conditions);
        return EMPTY;
    }

    public static void setPagingParams(Map<String, Object> params, Integer pageNumber, Integer pageSize) {
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
    }

    public static <T> void insertList(Map<String, Object> params, List<String> conditions, String columnName, String key,
                                      List<T> value) {
        if (CollectionUtils.isNotEmpty(value)) {
            conditions.add(columnName + " IN (:" + key + ")");
            params.put(key, value);
        }
    }
    public static <T> void insertUUID(Map<String, Object> params, List<String> conditions, String columnName, String key,
                                  List<T> value) {
        if (CollectionUtils.isNotEmpty(value)) {
            conditions.add(columnName + " = (:" + key + ")::uuid");
            params.put(key, value);
        }
    }

    public static void insertBoolean(Map<String, Object> params, List<String> conditions, String columnName, String key,
                                     String value) {
        if (StringUtils.isNotEmpty(value)) {
            conditions.add(columnName + " = :" + key);
            params.put(key, BooleanUtils.toBoolean(value));
        }
    }

    public static void insertFloatRange(Map<String, Object> params, List<String> conditions, String columnName, String key,
                                        Map<String, String> value) {
        if (MapUtils.isNotEmpty(value)) {
            if (value.get("$gt") != null) {
                conditions.add(columnName + " > :gt_" + key);
                params.put("gt_" + key, NumberUtils.toFloat(value.get("$gt")));
            }
            if (value.get("$lt") != null) {
                conditions.add(columnName + " < :lt_" + key);
                params.put("lt_" + key, NumberUtils.toFloat(value.get("$lt")));
            }
        }
    }

    public static void insertDateRange(List<String> conditions, String columnName,
                                       ImmutablePair<Long, Long> value) {
        if (CollectionUtils.isNotEmpty(Collections.singleton(value)) && value != null) {
            if (value.getLeft() != null) {
                conditions.add(columnName + " > TO_TIMESTAMP(" + value.getLeft() + ")");
            }
            if (value.getRight() != null) {
                conditions.add(columnName + " < TO_TIMESTAMP(" + value.getRight() + ")");
            }
        }
    }

    public static Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }

    public static Timestamp getTimeStamp(Long date) {
        return new Timestamp(date);
    }
}
