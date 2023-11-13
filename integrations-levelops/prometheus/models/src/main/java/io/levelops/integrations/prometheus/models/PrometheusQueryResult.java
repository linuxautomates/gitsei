package io.levelops.integrations.prometheus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PrometheusQueryResult.PrometheusQueryResultBuilder.class)
public class PrometheusQueryResult {

    @JsonProperty("result_type")
    String resultType;

    @JsonProperty
    List<JsonNode> result;

}
