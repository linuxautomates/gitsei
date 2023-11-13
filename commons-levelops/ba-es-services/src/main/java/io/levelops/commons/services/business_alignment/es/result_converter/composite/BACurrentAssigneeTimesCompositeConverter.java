package io.levelops.commons.services.business_alignment.es.result_converter.composite;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.services.business_alignment.es.models.FTEPartial;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.services.business_alignment.es.result_converter.composite.CompositeAggsAfterKeyUtils.formatStringData;

@Value
@Builder(builderMethodName = "hiddenBuilder")
public class BACurrentAssigneeTimesCompositeConverter implements BAESResultConverter {
    private final String categoryName;
    private final ESAggInterval aggInterval;

    public static BACurrentAssigneeTimesCompositeConverterBuilder builder(String categoryName, ESAggInterval aggInterval) {
        return hiddenBuilder().categoryName(categoryName).aggInterval(aggInterval);
    }
    @Override
    public List<FTEPartial> convert(SearchResponse<Void> sr) {
        List<FTEPartial> ftePartial = new ArrayList<>();
        sr.aggregations().get("ba_agg").composite().buckets().array().forEach( ca -> {
            Long effortOrTotalEffort = (long) ca.aggregations().get("nested_w_hist_assignee_statuses").nested().aggregations().get("filter_w_hist_assignee_statuses.issue_status_category").filter().aggregations().get("sum_w_hist_assignee_statuses.hist_assignee_time").sum().value();
            Long interval = Long.parseLong(ca.key().get("interval").toString()) /1000;
            String intervalAsString = aggInterval.formatEpochSeconds(interval);
            ftePartial.add(FTEPartial.builder()
                    .ticketCategory(categoryName)
                    .assigneeId(formatStringData(ca.key().get("assignee_id").toString()))
                    .assigneeName(formatStringData(ca.key().get("assignee_name").toString()))
                    .interval(interval)
                    .intervalAsString(intervalAsString)
                    .effortOrTotalEffort(effortOrTotalEffort)
                    .build());
        });
        return ftePartial;
    }

    @Override
    public Map<String, String> parseAfterKey(SearchResponse<Void> sr) {
        Map<String, JsonData> afterKeyFromSr = sr.aggregations().get("ba_agg").composite().afterKey();
        if(MapUtils.isEmpty(afterKeyFromSr)) {
            return null;
        }
        Map<String, String> afterKey = new HashMap<>();
        afterKey.put("assignee_id", formatStringData(afterKeyFromSr.get("assignee_id").toString()));
        afterKey.put("assignee_name", formatStringData(afterKeyFromSr.get("assignee_name").toString()));
        afterKey.put("interval", afterKeyFromSr.get("interval").toString());
        //afterKey.put("interval_as_string", afterKeyFromSr.get("interval_as_string").toString());
        return afterKey;
    }
}
