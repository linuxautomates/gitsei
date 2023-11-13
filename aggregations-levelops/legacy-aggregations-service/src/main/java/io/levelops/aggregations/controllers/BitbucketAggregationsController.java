package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.BitbucketAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.dates.DateUtils;
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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Service
@Log4j2
public class BitbucketAggregationsController implements AggregationsController<AppAggMessage> {

    private final BitbucketAggHelper tableHelper;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;
    private final EventsClient eventsClient;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public BitbucketAggregationsController(Storage storage,
                                           IntegrationService integrationService,
                                           BitbucketAggHelper helper,
                                           ControlPlaneService controlPlaneService,
                                           @Value("${BITBUCKET_AGG_SUB:dev-bitbucket-sub}") String bitbucketSub,
                                           @Value("${GOOGLE_CLOUD_PROJECT}") String projectName,
                                           AggregationsDatabaseService aggregationsDatabaseService,
                                           final EventsClient eventsClient,
                                           IntegrationTrackingService trackingService) {
        this.subscriptionName = bitbucketSub;
        this.tableHelper = helper;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.eventsClient = eventsClient;
        this.trackingService = trackingService;
    }

    @Override
    @Async("bitBucketTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Bitbucket Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId())
                    .orElse(null);
            if (it == null || IntegrationType.BITBUCKET != IntegrationType.fromString(it.getApplication())) {
                return;
            }

            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);

            //First insert commits. Needed for Bitbucket
            tableHelper.insertBitbucketCommits(message.getCustomer(), message.getIntegrationId(), results);

            //Insert PRs after commits.
            tableHelper.insertBitbucketPrs(message.getCustomer(), message.getIntegrationId(), results);

            tableHelper.insertBitbucketTags(message.getCustomer(), message.getIntegrationId(), results);

            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(message.getCustomer())
                    .integrationId(it.getId())
                    .build();


            // emit event
            try {
                eventsClient.emitEvent(message.getCustomer(), EventType.BITBUCKET_NEW_AGGREGATION, Map.of("integration_key", integrationKey));
            } catch (EventsClientException e) {
                log.error("Error sending event for tenant={}, eventType={}", message.getCustomer(),
                        EventType.BITBUCKET_NEW_AGGREGATION, e);
            }
            trackingService.upsert(message.getCustomer(),
                    IntegrationTracker.builder()
                            .integrationId(message.getIntegrationId())
                            .latestIngestedAt(DateUtils.truncate(new Date(), Calendar.DATE))
                            .build());
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.BITBUCKET;
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
