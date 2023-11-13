package io.levelops.integrations.prometheus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PrometheusWriteQueryResult.PrometheusWriteQueryResultBuilder.class)
public class PrometheusWriteQueryResult implements ControllerIngestionResult {

    @JsonProperty
    boolean success;

    @JsonProperty("prometheus_query_response")
    PrometheusQueryResponse prometheusQueryResponse;
}
