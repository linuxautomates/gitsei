package io.levelops.controlplane.models;

import io.levelops.ingestion.models.controlplane.JobDTO;

public class JobDTOConverters {

    public static JobDTO convertFromDbJob(DbJob dbJob) {
        return toBuilder(dbJob)
                .build();
    }

    public static JobDTO convertFromDbJobAndTriggeredJob(DbJob dbJob, DbTriggeredJob triggeredJob) {
        return toBuilder(dbJob)
                .partial(triggeredJob.getPartial())
                .iterationId(triggeredJob.getIterationId())
                .iterationTs(triggeredJob.getIterationTs())
                .triggerId(triggeredJob.getTriggerId())
                .build();
    }

    private static JobDTO.JobDTOBuilder toBuilder(DbJob dbJob) {
        return JobDTO.builder()
                .id(dbJob.getId())
                .agentId(dbJob.getAgentId())
                .status(dbJob.getStatus())
                .tenantId(dbJob.getTenantId())
                .integrationId(dbJob.getIntegrationId())
                .reserved(dbJob.getReserved())
                .tags(dbJob.getTags())
                .level(dbJob.getLevel())
                .parentId(dbJob.getParentId())
                .attemptCount(dbJob.getAttemptCount())
                .attemptMax(dbJob.getAttemptMax())
                .query(dbJob.getQuery())
                .callbackUrl(dbJob.getCallbackUrl())
                .controllerName(dbJob.getControllerName())
                .createdAt(dbJob.getCreatedAt())
                .statusChangedAt(dbJob.getStatusChangedAt())
                .result(dbJob.getResult())
                .intermediateState(dbJob.getIntermediateState())
                .error(dbJob.getError())
                .ingestionFailures(dbJob.getIngestionFailures());
    }
}
