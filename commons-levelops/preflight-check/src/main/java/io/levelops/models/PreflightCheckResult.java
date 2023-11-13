package io.levelops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PreflightCheckResult.PreflightCheckResultBuilder.class)
public class PreflightCheckResult {

    @JsonProperty("success")
    Boolean success;

    @JsonProperty("name")
    String name;

    @JsonProperty("error")
    String error;

    @JsonProperty("exception")
    String exception;

    @JsonProperty("warning")
    String warning;
}
