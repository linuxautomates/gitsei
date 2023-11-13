package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ListTicketMetricEventResponse.ListTicketMetricEventResponseBuilder.class)
public class ListTicketMetricEventResponse {

    @JsonProperty("ticket_metric_events")
    List<TicketMetricEvent> ticketMetricEvents;

    @JsonProperty("next_page")
    String nextPage;

    @JsonProperty
    int count;

    @JsonProperty("end_time")
    Long endTime;

}
