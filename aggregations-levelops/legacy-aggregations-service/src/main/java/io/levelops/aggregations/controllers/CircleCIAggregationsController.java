package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.CircleCIAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * Implementation of {@link AggregationsController<AppAggMessage>} for CircleCI
 */
@Log4j2
@Service
public class CircleCIAggregationsController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final CircleCIAggHelper helper;
    private final String subscriptionName;
    private final EventsClient eventsClient;

    @Autowired
    CircleCIAggregationsController(@Value("${CIRCLECI_AGG_SUB:dev-circleci-sub}") String subscriptionName,
                                   IntegrationService integrationService,
                                   ControlPlaneService controlPlaneService,
                                   CircleCIAggHelper helper,
                                   Storage storage,
                                   final EventsClient eventsClient) {
        this.subscriptionName = subscriptionName;
        this.controlPlaneService = controlPlaneService;
        this.helper = helper;
        this.helper.setStorage(storage);
        this.integrationService = integrationService;
        this.eventsClient = eventsClient;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.CIRCLECI;
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
    @Async("circleCITaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("doTask: received message: {}", message.getMessageId());
            Integration integration = integrationService.get(message.getCustomer(), message.getIntegrationId()).orElse(null);
            if (integration == null ||
                    IntegrationType.CIRCLECI != IntegrationType.fromString(integration.getApplication())) {
                return;
            }
            MultipleTriggerResults multipleTriggerResults = controlPlaneService.getAllTriggerResults(
                    IntegrationKey.builder().tenantId(message.getCustomer()).integrationId(integration.getId()).build(),
                    false,
                    false,
                    true);
            final Date currentTime = new Date();
            if (!helper.setupCircleCIBuilds(message.getCustomer(), message.getIntegrationId(), multipleTriggerResults, currentTime)) {
                log.warn("doTask: Failed to setup circleci builds aggregation");
            }
            log.info("doTask: task completed for messageId={}", message.getMessageId());

            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(message.getCustomer())
                    .integrationId(integration.getId())
                    .build();

            eventsClient.emitEvent(message.getCustomer(), EventType.CIRCLECI_NEW_AGGREGATION, Map.of("integration_key",integrationKey));

        } catch (IngestionServiceException e) {
            log.error("doTask: error performing aggregations: " + e.getMessage(), e);
            LoggingUtils.clearThreadLocalContext();
        } catch (SQLException | EventsClientException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
        }
    }
}
