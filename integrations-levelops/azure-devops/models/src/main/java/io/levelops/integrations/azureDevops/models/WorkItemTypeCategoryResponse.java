package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemTypeCategoryResponse.WorkItemTypeCategoryResponseBuilder.class)
public class WorkItemTypeCategoryResponse {
    @JsonProperty("count")
    int count;

    @JsonProperty("value")
    List<WorkItemTypeCategory> workItemTypeCategories;
}
