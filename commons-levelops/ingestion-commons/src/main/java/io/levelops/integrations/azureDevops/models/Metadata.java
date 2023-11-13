package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Metadata.MetadataBuilder.class)
public class Metadata {

    @JsonProperty("workItemCategories")
    List<WorkItemTypeCategory> workItemTypeCategories;

    @JsonProperty("workItemTypes")
    List<WorkItemType> workItemTypes;

    @JsonProperty("workItemTypeStates")
    List<WorkItemTypeState> workItemTypeStates;
}