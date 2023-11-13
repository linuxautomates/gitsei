package io.levelops.aggregations.controllers;

import io.levelops.aggregations.helpers.GitlabAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
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
 * Implementation of {@link AggregationsController<AppAggMessage>} for gitlab
 */
@Service
@Log4j2
public class GitlabAggregationsController implements AggregationsController<AppAggMessage> {

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final GitlabAggHelper gitlabAggHelper;
    private final String subscriptionName;
    private final EventsClient eventsClient;

    @Autowired
    public GitlabAggregationsController(@Value("${GITLAB_AGG_SUB:dev-gitlab-sub}") String subscriptionName,
                                        IntegrationService integrationService,
                                        ControlPlaneService controlPlaneService,
                                        GitlabAggHelper gitlabAggHelper,
                                        final EventsClient eventsClient) {
        this.subscriptionName = subscriptionName;
        this.controlPlaneService = controlPlaneService;
        this.gitlabAggHelper = gitlabAggHelper;
        this.integrationService = integrationService;
        this.eventsClient = eventsClient;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.GITLAB;
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
    @Async("gitlabTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Gitlab Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId()).orElse(null);
            if (it == null || IntegrationType.GITLAB != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);
            gitlabAggHelper.setupGitlabCommit(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Ingested commits data for integration-{}", message.getIntegrationId());
            gitlabAggHelper.setupGitlabMergeRequests(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Ingested Merge requests data for integration-{}", message.getIntegrationId());
            gitlabAggHelper.updateGitCommitsForDirectMerge(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Updated direct_merge of commits data for integration-{}", message.getIntegrationId());
            gitlabAggHelper.setupGitlabIssue(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Ingested gitlab issues data for integration-{}", message.getIntegrationId());
            gitlabAggHelper.setupGitlabPipeline(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Ingested gitlab pipelines data for integration-{}", message.getIntegrationId());
            gitlabAggHelper.setupGitlabTag(message.getCustomer(), message.getIntegrationId(), results);
            log.debug("doTask: Ingested gitlab tags data for integration-{}", message.getIntegrationId());
            // emit event

            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(message.getCustomer())
                    .integrationId(it.getId())
                    .build();

            Long startTime = gitlabAggHelper.getOldestJobRunStartTime(message.getCustomer(), message.getIntegrationId() );

            eventsClient.emitEvent(message.getCustomer(), EventType.GITLAB_NEW_AGGREGATION, Map.of("integration_key", integrationKey, "start_time", startTime));
        } catch (Throwable e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }
}
