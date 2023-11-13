package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

/**
 * Query for the ingesting a ticket to zendesk
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TicketIngestionQuery.TicketIngestionQueryBuilder.class)
public class TicketIngestionQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty
    TicketRequest ticketRequest;

    @JsonProperty
    boolean update;

    @JsonProperty
    Long id;
}
