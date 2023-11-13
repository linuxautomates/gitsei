package io.levelops.aggregations.controllers;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import io.levelops.aggregations.models.AggJobType;
import io.levelops.aggregations.models.ScmCommitPRMappingMessage;
import io.levelops.aggregations.services.AggTaskManagementService;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.services.dev_productivity.models.JobStatus;
import io.levelops.commons.databases.services.scm.ScmCommitPRMappingService;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ScmCommitPRMappingsController implements AckAggregationsController<ScmCommitPRMappingMessage> {
    private final String subscriptionName;
    private final ScmCommitPRMappingService scmCommitPRMappingService;
    private final AggTaskManagementService aggTaskManagementService;

    @Autowired
    public ScmCommitPRMappingsController(@Value("${SCM_COMMIT_PR_MAPPING_SUB}") String subscriptionName, ScmCommitPRMappingService scmCommitPRMappingService, AggTaskManagementService aggTaskManagementService) {
        this.subscriptionName = subscriptionName;
        this.scmCommitPRMappingService = scmCommitPRMappingService;
        this.aggTaskManagementService = aggTaskManagementService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return null;
    }

    @Override
    public Class<ScmCommitPRMappingMessage> getMessageType() {
        return ScmCommitPRMappingMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    @Override
    @Async("scmCommitPRMappingTaskExecutor")
    public void doTask(ScmCommitPRMappingMessage task, AckReplyConsumer consumer) {
        boolean success = true;
        try {
            LoggingUtils.setupThreadLocalContext(task.getMessageId(), task.getCustomer(), "scm_commit_pr_mapping", "");
            scmCommitPRMappingService.persistScmCommitPRMapping(task.getCustomer(), task.getCreatedAtFrom());
            success = true;
        } catch (Exception e) {
            log.error("Error in SCM Commit PR Mapping!!", e);
            success = false;
        } finally {
            JobStatus jobStatus = (success) ? JobStatus.SUCCESS : JobStatus.FAILURE;
            boolean status = aggTaskManagementService.updateStatusByType(task.getCustomer(), AggJobType.SCM_COMMIT_PR_MAPPING.toString(), jobStatus.toString());
            consumer.ack();
            LoggingUtils.clearThreadLocalContext();
        }

    }
}
