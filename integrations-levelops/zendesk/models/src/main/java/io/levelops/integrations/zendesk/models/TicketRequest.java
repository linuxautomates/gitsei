package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * Bean defining input variables for creating a ticket
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TicketRequest.TicketRequestBuilder.class)
public class TicketRequest {

    @JsonProperty
    TicketRequestBody.TicketComment comment;

    @JsonProperty
    String subject;

    @JsonProperty
    String priority;

    @JsonProperty
    String status;

    @JsonProperty
    String requesterEmail;

    @JsonProperty
    String assignee;

    @JsonProperty
    String type;

    @JsonProperty
    boolean isGroup;

    @JsonProperty
    List<String> followerEmails;

    @JsonProperty
    Date dueDate;

}
