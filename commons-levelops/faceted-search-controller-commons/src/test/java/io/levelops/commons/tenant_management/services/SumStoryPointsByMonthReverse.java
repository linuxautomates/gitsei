package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = SumStoryPointsByMonthReverse.SumStoryPointsByMonthReverseBuilder.class)
public class SumStoryPointsByMonthReverse {
    @JsonProperty("sum_story_points_by_month")
    private final SumStoryPointsByMonth sum_story_points_by_month;
}
