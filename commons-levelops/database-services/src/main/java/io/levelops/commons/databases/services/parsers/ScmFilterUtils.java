package io.levelops.commons.databases.services.parsers;

import io.levelops.commons.utils.MapUtils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScmFilterUtils {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<String> getListOrDefault(Map<String, Object> filter, String key) {
        try {
            var value = (Collection) MapUtils.emptyIfNull(filter).getOrDefault(key, Collections.emptyList());
            if (value == null || value.size() < 1){
                return List.of();
            }
            if (value.size() > 0 && !(value.iterator().next() instanceof String)){
                return (List<String>) value.stream().map(i -> i.toString()).collect(Collectors.toList());
            }
            // to handle list or sets
            return (List<String>) value.stream().collect(Collectors.toList());
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

    public static <K, V> Optional<Map<K, V>> getFilterValueAsMap(Map<String, Object> filter, String key) {
        return Optional.ofNullable(filter)
                .map(f -> f.get(key))
                .map(Map.class::cast);
    }
}
