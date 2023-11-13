package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Bean for the response of create/update ticket
 * (https://developer.zendesk.com/rest_api/docs/support/tickets#create-ticket) api
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WriteTicketResponse.WriteTicketResponseBuilder.class)
public class WriteTicketResponse {

    @JsonProperty
    Ticket ticket;
}
