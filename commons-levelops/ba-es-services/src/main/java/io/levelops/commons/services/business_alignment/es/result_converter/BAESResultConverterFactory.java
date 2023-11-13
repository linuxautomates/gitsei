package io.levelops.commons.services.business_alignment.es.result_converter;

import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.faceted_search.querybuilders.ESAggInterval;

public interface BAESResultConverterFactory {
    BAESResultConverter getConverter(BaJiraOptions.AttributionMode attributionMode, Calculation calculation, ESAggInterval aggInterval, String categoryName);
}
