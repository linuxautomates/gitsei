package io.levelops.commons.services.business_alignment.es.result_converter.composite;

import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverterFactory;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BACompositeESResultConverterFactory implements BAESResultConverterFactory {
    public BAESResultConverter getConverter(BaJiraOptions.AttributionMode attributionMode, Calculation calculation, ESAggInterval aggInterval, String categoryName){
        switch (attributionMode) {
            case CURRENT_AND_PREVIOUS_ASSIGNEES:
                switch (calculation) {
                    case TICKET_COUNT:
                    case STORY_POINTS:
                        return BAHistAssigneeCountsCompositeConverter.builder(categoryName, aggInterval, calculation).build();
                    case TICKET_TIME_SPENT:
                        return BAHistAssigneeTimesCompositeConverter.builder(categoryName, aggInterval).build();
                    default:
                        throw new RuntimeException("Calculation " + calculation + " not supported!");
                }
            case CURRENT_ASSIGNEE:
                switch (calculation) {
                    case TICKET_COUNT:
                    case STORY_POINTS:
                        return BACurrentAssigneeCountsCompositeConverter.builder(categoryName, aggInterval, calculation).build();
                    case TICKET_TIME_SPENT:
                        return BACurrentAssigneeTimesCompositeConverter.builder(categoryName, aggInterval).build();
                    default:
                        throw new RuntimeException("Calculation " + calculation + " not supported!");
                }
            default:
                throw new RuntimeException("Attribution Mode " + attributionMode + " not supported!");
        }
    }
}
