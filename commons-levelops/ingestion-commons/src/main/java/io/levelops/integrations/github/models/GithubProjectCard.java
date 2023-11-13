package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubProjectCard.GithubProjectCardBuilder.class)
public class GithubProjectCard {

    @JsonProperty("url")
    String url;

    @JsonProperty("column_id")
    String columnId;
    
    @JsonProperty("id")
    String id;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("note")
    String note;

    @JsonProperty("creator")
    GithubCreator creator;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("column_url")
    String columnUrl;

    @JsonProperty("content_url")
    String contentUrl;

    @JsonProperty("project_url")
    String projectUrl;

    @JsonProperty("archived")
    Boolean archived;

    @JsonProperty("after_id")
    String afterId;
}
