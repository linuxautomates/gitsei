package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.AzureDevopsAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class AzureDevopsAggregationController implements AggregationsController<AppAggMessage> {

    private static final String PIPELINE = "pipelines";
    private static final String RELEASE = "releases";
    private static final String COMMIT = "commits";
    private static final String PULLREQUEST = "pullrequests";
    private static final String BUILDS = "builds";
    private static final String ITERATIONS = "iterations";
    private static final String CHANGESETS = "changesets";
    private static final String WORKITEMS = "workitems";
    private static final String WORKITEMS_HISTORY = "workitemshistories";
    private static final String METADATA = "metadata";
    private static final String TEAMS = "teams";
    private static final String TAGS = "tags";

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final AzureDevopsAggHelper azureDevopsAggHelper;
    private final String subscriptionName;
    private final EventsClient eventsClient;
    private final IntegrationTrackingService trackingService;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;
    private final int azureAggFrequency;
    private final WorkItemsService workItemsService;

    @Autowired
    public AzureDevopsAggregationController(@Value("${AZURE_DEVOPS_AGG_SUB:dev-azure-devops-sub}") String subscriptionName,
                                            IntegrationService integrationService, ControlPlaneService controlPlaneService,
                                            AzureDevopsAggHelper azureDevopsAggHelper,
                                            Storage storage,
                                            final EventsClient eventsClient,
                                            IntegrationTrackingService trackingService,
                                            WorkItemFieldsMetaService workItemFieldsMetaService,
                                            @Value("${AZURE_DEVOPS_AGG_FREQUENCY_MINS:30}") int azureAggFrequency, WorkItemsService workItemsService) {
        this.subscriptionName = subscriptionName;
        this.controlPlaneService = controlPlaneService;
        this.azureDevopsAggHelper = azureDevopsAggHelper;
        this.azureDevopsAggHelper.setStorage(storage);
        this.integrationService = integrationService;
        this.eventsClient = eventsClient;
        this.trackingService = trackingService;
        this.workItemFieldsMetaService = workItemFieldsMetaService;
        this.azureAggFrequency = azureAggFrequency;
        this.workItemsService = workItemsService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.AZURE_DEVOPS;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    private boolean shouldStartNextAggs(String company, String integrationId, IntegrationTracker currentIntegrationTracker, Instant currentStartTime) {
        if (currentIntegrationTracker == null) {
            log.info("startNextAgg is true, company = {}, integrationId = {}, currentIntegrationTracker is null", company, integrationId);
            return true;
        }
        if ((currentIntegrationTracker.getLastAggStartedAt() == null) || (currentIntegrationTracker.getLastAggStartedAt().equals(0L))) {
            log.info("startNextAgg is true, company = {}, integrationId = {}, currentIntegrationTracker.getLastAggStartedAt is not init", company, integrationId);
            return true;
        }
        Instant lastAggStartedAt = Instant.ofEpochSecond(currentIntegrationTracker.getLastAggStartedAt());
        if (currentStartTime.isAfter(lastAggStartedAt.plus(azureAggFrequency, ChronoUnit.MINUTES))) {
            log.info("startNextAgg is true, company = {}, integrationId = {}, lastAggStartedAt = {} was before allowed time", company, integrationId, lastAggStartedAt);
            return true;
        } else {
            log.info("startNextAgg is false, company = {}, integrationId = {}, lastAggStartedAt = {} was NOT before allowed time", company, integrationId, lastAggStartedAt);
            return false;
        }
    }

    @Override
    @Async("azureDevopsTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            String integrationId = message.getIntegrationId();
            Integration integration = integrationService.get(message.getCustomer(), integrationId).orElse(null);
            if (integration == null ||
                    IntegrationType.AZURE_DEVOPS != IntegrationType.fromString(integration.getApplication())) {
                log.error("Invalid integration Id or message Id");
                return;
            }
            log.debug("Found integration type :" + integration.getApplication());

            Instant currentStartTime = new Date().toInstant();
            IntegrationTracker currentIntegrationTracker = trackingService.get(message.getCustomer(), integrationId).orElse(null);
            if (!shouldStartNextAggs(message.getCustomer(), integrationId, currentIntegrationTracker, currentStartTime)) {
                log.info("doTask: Azure Devops skipping aggs shouldStartNextAggs returned false. company = {}, integrationId = {}", message.getCustomer(), integrationId);
                return;
            }
            int affectedRows = trackingService.updateLastAggStarted(message.getCustomer(), Integer.parseInt(integrationId), currentStartTime.getEpochSecond());
            log.info("updated last_agg_started_at customer = {}, integration_id = {}, last_agg_started_at = {}, affectedRows = {}", message.getCustomer(), integrationId, currentStartTime.getEpochSecond(), affectedRows);
            Long ingestedAt = null;
            boolean aggSuccess = false;
            try {
                MultipleTriggerResults multipleTriggerResults = controlPlaneService
                        .getAllTriggerResults(IntegrationKey.builder()
                                        .tenantId(message.getCustomer())
                                        .integrationId(integration.getId()).build(),
                                false, false, true);
                IntegrationConfig config = integrationService.listConfigs(message.getCustomer(), List.of(integration.getId()),
                        0, 1).getRecords().stream().findFirst().orElse(null);
                List<DbWorkItemField> customFields = workItemFieldsMetaService.listByFilter(
                        message.getCustomer(), List.of(message.getIntegrationId()),
                        null, null, null, null, null, null,
                        0, 1000000).getRecords();
                List<IntegrationConfig.ConfigEntry> aggCustomFields = (config == null || config.getConfig() == null)
                        ? null : config.getConfig().get("agg_custom_fields");
                Date currentTime = new Date();
                ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
                azureDevopsAggHelper.setupAzureDevopsFields(message.getCustomer(), integrationId, multipleTriggerResults);
                log.info("doTask: processed Azure Devops Fields, integration {}, message={}", integrationId, message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsPipelineRuns(message.getCustomer(),
                        integrationId, message.getProductId(), multipleTriggerResults, currentTime, PIPELINE);
                log.info("doTask: ingested Azure Devops PipelineRuns Aggregation, integration {}, message={}",
                        integrationId, message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsReleases(message.getCustomer(),
                        integrationId, message.getProductId(), multipleTriggerResults, currentTime, RELEASE);
                log.info("doTask: ingested Azure Devops Releases Aggregation, integration {}, message={}",
                        integrationId, message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsBuilds(message.getCustomer(),
                        integrationId, multipleTriggerResults, currentTime, BUILDS);
                log.info("doTask: ingested Azure Devops Builds Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsCommits(message.getCustomer(),
                        integrationId, multipleTriggerResults, currentTime, COMMIT);
                log.info("doTask: ingested Azure Devops Commits Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsPullRequests(message.getCustomer(),
                        integrationId, multipleTriggerResults, PULLREQUEST);
                log.info("doTask: ingested Azure Devops PullRequests Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsChangesets(message.getCustomer(),
                        integrationId, multipleTriggerResults, currentTime, CHANGESETS);
                log.info("doTask: ingested Azure Devops Changesets Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsWorkItemsTimelines(message.getCustomer(),
                        NumberUtils.toInteger(integrationId), multipleTriggerResults, WORKITEMS_HISTORY, currentTime);
                log.info("doTask: ingested Azure Devops WorkItems Timeline Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsIterations(message.getCustomer(),
                        integrationId, multipleTriggerResults, ITERATIONS, currentTime);
                log.info("doTask: ingested Azure Devops Iterations Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsWorkItemsMetadata(message.getCustomer(),
                        integrationId, multipleTriggerResults, METADATA);
                log.info("doTask: ingested Azure Devops WorkItemsMetadata Aggregation," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                //Work Item Issue has status which has associated status category. Status category is fetched in Work Item Metadata. So METADATA should be persisted before WORKITEMS
                azureDevopsAggHelper.setupAzureDevopsWorkItems(message.getCustomer(), integrationId,
                        currentTime, multipleTriggerResults, WORKITEMS, aggCustomFields, customFields, getStoryPointsFieldFromConfig(config).orElse(null));
                log.info("doTask: ingested Azure Devops WorkItems Aggregation, integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsTeams(message.getCustomer(),
                        integrationId, multipleTriggerResults, TEAMS);
                log.info("doTask: ingested Azure Devops Teams ," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.setupAzureDevopsTags(message.getCustomer(),
                        integrationId, multipleTriggerResults, TAGS);
                log.info("doTask: ingested Azure Devops tags ," + " integration {}, message={}", integrationId,
                        message.getMessageId());
                azureDevopsAggHelper.updateTimelineZeroStartDates(message.getCustomer(), integrationId, ingestedAt);
                log.info("doTask: updated Azure Devops Timeline zero start dates, company={}, integration={}, message={}", message.getCustomer(), integrationId,
                        message.getMessageId());
                aggSuccess = true;
                //events
                eventsClient.emitEvent(message.getCustomer(), EventType.AZURE_DEVOPS_NEW_AGGREGATON, Map.of());

                //cleanup data older than 91 days.
                log.info("cleaning up data: issues count - {}",
                        workItemsService.cleanUpOldData(message.getCustomer(),
                                currentStartTime.getEpochSecond(),
                                86400 * 91L));

            } catch (Throwable e) {
                log.error("doTask: Error in aggregating results for integration type :" +
                        integration.getApplication(), e);
            } finally {
                log.info("Aggregations completed with {} for  company {}, integration {}, message={}", aggSuccess ? "no failures" : "failures",
                        message.getCustomer(), integrationId, message.getMessageId());
                try {
                    trackingService.upsertJiraWIDBAggregatedAt(message.getCustomer(), Integer.parseInt(integrationId), ingestedAt);
                } catch (SQLException e) {
                    log.error("Error upserting latest_aggregated_at!, company {}, integrationId {}, ingestedAt {}", message.getCustomer(), integrationId, ingestedAt,  e);
                }
                log.info("doTask: completed azure devops aggregation for company {}, integration {}, message={}",
                        message.getCustomer(), integrationId, message.getMessageId());
            }
        } finally {
            LoggingUtils.clearThreadLocalContext();
        }
    }

    private Optional<String> getStoryPointsFieldFromConfig(IntegrationConfig config) {
        return getFieldKeyFromConfig(config, "story_points_field");
    }

    private Optional<String> getFieldKeyFromConfig(@Nullable IntegrationConfig config, String fieldKeyInConfig) {
        return Optional.ofNullable(config)
                .map(IntegrationConfig::getConfig)
                .map(map -> map.get(fieldKeyInConfig))
                .filter(CollectionUtils::isNotEmpty)
                .map(entries -> entries.get(0).getKey())
                .stream()
                .findFirst();
    }
}
