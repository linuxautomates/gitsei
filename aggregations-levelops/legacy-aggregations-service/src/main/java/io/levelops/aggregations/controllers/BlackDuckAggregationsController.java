package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.BlackDuckAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class BlackDuckAggregationsController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;
    private final BlackDuckAggHelper aggHelper;

    @Autowired
    public BlackDuckAggregationsController(IntegrationService integrationService,
                                           ControlPlaneService controlPlaneService,
                                           @Value("${BLACKDUCK_AGG_SUB:dev-blackduck-sub}") String blackDuckSub,
                                           BlackDuckAggHelper aggHelper) {
        this.integrationService = integrationService;
        this.subscriptionName = blackDuckSub;
        this.controlPlaneService = controlPlaneService;
        this.aggHelper = aggHelper;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.BLACKDUCK;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    @Override
    @Async("blackDuckTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("doTask: received message: {}", message.getMessageId());
            final String integrationId = message.getIntegrationId();
            Integration integration = integrationService.get(message.getCustomer(), integrationId)
                    .orElse(null);
            if (integration == null ||
                    IntegrationType.BLACKDUCK != IntegrationType.fromString(integration.getApplication())) {
                return;
            }
            MultipleTriggerResults multipleTriggerResults = controlPlaneService.getAllTriggerResults(
                    IntegrationKey.builder().tenantId(message.getCustomer()).integrationId(integration.getId()).build(),
                    false,
                    false,
                    true);
            if (!aggHelper.setupProjects(message.getCustomer(), integrationId, multipleTriggerResults)) {
                log.warn("doTask: Failed to setup blackduck project aggregation");
            }
            log.info("doTask: task completed for {}", message.getMessageId());
        } catch (IngestionServiceException e) {
            log.error("doTask: error performing aggregation for blackduck: " + e.getMessage(), e);
        } finally {
            LoggingUtils.clearThreadLocalContext();
        }
    }
}
