package io.levelops.bullseye_converter_commons.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Result.ResultBuilder.class)
public class Result {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("standard_output")
    private String standardOutput;
    @JsonProperty("error_output")
    private String errorOutput;
    @JsonProperty("timed_out")
    private boolean timedOut;
    @JsonProperty("exit_code")
    private int exitCode;
}