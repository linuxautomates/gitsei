package io.propelo.trellis_framework.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= EventResult.EventResultBuilder.class)
public class EventResult {
    @JsonProperty("event_id")
    private final UUID eventId;
    @JsonProperty("event_type")
    private final EventType eventType;

    @JsonProperty("status")
    private final JobStatus status;

    @JsonProperty("error")
    private final Map<String, Object> error; // critical error

}
