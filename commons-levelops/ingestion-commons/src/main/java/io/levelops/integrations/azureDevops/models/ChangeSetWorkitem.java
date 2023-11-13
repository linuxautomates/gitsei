package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ChangeSetWorkitem.ChangeSetWorkitemBuilder.class)
public class ChangeSetWorkitem {

    @JsonProperty("webUrl")
    String webUrl;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("title")
    String title;

    @JsonProperty("workItemType")
    String workItemType;

    @JsonProperty("state")
    String state;

    @JsonProperty("assignedTo")
    String assignedTo;

}
