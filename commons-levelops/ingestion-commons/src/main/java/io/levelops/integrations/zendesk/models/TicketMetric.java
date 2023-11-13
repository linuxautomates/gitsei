package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

/**
 * Bean definition for zendesk ticket metric (https://developer.zendesk.com/rest_api/docs/support/ticket_metrics)
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TicketMetric.TicketMetricBuilder.class)
public class TicketMetric {

    @JsonProperty
    String url;

    @JsonProperty
    Long id;

    @JsonProperty("ticket_id")
    Long ticketId;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("group_stations")
    Integer groupStations;

    @JsonProperty("assignee_stations")
    Integer assigneeStations;

    @JsonProperty
    Integer reopens;

    @JsonProperty
    Integer replies;

    @JsonProperty("assignee_updated_at")
    Date assigneeUpdatedAt;

    @JsonProperty("requester_updated_at")
    Date requesterUpdatedAt;

    @JsonProperty("status_updated_at")
    Date statusUpdatedAt;

    @JsonProperty("initially_assigned_at")
    Date initiallyAssignedAt;

    @JsonProperty("assigned_at")
    Date assignedAt;

    @JsonProperty("solved_at")
    Date solvedAt;

    @JsonProperty("last_comment_added_at")
    Date lastCommentAddedAt;

    @JsonProperty("reply_time_in_minutes")
    ZendeskDuration replyTimeInMins;

    @JsonProperty("first_resolution_time_in_minutes")
    ZendeskDuration firstResolutionTimeInMins;

    @JsonProperty("full_resolution_time_in_minutes")
    ZendeskDuration fullResolutionTimeInMins;

    @JsonProperty("agent_wait_time_in_minutes")
    ZendeskDuration agentWaitTimeInMins;

    @JsonProperty("requester_wait_time_in_minutes")
    ZendeskDuration requesterWaitTimeInMins;

    @JsonProperty("on_hold_time_in_minutes")
    ZendeskDuration onHoldTimeInMins;

}
