package io.levelops.commons.client.graphql;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GraphQlQuery.GraphQlQueryBuilder.class)
public class GraphQlQuery {

    @JsonProperty("query")
    String query;

}
