package io.levelops.aggregations.controllers;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.common.base.Stopwatch;
import io.levelops.aggregations.models.AggJobType;
import io.levelops.aggregations.models.CiCdArtifactMappingMessage;
import io.levelops.aggregations.services.AggTaskManagementService;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService;
import io.levelops.commons.databases.services.dev_productivity.models.JobStatus;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class CiCdArtifactMappingController implements AckAggregationsController<CiCdArtifactMappingMessage> {
    private final String subscriptionName;
    private final AggTaskManagementService aggTaskManagementService;
    private final CicdJobRunArtifactCorrelationService correlationService;

    @Autowired
    public CiCdArtifactMappingController(@Value("${CICD_ARTIFACT_MAPPING_SUB}") String subscriptionName,
                                         AggTaskManagementService aggTaskManagementService,
                                         CicdJobRunArtifactCorrelationService correlationService) {
        this.subscriptionName = subscriptionName;
        this.aggTaskManagementService = aggTaskManagementService;
        this.correlationService = correlationService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        // not integration specific
        return null;
    }

    @Override
    public Class<CiCdArtifactMappingMessage> getMessageType() {
        return CiCdArtifactMappingMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    @Override
    @Async("cicdArtifactMappingTaskExecutor")
    public void doTask(CiCdArtifactMappingMessage task, AckReplyConsumer consumer) {
        boolean success = true;
        Stopwatch watch = Stopwatch.createStarted();
        try {
            LoggingUtils.setupThreadLocalContext(task.getMessageId(), task.getCustomer(), "cicd_artifact_mapping", "");

            log.info("CICD Artifact Mapping task started for tenant={}", task.getCustomer());
            task(task.getCustomer());

        } catch (Exception e) {
            log.error("Error in CICD Artifact Mapping task", e);
            success = false;
        } finally {
            JobStatus jobStatus = (success) ? JobStatus.SUCCESS : JobStatus.FAILURE;
            log.info("CICD Artifact Mapping task completed: tenant={}, status={}, elapsed={} min", task.getCustomer(), jobStatus.toString(), watch.elapsed(TimeUnit.MINUTES));
            aggTaskManagementService.updateStatusByType(task.getCustomer(), AggJobType.CICD_ARTIFACT_MAPPING.toString(), jobStatus.toString());
            consumer.ack();
            LoggingUtils.clearThreadLocalContext();
        }
    }

    public void task(String customer) {
        correlationService.correlateAndUpdateArtifactMappings(customer);
    }
}
