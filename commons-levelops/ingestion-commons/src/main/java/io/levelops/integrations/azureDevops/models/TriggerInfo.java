package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TriggerInfo.TriggerInfoBuilder.class)
public class TriggerInfo {

    @JsonProperty("ci.sourceBranch")
    String sourceBranch;

    @JsonProperty("ci.sourceSha")
    String sourceSha;

    @JsonProperty("ci.message")
    String message;

    @JsonProperty("ci.triggerRepository")
    String triggerRepository;
}
