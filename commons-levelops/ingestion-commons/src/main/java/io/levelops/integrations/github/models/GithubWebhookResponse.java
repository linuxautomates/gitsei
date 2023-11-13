package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubWebhookResponse.GithubWebhookResponseBuilder.class)
public class GithubWebhookResponse {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("url")
    String url;

    @JsonProperty("ping_url")
    String pingUrl;

    @JsonProperty("name")
    String name;

    @JsonProperty("events")
    List<String> events;

    @JsonProperty("active")
    Boolean active;

    @JsonProperty("config")
    GithubWebhookConfig config;

    @JsonProperty("type")
    String type;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("created_at")
    Date createdAt;
}
