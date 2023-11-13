package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Bean definition for all zendesk duration type fields
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ZendeskDuration.ZendeskDurationBuilder.class)
public class ZendeskDuration {

    @JsonProperty("calendar")
    Integer calendarTimeInMins;

    @JsonProperty("business")
    Integer businessTimeInMins;
}
