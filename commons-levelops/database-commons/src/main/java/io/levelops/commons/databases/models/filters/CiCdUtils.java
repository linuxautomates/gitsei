package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

public class CiCdUtils {

    private static final Set<String> CICD_CATEGORICAL_FIELDS = Set.of("status", "cicd_user_id", "job_name",
            "job_normalized_full_name", "project_name", "cicd_instance_name", "cicd_instance_guid",
            "test_suite", "test_name", "job_status", "change_type");

    private static final Set<String> CICD_NON_SORTABLE_FIELDS = Set.of("params", "commits");

    public static void parseSortBy(String field, Set<String> orderByString,
                                   Map<String, SortingOrder> sortBy, String column) {
        parseSortBy(field, orderByString, sortBy, column, false);
    }

    public static void parseSortBy(String field, Set<String> orderByString,
                                   Map<String, SortingOrder> sortBy, String column, boolean useDefault) {
        if (MapUtils.isNotEmpty(sortBy)) {
            if (sortBy.get(field) != null) {
                orderByString.add(column + " " + sortBy.get(field));
            }
        } else if (useDefault) {
            orderByString.add(column + " DESC");
        }
    }

    public static List<String> getListSortBy(Map<String, SortingOrder> sortBy) {
        return getListSortBy(sortBy, "duration");
    }

    public static List<String> getListSortBy(Map<String, SortingOrder> sortBy, String defaultSort) {
        List<String> orderBy = new ArrayList<>();
        if (MapUtils.isNotEmpty(sortBy)) {
            orderBy = sortBy.entrySet().stream()
                    .map(es -> {
                        if (CICD_NON_SORTABLE_FIELDS.contains(es.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot sort the list by \"" + es.getKey() + "\" field.");
                        }
                        if (CICD_CATEGORICAL_FIELDS.contains(es.getKey())) {
                            return lowerOf(es.getKey()) + " " + es.getValue();
                        } else {
                            return es.getKey() + " " + es.getValue();
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            orderBy.add(defaultSort + " DESC");
        }
        return orderBy;
    }

    public static String lowerOf(String column) {
        return "lower(" + column + ")";
    }

    public static List<CiCdJobRunParameter> parseCiCdJobRunParameters(ObjectMapper objectMapper, List<Object> parameterObjects) {
        if (CollectionUtils.isEmpty(parameterObjects)) {
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(parameterObjects);
            return objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, CiCdJobRunParameter.class));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: parameters");
        }
    }

    public static List<CiCdJobQualifiedName> parseCiCdQualifiedJobNames(ObjectMapper objectMapper, List<Object> qualifiedJobNameObjects) {
        if (CollectionUtils.isEmpty(qualifiedJobNameObjects)) {
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(qualifiedJobNameObjects);
            return objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, CiCdJobQualifiedName.class));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: qualified_job_names");
        }
    }

    public static List<String> getIntegrationIds(Map<String, Object> filter) {
        return CollectionUtils.isNotEmpty(getListOrDefault(filter, "cicd_integration_ids")) ?
                getListOrDefault(filter, "cicd_integration_ids") : getListOrDefault(filter, "integration_ids");
    }

    @NotNull
    public static ImmutablePair<Long, Long> getImmutablePair(DbAggregationResult row, CICD_AGG_INTERVAL aggInterval) {
        Calendar cal = Calendar.getInstance();
        long startTimeInSeconds = Long.parseLong(row.getKey());
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if(aggInterval.equals(CICD_AGG_INTERVAL.month))
            cal.add(Calendar.MONTH, 1);
        else if (aggInterval.equals(CICD_AGG_INTERVAL.day))
            cal.add(Calendar.DATE, 1);
        else if (aggInterval.equals(CICD_AGG_INTERVAL.year))
            cal.add(Calendar.YEAR, 1);
        else if (aggInterval.equals(CICD_AGG_INTERVAL.quarter))
            cal.add(Calendar.MONTH, 3);
        else
            cal.add(Calendar.DATE, 7);
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
        ImmutablePair<Long, Long> timeRange = ImmutablePair.of(startTimeInSeconds, endTimeInSeconds);
        return timeRange;
    }

}
