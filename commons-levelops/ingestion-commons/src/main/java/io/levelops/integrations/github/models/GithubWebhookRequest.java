package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubWebhookRequest.GithubWebhookRequestBuilder.class)
public class GithubWebhookRequest {

    @JsonProperty("name")
    String name;

    @JsonProperty("events")
    List<String> events;

    @JsonProperty("config")
    GithubWebhookConfig config;
}
