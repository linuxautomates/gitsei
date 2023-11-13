package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import io.levelops.aggregations.models.AggJobType;
import io.levelops.aggregations.models.CiCdArtifactMappingMessage;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.utils.CommaListSplitter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class CiCdArtifactMappingSchedulerService {
    private static final int WARMUP_DELAY_SECS = 10;
    private static final String JOB_TYPE = AggJobType.CICD_ARTIFACT_MAPPING.toString();

    private final ObjectMapper objectMapper;
    private final AggTaskManagementService aggTaskManagementService;
    private final Publisher publisher;
    @SuppressWarnings("FieldCanBeLocal")
    private final ScheduledExecutorService scheduler;
    private final TenantService tenantService;
    private final long jobIntervalInMin;
    private final boolean enableWhitelist;
    private final List<String> whitelistEntries;

    /**
     * @param schedulingIntervalInMin Frequency of the scheduling thread
     * @param jobIntervalInMin        To dedupe tasks using the db global task tracker
     */
    @Autowired
    public CiCdArtifactMappingSchedulerService(ObjectMapper objectMapper,
                                               TenantService tenantService,
                                               AggTaskManagementService aggTaskManagementService,
                                               @Qualifier("cicdArtifactMappingPublisher") Publisher publisher,
                                               @Value("${CICD_ARTIFACT_MAPPING_SCHEDULING_INTERVAL:15}") Long schedulingIntervalInMin,
                                               @Value("${CICD_ARTIFACT_MAPPING_JOB_INTERVAL:480}") Long jobIntervalInMin,
                                               @Value("${GLOBAL_CICD_MAPPING_JOB_WHITELIST:}") String globalCiCdMappingJobWhitelist,
                                               @Value("${ENABLE_GLOBAL_CICD_MAPPING_JOB_WHITELIST:true}") boolean enableGlobalCiCdMappingJobWhitelist
    ) {
        Validate.notNull(schedulingIntervalInMin, "schedulingIntervalInMin cannot be null.");
        Validate.notNull(jobIntervalInMin, "jobIntervalInMin cannot be null.");
        this.objectMapper = objectMapper;
        this.aggTaskManagementService = aggTaskManagementService;
        this.publisher = publisher;
        scheduler = initScheduling(this, schedulingIntervalInMin);
        this.tenantService = tenantService;
        this.jobIntervalInMin = jobIntervalInMin;
        this.enableWhitelist = enableGlobalCiCdMappingJobWhitelist;
        this.whitelistEntries = CommaListSplitter.split(globalCiCdMappingJobWhitelist);
    }

    private static ScheduledExecutorService initScheduling(CiCdArtifactMappingSchedulerService ciCdArtifactMappingSchedulerService,
                                                           long schedulingIntervalInMin) {
        if (schedulingIntervalInMin <= 0) {
            log.info("CICD Artifact Mapping Scheduling is DISABLED");
            return null;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("cicd-artifact-mapping-sched-%d")
                .build());
        executor.scheduleAtFixedRate(new CiCdArtifactMappingSchedulerService.SchedulingTask(ciCdArtifactMappingSchedulerService), WARMUP_DELAY_SECS, schedulingIntervalInMin, TimeUnit.SECONDS);
        log.info("CICD Artifact Mapping Scheduling is ENABLED (interval={}min)", schedulingIntervalInMin);
        return executor;
    }

    public static class SchedulingTask implements Runnable {
        private final CiCdArtifactMappingSchedulerService ciCdArtifactMappingSchedulerService;

        public SchedulingTask(CiCdArtifactMappingSchedulerService ciCdArtifactMappingSchedulerService) {
            this.ciCdArtifactMappingSchedulerService = ciCdArtifactMappingSchedulerService;
        }

        @Override
        public void run() {
            try {
                log.info("CICD Artifact Mapping scheduling started");
                ciCdArtifactMappingSchedulerService.scheduleCicdArtifactMappingForAllTenants();
                log.info("CICD Artifact Mapping scheduling done");
            } catch (Throwable e) {
                log.warn("Failed to schedule CICD Artifact Mapping", e);
            }
        }
    }

    public void scheduleCicdArtifactMappingForAllTenants() throws SQLException {
        PaginationUtils.streamThrowingRuntime(0, 1,
                pageNumber -> tenantService.list("", pageNumber, 1000).getRecords()
        ).forEach(tenant -> scheduleCicdArtifactMappingForTenant(tenant.getId()));
    }

    public void scheduleCicdArtifactMappingForTenant(String tenantId) {
        try {
            if (enableWhitelist && whitelistEntries.stream().noneMatch(entry -> entry.equals(tenantId))) {
                log.debug("Skipping CICD Artifact Mapping scheduling for tenant_id={} as it is not whitelisted", tenantId);
                return;
            }
            if (!aggTaskManagementService.getUnAssignedJob(tenantId, JOB_TYPE, jobIntervalInMin)) {
                return;
            }
            CiCdArtifactMappingMessage msg = CiCdArtifactMappingMessage.builder()
                    .customer(tenantId)
                    .messageId(UUID.randomUUID().toString())
                    .build();
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(msg)))
                    .build();
            publisher.publish(pubsubMessage);
        } catch (Exception e) {
            log.error("CICD Artifact Mapping scheduling failed for tenant_id={}", tenantId, e);
        }
    }
}
