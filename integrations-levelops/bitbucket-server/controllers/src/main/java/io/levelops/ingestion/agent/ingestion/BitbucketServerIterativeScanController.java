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
import io.levelops.integrations.bitbucket_server.models.BitbucketServerIterativeScanQuery;
import io.levelops.integrations.bitbucket_server.sources.BitbucketServerProjectDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Log4j2
public class BitbucketServerIterativeScanController implements DataController<BitbucketServerIterativeScanQuery> {

    private static final int BITBUCKET_SERVER_ONBOARDING_IN_DAYS = 30;

    private static final String BITBUCKET_SERVER_REPOS_METADATA_FIELD = "repos";
    private static final String BITBUCKET_SERVER_PROJECTS_METADATA_FIELD = "projects";
    private static final String BITBUCKET_SERVER_FETCH_PR_METADATA_FIELD = "fetch_prs";
    private static final String BITBUCKET_SERVER_FETCH_COMMITS_METADATA_FIELD = "fetch_commits";
    private static final String BITBUCKET_SERVER_FETCH_TAGS_METADATA_FIELD = "fetch_tags";
    private static final String BITBUCKET_SERVER_FETCH_COMMIT_FILES_METADATA_FIELD = "fetch_commit_files";
    private static final String BITBUCKET_SERVER_FETCH_PR_REVIEWS_METADATA_FIELD = "fetch_pr_reviews";
    private static final String BITBUCKET_SERVER_FETCH_PR_COMMITS_METADATA_FIELD = "fetch_pr_commits";
    private static final String BITBUCKET_SERVER_FETCH_PR_PATCHES_METADATA_FIELD = "fetch_pr_patches";//Placeholder

    private final ObjectMapper objectMapper;
    private final IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> commitController;
    private final IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> pullRequestController;
    private final IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> tagController;
    private final IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> repositoryController;
    private final InventoryService inventoryService;
    private final int onboardingInDays;

    @Builder
    public BitbucketServerIterativeScanController(ObjectMapper objectMapper,
                                                  IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> commitController,
                                                  IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> pullRequestController,
                                                  IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> tagController,
                                                  IntegrationController<BitbucketServerProjectDataSource.BitbucketServerProjectQuery> repositoryController,
                                                  InventoryService inventoryService,
                                                  Integer onboardingInDays) {
        this.inventoryService = inventoryService;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, BITBUCKET_SERVER_ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.repositoryController = repositoryController;
        this.commitController = commitController;
        this.pullRequestController = pullRequestController;
        this.tagController = tagController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, BitbucketServerIterativeScanQuery iterativeScanQuery) throws IngestException {
        boolean onboarding = iterativeScanQuery.getFrom() == null;
        iterativeScanQuery = fillNullValuesWithDefaults(iterativeScanQuery);
        Date from = iterativeScanQuery.getFrom() != null ?
                iterativeScanQuery.getFrom() :
                Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));
        Date to = iterativeScanQuery.getTo();

        // get custom repos list from integration metadata
        List<String> repos = null;
        List<String> projects = null;
        boolean fetchPRs = true;
        boolean fetchCommits = true;
        boolean fetchTags = true;
        boolean fetchCommitFiles = true;
        boolean fetchPRReviews = true;
        boolean fetchPRCommits = true;
        try {
            Integration integration = inventoryService.getIntegration(iterativeScanQuery.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            String reposCommaList = (String) metadata.get(BITBUCKET_SERVER_REPOS_METADATA_FIELD);
            String projectsCommaList = (String) metadata.get(BITBUCKET_SERVER_PROJECTS_METADATA_FIELD);
            if (StringUtils.isNotBlank(reposCommaList)) {
                repos = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(reposCommaList);
                log.info("Scanning specific Bitbucket repos: {}", repos);
            }
            if (StringUtils.isNotBlank(projectsCommaList)) {
                projects = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(projectsCommaList);
                log.info("Scanning specific Bitbucket projects: {}", projects);
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(BITBUCKET_SERVER_FETCH_PR_METADATA_FIELD))) {
                fetchPRs = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(BITBUCKET_SERVER_FETCH_COMMITS_METADATA_FIELD))) {
                fetchCommits = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(BITBUCKET_SERVER_FETCH_TAGS_METADATA_FIELD))) {
                fetchTags = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(BITBUCKET_SERVER_FETCH_COMMIT_FILES_METADATA_FIELD))) {
                fetchCommitFiles = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(BITBUCKET_SERVER_FETCH_PR_REVIEWS_METADATA_FIELD))) {
                fetchPRReviews = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(BITBUCKET_SERVER_FETCH_PR_COMMITS_METADATA_FIELD))) {
                fetchPRCommits = false;
            }
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " + iterativeScanQuery.getIntegrationKey(), e);
        }

        log.info("Bitbucket Server iterative scan: integration={}, from={}, to={}, job_id={}", iterativeScanQuery.getIntegrationKey(), from, to, jobContext.getJobId());

        BitbucketServerProjectDataSource.BitbucketServerProjectQuery query = BitbucketServerProjectDataSource.BitbucketServerProjectQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .repos(repos)
                .projects(projects)
                .fetchCommitFiles(fetchCommitFiles).fetchPrReviews(fetchPRReviews).fetchPRCommits(fetchPRCommits)
                .build();

        List<ControllerIngestionResult> results = new ArrayList<>();
        if (fetchCommits) {
            results.add(commitController.ingest(jobContext, query));
        }
        if (fetchTags && onboarding) {
            results.add(tagController.ingest(jobContext, query));
        }
        if (fetchPRs) {
            results.add(pullRequestController.ingest(jobContext, query));
        }

        boolean fetchRepos = BooleanUtils.isTrue(iterativeScanQuery.getShouldFetchRepos());
        boolean isEverythingEmpty = results.stream()
                .map(StorageResult.class::cast)
                .map(StorageResult::getRecords)
                .allMatch(CollectionUtils::isEmpty);
        if (!fetchRepos && isEverythingEmpty) {
            return new EmptyIngestionResult();
        }
        if (fetchRepos) {
            results.add(repositoryController.ingest(jobContext, query));
        }
        return new ControllerIngestionResultList(results);
    }

    @Override
    public BitbucketServerIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, BitbucketServerIterativeScanQuery.class);
    }

    private BitbucketServerIterativeScanQuery fillNullValuesWithDefaults(BitbucketServerIterativeScanQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onboardingInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        Date to = query.getTo() == null ? Date.from(Instant.now()) : query.getTo();
        return BitbucketServerIterativeScanQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .to(to)
                .shouldFetchRepos(query.getShouldFetchRepos())
                .build();
    }
}
