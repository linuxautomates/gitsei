package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import lombok.Builder;
import lombok.Value;

/**
 * {@link ControllerIngestionResult} to be returned for a create/update zendesk ticket job
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ZendeskWriteTicketResult.ZendeskWriteTicketResultBuilder.class)
public class ZendeskWriteTicketResult implements ControllerIngestionResult {

    @JsonProperty
    boolean success;

    @JsonProperty
    Ticket ticket;
}