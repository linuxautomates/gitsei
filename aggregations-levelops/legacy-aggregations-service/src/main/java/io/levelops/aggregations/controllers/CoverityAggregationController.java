package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.CoverityAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
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

import java.util.Calendar;
import java.util.Date;

@Log4j2
@Service
public class CoverityAggregationController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final CoverityAggHelper coverityAggHelper;
    private final String subscriptionName;
    private final EventsClient eventsClient;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public CoverityAggregationController(@Value("${COVERITY_AGG_SUB:dev-coverity-sub}") String subscriptionName,
                                         IntegrationService integrationService, ControlPlaneService controlPlaneService,
                                         CoverityAggHelper coverityAggHelper, final EventsClient eventsClient,
                                         IntegrationTrackingService trackingService) {
        this.subscriptionName = subscriptionName;
        this.controlPlaneService = controlPlaneService;
        this.coverityAggHelper = coverityAggHelper;
        this.integrationService = integrationService;
        this.eventsClient = eventsClient;
        this.trackingService = trackingService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.COVERITY;
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
    @Async("coverityTaskExecutor")
    public void doTask(AppAggMessage message) {
        String integrationId = message.getIntegrationId();
        Integration integration = integrationService.get(message.getCustomer(), integrationId).orElse(null);
        if (integration == null ||
                IntegrationType.COVERITY != IntegrationType.fromString(integration.getApplication())) {
            log.error("Invalid integration Id or message Id");
            return;
        }
        log.debug("Found integration type :" + integration.getApplication());
        try {
            LoggingUtils.setupThreadLocalContext(message);
            MultipleTriggerResults multipleTriggerResults = controlPlaneService
                    .getAllTriggerResults(IntegrationKey.builder()
                                    .tenantId(message.getCustomer())
                                    .integrationId(integration.getId()).build(),
                            false, false, true);
            Date currentTime = new Date();
            boolean isOnboarding = trackingService.get(message.getCustomer(), message.getIntegrationId()).isEmpty();
            coverityAggHelper.setupCoverityStreams(message.getCustomer(), integrationId, multipleTriggerResults);
            log.info("doTask: ingested Coverity Streams Aggregation, integration {}, message={}",
                    integrationId, message.getMessageId());
            coverityAggHelper.setupCoveritySnapshots(message.getCustomer(), integrationId, multipleTriggerResults);
            log.info("doTask: ingested Coverity Snapshots Aggregation, integration {}, message={}",
                    integrationId, message.getMessageId());
            coverityAggHelper.setupCoverityDefects(message.getCustomer(), integrationId, multipleTriggerResults, isOnboarding);
            log.info("doTask: ingested Coverity Defects Aggregation, integration {}, message={}",
                    integrationId, message.getMessageId());
            trackingService.upsert(message.getCustomer(),
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE))
                            .build());
        } catch (Throwable e) {
            log.error("doTask: Error in aggregating results for integration type :" +
                    integration.getApplication(), e);
        } finally {
            log.info("doTask: completed coverity aggregation for company {}, integration {}, message={}",
                    message.getCustomer(), integrationId, message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }
}
