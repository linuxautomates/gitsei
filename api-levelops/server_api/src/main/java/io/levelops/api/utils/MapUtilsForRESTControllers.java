package io.levelops.api.utils;

import org.apache.commons.collections4.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtilsForRESTControllers {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<String> getListOrDefault(Map<String, Object> filter, String key) {
        try {
            var value = (Collection) MapUtils.emptyIfNull(filter).getOrDefault(key, List.of());
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
    
}
