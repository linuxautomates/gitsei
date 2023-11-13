package io.levelops.commons.services.business_alignment.es.result_converter.terms;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.services.business_alignment.es.models.FTEPartial;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
@Builder(builderMethodName = "hiddenBuilder")
public class BACurrentAssigneeCountsConverter implements BAESResultConverter {
    private final String categoryName;
    private final ESAggInterval aggInterval;
    private final Calculation calculation;

    public static BACurrentAssigneeCountsConverterBuilder builder(String categoryName, ESAggInterval aggInterval, Calculation calculation) {
        return hiddenBuilder().categoryName(categoryName).aggInterval(aggInterval).calculation(calculation);
    }
    @Override
    public List<FTEPartial> convert(SearchResponse<Void> sr) {
        List<FTEPartial> ftePartial = new ArrayList<>();
        sr.aggregations().get("across_w_assignee.id").sterms().buckets().array().forEach(term1 -> {
            term1.aggregations().get("across_w_assignee.display_name").sterms().buckets().array().forEach(term2 -> {
                term2.aggregations().get("date_histo_w_resolved_at").dateHistogram().buckets().array().forEach( term3 -> {
                    Long effortOrTotalEffort = null;
                    if (Calculation.STORY_POINTS.equals(calculation)) {
                        effortOrTotalEffort = (long) term3.aggregations().get("sum_w_story_points").sum().value();
                    } else if (Calculation.TICKET_COUNT.equals(calculation)) {
                        effortOrTotalEffort = term3.docCount();
                    } else {
                        throw new RuntimeException("Calculation " + calculation + " is not supported!");
                    }
                    ftePartial.add(FTEPartial.builder()
                            .ticketCategory(categoryName)
                            .assigneeId(term1.key())
                            .assigneeName(term2.key())
                            .interval(term3.key().toInstant().getEpochSecond())
                            .intervalAsString(term3.keyAsString())
                            .effortOrTotalEffort(effortOrTotalEffort)
                            .build());
                });
            });
        });
        return ftePartial;
    }

    @Override
    public Map<String, String> parseAfterKey(SearchResponse<Void> sr) {
        return null;
    }
}
