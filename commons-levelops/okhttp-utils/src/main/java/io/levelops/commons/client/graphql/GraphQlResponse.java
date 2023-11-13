package io.levelops.commons.client.graphql;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GraphQlResponse.GraphQlResponseBuilder.class)
public class GraphQlResponse {

    @JsonProperty("data")
    Map<String, Object> data;

    @JsonProperty("errors")
    List<Map<String, Object>> errors;

}
