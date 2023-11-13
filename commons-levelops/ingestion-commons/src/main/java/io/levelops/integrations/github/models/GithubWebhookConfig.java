package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubWebhookConfig.GithubWebhookConfigBuilder.class)
public class GithubWebhookConfig {

    @JsonProperty("url")
    String url;

    @JsonProperty("content_type")
    String contentType;

    @JsonProperty("secret")
    String secret;

    @JsonProperty("insecure_ssl")
    String insecureSsl;
}
