package io.levelops.commons.services.business_alignment.es.result_converter.terms;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.services.business_alignment.es.models.FTEPartial;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
@Builder(builderMethodName = "hiddenBuilder")
public class BACurrentAssigneeTimesConverter implements BAESResultConverter {
    private final String categoryName;
    private final ESAggInterval aggInterval;

    public static BACurrentAssigneeTimesConverterBuilder builder(String categoryName, ESAggInterval aggInterval) {
        return hiddenBuilder().categoryName(categoryName).aggInterval(aggInterval);
    }
    @Override
    public List<FTEPartial> convert(SearchResponse<Void> sr) {
        List<FTEPartial> ftePartial = new ArrayList<>();
        sr.aggregations().get("across_w_assignee.id").sterms().buckets().array().forEach(term1 -> {
            term1.aggregations().get("across_w_assignee.display_name").sterms().buckets().array().forEach(term2 -> {
                term2.aggregations().get("date_histo_w_resolved_at").dateHistogram().buckets().array().forEach( term3 -> {
                    Long effortOrTotalEffort = (long) term3.aggregations().get("nested_w_hist_assignee_statuses").nested().aggregations().get("filter_w_hist_assignee_statuses.issue_status_category").filter().aggregations().get("sum_w_hist_assignee_statuses.hist_assignee_time").sum().value();
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
