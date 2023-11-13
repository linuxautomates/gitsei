package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmUser.HelixSwarmUserBuilder.class)
public class HelixSwarmUser {

    @JsonProperty("User")
    String user;

    @JsonProperty("Type")
    String type;

    @JsonProperty("Email")
    String email;

    @JsonProperty("Updated")
    Date updatedAt;

    @JsonProperty("Accessed")
    Date accessedAt;

    @JsonProperty("FullName")
    String fullName;
}
