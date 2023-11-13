package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemTypeCategory.WorkItemTypeCategoryBuilder.class)
public class WorkItemTypeCategory {

    @JsonProperty("defaultWorkItemType")
    WorkItemTypeReference defaultWorkItemType;

    @JsonProperty("name")
    String name;

    @JsonProperty("referenceName")
    String referenceName;

    @JsonProperty("url")
    String url;

    @JsonProperty("workItemTypes")
    List<WorkItemTypeReference> workItemTypeReferences;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemTypeReference.WorkItemTypeReferenceBuilder.class)
    private static class WorkItemTypeReference{
        @JsonProperty("name")
        String name;

        @JsonProperty("url")
        String url;
    }
}
