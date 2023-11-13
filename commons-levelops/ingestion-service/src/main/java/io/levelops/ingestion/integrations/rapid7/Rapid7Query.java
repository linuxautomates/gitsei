package io.levelops.ingestion.integrations.rapid7;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Rapid7Query.Rapid7QueryBuilder.class)
public class Rapid7Query implements IntegrationQuery, DataQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;
}