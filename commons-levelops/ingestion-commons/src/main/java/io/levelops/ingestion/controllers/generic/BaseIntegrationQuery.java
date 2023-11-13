package io.levelops.ingestion.controllers.generic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BaseIntegrationQuery.BaseIntegrationQueryBuilder.class)
public class BaseIntegrationQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    private IntegrationKey integrationKey;

}
