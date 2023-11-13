package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Value
// @AllArgsConstructor
// @Builder(toBuilder = true)
@JsonDeserialize(builder = DefaultListRequest.DefaultListRequestBuilder.class)
public class DefaultListRequest {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 1000;//TODO: revert to 100 after UI make changes
    private static final String DAY_TIME_FIELD = "day";
    public static final int DEFAULT_ACROSS_LIMIT = 90;

    // @Default
    @JsonProperty("page")
    int page;
    // @Default
    @JsonProperty("page_size")
    int pageSize;
    @JsonProperty("across")
    String across;
    // @Default
    @JsonProperty("filter_across_values")
    Boolean filterAcrossValues;
    // @Default
    @JsonProperty("across_limit")
    int acrossLimit;
    // @Default
    @JsonProperty("interval")
    String aggInterval;
    // @Default
    @JsonProperty("fields")
    List<String> fields;
    // @Default
    @JsonProperty("filter")
    Map<String, Object> filter;
    // @Default
    @JsonProperty("sort")
    List<Map<String, Object>> sort;
    // @Default
    @JsonProperty("stacks")
    List<String> stacks;
    // @Default
    @JsonProperty("ou_ids")
    Set<Integer> ouIds;
    @JsonProperty("ou_exclusions")
    List<String> ouExclusions;
    @JsonProperty("apply_ou_on_velocity_report")
    Boolean applyOuOnVelocityReport;

    // @Default
    @JsonProperty("ou_user_filter_designation")
    Map<String, Set<String>> ouUserFilterDesignation;

    @JsonProperty("widget_id")
    String widgetId;

    @JsonProperty("widget")
    String widget;
    // @JsonPOJOBuilder(withPrefix = "")
    // public static class DefaultListRequestBuilderImpl extends DefaultListRequestBuilder {
    //     @Override
    //     public DefaultListRequestBuilder pageSize(int pageSize) {
    //         return super.pageSize((pageSize > 0) ? pageSize : DEFAULT_PAGE_SIZE);
    //     }

    //     @Override
    //     public DefaultListRequestBuilder page(int page) {
    //         return super.page((page > -1) ? page : DEFAULT_PAGE);
    //     }
    // }

    @Builder(toBuilder = true)
    public DefaultListRequest(@JsonProperty("page") Integer page,
                              @JsonProperty("across") String across,
                              @JsonProperty("filter_across_values") Boolean filterAcrossValues,
                              @JsonProperty("across_limit") Integer acrossLimit,
                              @JsonProperty("interval") String aggInterval,
                              @JsonProperty("fields") List<String> fields,
                              @JsonProperty("page_size") Integer pageSize,
                              @JsonProperty("filter") Map<String, Object> filter,
                              @JsonProperty("sort") List<Map<String, Object>> sort,
                              @JsonProperty("stacks") List<String> stacks,
                              @JsonProperty("ou_ids") Set<Integer> ouIds,
                              @JsonProperty("ou_exclusions") List<String> ouExclusions,
                              @JsonProperty("apply_ou_on_velocity_report") Boolean applyOuOnVelocityReport,
                              @JsonProperty("ou_user_filter_designation") Map<String, Set<String>> ouUserFilterDesignation,
                              @JsonProperty("widget_id") String widgetId,
                              @JsonProperty("widget") String widget) {
        this.across = across;
        this.filterAcrossValues = (filterAcrossValues != null) ? filterAcrossValues : true;
        this.acrossLimit = (acrossLimit != null && acrossLimit > 0) ? acrossLimit : DEFAULT_ACROSS_LIMIT;
        this.aggInterval = (aggInterval != null) ? aggInterval : DAY_TIME_FIELD;
        this.sort = (sort != null) ? sort : Collections.emptyList();
        this.page = (page != null && page > -1) ? page : DEFAULT_PAGE;
        this.filter = (filter != null) ? filter : Collections.emptyMap();
        this.fields = (fields != null) ? fields : Collections.emptyList();
        this.pageSize = (pageSize != null && pageSize > -1) ? pageSize : DEFAULT_PAGE_SIZE;
        this.stacks = (stacks != null) ? stacks : Collections.emptyList();
        this.ouIds = ouIds != null ? ouIds : Set.of();
        this.ouExclusions = ouExclusions != null ? ouExclusions : List.of();
        this.applyOuOnVelocityReport = (applyOuOnVelocityReport != null) ? applyOuOnVelocityReport : true;
        this.ouUserFilterDesignation = ouUserFilterDesignation != null ? ouUserFilterDesignation : Map.of();
        this.widgetId = widgetId;
        this.widget = widget;
    }

    public <T> Optional<T> getFilterValue(String key, Class<T> clazz) {
        return Optional.ofNullable(filter)
                .map(f -> f.get(key))
                .map(clazz::cast);
    }

    public <T> Optional<List<T>> getFilterValueAsList(String key) {
        //noinspection unchecked
        return Optional.ofNullable(filter)
                .map(f -> f.get(key))
                .map(Collection.class::cast)
                .map(f -> List.<T>copyOf(f));
    }

    public <T> Optional<Set<T>> getFilterValueAsSet(String key) {
        //noinspection unchecked
        return Optional.ofNullable(filter)
                .map(f -> f.get(key))
                .map(Collection.class::cast)
                .map(f -> Set.copyOf(f));
    }

    public <K, V> Optional<Map<K, V>> getFilterValueAsMap(String key) {
        //noinspection unchecked
        return Optional.ofNullable(filter)
                .map(f -> f.get(key))
                .map(Map.class::cast);
    }

    public ImmutablePair<Long, Long> getNumericRangeFilter(String field) {
        Map<String, String> timeRange = getFilterValue(field, Map.class).orElse(Map.of());
        final Long timeRangeStart = timeRange.get("$gt") != null ? Long.valueOf(timeRange.get("$gt")) : null;
        final Long timeRangeEnd = timeRange.get("$lt") != null ? Long.valueOf(timeRange.get("$lt")) : null;
        return ImmutablePair.of(timeRangeStart, timeRangeEnd);
    }
}