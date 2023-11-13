package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class ESRequest {
    private final List<Query> must;

    private final List<Query> mustNot;

    private final List<Query> should;

    private final Map<String, Aggregation> aggs;
}
