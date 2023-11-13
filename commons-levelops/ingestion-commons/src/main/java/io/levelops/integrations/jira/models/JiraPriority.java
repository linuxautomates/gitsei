package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraPriority.JiraPriorityBuilder.class)
public class JiraPriority {

    @JsonProperty("self")
    String self;

    @JsonProperty("statusColor")
    String statusColor;

    @JsonProperty("description")
    String description;

    @JsonProperty("iconUrl")
    String iconUrl;

    @JsonProperty("name")
    String name;

    @JsonProperty("id")
    String id;

    @JsonProperty("priority_order")
    int priorityOrder;
}
