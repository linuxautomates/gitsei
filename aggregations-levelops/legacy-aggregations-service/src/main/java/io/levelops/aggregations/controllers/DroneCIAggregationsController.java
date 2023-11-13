
package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.DroneCIAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
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

/**
 * Implementation of {@link AggregationsController<AppAggMessage>} for droneci
 */
@Service
@Log4j2
public class DroneCIAggregationsController implements AggregationsController<AppAggMessage> {
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final DroneCIAggHelper droneciAggHelper;
    private final String subscriptionName;
    private final EventsClient eventsClient;

    @Autowired
    public DroneCIAggregationsController(@Value("${DRONECI_AGG_SUB:dev-droneci-sub}") String subscriptionName,
                                         IntegrationService integrationService,
                                         ControlPlaneService controlPlaneService,
                                         DroneCIAggHelper droneciAggHelper,
                                         Storage storage,
                                         final EventsClient eventsClient) {
        this.subscriptionName = subscriptionName;
        this.controlPlaneService = controlPlaneService;
        this.droneciAggHelper = droneciAggHelper;
        this.droneciAggHelper.setStorage(storage);
        this.integrationService = integrationService;
        this.eventsClient = eventsClient;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.DRONECI;
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
    @Async("droneCITaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            log.info("Starting work on DroneCI Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId()).orElse(null);
            if (it == null || IntegrationType.DRONECI != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);

            droneciAggHelper.setupDroneCIBuilds(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Ingested droneci builds data for integration-{}", message.getIntegrationId());

            // emit event

            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(message.getCustomer())
                    .integrationId(it.getId())
                    .build();

            eventsClient.emitEvent(message.getCustomer(), EventType.DRONECI_NEW_AGGREGATION, Map.of("integration_key", integrationKey));
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
        }
    }
}
