package io.levelops.commons.services.business_alignment.es.result_converter.composite;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.services.business_alignment.es.models.FTEPartial;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import io.levelops.commons.services.business_alignment.models.Calculation;
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
public class BACurrentAssigneeCountsCompositeConverter implements BAESResultConverter {
    private final String categoryName;
    private final ESAggInterval aggInterval;
    private final Calculation calculation;

    public static BACurrentAssigneeCountsCompositeConverterBuilder builder(String categoryName, ESAggInterval aggInterval, Calculation calculation) {
        return hiddenBuilder().categoryName(categoryName).aggInterval(aggInterval).calculation(calculation);
    }
    @Override
    public List<FTEPartial> convert(SearchResponse<Void> sr) {
        List<FTEPartial> ftePartial = new ArrayList<>();
        sr.aggregations().get("ba_agg").composite().buckets().array().forEach( ca -> {
            Long effortOrTotalEffort = null;
            if (Calculation.STORY_POINTS.equals(calculation)) {
                effortOrTotalEffort = (long) ca.aggregations().get("sum_w_story_points").sum().value();
            } else if (Calculation.TICKET_COUNT.equals(calculation)) {
                effortOrTotalEffort = ca.docCount();
            } else {
                throw new RuntimeException("Calculation " + calculation + " is not supported!");
            }
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
