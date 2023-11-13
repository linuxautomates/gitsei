package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for the ticket api (https://developer.zendesk.com/rest_api/docs/support/incremental_export) response
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ExportTicketsResponse.ExportTicketsResponseBuilder.class)
public class ExportTicketsResponse {

    @JsonProperty("tickets")
    List<Ticket> tickets;

    @JsonProperty("after_url")
    String afterUrl;

    @JsonProperty("before_url")
    String beforeUrl;

    @JsonProperty("after_cursor")
    String afterCursor;

    @JsonProperty("before_cursor")
    String beforeCursor;

    @JsonProperty("end_of_stream")
    boolean endOfStream;

    @JsonProperty
    List<User> users; //sideloaded

    @JsonProperty
    List<Group> groups; //sideloaded

    @JsonProperty
    List<Brand> brands; //sideloaded

    @JsonProperty
    List<Organization> organizations; //sideloaded

    @JsonProperty("metric_sets")
    List<TicketMetric> metrics; //sideloaded
}
