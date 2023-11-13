package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = OuterBuckets.OuterBucketsBuilder.class)
public class OuterBuckets {
    @JsonProperty("key")
    private final String key;
    @JsonProperty("sum_story_points_by_month_reverse")
    private final SumStoryPointsByMonthReverse sumStoryPointsByMonthReverse;
}
