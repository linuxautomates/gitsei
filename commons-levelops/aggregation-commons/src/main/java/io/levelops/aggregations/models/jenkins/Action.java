package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonDeserialize(builder = Action.ActionBuilder.class)
public class Action {
    @JsonProperty("description")
    private final String description;

    @JsonProperty("link")
    private final Link link;
}
