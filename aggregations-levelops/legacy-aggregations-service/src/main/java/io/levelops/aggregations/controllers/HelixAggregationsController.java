package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.HelixCoreAggHelper;
import io.levelops.aggregations.helpers.HelixSwarmAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.events.clients.EventsClient;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AggregationsController<AppAggMessage>} for helix
 */
@Service
@Log4j2
public class HelixAggregationsController implements AggregationsController<AppAggMessage> {

    private final HelixCoreAggHelper helixCoreTableHelper;
    private final HelixSwarmAggHelper helixSwarmTableHelper;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;
    private final EventsClient eventsClient;

    @Autowired
    public HelixAggregationsController(IntegrationService integrationService,
                                       HelixCoreAggHelper helper,
                                       HelixSwarmAggHelper helixSwarmTableHelper,
                                       ControlPlaneService controlPlaneService,
                                       final EventsClient eventsClient,
                                       @Value("${HELIX_AGG_SUB:dev-helix-agg-sub}") String subscriptionName) {
        this.subscriptionName = subscriptionName;
        this.helixCoreTableHelper = helper;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.eventsClient = eventsClient;
        this.helixSwarmTableHelper = helixSwarmTableHelper;
    }

    @Override
    @Async("helixTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Helix Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId())
                    .orElse(null);
            if (it == null || IntegrationType.HELIX != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);
            IntegrationConfig config = integrationService
                    .listConfigs(message.getCustomer(), List.of(it.getId()), 0, 1)
                    .getRecords().stream().findFirst().orElse(null);
            List<IntegrationConfig.RepoConfigEntry> repoConfig = config == null ? null : config.getRepoConfig();
            Date ingestedAt = new Date();
            if (!helixCoreTableHelper.insertHelixCoreCommits(
                    message.getCustomer(), message.getIntegrationId(),
                    repoConfig,
                    results, ingestedAt, it.getMetadata())) {
                log.warn("doTask: Insert Helix Core commits aggregation failed" +
                        " for integration id {} and message id {}", it.getId(), message.getMessageId());
            }
            if (!helixSwarmTableHelper.setupHelixSwarmReviews(message.getMessageId(), message.getCustomer(), message.getIntegrationId(), repoConfig,
                    results, ingestedAt)) {
                log.warn("doTask: Failed to setup helix swarm review aggregation");
            }
            eventsClient.emitEvent(message.getCustomer(), EventType.HELIX_NEW_AGGREGATION, Map.of());
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }

    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.HELIX;
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
