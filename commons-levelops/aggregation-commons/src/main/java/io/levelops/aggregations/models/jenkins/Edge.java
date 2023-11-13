package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = Edge.EdgeBuilder.class)
public class Edge {
    @JsonProperty("id")
    private final String id;
    @JsonProperty("type")
    private final String type;
}
