package io.propelo.commons.generic_events.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Builder(toBuilder = true)
@Value
@JsonDeserialize(builder = GenericEventResponse.GenericEventResponseBuilder.class)
public class GenericEventResponse {
    @JsonProperty("id")
    private final String id;
}
