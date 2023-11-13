package io.levelops.commons.services.business_alignment.es.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FTEPartial.FTEPartialBuilder.class)
public class FTEPartial {
    @JsonProperty("ticket_category")
    private String ticketCategory;
    @JsonProperty("assignee_id")
    private String assigneeId;
    @JsonProperty("assignee_name")
    private String assigneeName;
    @JsonProperty("interval")
    private Long interval;
    @JsonProperty("interval_as_string")
    private String intervalAsString;
    @JsonProperty("effort_or_total_effort")
    private Long effortOrTotalEffort;
}
