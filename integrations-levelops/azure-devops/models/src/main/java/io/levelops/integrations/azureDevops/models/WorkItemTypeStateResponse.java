package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemTypeStateResponse.WorkItemTypeStateResponseBuilder.class)
public class WorkItemTypeStateResponse {

    @JsonProperty("count")
    int count;

    @JsonProperty("value")
    List<WorkItemTypeState> workItemTypeStates;
}
