package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmReviewInfo.HelixSwarmReviewInfoBuilder.class)
public class HelixSwarmReviewInfo {

    @JsonProperty("id")
    Long id;

    @JsonProperty("author")
    String author;

    @JsonProperty("changes")
    List<Long> changes;

    @JsonProperty("commits")
    List<Long> commits;

    @JsonProperty("created")
    Date createdAt;

    @JsonProperty("description")
    String description;

    @JsonProperty("groups")
    List<String> groups;

    @JsonProperty("participants")
    JsonNode participants;

    @JsonProperty("pending")
    Boolean pending;

    @JsonProperty("state")
    String state;

    @JsonProperty("stateLabel")
    String stateLabel;

    @JsonProperty("type")
    String type;

    @JsonProperty("updated")
    Long updatedAt;

    @JsonProperty("versions")
    List<HelixSwarmChange> versions;
}
