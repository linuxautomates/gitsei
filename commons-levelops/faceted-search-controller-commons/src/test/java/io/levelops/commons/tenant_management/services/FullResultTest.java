package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FullResultTest {
    ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void test() throws IOException {
        String data = ResourceUtils.getResourceAsString("del.json");
        FullResult fullResult = MAPPER.readValue(data, FullResult.class);
        Assert.assertNotNull(fullResult);

        Map<String, Map<String, Map<String, Integer>>> result = new HashMap<>();
        for (String category : fullResult.getAggregations().keySet()) {
            CategoryResult cr = fullResult.getAggregations().get(category);
            for (OuterBuckets outerBuckets : cr.getAssigneeIdSumStoryPointsByMonth().getBuckets()) {
                String key = outerBuckets.getKey();

                for(InnerBucket innerBucket : outerBuckets.getSumStoryPointsByMonthReverse().getSum_story_points_by_month().getBuckets()) {
                    String interval = innerBucket.getKey_as_string();
                    Integer count = innerBucket.getDoc_count();
                    result.computeIfAbsent(key, k -> new HashMap<>()).computeIfAbsent(interval, k -> new HashMap<>()).put(category, count);
                }
            }
        }

        Assert.assertNotNull(result);
        String out = DefaultObjectMapper.writeAsPrettyJson(result);
        Assert.assertNotNull(out);

        /*
        CategoryResult cr = CategoryResult.builder()
                .assigneeIdSumStoryPointsByMonth(
                        AssigneeIdSumStoryPointsByMonth.builder()
                                .buckets(
                                        List.of(
                                                OuterBuckets.builder()
                                                        .key("b693dac8-e522-495f-b5e4-5488042d9ca8")
                                                        .sumStoryPointsByMonthReverse(
                                                                SumStoryPointsByMonthReverse.builder()
                                                                        .sum_story_points_by_month(
                                                                                SumStoryPointsByMonth.builder()
                                                                                        .buckets(
                                                                                                List.of(
                                                                                                        InnerBucket.builder()
                                                                                                                .key_as_string("2023-01").key(1672531200000l).doc_count(85)
                                                                                                                .build()
                                                                                                )
                                                                                        )
                                                                                        .build()
                                                                        )
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
                .build();
        Map<String, CategoryResult> map = new HashMap<>();
        map.put("All", cr);
        FullResult fr = FullResult.builder()
                .aggregations(map)
                .build();
        String out = MAPPER.writeValueAsString(fr);
        Assert.assertNotNull(out);
         */
    }
}