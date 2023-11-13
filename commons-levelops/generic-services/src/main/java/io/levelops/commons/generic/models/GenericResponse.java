package io.levelops.commons.generic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GenericResponse.GenericResponseBuilder.class)
public class GenericResponse {
    @JsonProperty("response_type")
    private final String responseType;
    @JsonProperty("payload")
    private final String payload;
}