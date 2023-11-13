package io.levelops.ingestion.integrations.snyk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykQuery.SnykQueryBuilder.class)
public class SnykQuery implements IntegrationQuery, DataQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;
}