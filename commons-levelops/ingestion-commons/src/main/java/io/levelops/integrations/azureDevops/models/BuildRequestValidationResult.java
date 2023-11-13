package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildRequestValidationResult.BuildRequestValidationResultBuilder.class)
public class BuildRequestValidationResult {

    @JsonProperty("message")
    String message;

    @JsonProperty("result")
    String result;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ValidationResult.ValidationResultBuilder.class)
    public static class ValidationResult {

        @JsonProperty("error")
        String error;

        @JsonProperty("ok")
        String ok;

        @JsonProperty("warning")
        String warning;
    }
}