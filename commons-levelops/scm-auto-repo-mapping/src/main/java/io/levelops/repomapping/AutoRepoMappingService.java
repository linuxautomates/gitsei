package io.levelops.repomapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.ScmRepoMappingQuery;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.services.BaseIngestionService;
import io.levelops.ingestion.services.ControlPlaneJobService;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class AutoRepoMappingService extends BaseIngestionService {
    private static final String CONTROLLER_NAME = "ScmRepoMappingController";

    private final OrgUsersDatabaseService orgUserDatabaseService;
    private final ControlPlaneJobService controlPlaneJobService;
    private final ObjectMapper objectMapper;

    public AutoRepoMappingService(OrgUsersDatabaseService orgUserDatabaseService,
                                  ControlPlaneService controlPlaneService,
                                  InventoryService inventoryService,
                                  ObjectMapper objectMapper) {
        super(controlPlaneService, inventoryService, CONTROLLER_NAME);
        this.controlPlaneJobService = new ControlPlaneJobService(controlPlaneService, 5);
        this.orgUserDatabaseService = orgUserDatabaseService;
        this.objectMapper = objectMapper;
    }

    public Stream<String> streamIntegrationUserCloudIds(String tenantId, String integrationId) throws SQLException {
        return orgUserDatabaseService.stream(tenantId, QueryFilter.builder()
                        .strictMatch("integration_id", Integer.parseInt(integrationId))
                        .build(), 100)
                .flatMap(orgUser -> orgUser.getIds().stream()
                        .filter(integrationUser -> integrationUser.getIntegrationId() == Integer.parseInt(integrationId))
                        .map(DBOrgUser.LoginId::getCloudId));
    }

    public SubmitJobResponse createRepoMappingIngestionJob(IntegrationKey integrationKey, String callbackUrl, List<String> userIds) throws IngestionServiceException {
        return submitJob(callbackUrl, integrationKey, ScmRepoMappingQuery.builder()
                .userIds(userIds)
                .integrationKey(integrationKey)
                .build());
    }

    public ScmRepoMappingResult createAndWaitForRepoMappingJob(
            IntegrationKey integrationKey,
            List<String> userIds,
            int timeoutInSeconds,
            int pollDurationInSeconds
    ) throws IngestionServiceException, TimeoutException, InterruptedException {
        SubmitJobResponse submitJobResponse = createRepoMappingIngestionJob(integrationKey, "", userIds);
        Instant start = Instant.now();
        do {
            Optional<ScmRepoMappingResult> resultOpt = getJobResultIfAvailable(submitJobResponse.getJobId());
            if (resultOpt.isPresent()) {
                return resultOpt.get();
            }
            log.info("Job not complete yet, waiting for {} seconds", pollDurationInSeconds);
            TimeUnit.SECONDS.sleep(pollDurationInSeconds);
            if (Instant.now().minusSeconds(timeoutInSeconds).isAfter(start)) {
                throw new TimeoutException("Job timed out");
            }
        } while (true);
    }

    public Optional<ScmRepoMappingResult> getJobResultIfAvailable(String jobId) throws IngestionServiceException {
        Optional<JobDTO> jobOpt = controlPlaneJobService.getJobIfComplete(jobId);
        return jobOpt.map(job -> {
            if (job.getStatus() != JobStatus.SUCCESS) {
                throw new RuntimeException("Job failed: " + job.getError());
            }
            return objectMapper.convertValue(job.getResult(), ScmRepoMappingResult.class);
        });
    }

    public ScmRepoMappingResult createAndWaitForRepoMappingJob(IntegrationKey integrationKey, int timeoutInSeconds)
            throws IngestionServiceException, TimeoutException, InterruptedException, SQLException {
        List<String> userIds = streamIntegrationUserCloudIds(integrationKey.getTenantId(), integrationKey.getIntegrationId())
                .collect(Collectors.toList());
        return createAndWaitForRepoMappingJob(integrationKey, userIds, timeoutInSeconds, 5);
    }

    public String createRepoMappingJob(IntegrationKey integrationKey, String callbackUrl) throws IngestionServiceException, SQLException {
        List<String> userIds = streamIntegrationUserCloudIds(integrationKey.getTenantId(), integrationKey.getIntegrationId())
                .collect(Collectors.toList());
        return createRepoMappingIngestionJob(integrationKey, callbackUrl, userIds).getJobId();
    }


    @Override
    public IntegrationType getIntegrationType() {
        return null;
    }
}

// Commenting for potential future use
//    public Set<String> getAllOrgUserRepos(String tenantId, String integrationId) throws InventoryException, SQLException, ExecutionException {
//        AutoRepoMappingClient client = clientRegistry.getClient(tenantId, integrationId);
//        Set<String> uniqueRepos = streamIntegrationUserCloudIds(tenantId, integrationId)
//                .peek(cloudId -> System.out.println("Getting repos for user: " + cloudId))
//                .map(cloudId -> client.getReposForUser(tenantId, integrationId, cloudId))
//                .peek(repos -> System.out.println("Found repos: " + repos))
//                .flatMap(List::stream)
//                .collect(Collectors.toSet());
//        return uniqueRepos;
//    }

//    public Set<String> identifyMissingRepositories(String tenantId, Integration integration) throws InventoryException, SQLException, ExecutionException {
//        Set<String> repos = getAllOrgUserRepos(tenantId, integration.getId());
//        String rawExistingRepos = (String) integration.getMetadata().getOrDefault("repos", "");
//        Set<String> existingRepos = CommaListSplitter.splitToSet(rawExistingRepos);
//        log.info("Existing repos mappings: {}, Total repos detected by job: {}", existingRepos.size(), repos.size());
//
//        Set<String> reposToAdd = repos.stream()
//                .filter(repo -> !existingRepos.contains(repo))
//                .collect(Collectors.toSet());
//        log.info("Found {} repos to add: {}", reposToAdd.size(), reposToAdd);
//        return reposToAdd;
//    }

//    private void updateIntegrationWithMappedRepos(Set<String> reposToAdd, Integration integration, String tenantId) throws SQLException {
//        String reposToAddStr = String.join(",", reposToAdd);
//        String rawExistingRepos = (String) integration.getMetadata().getOrDefault("repos", "");
//        String newRepos = rawExistingRepos + "," + reposToAddStr;
//        log.info("Updating integration {} with new repos: {}", integration.getId(), newRepos);
//        HashMap<String, Object> newMetadata = new HashMap<>(integration.getMetadata());
//        newMetadata.put("repos", newRepos);
//        Integration updatedIntegration = Integration.builder()
//                .id(integration.getId())
//                .metadata(newMetadata)
//                .build();
//        boolean success = integrationService.update(tenantId, updatedIntegration);
//        if (!success) {
//            log.error("Failed to update integration {} with new auto mapped repos.", integration.getId());
//        } else {
//            log.info("Successfully updated integration {} with new auto mapped repos.", integration.getId());
//        }
//    }
//
//    public void autoMapRepos(String tenantId, Integration integration, boolean dryRun) {
//        try {
//            Set<String> reposToAdd = identifyMissingRepositories(tenantId, integration);
//            if (!dryRun && !reposToAdd.isEmpty()) {
//                updateIntegrationWithMappedRepos(reposToAdd, integration, tenantId);
//            }
//        } catch (InventoryException | SQLException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public void runAutomappingJobForTenant(String tenantId, boolean dryRun) throws SQLException {
//        Set<String> supportedApps = clientRegistry.getSupportedApps();
//        integrationService.stream(
//                        tenantId,
//                        null,
//                        false,
//                        new ArrayList<>(supportedApps),
//                        null,
//                        null,
//                        null,
//                        null,
//                        null,
//                        null)
//                .filter(integration -> {
//                    return true;
////                    Boolean enabled = (Boolean) integration.getMetadata().get("auto_repo_mapping_enabled");
////                    return BooleanUtils.isTrue(enabled);
//                }).forEach(integration -> {
//                    log.info("Running auto repo mapping for integration {} in tenant {} - dry run: {}", integration.getId(), tenantId, dryRun);
//                    autoMapRepos(tenantId, integration, dryRun);
//                });
//    }

