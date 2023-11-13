package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = SumStoryPointsByMonth.SumStoryPointsByMonthBuilder.class)
public class SumStoryPointsByMonth {
    @JsonProperty("buckets")
    private final List<InnerBucket> buckets;
}
