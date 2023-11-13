package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildReason.BuildReasonBuilder.class)
public class BuildReason {

    @JsonProperty("all")
    String all;

    @JsonProperty("batchedCI")
    String batchedCI;

    @JsonProperty("buildCompletion")
    String buildCompletion;

    @JsonProperty("checkInShelveset")
    String checkInShelveset;

    @JsonProperty("individualCI")
    String individualCI;

    @JsonProperty("manual")
    String manual;

    @JsonProperty("none")
    String none;

    @JsonProperty("pullRequest")
    String pullRequest;

    @JsonProperty("resourceTrigger")
    String resourceTrigger;

    @JsonProperty("schedule")
    String schedule;

    @JsonProperty("scheduleForced")
    String scheduleForced;

    @JsonProperty("triggered")
    String triggered;

    @JsonProperty("userCreated")
    String userCreated;

    @JsonProperty("validateShelveset")
    String validateShelveset;
}
