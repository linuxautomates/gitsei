package io.levelops.integrations.prometheus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PrometheusQueryRequest.PrometheusQueryRequestBuilder.class)
public class PrometheusQueryRequest implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("query_string")
    String queryString;

    @JsonProperty("start_time")
    Date startTime;

    @JsonProperty("end_time")
    Date endTime;

    @JsonProperty("step")
    String step;
}
