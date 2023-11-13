package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TicketMetricEvent.TicketMetricEventBuilder.class)
public class TicketMetricEvent {

    @JsonProperty
    Long id;

    @JsonProperty("ticket_id")
    Long ticketId;

    @JsonProperty
    String metric;

    @JsonProperty("instance_id")
    Long instanceId;

    @JsonProperty
    String type;

    @JsonProperty
    Date time;

    @JsonProperty
    Sla sla;

    @JsonProperty
    ZendeskDuration status;

    @JsonProperty
    Boolean deleted;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Sla.SlaBuilder.class)
    static class Sla {

        @JsonProperty("target")
        Long targetInMins;

        @JsonProperty("business_hours")
        Boolean businessHours;

        @JsonProperty
        Policy policy;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Policy.PolicyBuilder.class)
        static class Policy {

            @JsonProperty
            Long id;

            @JsonProperty
            String title;

            @JsonProperty
            String description;
        }
    }

}
