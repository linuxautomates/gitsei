package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmActivity.HelixSwarmActivityBuilder.class)
public class HelixSwarmActivity {

    @JsonProperty("id")
    Long id;

    @JsonProperty("action")
    String action;

    @JsonProperty("behalfOf")
    String behalfOf;

    @JsonProperty("behalfOfExists")
    Boolean behalfOfExists;

    @JsonProperty("behalfOfFullName")
    String behalfOfFullName;

    @JsonProperty("change")
    Long change;

    @JsonProperty("comments")
    List<Long> comments;

    @JsonProperty("depotFile")
    String depotFile;

    @JsonProperty("description")
    String description;

    @JsonProperty("streams")
    JsonNode streams;

    @JsonProperty("preposition")
    String preposition;

    @JsonProperty("target")
    String target;

    @JsonProperty("time")
    Long time;

    @JsonProperty("topic")
    String topic;

    @JsonProperty("type")
    String type;

    @JsonProperty("url")
    String url;

    @JsonProperty("user")
    String user;

    @JsonProperty("userExists")
    Boolean userExists;

    @JsonProperty("userFullName")
    String userFullName;

}
