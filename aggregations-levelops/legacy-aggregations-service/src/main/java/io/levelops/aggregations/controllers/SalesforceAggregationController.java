package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.SalesforceAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.SalesforceCaseService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Log4j2
@SuppressWarnings("unused")
public class SalesforceAggregationController implements AggregationsController<AppAggMessage> {
    private static final String AGGREGATION_VERSION = "V0.1";

    private final SalesforceAggHelper helper;
    private final SalesforceCaseService caseService;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;

    public SalesforceAggregationController(Storage storage,
                                           IntegrationService integrationService,
                                           SalesforceAggHelper helper,
                                           SalesforceCaseService caseService,
                                           ControlPlaneService controlPlaneService,
                                           @Value("${SALESFORCE_AGG_SUB:dev-salesforce-sub}") String salesforceSub,
                                           AggregationsDatabaseService aggregationsDatabaseService) {
        this.subscriptionName = salesforceSub;
        this.helper = helper;
        this.caseService = caseService;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
    }

    @Override
    @Async("salesforceTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Jira Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(
                            message.getCustomer(),
                            message.getIntegrationId())
                    .orElse(null);
            if (it == null ||
                    IntegrationType.SALESFORCE != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(
                    IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false,
                    false,
                    true);
            Date currentTime = new Date();
            if (!helper.setupSalesforceCases(
                    message.getCustomer(),
                    currentTime,
                    message.getIntegrationId(),
                    results)) {
                log.warn("Failed to setup salesforce cases. ending aggregation. Will not clean up old data from db");
                return;
            }
            if (!helper.setupSalesforceCaseHistories(
                    message.getCustomer(),
                    currentTime,
                    message.getIntegrationId(),
                    results)) {
                log.warn("Failed to setup salesforce case histories.");
            }
            //cleanup data older than 91 days.
            log.info("cleaning up data: issues count - {}",
                    caseService.cleanUpOldData(message.getCustomer(),
                            currentTime.toInstant().getEpochSecond(),
                            86400 * 91L));
        } catch (IngestionServiceException e) {
            log.error("Error while ingestion. ", e);
        } finally {
            log.info("Completed work on Salesforce Aggregation: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SALESFORCE;
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
