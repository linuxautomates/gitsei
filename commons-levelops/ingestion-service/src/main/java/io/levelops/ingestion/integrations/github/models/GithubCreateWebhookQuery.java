package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubCreateWebhookQuery.GithubCreateWebhookQueryBuilder.class)
public class GithubCreateWebhookQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("organizations")
    List<String> organizations;

    @JsonProperty("secret")
    String secret;

    @JsonProperty("url")
    String url;

    @JsonProperty("events")
    List<String> events;
}
