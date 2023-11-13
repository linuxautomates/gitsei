package io.levelops.ingestion.integrations.postgres.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PostgresQuery.PostgresQueryBuilder.class)
public class PostgresQuery implements DataQuery {
    @JsonProperty("integration_key")
    private IntegrationKey integrationKey;

    @JsonProperty("query")
    private String query;
}
