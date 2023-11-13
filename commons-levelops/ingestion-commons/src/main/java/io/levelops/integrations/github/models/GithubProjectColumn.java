package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubProjectColumn.GithubProjectColumnBuilder.class)
public class GithubProjectColumn {

    @JsonProperty("url")
    String url;

    @JsonProperty("project_url")
    String projectUrl;

    @JsonProperty("cards_url")
    String cardsUrl;

    @JsonProperty("id")
    String id;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("name")
    String name;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("cards")
    List<GithubProjectCard> cards;

}