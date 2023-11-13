package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmComment.HelixSwarmCommentBuilder.class)
public class HelixSwarmComment {

    @JsonProperty("id")
    String id;

    @JsonProperty("attachments")
    List<String> attachments;

    @JsonProperty("body")
    String body;

    @JsonProperty("context")
    List<String> context;

    @JsonProperty("flags")
    List<String> flags;

    @JsonProperty("likes")
    List<String> likes;

    @JsonProperty("taskState")
    String taskState;

    @JsonProperty("time")
    Long time;

    @JsonProperty("topic")
    String topic;

    @JsonProperty("updated")
    Long updatedAt;

    @JsonProperty("user")
    String user;
}
