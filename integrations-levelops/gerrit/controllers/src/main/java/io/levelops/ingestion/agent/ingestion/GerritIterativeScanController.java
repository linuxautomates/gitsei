package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.gerrit.models.GerritIterativeScanQuery;
import io.levelops.integrations.gerrit.sources.GerritRepositoryDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Log4j2
public class GerritIterativeScanController implements DataController<GerritIterativeScanQuery> {

    private static final int GERRIT_ONBOARDING_IN_DAYS = 7;
    private static final String GERRIT_REPOS_METADATA_FIELD = "repos";
    private static final String GERRIT_FETCH_PR_METADATA_FIELD = "fetch_prs";

    private final ObjectMapper objectMapper;
    private final IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> pullRequestController;
    private final IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> repositoryController;
    private final InventoryService inventoryService;
    private final int onboardingInDays;

    @Builder
    public GerritIterativeScanController(ObjectMapper objectMapper,
                                         IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> pullRequestController,
                                         IntegrationController<GerritRepositoryDataSource.GerritRepositoryQuery> repositoryController,
                                         InventoryService inventoryService,
                                         Integer onboardingInDays) {
        this.inventoryService = inventoryService;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, GERRIT_ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.repositoryController = repositoryController;
        this.pullRequestController = pullRequestController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GerritIterativeScanQuery iterativeScanQuery) throws IngestException {

        Date from = iterativeScanQuery.getFrom() != null
                ? iterativeScanQuery.getFrom()
                : Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));

        // get custom repos list from integration metadata
        List<String> repos = null;
        boolean fetchPRs = true;
        try {
            Integration integration = inventoryService.getIntegration(iterativeScanQuery.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            String reposCommaList = (String) metadata.get(GERRIT_REPOS_METADATA_FIELD);
            if (StringUtils.isNotBlank(reposCommaList)) {
                repos = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(reposCommaList);
                log.info("Scanning specific Gerrit repos: {}", repos);
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GERRIT_FETCH_PR_METADATA_FIELD))) {
                fetchPRs = false;
            }
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " + iterativeScanQuery.getIntegrationKey(), e);
        }

        log.info("Gerrit iterative scan: integration={}, from={}, job_id={}", iterativeScanQuery.getIntegrationKey(), from, jobContext.getJobId());

        GerritRepositoryDataSource.GerritRepositoryQuery query = GerritRepositoryDataSource.GerritRepositoryQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .repos(repos)
                .build();
        List<ControllerIngestionResult> results = new ArrayList<>();
        if (fetchPRs) {
            results.add(pullRequestController.ingest(jobContext, query));
        }

        // if we don't need to fetch repos AND results were empty - then return empty result
        boolean fetchRepos = BooleanUtils.isTrue(iterativeScanQuery.getShouldFetchRepos());
        boolean isEverythingEmpty = results.stream()
                .map(StorageResult.class::cast)
                .map(StorageResult::getRecords)
                .allMatch(CollectionUtils::isEmpty);
        if (!fetchRepos && isEverythingEmpty) {
            return new EmptyIngestionResult();
        }
        results.add(repositoryController.ingest(jobContext, query));

        return new ControllerIngestionResultList(results);
    }

    @Override
    public GerritIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GerritIterativeScanQuery.class);
    }
}
