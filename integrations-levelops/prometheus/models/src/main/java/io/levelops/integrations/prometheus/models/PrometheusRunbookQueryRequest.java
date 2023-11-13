package io.levelops.integrations.prometheus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PrometheusRunbookQueryRequest.PrometheusRunbookQueryRequestBuilder.class)
public class PrometheusRunbookQueryRequest {

    @JsonProperty("query_string")
    String queryString;

    @JsonProperty("is_instant")
    Boolean isInstant;

    @JsonProperty("start_time")
    Date startTime;

    @JsonProperty("end_time")
    Date endTime;

    @JsonProperty("step")
    String step;
}
