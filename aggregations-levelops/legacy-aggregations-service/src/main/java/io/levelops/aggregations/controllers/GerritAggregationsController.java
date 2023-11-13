package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.GerritAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
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

import java.util.Map;

@Service
@Log4j2
public class GerritAggregationsController implements AggregationsController<AppAggMessage> {

    private final GerritAggHelper tableHelper;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;
    private final EventsClient eventsClient;

    @Autowired
    public GerritAggregationsController(Storage storage,
                                        IntegrationService integrationService,
                                        GerritAggHelper helper,
                                        ControlPlaneService controlPlaneService,
                                        @Value("${GERRIT_AGG_SUB:dev-gerrit-sub}") String subscriptionName,
                                        AggregationsDatabaseService aggregationsDatabaseService,
                                        final EventsClient eventsClient) {
        this.subscriptionName = subscriptionName;
        this.tableHelper = helper;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.eventsClient = eventsClient;
    }

    @Override
    @Async("gerritTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Gerrit Agg: {} ", message.getMessageId());
            String tenantId = message.getCustomer();
            String integrationId = message.getIntegrationId();
            Integration integration = integrationService.get(tenantId, integrationId)
                    .orElse(null);
            if (integration == null || IntegrationType.GERRIT != IntegrationType.fromString(integration.getApplication())) {
                return;
            }
            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(tenantId)
                    .integrationId(integrationId)
                    .build();
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(integrationKey, false, false, true);

            tableHelper.processPullRequestsDataType(tenantId, integrationId, results);

            // emit event
            eventsClient.emitEvent(tenantId, EventType.GERRIT_NEW_AGGREGATION, Map.of("integration_key", integrationKey));
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.GERRIT;
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
