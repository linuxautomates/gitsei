package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IterationResponse.IterationResponseBuilder.class)
public class IterationResponse {

    @JsonProperty("count")
    int count;

    @JsonProperty("value")
    List<Iteration> iterations;
}