package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import io.levelops.aggregations.models.AggJobType;
import io.levelops.aggregations.models.ScmCommitPRMappingMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class TenantScmCommitPRMappingTaskScheduleService {
    private static final AggJobType JOB_TYPE = AggJobType.SCM_COMMIT_PR_MAPPING;

    private final ObjectMapper objectMapper;
    private final AggTaskManagementService aggTaskManagementService;
    private final Publisher scmCommitPRMappingPublisher;

    @Autowired
    public TenantScmCommitPRMappingTaskScheduleService(ObjectMapper objectMapper, AggTaskManagementService aggTaskManagementService, Publisher scmCommitPRMappingPublisher) {
        this.objectMapper = objectMapper;
        this.aggTaskManagementService = aggTaskManagementService;
        this.scmCommitPRMappingPublisher = scmCommitPRMappingPublisher;
    }

    //region Scm Commit PR Mapping
    public void scheduleScmCommitPRMappingForTenant(String company) {
        Stopwatch st = Stopwatch.createStarted();
        boolean success = true;

        try {
            if (!aggTaskManagementService.getUnAssignedJob(company, JOB_TYPE.toString(), JOB_TYPE.getTaskIntervalInSecs())) {
                return;
            }
            ScmCommitPRMappingMessage msg = ScmCommitPRMappingMessage.builder()
                    .messageId(UUID.randomUUID().toString()).customer(company)
                    .build();
            log.info("Scm Commit PR Mappings sending message starting {}", msg);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(msg))).build();
            scmCommitPRMappingPublisher.publish(pubsubMessage);
            log.info("Scm Commit PR Mappings sending message completed, company {}", company);
        } catch (Exception e) {
            log.error("Scm Commit PR Mappings Error, company {}", company, e);
            success = false;
        } finally {
            log.info("Scm Commit PR Mappings Schedule, company {}, duration ms {}, success = {}", company, st.elapsed(TimeUnit.MILLISECONDS), success);
        }
    }
    //endregion
}
