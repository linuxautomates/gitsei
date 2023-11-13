package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.AWSDevToolsAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.WorkItemsService;
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

import java.time.Instant;

@Log4j2
@Service
public class AWSDevToolsAggregationsController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final AWSDevToolsAggHelper helper;
    private final String subscriptionName;
    private final WorkItemsService workItemsService;

    @Autowired
    public AWSDevToolsAggregationsController(IntegrationService integrationService,
                                             ControlPlaneService controlPlaneService,
                                             AWSDevToolsAggHelper helper,
                                             @Value("${AWSDEVTOOLS_AGG_SUB:dev-awsdevtools-sub}") String subscriptionName, WorkItemsService workItemsService) {
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.helper = helper;
        this.subscriptionName = subscriptionName;
        this.workItemsService = workItemsService;
    }

    @Override
    @Async("aWSDevToolsTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("doTask: received message: {}", message.getMessageId());
            final String integrationId = message.getIntegrationId();
            Integration integration = integrationService.get(message.getCustomer(), integrationId)
                    .orElse(null);
            if (integration == null ||
                    IntegrationType.AWSDEVTOOLS != IntegrationType.fromString(integration.getApplication())) {
                return;
            }
            MultipleTriggerResults multipleTriggerResults = controlPlaneService.getAllTriggerResults(
                    IntegrationKey.builder().tenantId(message.getCustomer()).integrationId(integration.getId()).build(),
                    false,
                    false,
                    true);
            if (!helper.setupProjects(message.getCustomer(), integrationId, multipleTriggerResults)) {
                log.warn("doTask: Failed to setup awsdevtools project aggregation");
            }
            if (!helper.setupBuilds(message.getCustomer(), integrationId, multipleTriggerResults)) {
                log.warn("doTask: Failed to setup awsdevtools builds aggregation");
            }
            if (!helper.setupBuildBatches(message.getCustomer(), integrationId, multipleTriggerResults)) {
                log.warn("doTask: Failed to setup awsdevtools build batches aggregation");
            }

            //cleanup data older than 91 days.
            log.info("cleaning up data: issues count - {}",
                    workItemsService.cleanUpOldData(message.getCustomer(),
                            Instant.now().getEpochSecond(),
                            86400 * 91L));
            log.info("doTask: task completed for {}", message.getMessageId());
        } catch (IngestionServiceException e) {
            log.error("doTask: error performing aggregation for awsdevtools: " + e.getMessage(), e);
        } finally {
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.AWSDEVTOOLS;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return subscriptionName;
    }
}
