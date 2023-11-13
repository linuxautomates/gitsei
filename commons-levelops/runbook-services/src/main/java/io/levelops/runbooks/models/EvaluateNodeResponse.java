package io.levelops.runbooks.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.runbooks.RunbookError;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EvaluateNodeResponse.EvaluateNodeResponseBuilder.class)
public class EvaluateNodeResponse {
    @JsonProperty("success")
    Boolean success; // evaluation may be successful regardless of node update's new state
    @JsonProperty("node_update")
    Map<String, Object> nodeUpdate;
    @JsonProperty("error")
    RunbookError error;
}