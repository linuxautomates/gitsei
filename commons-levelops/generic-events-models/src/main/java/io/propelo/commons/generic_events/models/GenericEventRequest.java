package io.propelo.commons.generic_events.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Value
@JsonDeserialize(builder = GenericEventRequest.GenericEventRequestBuilder.class)
public class GenericEventRequest {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("component")
    private final Component component;

    @JsonProperty("key")
    private final String key;

    @JsonProperty("secondary_key")
    private final String secondaryKey;

    @JsonProperty("event_type")
    private final String eventType;
    @JsonProperty("event_time")
    private final Long eventTime;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    private final Instant updatedAt;

    /*
    Future enhancement
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
     */
}
