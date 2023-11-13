package io.levelops.commons.services.business_alignment.es.result_converter;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.services.business_alignment.es.models.FTEPartial;

import java.util.List;
import java.util.Map;

public interface BAESResultConverter {
    List<FTEPartial> convert(SearchResponse<Void> sr);
    Map<String, String> parseAfterKey(SearchResponse<Void> sr);
}
