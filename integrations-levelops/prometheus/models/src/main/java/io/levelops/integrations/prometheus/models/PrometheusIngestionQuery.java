package io.levelops.integrations.prometheus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PrometheusIngestionQuery.PrometheusIngestionQueryBuilder.class)
public class PrometheusIngestionQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("query_request")
    PrometheusRunbookQueryRequest queryRequest;

    @JsonProperty
    boolean update;

    @JsonProperty
    Long id;
}