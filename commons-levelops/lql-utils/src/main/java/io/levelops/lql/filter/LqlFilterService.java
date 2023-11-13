package io.levelops.lql.filter;

import com.fasterxml.jackson.databind.JsonNode;
import io.levelops.commons.models.ContentType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LqlFilterService {

    private final Map<ContentType, LqlFilter> filters;

    /**
     * If using Spring, auto-wire all lql filters in this constructor.
     */
    public LqlFilterService(List<LqlFilter> lqlFilters) {
        filters = lqlFilters.stream()
                .filter(l -> l.getContentType() != null)
                .collect(Collectors.toMap(
                        LqlFilter::getContentType,
                        Function.identity()));
    }

    public boolean isFilterable(ContentType contentType) {
        return filters.containsKey(contentType);
    }

    public boolean filter(String lql, ContentType contentType, JsonNode data) {
        if (!isFilterable(contentType)) {
            throw new UnsupportedOperationException(String.format("Filtering data not supported for contentType=%s", contentType));
        }
        return filters.get(contentType).eval(lql, data);
    }

}
