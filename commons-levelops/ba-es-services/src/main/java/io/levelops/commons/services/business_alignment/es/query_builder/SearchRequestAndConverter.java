package io.levelops.commons.services.business_alignment.es.query_builder;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SearchRequestAndConverter {
    private final SearchRequest searchRequest;
    private final BAESResultConverter converter;
}
