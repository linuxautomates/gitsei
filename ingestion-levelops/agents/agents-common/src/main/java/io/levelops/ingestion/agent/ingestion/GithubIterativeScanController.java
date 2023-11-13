package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.integrations.github.models.GithubIterativeScanQuery;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.github.model.GithubIntermediateState;
import io.levelops.integrations.github.model.GithubIntermediateState.Stage;
import io.levelops.integrations.github.sources.GithubProjectDataSource.GithubProjectQuery;
import io.levelops.integrations.github.sources.GithubRepositoryDataSource.GithubRepositoryQuery;
import io.levelops.integrations.github.sources.GithubUserDataSource;
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

import static io.levelops.integrations.github.model.GithubIntermediateState.markStageAsCompleted;
import static io.levelops.integrations.github.model.GithubIntermediateState.updateJobContext;

@Log4j2
public class GithubIterativeScanController implements DataController<GithubIterativeScanQuery> {

    private static final boolean ENABLE_RESUMABLE_ERRORS = true;
    private static final int GITHUB_ONBOARDING_IN_DAYS = 7;
    private static final String GITHUB_REPOS_METADATA_FIELD = "repos";
    private static final String GITHUB_PROJECTS_METADATA_FIELD = "projects";
    private static final String GITHUB_USER_ORGS_METADATA_FIELD = "user_orgs";
    private static final String GITHUB_FETCH_PR_METADATA_FIELD = "fetch_prs";
    private static final String GITHUB_FETCH_TAGS_METADATA_FIELD = "fetch_tags";
    private static final String GITHUB_FETCH_COMMITS_METADATA_FIELD = "fetch_commits";
    private static final String GITHUB_FETCH_ISSUES_METADATA_FIELD = "fetch_issues";
    private static final String GITHUB_FETCH_PROJECTS_METADATA_FIELD = "fetch_projects";
    private static final String GITHUB_FETCH_PROJECT_CARDS_METADATA_FIELD = "fetch_project_cards";
    private static final String GITHUB_FETCH_PR_COMMITS_METADATA_FIELD = "fetch_pr_commits";
    private static final String GITHUB_FETCH_PR_REVIEWS_METADATA_FIELD = "fetch_pr_reviews";
    private static final String GITHUB_FETCH_PR_PATCHES_METADATA_FIELD = "fetch_pr_patches";
    private static final String GITHUB_FETCH_USERS_METADATA_FIELD = "fetch_users";
    private static final String GITHUB_IS_PUSH_BASED_METADATA_FIELD = "is_push_based";
    private static final String GITHUB_APP_ID_METADATA_FIELD = "app_id";
    private final ObjectMapper objectMapper;
    private final IntegrationController<GithubRepositoryQuery> repositoryController;
    private final IntegrationController<GithubRepositoryQuery> commitController;
    private final IntegrationController<GithubRepositoryQuery> pullRequestController;
    private final IntegrationController<GithubRepositoryQuery> tagController;
    private final IntegrationController<GithubRepositoryQuery> issueController;
    private final IntegrationController<GithubProjectQuery> projectController;
    private final IntegrationController<GithubUserDataSource.GithubUserQuery> userController;
    private final InventoryService inventoryService;
    private final int defaultOnboardingInDays;

    @Builder
    public GithubIterativeScanController(ObjectMapper objectMapper,
                                         IntegrationController<GithubRepositoryQuery> repositoryController,
                                         IntegrationController<GithubRepositoryQuery> commitController,
                                         IntegrationController<GithubRepositoryQuery> pullRequestController,
                                         IntegrationController<GithubRepositoryQuery> tagController,
                                         IntegrationController<GithubRepositoryQuery> issueController,
                                         IntegrationController<GithubProjectQuery> projectController,
                                         IntegrationController<GithubUserDataSource.GithubUserQuery> userController,
                                         InventoryService inventoryService,
                                         Integer onboardingInDays) {
        this.inventoryService = inventoryService;
        this.defaultOnboardingInDays = MoreObjects.firstNonNull(onboardingInDays, GITHUB_ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.repositoryController = repositoryController;
        this.commitController = commitController;
        this.pullRequestController = pullRequestController;
        this.tagController = tagController;
        this.issueController = issueController;
        this.projectController = projectController;
        this.userController = userController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GithubIterativeScanQuery iterativeScanQuery) throws IngestException {

        List<String> repos = null;
        List<String> projects = null;
        List<String> organizations = null;
        boolean isGithubApp = false;
        boolean fetchPRs = true;
        boolean fetchTags = true;
        boolean fetchPrCommits = true;
        boolean fetchPrReviews = true;
        boolean fetchPrPatches = true;
        boolean fetchCommits = true;
        boolean fetchIssues = true;
        boolean fetchProjects = false;
        boolean fetchProjectCards = true;
        boolean fetchUsers = true;
        Integer onboardingInDays;
        try {
            Integration integration = inventoryService.getIntegration(iterativeScanQuery.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());

            boolean isPushBasedIntegration = (boolean) metadata.getOrDefault(GITHUB_IS_PUSH_BASED_METADATA_FIELD, false);
            if (isPushBasedIntegration) {
                log.info("Aborting ingestion for Push based integration. IntegrationKey : " + iterativeScanQuery.getIntegrationKey());
                return new EmptyIngestionResult();
            }

            String appId = (String) metadata.get(GITHUB_APP_ID_METADATA_FIELD);
            if (StringUtils.isNotBlank(appId)) {
                log.info("github_app=true");
                isGithubApp = true;
            }

            onboardingInDays = (Integer) metadata.getOrDefault("onboarding", defaultOnboardingInDays);

            // get comma separated repoIds from integration metadata
            String reposCommaList = (String) metadata.get(GITHUB_REPOS_METADATA_FIELD);
            if (StringUtils.isNotBlank(reposCommaList)) {
                repos = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(reposCommaList);
                log.info("Scanning specific Github repos: {}", repos);
            }

            // get comma separated projectsIds from integration metadata
            String projectsCommaList = (String) metadata.get(GITHUB_PROJECTS_METADATA_FIELD);
            if (StringUtils.isNotBlank(projectsCommaList)) {
                projects = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(projectsCommaList);
                log.info("Scanning specific Github projects: {}", projects);
            }

            // get comma separated organizations from integration metadata
            String orgsCommaList = (String) metadata.get(GITHUB_USER_ORGS_METADATA_FIELD);
            if (StringUtils.isNotBlank(projectsCommaList)) {
                organizations = CommaListSplitter.split(orgsCommaList);
                log.info("Scanning specific Github orgs for getting users: {}", organizations);
            }

            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_PR_METADATA_FIELD))) {
                fetchPRs = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_TAGS_METADATA_FIELD))) {
                fetchTags = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_COMMITS_METADATA_FIELD))) {
                fetchCommits = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_ISSUES_METADATA_FIELD))) {
                fetchIssues = false;
            }
            if (BooleanUtils.isTrue((Boolean) metadata.get(GITHUB_FETCH_PROJECTS_METADATA_FIELD))) {
                fetchProjects = true;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_PROJECT_CARDS_METADATA_FIELD))) {
                fetchProjectCards = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_PR_COMMITS_METADATA_FIELD))) {
                fetchPrCommits = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_PR_REVIEWS_METADATA_FIELD))) {
                fetchPrReviews = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_PR_PATCHES_METADATA_FIELD))) {
                fetchPrPatches = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITHUB_FETCH_USERS_METADATA_FIELD))) {
                fetchUsers = false;
            }
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " + iterativeScanQuery.getIntegrationKey(), e);
        }

        boolean onboarding = iterativeScanQuery.getFrom() == null;
        Date from = iterativeScanQuery.getFrom() != null
                ? iterativeScanQuery.getFrom()
                : Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));
        Date to = iterativeScanQuery.getTo() != null
                ? iterativeScanQuery.getTo()
                : new Date();

        GithubIntermediateState intermediateState = GithubIntermediateState.parseIntermediateState(jobContext.getIntermediateState());
        final boolean isResuming = intermediateState.isResuming();

        log.info("Github iterative scan: integration={}, from={}, to={}, job_id={}, alreadyCompletedStages={}, resumeFromRepo={}", iterativeScanQuery.getIntegrationKey(), from, to, jobContext.getJobId(),
                intermediateState.getCompletedStages(), intermediateState.getResumeFromRepo());

        GithubRepositoryQuery query = GithubRepositoryQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .onboarding(onboarding)
                .repos(repos)
                .githubApp(isGithubApp)
                .build();

        List<ControllerIngestionResult> results = new ArrayList<>();

        try {
            fetchUsers = fetchUsers && BooleanUtils.isTrue(iterativeScanQuery.getShouldFetchUsers());
            if (!intermediateState.getCompletedStages().contains(Stage.USERS) && fetchUsers) {
                log.info("Fetching users for integration: {}", iterativeScanQuery.getIntegrationKey());
                GithubUserDataSource.GithubUserQuery userQuery = GithubUserDataSource.GithubUserQuery.builder()
                        .integrationKey(iterativeScanQuery.getIntegrationKey())
                        .organizations(organizations)
                        .build();
                results.add(userController.ingest(jobContext, userQuery));
                intermediateState = markStageAsCompleted(intermediateState, Stage.USERS);
                jobContext = updateJobContext(jobContext, intermediateState);
            }
            if (!intermediateState.getCompletedStages().contains(Stage.COMMITS) && fetchCommits) {
                results.add(commitController.ingest(jobContext, query));

                intermediateState = markStageAsCompleted(intermediateState, Stage.COMMITS);
                jobContext = updateJobContext(jobContext, intermediateState);
            }
            if (!intermediateState.getCompletedStages().contains(Stage.PRS) && fetchPRs) {
                GithubRepositoryQuery prQuery = GithubRepositoryQuery.builder()
                        .integrationKey(iterativeScanQuery.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .onboarding(onboarding)
                        .repos(repos)
                        .githubApp(isGithubApp)
                        .fetchPrCommits(fetchPrCommits)
                        .fetchPrPatches(fetchPrPatches)
                        .fetchPrReviews(fetchPrReviews)
                        .build();
                results.add(pullRequestController.ingest(jobContext, prQuery));

                intermediateState = markStageAsCompleted(intermediateState, Stage.PRS);
                jobContext = updateJobContext(jobContext, intermediateState);
            }
            if (!intermediateState.getCompletedStages().contains(Stage.TAGS) && fetchTags) {
                results.add(tagController.ingest(jobContext, query));

                intermediateState = markStageAsCompleted(intermediateState, Stage.TAGS);
                jobContext = updateJobContext(jobContext, intermediateState);
            }
            if (!intermediateState.getCompletedStages().contains(Stage.ISSUES) && fetchIssues) {
                results.add(issueController.ingest(jobContext, query));

                intermediateState = markStageAsCompleted(intermediateState, Stage.ISSUES);
                jobContext = updateJobContext(jobContext, intermediateState);
            }
            if (!intermediateState.getCompletedStages().contains(Stage.PROJECTS) && fetchProjects) {
                GithubProjectQuery projectQuery = GithubProjectQuery.builder()
                        .integrationKey(iterativeScanQuery.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .fetchAllCards(fetchProjectCards && iterativeScanQuery.getShouldFetchAllCards())
                        .projects(projects)
                        .build();
                results.add(projectController.ingest(jobContext, projectQuery));

                intermediateState = markStageAsCompleted(intermediateState, Stage.PROJECTS);
                jobContext = updateJobContext(jobContext, intermediateState);
            }

            // if we don't need to fetch repos AND results were empty - then return empty result
            boolean fetchRepos = BooleanUtils.isTrue(iterativeScanQuery.getShouldFetchRepos());
            boolean isEverythingEmpty = results.stream()
                    .map(StorageResult.class::cast)
                    .map(StorageResult::getRecords)
                    .allMatch(CollectionUtils::isEmpty);
            if (!fetchRepos && isEverythingEmpty && !isResuming) {
                return new EmptyIngestionResult();
            }
            results.add(repositoryController.ingest(jobContext, query));
        } catch (Exception e) {
            log.error("Iterative scan was not completed successfully (completed_stages={}, resumable_error={})", intermediateState.getCompletedStages(), (e instanceof ResumableIngestException));
            if (!ENABLE_RESUMABLE_ERRORS) {
                throw e;
            }
            if (e instanceof ResumableIngestException) {
                // if the inner controller failed with a retry-able error, append the partial results
                ResumableIngestException resumableIngestException = (ResumableIngestException) e;
                results.add(resumableIngestException.getResult());
                throw resumableIngestException.toBuilder()
                        .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                        .build();
            } else if (!results.isEmpty()) {
                // if some results were fetched successfully, retry from the last step that failed
                throw ResumableIngestException.builder()
                        .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                        .intermediateState(ParsingUtils.toJsonObject(objectMapper, intermediateState))
                        .error(e)
                        .build();
            } else {
                throw e;
            }
        }
        // even if no failure occurred, we want to enable merging in case there are previous partial results
        return new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results);

    }

    @Override
    public GithubIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GithubIterativeScanQuery.class);
    }

}
