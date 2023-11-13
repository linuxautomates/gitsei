package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryFilter {
    @Singular
    Map<String, Object> strictMatches;
    @Singular
    Map<String, Object> partialMatches;

    Boolean count; // include total count in results

    @SuppressWarnings("unchecked")
    public static final QueryFilter fromRequestFilters(final Map<String, Object> requestFilters){
        var partial = Map.<String, Object>of();
        var strict = new HashMap<>(requestFilters);
        var tmp = strict.remove("partial");
        if(tmp != null && tmp instanceof Map){
            partial = (Map<String, Object>) tmp;
        }
        return QueryFilter.builder().partialMatches(partial).strictMatches(strict).build();
    }
}