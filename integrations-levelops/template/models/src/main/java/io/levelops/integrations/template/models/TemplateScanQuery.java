package io.levelops.ingestion.integrations.template.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TemplateScanQuery.TemplateScanQueryBuilder.class)
public class TemplateScanQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    private IntegrationKey integrationKey;
    @JsonProperty("from")
    private Date from;
    @JsonProperty("to")
    private Date to;
}