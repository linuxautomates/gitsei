package io.levelops.runbooks.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EvaluateNodeRequest.EvaluateNodeRequestBuilder.class)
public class EvaluateNodeRequest {
    @JsonProperty("node_type")
    String nodeType;
    @JsonProperty("input")
    Map<String, RunbookVariable> input;
    @JsonProperty("options")
    EvalOptionsDTO options;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EvalOptionsDTO.EvalOptionsDTOBuilder.class)
    public static class EvalOptionsDTO {
        @JsonProperty("read_only")
        Boolean readOnly;
    }
}