package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.GithubAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.ProductMappingService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@SuppressWarnings("unused")
public class GithubAggregationsController implements AggregationsController<AppAggMessage> {
    private static final String AGGREGATION_VERSION = "V0.1";
    private static final String WEBHOOK_CONTROLLER = "GithubWebhookController";
    private static final String PROJECT_CARD_EVENT = "project_card";

    private final GithubAggHelper tableHelper;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final GitRepositoryService gitRepositoryService;
    private final EventsClient eventsClient;
    private final String subscriptionName;
    private final String serverApiUrl;
    private final ProductMappingService productMappingService;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public GithubAggregationsController(Storage storage,
                                        IntegrationService integrationService,
                                        GithubAggHelper helper,
                                        ControlPlaneService controlPlaneService,
                                        @Value("${GITHUB_AGG_SUB:dev-github-sub}") String gitSub,
                                        @Value("${GOOGLE_CLOUD_PROJECT}") String projectName,
                                        @Value("${SERVERAPI_URL}") String serverApiUrl,
                                        GitRepositoryService gitRepositoryService,
                                        AggregationsDatabaseService aggregationsDatabaseService,
                                        final EventsClient eventsClient, ProductMappingService productMappingService,
                                        IntegrationTrackingService trackingService) {
        this.subscriptionName = gitSub;
        this.serverApiUrl = serverApiUrl;
        this.tableHelper = helper;
        this.integrationService = integrationService;
        this.gitRepositoryService = gitRepositoryService;
        this.controlPlaneService = controlPlaneService;
        this.eventsClient = eventsClient;
        this.productMappingService = productMappingService;
        this.trackingService = trackingService;
    }

    @Override
    @Async("githubTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Github Agg: {} ", message.getMessageId());
            String customer = message.getCustomer();
            String integrationId = message.getIntegrationId();
            Integration it = integrationService.get(customer, integrationId)
                    .orElse(null);
            if (it == null || IntegrationType.GITHUB != IntegrationType.fromString(it.getApplication())) {
                return;
            }

            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(customer)
                    .integrationId(it.getId())
                    .build();
            List<String> productIds = productMappingService.getProductIds(customer, integrationId);
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(integrationKey,
                    false, false, true);
            tableHelper.processGitCommits(customer, integrationId, results);
            tableHelper.processGitPrs(customer, integrationId, results, productIds);
            tableHelper.updateGitCommitsForDirectMerge(customer, integrationId, results);
            tableHelper.insertGitIssues(customer, integrationId, results);
            tableHelper.processGitProjects(customer, integrationId, results);
            tableHelper.linkIssuesAndProjectCards(customer, integrationId);
            tableHelper.insertGitTags(customer, integrationId, results);
            dispatchCreateWebhookJobRequest(customer, integrationId, it, integrationKey);

            String adminEmail = null;

            // emit event
            try {
                eventsClient.emitEvent(customer, EventType.GITHUB_NEW_AGGREGATION, Map.of("integration_key", integrationKey));
            } catch (EventsClientException e) {
                log.error("Error sending event for tenant={}, eventType={}", customer,
                        EventType.GITHUB_NEW_AGGREGATION, e);
            }
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(DateUtils.truncate(new Date(), Calendar.DATE))
                            .build());
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }

    private void dispatchCreateWebhookJobRequest(String customer, String integrationId, Integration integration,
                                                 IntegrationKey integrationKey) throws IngestionServiceException {
        List<String> organizations = gitRepositoryService.getOrganizations(customer, integrationId);
        String webhookSecret = getWebhookSecret(integration);
        if (CollectionUtils.isNotEmpty(organizations) && StringUtils.isNotEmpty(webhookSecret)) {
            controlPlaneService.submitJob(CreateJobRequest.builder()
                    .controllerName(WEBHOOK_CONTROLLER)
                    .integrationId(integrationId)
                    .tenantId(customer)
                    .query(GithubCreateWebhookQuery.builder()
                            .integrationKey(integrationKey)
                            .organizations(organizations)
                            .secret(webhookSecret)
                            .url(createWebhookUrl(customer, integrationId))
                            .events(List.of(PROJECT_CARD_EVENT))
                            .build())
                    .build());
        }
    }

    private String createWebhookUrl(String customer, String integrationId) {
        return serverApiUrl + "/webhooks/github/" + customer + "/" + integrationId;
    }

    @Nullable
    private String getWebhookSecret(Integration integration) {
        Map<String, Object> metadata = integration.getMetadata();
        String webhookSecret = null;
        if (MapUtils.isNotEmpty(metadata) && metadata.containsKey("__secrets__")) {
            Map<String, String> secret = (Map<String, String>) metadata.get("__secrets__");
            if (MapUtils.isNotEmpty(secret) && secret.containsKey("webhook_secret")) {
                webhookSecret = secret.get("webhook_secret");
            }
        }
        return webhookSecret;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.GITHUB;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

}
