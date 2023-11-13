package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.OktaAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.OktaAggService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Aggregation Controller for Okta which uses {@link OktaAggHelper} and {@link OktaAggService}
 * for inserting/upserting new data and deleting old data.
 */
@Service
@Log4j2
public class OktaAggregationController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;
    private final OktaAggHelper aggHelper;
    private final OktaAggService oktaAggService;

    @Autowired
    public OktaAggregationController(IntegrationService integrationService,
                                     OktaAggHelper aggHelper,
                                     ControlPlaneService controlPlaneService,
                                     @Value("${OKTA_AGG_SUB:dev-okta-sub}") String oktaSub,
                                     OktaAggService oktaAggService) {
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.aggHelper = aggHelper;
        this.subscriptionName = oktaSub;
        this.oktaAggService = oktaAggService;
    }

    @Override
    @Async("oktaTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Okta Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId())
                    .orElse(null);
            if (it == null || IntegrationType.OKTA != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);
            Date currentTime = new Date();
            if (!aggHelper.insertGroups(message.getCustomer(), currentTime, message.getIntegrationId(), results)) {
                log.warn("Failed to insert Okta groups for integration {}", message.getIntegrationId());
                return;
            }
            if (!aggHelper.insertUsers(message.getCustomer(), currentTime, message.getIntegrationId(), results)) {
                log.warn("Failed to insert Okta users for integration {}", message.getIntegrationId());
            }
            log.debug("doTask: task completed");
           /* oktaAggService.cleanUpOldData(message.getCustomer(), currentTime.toInstant().getEpochSecond(),
                    86400 * 91L); // 91 days older data*/
            log.debug("doTask: cleaned up older data");
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.OKTA;
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
