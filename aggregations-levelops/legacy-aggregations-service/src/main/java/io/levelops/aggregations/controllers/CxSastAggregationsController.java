package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.CxSastAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * Implementation of {@link AggregationsController<AppAggMessage>} for cxsast
 */
@Service
@Log4j2
public class CxSastAggregationsController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final CxSastAggHelper cxSastAggHelper;
    private final String subscriptionName;
    private final EventsClient eventsClient;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public CxSastAggregationsController(@Value("${CXSAST_AGG_SUB:dev-checkmarx-sub}") String subscriptionName,
                                        IntegrationService integrationService,
                                        ControlPlaneService controlPlaneService,
                                        CxSastAggHelper cxSastAggHelper,
                                        EventsClient eventsClient,
                                        IntegrationTrackingService trackingService) {
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.cxSastAggHelper = cxSastAggHelper;
        this.subscriptionName = subscriptionName;
        this.eventsClient = eventsClient;
        this.trackingService = trackingService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.CXSAST;
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
    @Async("cxsastTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on CxSast Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId())
                    .orElse(null);
            if (it == null || IntegrationType.CXSAST != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            boolean isOnboarding = trackingService.get(message.getCustomer(), message.getIntegrationId()).isEmpty();
            log.debug("Found integration type :" + it.getApplication());
            Date currentTime = new Date();
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);
            cxSastAggHelper.setupCxSastProject(message.getCustomer(), message.getIntegrationId(), results, currentTime);
            cxSastAggHelper.setupCxSastScan(message.getCustomer(), message.getIntegrationId(), results, currentTime, isOnboarding);
            // emit event
            try {
                eventsClient.emitEvent(message.getCustomer(), EventType.CHECKMARX_SAST_NEW_AGGREGATION, Map.of());
            } catch (EventsClientException e) {
                log.error("Error sending event for tenant={}, eventType={}", message.getCustomer(),
                        EventType.CHECKMARX_SAST_NEW_AGGREGATION, e);
            }
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }
}
