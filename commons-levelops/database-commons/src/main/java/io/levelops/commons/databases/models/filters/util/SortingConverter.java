package io.levelops.commons.databases.models.filters.util;

import io.levelops.commons.databases.models.database.SortingOrder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SortingConverter {
    public static Map<String, SortingOrder> fromFilter(List<Map<String, Object>> sort) {
        return CollectionUtils.emptyIfNull(sort).stream().filter(criteria -> criteria.get("id") != null).flatMap(criteria -> Map.of(
                criteria.get("id").toString(),
                getSortingOrder(criteria))
                .entrySet().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
                        Collections::<String, SortingOrder>unmodifiableMap));
    }

    private static SortingOrder getSortingOrder(Map<String, Object> criteria) {
        if (!criteria.containsKey("desc")) { // order not specified
            return criteria.get("id").equals("trend") ? SortingOrder.ASC : SortingOrder.DESC;
        } else {
            return Boolean.FALSE.equals(criteria.get("desc")) ? SortingOrder.ASC : SortingOrder.DESC;
        }
    }
}
