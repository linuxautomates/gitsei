package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.integrations.github.models.GithubWebhookEnrichmentRequests;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubWebhookEnrichQuery.GithubWebhookEnrichQueryBuilder.class)
public class GithubWebhookEnrichQuery implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("enrichment_requests")
    GithubWebhookEnrichmentRequests requests;
}
