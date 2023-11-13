package io.levelops.ingestion.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.GenericIntegrationQuery;
import io.levelops.ingestion.models.GenericMultiIntegrationQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
public class GenericIngestionService {

    private static final String CONTROLLER_NAME = "GenericIntegrationController";
    private static final String MULTI_INTEGRATION_CONTROLLER_NAME = "GenericMultiIntegrationController";
    private final IngestionAgentService ingestionAgentService;
    private final String agentUri;

    public Job submitJob(IntegrationKey integrationKey, IntegrationType integrationType, String dataType, @Nullable String callbackUrl) throws IngestionServiceException {
        return ingestionAgentService.submitJob(agentUri, CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .query(GenericIntegrationQuery.builder()
                        .integrationKey(integrationKey)
                        .integrationType(integrationType.toString())
                        .dataType(dataType)
                        .build())
                .callbackUrl(callbackUrl)
                .build());
    }

    public Job submitMultiJob(List<GenericIntegrationJob> jobs, @Nullable String callbackUrl) throws IngestionServiceException {
        return ingestionAgentService.submitJob(agentUri, CreateJobRequest.builder()
                .controllerName(MULTI_INTEGRATION_CONTROLLER_NAME)
                .query(GenericMultiIntegrationQuery.builder()
                        .queries(jobs.stream()
                                .map(job -> GenericIntegrationQuery.builder()
                                        .integrationKey(job.getIntegrationKey())
                                        .integrationType(job.getIntegrationType().toString())
                                        .dataType(job.getDataType())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .callbackUrl(callbackUrl)
                .build());
    }

    @Value
    @Builder(toBuilder = true)
    public static class GenericIntegrationJob {
        IntegrationKey integrationKey;
        IntegrationType integrationType;
        String dataType;
    }

}
