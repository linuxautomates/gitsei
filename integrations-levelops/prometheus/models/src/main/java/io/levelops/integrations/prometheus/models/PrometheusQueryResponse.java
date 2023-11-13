package io.levelops.integrations.prometheus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PrometheusQueryResponse.PrometheusQueryResponseBuilder.class)
public class PrometheusQueryResponse {

    public enum Status{
        success,
        error;
    }

    @JsonProperty
    Status status;

    @JsonProperty
    PrometheusQueryResult data;

    @JsonProperty("error_type")
    String errorType;

    @JsonProperty
    String error;

    @JsonProperty
    String warning;
}