package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.SonarQubeAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
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

import java.util.Date;

@Log4j2
@Service
public class SonarQubeAggregationsController implements AggregationsController<AppAggMessage> {

    private static final String PROJECT = "project";
    private static final String PROJECT_ANALYSES = "analyses";
    private static final String PROEJCT_BRANCH = "branch";
    private static final String PROJECT_PRS = "pr-issues";

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final SonarQubeAggHelper sonarqubeAggHelper;
    private final String subscriptionName;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public SonarQubeAggregationsController(@Value("${SONARQUBE_AGG_SUB:dev-sonarqube-sub}") String subscriptionName,
                                           IntegrationService integrationService, ControlPlaneService controlPlaneService,
                                           SonarQubeAggHelper sonarqubeAggHelper, IntegrationTrackingService trackingService) {
        this.subscriptionName = subscriptionName;
        this.controlPlaneService = controlPlaneService;
        this.sonarqubeAggHelper = sonarqubeAggHelper;
        this.integrationService = integrationService;
        this.trackingService = trackingService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SONARQUBE;
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
    @Async("sonarQubeTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            String integrationId = message.getIntegrationId();
            Integration integration = integrationService.get(message.getCustomer(), integrationId).orElse(null);
            if (integration == null ||
                    IntegrationType.SONARQUBE != IntegrationType.fromString(integration.getApplication())) {
                log.warn("Invalid integration Id or message Id");
                return;
            }
            boolean isOnboarding = trackingService.get(message.getCustomer(), integrationId).isEmpty();
            log.debug("Found integration type :" + integration.getApplication());
            MultipleTriggerResults multipleTriggerResults = controlPlaneService
                    .getAllTriggerResults(IntegrationKey.builder()
                                    .tenantId(message.getCustomer())
                                    .integrationId(integration.getId()).build(),
                            false, false, true);
            Date currentTime = new Date();
            sonarqubeAggHelper.setupSonarQubeProjectEntities(message.getCustomer(), integrationId,
                    multipleTriggerResults, currentTime, PROJECT);
            sonarqubeAggHelper.setupSonarQubeProjectEntities(message.getCustomer(), integrationId,
                    multipleTriggerResults, currentTime, PROJECT_ANALYSES);
            sonarqubeAggHelper.setupSonarQubeProjectEntities(message.getCustomer(), integrationId,
                    multipleTriggerResults, currentTime, PROEJCT_BRANCH);
            sonarqubeAggHelper.setupSonarQubeProjectEntities(message.getCustomer(), integrationId,
                    multipleTriggerResults, currentTime, PROJECT_PRS);
            sonarqubeAggHelper.setupSonarQubePRIssues(message.getCustomer(), integrationId, multipleTriggerResults,
                    currentTime, isOnboarding);
            sonarqubeAggHelper.setupSonarQubeIssues(message.getCustomer(), integrationId, multipleTriggerResults,
                    currentTime, isOnboarding);
            log.info("doTask: completed Sonarqube aggregation for integration {}", integrationId);

            log.info("Cleaning up old data: metrics deleted count={}",
                    sonarqubeAggHelper.cleanUpOldData(message.getCustomer(),
                            currentTime.toInstant().getEpochSecond(),
                            86400 * 91L));
        } catch (IngestionServiceException e) {
            log.error("doTask: Error in aggregating results for integration type : sonarqube", e);
            LoggingUtils.clearThreadLocalContext();
        }
    }
}
