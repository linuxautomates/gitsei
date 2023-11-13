package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraBoard.JiraBoardBuilder.class)
public class JiraBoard {

    @JsonProperty("id")
    String id;
    @JsonProperty("self")
    String self;
    @JsonProperty("name")
    String name;
    @JsonProperty("type")
    String type;
    @JsonProperty("location")
    BoardLocation location;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BoardLocation.BoardLocationBuilder.class)
    private static class BoardLocation {
        @JsonProperty("projectId")
        String projectId;
        @JsonProperty("displayName")
        String displayName;
        @JsonProperty("projectName")
        String projectName;
        @JsonProperty("projectKey")
        String projectKey;
        @JsonProperty("projectTypeKey")
        String projectTypeKey;
        @JsonProperty("avatarURI")
        String avatarURI;
        @JsonProperty("name")
        String name;
    }
}
