package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Log4j2
public class DefaultListRequestUtils {

    public static List<String> getListOrDefault(DefaultListRequest request, String key) {
        return getListOrDefault(request.getFilter(), key);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<String> getListOrDefault(Map<String, Object> filter, String key)  {
        try {
            var value = (Collection) MapUtils.emptyIfNull(filter).getOrDefault(key, Collections.emptyList());
            if (value.size() > 0 && !(value.iterator().next() instanceof String)){
                return (List<String>) value.stream().map(i -> i.toString()).collect(Collectors.toList());
            }
            // to handle list or sets
            return (List<String>) value.stream().collect(Collectors.toList());
        } catch (ClassCastException exception) {
            log.debug("Invalid filter parameter: " + key);
            return Collections.emptyList();
        }
    }

    public static List<Object> getListOfObjectOrDefault(DefaultListRequest request, String key) throws BadRequestException {
        try {
            return getListOfObjectOrDefault(request.getFilter(), key);
        } catch (ClassCastException exception) {
            throw new BadRequestException("Invalid filter parameter: " + key);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getListOfObjectOrDefault(Map<String, Object> filter, String key)  {
        try {
            return (List<Object>) MapUtils.emptyIfNull(filter).getOrDefault(key, Collections.emptyList());
        } catch (ClassCastException exception) {
            log.debug("Invalid filter parameter: " + key);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getListOfObjectOrDefault(DefaultListRequest request, ObjectMapper mapper, String key, Class<T> clazz) throws BadRequestException {
        try {
            List<Object> objects = (List<Object>) MapUtils.emptyIfNull(request.getFilter()).getOrDefault(key, Collections.emptyList());
            log.debug("objects {}", objects);
            String serialized = mapper.writeValueAsString(objects);
            log.debug("serialized = {}", serialized);
            List<T> parsed = mapper.readValue(serialized, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
            log.debug("parsed = {}", parsed);
            return parsed;
        } catch (ClassCastException | JsonProcessingException exception) {
            throw new BadRequestException("Invalid filter parameter: " + key);
        }
    }

    public static ImmutablePair<Long, Long> getExcludedRange(@Nonnull DefaultListRequest filter, String prefix, String field) throws BadRequestException {
        Map<String, Object> excludedRange = filter.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
        Map<String, Object> dateRange = (Map<String, Object>)excludedRange.getOrDefault(prefix + field, Map.of());
        return getRangeImmutablePair(prefix, field, dateRange);
    }

    public static ImmutablePair<Long, Long> getTimeRange(@Nonnull DefaultListRequest filter, String prefix, String field) throws BadRequestException {
        Map<String, Object> dateRange = filter.<String, Object>getFilterValueAsMap(prefix + field).orElse(Map.of());
        return getRangeImmutablePair(prefix, field, dateRange);
    }

    @NotNull
    private static ImmutablePair<Long, Long> getRangeImmutablePair(String prefix, String field, Map<String, Object> dateRange) throws BadRequestException {
        try {
            Long start = dateRange.get("$gte") != null ? Long.parseLong(dateRange.get("$gte").toString()) - 1 : null;
            Long end = dateRange.get("$lte") != null ? Long.parseLong(dateRange.get("$lte").toString()) + 1 : null;
            start = dateRange.get("$gt") != null ? Long.valueOf(dateRange.get("$gt").toString()) : start;
            end = dateRange.get("$lt") != null ? Long.valueOf(dateRange.get("$lt").toString()) : end;
            return ImmutablePair.of(start, end);
        } catch (Exception e) {
            throw new BadRequestException("Could not parse " + prefix + field + " field", e);
        }
    }

    public static ImmutablePair<Long, Long> getTimeRange(DefaultListRequest request, String key) throws BadRequestException {
        return getTimeRange(request, "", key);
    }

    @SuppressWarnings("unchecked")
    public static ImmutablePair<Float, Float> parseFloatRange(DefaultListRequest request, String field) {
        Map<String, String> floatRange = request.getFilterValue(field, Map.class).orElse(Map.of());
        Float start = floatRange.get("$gte") != null ? Float.parseFloat(floatRange.get("$gte")) - 0.1F : null;
        Float end = floatRange.get("$lte") != null ?  Float.parseFloat(floatRange.get("$lte")) + 0.1F : null;
        start = floatRange.get("$gt") != null ? Float.valueOf(floatRange.get("$gt")) : start;
        end = floatRange.get("$lt") != null ? Float.valueOf(floatRange.get("$lt")) : end;
        return ImmutablePair.of(start, end);
    }


    public static ImmutablePair<Long, Long> getNumericRange(DefaultListRequest request, String key) throws BadRequestException {
        return getTimeRange(request, key);
    }
}
