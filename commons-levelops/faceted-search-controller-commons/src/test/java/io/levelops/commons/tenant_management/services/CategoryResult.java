package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = CategoryResult.CategoryResultBuilder.class)
public class CategoryResult {
    @JsonProperty("assignee_id_sum_story_points_by_month")
    private final AssigneeIdSumStoryPointsByMonth assigneeIdSumStoryPointsByMonth;
}
