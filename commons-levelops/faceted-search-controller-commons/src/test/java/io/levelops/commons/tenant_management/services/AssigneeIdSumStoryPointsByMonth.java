package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = AssigneeIdSumStoryPointsByMonth.AssigneeIdSumStoryPointsByMonthBuilder.class)
public class AssigneeIdSumStoryPointsByMonth {
    @JsonProperty("buckets")
    private final List<OuterBuckets> buckets;
}
