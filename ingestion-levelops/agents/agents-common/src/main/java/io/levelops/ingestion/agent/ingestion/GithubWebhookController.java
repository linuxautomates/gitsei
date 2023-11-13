package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookQuery;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookResult;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubWebhookConfig;
import io.levelops.integrations.github.models.GithubWebhookRequest;
import io.levelops.integrations.github.models.GithubWebhookResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubWebhookController implements DataController<GithubCreateWebhookQuery> {

    private final static String WEBHOOK_NAME = "web";
    private final static String WEBHOOK_CONTENT_TYPE = "json";

    private final ObjectMapper objectMapper;
    private final GithubClientFactory clientFactory;

    @Builder
    public GithubWebhookController(ObjectMapper objectMapper, GithubClientFactory clientFactory) {
        this.objectMapper = objectMapper;
        this.clientFactory = clientFactory;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GithubCreateWebhookQuery query) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            GithubClient githubClient = clientFactory.get(query.getIntegrationKey(), false);
            GithubWebhookRequest createWebhook = GithubWebhookRequest.builder()
                    .name(WEBHOOK_NAME)
                    .config(GithubWebhookConfig.builder()
                            .contentType(WEBHOOK_CONTENT_TYPE)
                            .url(query.getUrl())
                            .secret(query.getSecret())
                            .build())
                    .events(query.getEvents())
                    .build();
            List<GithubWebhookResponse> webhooks = new ArrayList<>();
            Map<String, Boolean> resultsMap = new HashMap<>();
            List<String> orgs = getOrgs(githubClient, query);
            for (String org : orgs) {
                GithubWebhookResponse webhook = createOrUpdateWebhooks(org, createWebhook, query, githubClient);
                if (webhook != null) {
                    webhooks.add(webhook);
                    resultsMap.put(org, true);
                } else {
                    resultsMap.put(org, false);
                }
            }
            if (resultsMap.containsValue(false)) {
                log.warn("The result of creating or updating webhook for each organization is: " + resultsMap);
                throw new GithubClientException("Failed to create or update one or more webhooks for integration=" + query.getIntegrationKey());
            }
            return GithubCreateWebhookResult.builder()
                    .webhooks(webhooks)
                    .build();
        } catch (GithubClientException e) {
            throw new IngestException("Failed to create github webhook for integration=" + query.getIntegrationKey(), e);
        }
    }

    @NotNull
    private List<String> getOrgs(GithubClient githubClient, GithubCreateWebhookQuery query) {
        List<String> orgs = query.getOrganizations();
        if (CollectionUtils.isEmpty(orgs)) {
            try {
                orgs = githubClient.streamOrganizations()
                        .map(GithubOrganization::getLogin)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("getOrgs: Could not fetch organisations for IntegrationKey=" + query.getIntegrationKey(), e);
            }
        }
        return orgs;
    }

    private GithubWebhookResponse createOrUpdateWebhooks(String org, GithubWebhookRequest createWebhook,
                                                         GithubCreateWebhookQuery query,
                                                         GithubClient githubClient) {
        log.info("createOrUpdateWebhooks: Fetching all webhooks of organization={} for IntegrationKey={}",
                org, query.getIntegrationKey());
        Stream<GithubWebhookResponse> streamWebhooks = githubClient.streamWebhooks(org, query.getIntegrationKey());
        if (streamWebhooks != null) {
            Map<Boolean, List<GithubWebhookResponse>> webhookGroups = streamWebhooks
                    .filter(webhook -> WEBHOOK_NAME.equalsIgnoreCase(webhook.getName()))
                    .collect(Collectors.groupingBy(webhook -> StringUtils.equalsIgnoreCase(webhook.getConfig().getUrl(),
                            createWebhook.getConfig().getUrl())));
            List<GithubWebhookResponse> existingWebhooks = ObjectUtils.firstNonNull(webhookGroups.get(Boolean.TRUE), List.of());
            if (existingWebhooks.isEmpty()) {
                log.info("createOrUpdateWebhooks: Creating a new webhook for organization={}, IntegrationKey={}",
                        org, query.getIntegrationKey());
                return createOrgWebhook(org, createWebhook, query, githubClient);
            } else {
                log.info("createOrUpdateWebhooks: Updating the requested webhooks of organization={} for IntegrationKey={}",
                        org, query.getIntegrationKey());
                Optional<GithubWebhookResponse> githubWebhookResponse = existingWebhooks.stream().findFirst();
                if (githubWebhookResponse.isPresent()) {
                    return updateOrgWebhook(org, createWebhook, query, githubClient, githubWebhookResponse.get());
                }
            }
        }
        return null;
    }

    @Nullable
    private GithubWebhookResponse updateOrgWebhook(String org, GithubWebhookRequest createWebhook,
                                                   GithubCreateWebhookQuery query, GithubClient githubClient,
                                                   GithubWebhookResponse existingWebhook) {
        Integer webhookId = existingWebhook.getId();
        GithubWebhookResponse webhook = null;
        try {
            webhook = githubClient.updateWebhook(org, webhookId, createWebhook);
            log.info("Updated the webhook id={} with requested configuration in the organization={} "
                    + "for IntegrationKey = {}", webhook.getId(), org, query.getIntegrationKey());
        } catch (GithubClientException e) {
            log.warn("Failed to update Webhook for Integration=" + query.getIntegrationKey(), e);
        }
        return webhook;
    }

    @Nullable
    private GithubWebhookResponse createOrgWebhook(String org, GithubWebhookRequest createWebhook,
                                                   GithubCreateWebhookQuery query, GithubClient githubClient) {
        GithubWebhookResponse webhook = null;
        try {
            webhook = githubClient.createWebhook(org, createWebhook);
            log.info("Created the new webhook id={} with requested configuration in the organization={} "
                    + "for IntegrationKey = {}", webhook.getId(), org, query.getIntegrationKey());
        } catch (GithubClientException e) {
            log.warn("Failed to create Webhook for Integration=" + query.getIntegrationKey(), e);
        }
        return webhook;
    }

    @Override
    public GithubCreateWebhookQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GithubCreateWebhookQuery.class);
    }

}
