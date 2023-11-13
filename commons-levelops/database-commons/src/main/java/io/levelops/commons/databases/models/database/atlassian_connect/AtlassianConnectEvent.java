package io.levelops.commons.databases.models.database.atlassian_connect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AtlassianConnectEvent.AtlassianConnectEventBuilder.class)
public class AtlassianConnectEvent {
    @JsonProperty("eventType")
    String eventType;

    @JsonProperty("timestamp")
    Instant timestamp;
}
