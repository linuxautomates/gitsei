package io.levelops.api.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.models.PreflightCheckResults;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PreflightCheckFailedResponse.PreflightCheckFailedResponseBuilder.class)
public class PreflightCheckFailedResponse {

    @JsonProperty("timestamp")
    Long timestamp;
    @JsonProperty("status")
    Integer status;
    @JsonProperty("error")
    String error;
    @JsonProperty("message")
    String message;
    @JsonProperty("preflight_check")
    PreflightCheckResults preflightCheck;
    @JsonProperty("path")
    String path;

}
