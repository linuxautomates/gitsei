package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ZendeskTicketFieldsQuery.ZendeskTicketFieldsQueryBuilder.class)
public class ZendeskTicketFieldsQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;
}
