package io.levelops.ingestion.controllers.generic;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;

public interface IntegrationQuery extends DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey getIntegrationKey();

}
