package io.levelops.ingestion.integrations.custom.rest.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CustomRestCallResult.CustomRestCallResultBuilder.class)
public class CustomRestCallResult implements ControllerIngestionResult {
    @JsonProperty("headers")
    Map<String, List<String>> headers;
    @JsonProperty("code")
    Integer code;
    @JsonProperty("body")
    String body;
    @JsonProperty("json_body")
    Map<String, Object> jsonBody;
}
