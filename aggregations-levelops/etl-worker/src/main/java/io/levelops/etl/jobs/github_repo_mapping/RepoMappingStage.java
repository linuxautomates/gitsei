package io.levelops.etl.jobs.github_repo_mapping;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.MapUtils;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.repomapping.AutoRepoMappingService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Log4j2
@Service
public class RepoMappingStage implements GenericJobProcessingStage<RepoMappingJobState> {
    private final static String METADATA_KEY_REPOS = "repos";
    private final AutoRepoMappingService autoRepoMappingService;
    private final InventoryService inventoryService;
    private final int timeoutSeconds;


    @Autowired
    public RepoMappingStage(
            AutoRepoMappingService autoRepoMappingService,
            InventoryService inventoryService,
            @Value("${GITHUB_REPO_MAPPING_TIMEOUT_SECONDS:7200}") int timeoutSeconds
    ) {
        this.autoRepoMappingService = autoRepoMappingService;
        this.inventoryService = inventoryService;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getUpdatedRepos(ScmRepoMappingResult repoMappingResult, Integration integration) {
        Map<String, Object> integrationMetadata = MapUtils.emptyIfNull(integration.getMetadata());
        String repoString = (String) integrationMetadata.getOrDefault(METADATA_KEY_REPOS, "");
        Set<String> existingRepoSet = CommaListSplitter.splitToSet(repoString);
        Set<String> mappedRepoSet = new HashSet<>(repoMappingResult.getMappedRepos());
        // Find the difference between the existing repos and the mapped repos
        Set<String> newRepos = SetUtils.difference(mappedRepoSet, existingRepoSet);
        // Sort to make the output deterministic
        List<String> newReposSortedList = newRepos.stream().sorted().toList();
        String newRepoString = String.join(",", newReposSortedList);
        String finalString = repoString + (repoString.isEmpty() ? "" : ",") + newRepoString;
        log.info("Found {} new repos for integration {}. Final repos string: {} ",
                newRepos.size(), integration.getId(), finalString);
        return finalString;
    }

    @Override
    public void process(JobContext context, RepoMappingJobState jobState) {
        try {
            ScmRepoMappingResult repoMappingResult = autoRepoMappingService
                    .createAndWaitForRepoMappingJob(IntegrationKey.builder()
                            .integrationId(context.getIntegrationId())
                            .tenantId(context.getTenantId())
                            .build(), timeoutSeconds);

            // Update the integration with the new repos
            Integration integration = inventoryService.getIntegration(
                    context.getTenantId(), context.getIntegrationId());
            String finalRepoString = getUpdatedRepos(repoMappingResult, integration);
            Map<String, Object> integrationMetadata = MapUtils.append(integration.getMetadata(), METADATA_KEY_REPOS, finalRepoString);
            Integration updatedIntegration = integration.toBuilder()
                    .metadata(integrationMetadata)
                    .build();
            inventoryService.updateIntegration(context.getTenantId(), context.getIntegrationId(), updatedIntegration);
        } catch (IngestionServiceException | SQLException | TimeoutException |
                 InterruptedException | InventoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "RepoMappingStage";
    }

    @Override
    public void preStage(JobContext context, RepoMappingJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, RepoMappingJobState jobState) throws SQLException {

    }
}
