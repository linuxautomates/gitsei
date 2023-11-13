package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TenableMetadataQuery.TenableMetadataQueryBuilder.class)
public class TenableMetadataQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;
}
