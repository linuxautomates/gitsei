package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmChange.HelixSwarmChangeBuilder.class)
public class HelixSwarmChange {
    //I believe this is not used
    @JsonProperty("change")
    Long change;

    //I believe this is not used
    @JsonProperty("user")
    String user;

    //I believe this is not used
    @JsonProperty("time")
    Long time;

    //I believe this is not used
    @JsonProperty("pending")
    Boolean pending;

    //I believe this is not used
    @JsonProperty("difference")
    Integer difference;

    //I believe this is not used
    @JsonProperty("addChangeMode")
    String addChangeMode;

    @JsonProperty("stream")
    String stream;
}
