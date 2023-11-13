package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.utils.JobCategory;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState.markStageAsCompleted;
import static io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState.updateJobContext;

@Log4j2
public class AzureDevopsIterativeScanController implements DataController<AzureDevopsIterativeScanQuery> {

    private static final int ONBOARDING_IN_DAYS = 90;
    private static final boolean ENABLE_RESUMABLE_ERRORS = true;
    private static final String FETCH_PRS = "fetch_prs";
    private static final String FETCH_ITERATIONS = "fetch_iterations";
    private static final String FETCH_TEAMS = "fetch_teams";
    private static final String FETCH_PIPELINES = "fetch_pipelines";
    private static final String FETCH_RELEASES = "fetch_releases";
    private static final String FETCH_BUILDS = "fetch_builds";
    private static final String FETCH_WORKITEMS = "fetch_work_items";
    private static final String FETCH_WORKITEM_COMMENTS = "fetch_work_items_comments";
    private static final String FETCH_CHANGESETS = "fetch_change_sets";
    private static final String FETCH_METADATA = "fetch_metadata";
    private static final String FETCH_LABELS = "fetch_labels";
    private static final String FETCH_BRANCHES = "fetch_branches";
    private static final String FETCH_WORKITEM_HISTORIES = "fetch_workitem_histories";
    private static final String FETCH_WORKITEM_FIELDS = "fetch_workitem_fields";
    private static final String FETCH_COMMITS = "fetch_commits";
    private static final String FETCH_TAGS = "fetch_tags";

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final int onboardingInDays;

    private final IntegrationController<AzureDevopsIterativeScanQuery> commitController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> pullRequestController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> pipelineController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> releaseController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> buildsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> workItemsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> changesetsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> metadataController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> branchesController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> labelsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> workItemsHistoriesController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> workItemsFieldsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> iterationsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> teamsController;
    private final IntegrationController<AzureDevopsIterativeScanQuery> tagsController;

    @Builder
    public AzureDevopsIterativeScanController(ObjectMapper objectMapper,
                                              InventoryService inventoryService,
                                              IntegrationController<AzureDevopsIterativeScanQuery> commitController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> pullRequestController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> pipelineController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> releaseController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> buildsController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> workItemsController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> changesetsController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> metadataController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> branchesController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> labelsController,
                                              Integer onboardingInDays,
                                              IntegrationController<AzureDevopsIterativeScanQuery> workItemsHistoriesController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> workItemsFieldsController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> iterationsController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> teamsController,
                                              IntegrationController<AzureDevopsIterativeScanQuery> tagsController) {
        this.inventoryService = inventoryService;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.commitController = commitController;
        this.pullRequestController = pullRequestController;
        this.pipelineController = pipelineController;
        this.releaseController = releaseController;
        this.buildsController = buildsController;
        this.workItemsController = workItemsController;
        this.changesetsController = changesetsController;
        this.metadataController = metadataController;
        this.branchesController = branchesController;
        this.labelsController = labelsController;
        this.workItemsHistoriesController = workItemsHistoriesController;
        this.workItemsFieldsController = workItemsFieldsController;
        this.iterationsController = iterationsController;
        this.teamsController = teamsController;
        this.tagsController = tagsController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext,
                                            AzureDevopsIterativeScanQuery iterativeScanQuery) throws IngestException {
        boolean onboarding = iterativeScanQuery.getFrom() == null || iterativeScanQuery.getFetchOnce();
        iterativeScanQuery = fillNullValuesWithDefaults(iterativeScanQuery);
        Date from = iterativeScanQuery.getFrom() != null ?
                iterativeScanQuery.getFrom() :
                Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));
        Date to = iterativeScanQuery.getTo() != null
                ? iterativeScanQuery.getTo()
                : new Date();
        AzureDevopsIntermediateState intermediateState = AzureDevopsIntermediateState.parseIntermediateState(jobContext.getIntermediateState());

        log.info("AzureDevops iterative scan: integration={}, from={}, to={}, job_id={}, alreadyCompletedStages={}, resumeFromProject={}," +
                        "resumeFromOrg={}", iterativeScanQuery.getIntegrationKey(), from, to, jobContext.getJobId(),
                intermediateState.getCompletedStages(), intermediateState.getResumeFromProject(),intermediateState.getResumeFromOrganization());
        // get integration metadata
        boolean fetchPRs = true;
        boolean fetchIterations = true;
        boolean fetchTeams = true;
        boolean fetchPipelines = true;
        boolean fetchReleases = true;
        boolean fetchBuilds = true;
        boolean fetchWorkItems = true;
        boolean fetchWorkItemComments = true;
        boolean fetchChangeSets = true;
        boolean fetchMetadata = true;
        boolean fetchLabels = true;
        boolean fetchBranches = true;
        boolean fetchWorkItemHistories = true;
        boolean fetchWorkItemFields = true;
        boolean fetchCommits = true;
        boolean fetchTags = true;
        try {
            Integration integration = inventoryService.getIntegration(iterativeScanQuery.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_PRS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_PRS))) {
                fetchPRs = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_ITERATIONS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_ITERATIONS))) {
                fetchIterations = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_TEAMS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_TEAMS))) {
                fetchTeams = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_PIPELINES))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_PIPELINES))) {
                fetchPipelines = false;
            }
            if (metadata.get(FETCH_RELEASES) == null || BooleanUtils.isFalse((Boolean) metadata.get(FETCH_RELEASES))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_RELEASES))) {
                fetchReleases = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_BUILDS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_BUILDS))) {
                fetchBuilds = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_BRANCHES))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_BRANCHES))) {
                fetchBranches = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_WORKITEMS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_WORKITEMS))) {
                fetchWorkItems = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_WORKITEM_COMMENTS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_WORKITEM_COMMENTS))) {
                fetchWorkItemComments = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_WORKITEM_HISTORIES))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_WORKITEM_HISTORIES))) {
                fetchWorkItemHistories = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_WORKITEM_FIELDS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_WORKITEM_FIELDS))) {
                fetchWorkItemFields = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_CHANGESETS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_CHANGESETS))) {
                fetchChangeSets = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_METADATA))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_METADATA))) {
                fetchMetadata = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_LABELS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_LABELS))) {
                fetchLabels = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_COMMITS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_COMMITS))) {
                fetchCommits = false;
            }
            if (BooleanUtils.isFalse((Boolean) metadata.get(FETCH_TAGS))
                    || BooleanUtils.isFalse((Boolean) iterativeScanQuery.getIngestionFlags().get(FETCH_TAGS)) || !onboarding) {
                fetchTags = false;
            }
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " + iterativeScanQuery.getIntegrationKey(), e);
        }

        iterativeScanQuery = iterativeScanQuery.toBuilder()
                .ingestionFlags(MapUtils.append(iterativeScanQuery.getIngestionFlags(),
                        FETCH_WORKITEM_COMMENTS, fetchWorkItemComments))
                .build();

        List<ControllerIngestionResult> results = new ArrayList<>();
        try {
            if ((iterativeScanQuery.getJobCategory() == null || iterativeScanQuery.getJobCategory() == JobCategory.SCM_GIT)) {
                if (fetchCommits && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.COMMITS)) {
                    results.add(commitController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.COMMITS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchPRs && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.PRS)) {
                    results.add(pullRequestController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.PRS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchTags && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.TAGS)) {
                    results.add(tagsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.TAGS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
            }

            if ((iterativeScanQuery.getJobCategory() == null || iterativeScanQuery.getJobCategory() == JobCategory.SCM_TFVC)) {
                if (fetchBranches && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.BRANCHES)) {
                    results.add(branchesController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.BRANCHES);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchChangeSets && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.CHANGESETS)) {
                    results.add(changesetsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.CHANGESETS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchLabels && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.LABELS)) {
                    results.add(labelsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.LABELS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
            }

            if ((iterativeScanQuery.getJobCategory() == null || iterativeScanQuery.getJobCategory() == JobCategory.CICD)) {
                if (fetchPipelines && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.PIPELINES)) {
                    results.add(pipelineController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.PIPELINES);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchReleases && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.RELEASES)) {
                    results.add(releaseController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.RELEASES);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }

                if (fetchBuilds && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.BUILDS)) {
                        results.add(buildsController.ingest(jobContext, iterativeScanQuery));
                        intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.BUILDS);
                        jobContext = updateJobContext(jobContext, intermediateState);
                }
            }

            if ((iterativeScanQuery.getJobCategory() == null || iterativeScanQuery.getJobCategory() == JobCategory.BOARDS_1)) {
                if (fetchWorkItemFields && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.WORKITEM_FIELDS)) {
                    results.add(workItemsFieldsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.WORKITEM_FIELDS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchMetadata && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.METADATA)) {
                    results.add(metadataController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.METADATA);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchWorkItems && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.WORKITEMS)) {
                    results.add(workItemsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.WORKITEMS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchTeams && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.TEAMS)) {
                    results.add(teamsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.TEAMS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
            }

            if ((iterativeScanQuery.getJobCategory() == null || iterativeScanQuery.getJobCategory() == JobCategory.BOARDS_2)) {
                if (fetchIterations && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.ITERATIONS)) {
                    results.add(iterationsController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.ITERATIONS);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
                if (fetchWorkItemHistories && !intermediateState.getCompletedStages().contains(AzureDevopsIntermediateState.Stage.WORKITEM_HISTORIES)) {
                    results.add(workItemsHistoriesController.ingest(jobContext, iterativeScanQuery));
                    intermediateState = markStageAsCompleted(intermediateState, AzureDevopsIntermediateState.Stage.WORKITEM_HISTORIES);
                    jobContext = updateJobContext(jobContext, intermediateState);
                }
            }

        } catch (Exception e) {
            log.error("Iterative scan was not completed successfully (completed_stages={}, resumable_error={})",
                    intermediateState.getCompletedStages(), (e instanceof ResumableIngestException));
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
        return new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results);
    }

    @Override
    public AzureDevopsIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, AzureDevopsIterativeScanQuery.class);
    }

    private AzureDevopsIterativeScanQuery fillNullValuesWithDefaults(AzureDevopsIterativeScanQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onboardingInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        Date to = query.getTo() == null ? Date.from(Instant.now().atZone(ZoneId.systemDefault()).toInstant()) :
                Date.from(query.getTo().toInstant().atZone(ZoneId.systemDefault()).toInstant());
        return AzureDevopsIterativeScanQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .to(to)
                .fetchAllIterations(query.getFetchAllIterations())
                .fetchMetadata(query.getFetchMetadata())
                .ingestionFlags(MapUtils.emptyIfNull(query.getIngestionFlags()))
                .jobCategory(query.getJobCategory())
                .build();
    }
}
