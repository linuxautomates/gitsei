package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.integrations.github.models.GithubWebhookResponse;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubCreateWebhookResult.GithubCreateWebhookResultBuilder.class)
public class GithubCreateWebhookResult implements ControllerIngestionResult {

    @JsonProperty("webhooks")
    List<GithubWebhookResponse> webhooks;
}
