package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.gitlab.models.GitlabIntermediateState;
import io.levelops.integrations.gitlab.models.GitlabIntermediateState.Stage;
import io.levelops.integrations.gitlab.sources.GitlabProjectDataSource;
import io.levelops.integrations.gitlab.sources.GitlabProjectPipelineDataSource;
import io.levelops.integrations.gitlab.models.GitlabIterativeScanQuery;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import io.levelops.integrations.gitlab.sources.GitlabUsersDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.integrations.gitlab.models.GitlabIntermediateState.updateJobContext;

@Log4j2
public class GitlabIterativeScanController implements DataController<GitlabIterativeScanQuery> {
    public static final int PR_COMMITS_LIMIT = 250;
    private static final int GITLAB_ONBOARDING_IN_DAYS = 14;
    private static final String GITLAB_PROJECTS_METADATA_FIELD = "repos";
    private static final String GITLAB_PROJECT_IDS_TO_EXCLUDE_METADATA_FIELD = "project_ids_to_exclude";
    private static final String GITLAB_FETCH_MR_METADATA_FIELD = "fetch_mrs";
    private static final String GITLAB_FETCH_COMMITS_METADATA_FIELD = "fetch_commits";
    private static final String GITLAB_FETCH_TAGS_METADATA_FIELD = "fetch_tags";
    private static final String GITLAB_FETCH_USERS_METADATA_FIELD = "fetch_users";
    private static final String GITLAB_FETCH_MILESTONES_METADATA_FIELD = "fetch_milestones";
    private static final String GITLAB_FETCH_BRANCHES_METADATA_FIELD = "fetch_branches";
    private static final String GITLAB_FETCH_PIPELINES_METADATA_FIELD = "fetch_pipelines";
    private static final String GITLAB_CHECK_PROJECT_MEMBERSHIP_METADATA_FIELD = "check_project_membership";
    private static final String GITLAB_FETCH_PR_PATCHES_METADATA_FIELD = "fetch_pr_patches";
    private static final String GITLAB_FETCH_COMMIT_PATCHES_METADATA_FIELD = "fetch_commit_patches";
    private static final String GITLAB_FETCH_STATE_EVENTS_METADATA_FIELD = "fetch_state_events";
    private static final String GITLAB_FETCH_PR_COMMIT_LIMIT_METADATA_FIELD = "pr_commit_limit";

    private final ObjectMapper objectMapper;
    private final IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> commitController;
    private final IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> tagController;
    private final IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> mergeRequestController;
    private final IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> projectController;
    private final IntegrationController<GitlabUsersDataSource.GitlabUserQuery> userController;
    private final IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> milestoneController;
    private final IntegrationController<GitlabQuery> issueController;
    private final IntegrationController<GitlabQuery> groupController;
    private final IntegrationController<GitlabProjectPipelineDataSource.GitlabProjectQuery> projectPipelineController;
    private final InventoryService inventoryService;
    private final int onboardingInDays;

    @Builder
    public GitlabIterativeScanController(ObjectMapper objectMapper,
                                         IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> commitController,
                                         IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> tagController,
                                         IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> mergeRequestController,
                                         IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> projectController,
                                         IntegrationController<GitlabUsersDataSource.GitlabUserQuery> userController,
                                         IntegrationController<GitlabProjectDataSource.GitlabProjectQuery> milestoneController,
                                         IntegrationController<GitlabProjectPipelineDataSource.GitlabProjectQuery> projectPipelineController,
                                         IntegrationController<GitlabQuery> issueController,
                                         IntegrationController<GitlabQuery> groupController,
                                         InventoryService inventoryService,
                                         Integer onboardingInDays) {
        this.inventoryService = inventoryService;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, GITLAB_ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.projectController = projectController;
        this.commitController = commitController;
        this.tagController = tagController;
        this.mergeRequestController = mergeRequestController;
        this.userController = userController;
        this.milestoneController = milestoneController;
        this.groupController = groupController;
        this.projectPipelineController = projectPipelineController;
        this.issueController = issueController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GitlabIterativeScanQuery gitlabIterativeScanQuery) throws IngestException {
        throw new NotImplementedException("This method is not implemented since the intermediateStateUpdater version is used");
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GitlabIterativeScanQuery iterativeScanQuery, IntermediateStateUpdater intermediateStateUpdater)
            throws IngestException {
        boolean onboarding = iterativeScanQuery.getFrom() == null;
        Date from = onboarding ? Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays))) :
                iterativeScanQuery.getFrom();
        Date to = iterativeScanQuery.getTo();

        List<String> projectIdsToExclude = null;
        List<String> projects = null;
        boolean fetchMRs = true;
        boolean fetchCommits = true;
        boolean fetchTags = true;
        boolean fetchMilestones = false;
        boolean fetchUsers = false;
        boolean fetchPipelines = true;
        boolean fetchGroups = false;
        boolean checkProjectMembership = true;
        boolean fetchCommitPatches = true;
        boolean fetchPrPatches = false;
        boolean fetchStateEvents = false;
        int prCommitsLimit;
        try {
            Integration integration = inventoryService.getIntegration(iterativeScanQuery.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            String projectsCommaList = (String) metadata.get(GITLAB_PROJECTS_METADATA_FIELD);
            String projectIdsToExcludeCommaList = (String) metadata.get(GITLAB_PROJECT_IDS_TO_EXCLUDE_METADATA_FIELD);
            if (StringUtils.isNotBlank(projectsCommaList)) {
                projects = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(projectsCommaList);
                log.info("Scanning specific Gitlab projects: {}", projects);
            }
            if (StringUtils.isNotBlank(projectIdsToExcludeCommaList)) {
                projectIdsToExclude = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(projectIdsToExcludeCommaList);
                log.info("Excluding Gitlab projects: {}", projectIdsToExclude);
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_FETCH_MR_METADATA_FIELD))) {
                fetchMRs = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_FETCH_COMMITS_METADATA_FIELD))) {
                fetchCommits = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_FETCH_TAGS_METADATA_FIELD))) {
                fetchTags = false;
            }
            if (BooleanUtils.isTrue((Boolean) metadata.get(GITLAB_FETCH_USERS_METADATA_FIELD))) {
                fetchUsers = true;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_FETCH_MILESTONES_METADATA_FIELD))) {
                fetchMilestones = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_FETCH_PIPELINES_METADATA_FIELD))) {
                fetchPipelines = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_CHECK_PROJECT_MEMBERSHIP_METADATA_FIELD))) {
                checkProjectMembership = false;
            }
            if (BooleanUtils.isTrue(iterativeScanQuery.getShouldFetchGroups())) {
                fetchGroups = true;
            }
            if (BooleanUtils.isTrue((Boolean) metadata.get(GITLAB_FETCH_PR_PATCHES_METADATA_FIELD))) {
                fetchPrPatches = true;
            }
            if (BooleanUtils.isTrue((Boolean) metadata.get(GITLAB_FETCH_STATE_EVENTS_METADATA_FIELD))) {
                fetchStateEvents = true;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(GITLAB_FETCH_COMMIT_PATCHES_METADATA_FIELD))) {
                fetchCommitPatches = false;
            }
            prCommitsLimit = ((Integer) metadata.getOrDefault(GITLAB_FETCH_PR_COMMIT_LIMIT_METADATA_FIELD, PR_COMMITS_LIMIT));
            log.info("FetchPrPatches: {}, fetchCommitPatches: {}, fetchStateEvents: {}, prCommitsLimit: {}",
                    fetchPrPatches, fetchCommitPatches, fetchStateEvents, prCommitsLimit);
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " +
                    iterativeScanQuery.getIntegrationKey(), e);
        }

        GitlabIntermediateState intermediateState = GitlabIntermediateState.parseIntermediateState(jobContext.getIntermediateState());
        final boolean isResuming = intermediateState.isResuming();

        log.info("Gitlab iterative scan: integration={}, from={}, to={}, job_id={}, alreadyCompletedStages={}, resumeFromRepo={}",
                iterativeScanQuery.getIntegrationKey(), from, to, jobContext.getJobId(), intermediateState.getCompletedStages(), intermediateState.getResumeFromRepo());

        GitlabProjectDataSource.GitlabProjectQuery projectQuery = GitlabProjectDataSource.GitlabProjectQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .projects(projects)
                .projectsIdsToExclude(projectIdsToExclude)
                .checkProjectMembership(checkProjectMembership)
                .fetchCommitPatches(fetchCommitPatches)
                .fetchPrPatches(fetchPrPatches)
                .fetchStateEvents(fetchStateEvents)
                .prCommitsLimit(prCommitsLimit)
                .build();

        GitlabProjectPipelineDataSource.GitlabProjectQuery pipelineQuery = GitlabProjectPipelineDataSource
                .GitlabProjectQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .projects(projects)
                .projectsIdsToExclude(projectIdsToExclude)
                .checkProjectMembership(checkProjectMembership)
                .build();

        GitlabQuery query = GitlabQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .build();

        GitlabUsersDataSource.GitlabUserQuery userQuery = GitlabUsersDataSource.GitlabUserQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(iterativeScanQuery.getShouldFetchAllUsers() ? null : from)
                .to(iterativeScanQuery.getShouldFetchAllUsers() ? null : to)
                .build();

        List<ControllerIngestionResult> results = new ArrayList<>();
        if (!intermediateState.getCompletedStages().contains(Stage.COMMITS) && fetchCommits) {
            results.add(commitController.ingest(jobContext, projectQuery, intermediateStateUpdater));
            log.info("Finished ingesting gitlab commits");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.COMMITS);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.TAGS) && fetchTags && onboarding) {
            results.add(tagController.ingest(jobContext, projectQuery, intermediateStateUpdater));
            log.info("Finished ingesting gitlab tags");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.TAGS);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.MRS) && fetchMRs) {
            results.add(mergeRequestController.ingest(jobContext, projectQuery, intermediateStateUpdater));
            log.info("Finished ingesting gitlab MRs");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.MRS);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.USERS) && fetchUsers) {
            results.add(userController.ingest(jobContext, userQuery, intermediateStateUpdater));
            log.info("Finished ingesting gitlab users");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.USERS);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.MILESTONES) && fetchMilestones) {
            results.add(milestoneController.ingest(jobContext, projectQuery, intermediateStateUpdater));
            log.info("Finished ingesting gitlab milestones");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.MILESTONES);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.GROUPS) && fetchGroups) {
            results.add(groupController.ingest(jobContext, query, intermediateStateUpdater));
            log.info("Finished ingesting gitlab groups");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.GROUPS);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.PIPELINES) && fetchPipelines) {
            results.add(projectPipelineController.ingest(jobContext, pipelineQuery, intermediateStateUpdater));
            log.info("Finished ingesting gitlab pipelines");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.PIPELINES);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }
        if (!intermediateState.getCompletedStages().contains(Stage.ISSUES)) {
            results.add(issueController.ingest(jobContext, query, intermediateStateUpdater));
            log.info("Finished ingesting gitlab issues");
            intermediateState = GitlabIntermediateState.markStageAsCompleted(intermediateState, Stage.ISSUES);
            jobContext = updateJobContext(jobContext, intermediateState);
            updateIntermediateState(intermediateStateUpdater, intermediateState);
        }

        // if we don't need to fetch projects AND results were empty - then return empty result
        boolean fetchProjects = false;
        //fetchProjects = BooleanUtils.isTrue(iterativeScanQuery.getShouldFetchProjects());
        boolean isEverythingEmpty = results.stream()
                .map(StorageResult.class::cast)
                .map(StorageResult::getRecords)
                .allMatch(CollectionUtils::isEmpty);
        if (!fetchProjects && isEverythingEmpty && !isResuming) {
            return new EmptyIngestionResult();
        }
        results.add(projectController.ingest(jobContext, projectQuery, intermediateStateUpdater));

        return new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results);
    }

    private void updateIntermediateState(IntermediateStateUpdater updater, GitlabIntermediateState intermediateState) {
        updater.updateIntermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), intermediateState));
    }

    @Override
    public GitlabIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GitlabIterativeScanQuery.class);
    }
}
