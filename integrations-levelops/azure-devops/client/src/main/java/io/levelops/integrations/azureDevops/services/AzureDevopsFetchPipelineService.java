package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.AzureDevopsPipelineRunStageStep;
import io.levelops.integrations.azureDevops.models.BuildChange;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Pipeline;
import io.levelops.integrations.azureDevops.models.PipelineResponse;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.Run;
import io.levelops.integrations.azureDevops.models.RunResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchPipelineService {
    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();

    private static final String STARTING_CURSOR = StringUtils.EMPTY;
    private static final int BATCH_SIZE = 5;
    private static final int MAX_LOG_SIZE = 102400; // 100 KB

    public Stream<EnrichedProjectData> fetchPipelines(IngestionCachingService ingestionCachingService,
                                                      AzureDevopsClient azureDevopsClient,
                                                      AzureDevopsIterativeScanQuery query,
                                                      AzureDevopsIntermediateState intermediateState) {
        Stream<ImmutablePair<Pipeline, Run>> pipelineRunStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> {
                    Stream<Pipeline> pipelineStream = Stream.empty();
                    {
                        try {
                            pipelineStream = fetchPaginatedPipelines(azureDevopsClient, query, project, intermediateState);
                            return pipelineStream;
                        } catch (Exception e) {
                            log.warn("Failed to ingest Pipelines for project : " + project.getName() + "  " +
                                    "of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() +
                                    "-----> " + e);
                            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                    .resumeFromOrganization(project.getOrganization())
                                    .resumeFromProject(project.getName())
                                    .build();
                            List<ControllerIngestionResult> results = List.of();
                            ResumableIngestException build = ResumableIngestException.builder()
                                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                            .customMessage("Failed to ingest Pipelines for project : " + project.getName() + " of organization : " + project.getOrganization()
                                                    + " with completed stages : " + intermediateState.getCompletedStages())
                                            .error(e)
                                            .build();
                                    throwException(build);
                                }
                                return pipelineStream;
                            }
                        }
                )
                .flatMap(pipeline -> {
                    Stream<ImmutablePair<Pipeline, Run>> runs = Stream.empty();
                    try {
                        runs = streamRuns(azureDevopsClient, pipeline, query, intermediateState)
                                .map(run -> {
                                    List<AzureDevopsPipelineRunStageStep> stageSteps = fetchBuildStageStep(azureDevopsClient, pipeline.getProject(), run.getId(), intermediateState);
                                    List<AzureDevopsPipelineRunStageStep> stages = enrichBuildStages(azureDevopsClient, stageSteps);
                                    return ImmutablePair.of(pipeline, run.toBuilder().stages(stages).build());
                                });
                        return runs;
                    } catch (Exception e) {
                        log.warn("Failed to ingest Runs for project : " + pipeline.getProject().getName() + "  " +
                                "of organization : " + pipeline.getProject().getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() +
                                "-----> " + e);
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(pipeline.getProject().getOrganization())
                                .resumeFromProject(pipeline.getProject().getName())
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest project : " + pipeline.getProject().getName() + " of organization : " + pipeline.getProject().getOrganization()
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    return runs;
                });

        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(pipelineRunStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Pipeline, List<ImmutablePair<Pipeline, Run>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .pipeline(entry.getKey())
                                    .pipelineRuns(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    private static List<AzureDevopsPipelineRunStageStep> fetchBuildStageStep(AzureDevopsClient azureDevopsClient,
                                                                             Project project,
                                                                             int runId,
                                                                             AzureDevopsIntermediateState intermediateState) {
        List<AzureDevopsPipelineRunStageStep> stageSteps = new ArrayList<>();
        try {
            stageSteps = azureDevopsClient.getBuildTimeline(project.getOrganization(), project.getName(), runId);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to ingest build stages for project : " + project.getName() + "  " +
                    "of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() +
                    "-----> " + e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest Build stages for project : " + project.getName() + " of organization : " + project.getOrganization()
                            + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return stageSteps;
    }
    private static List<AzureDevopsPipelineRunStageStep> enrichBuildStages(AzureDevopsClient azureDevopsClient, List<AzureDevopsPipelineRunStageStep> stageSteps) {
        List<AzureDevopsPipelineRunStageStep> stages = stageSteps.stream()
                .filter(stage-> Objects.equals(stage.getType(), "Stage")).collect(Collectors.toList());
        return stages.stream()
                .map(stage -> stage.toBuilder().steps(enrichBuildSteps(azureDevopsClient, stageSteps, stage)).build())
                .collect(Collectors.toList());
    }

    private static List<AzureDevopsPipelineRunStageStep> enrichBuildSteps(AzureDevopsClient azureDevopsClient,
                                                                          List<AzureDevopsPipelineRunStageStep> stageSteps,
                                                                          AzureDevopsPipelineRunStageStep stage) {
        List<AzureDevopsPipelineRunStageStep> phases = ListUtils.emptyIfNull(stageSteps).stream()
                .filter(s -> s.getParentId() != null && s.getParentId().equals(stage.getId()) && "Phase".equals(s.getType()))
                .collect(Collectors.toList());
        List<AzureDevopsPipelineRunStageStep> allSteps = new ArrayList<>();
        phases.stream().forEach(phase -> {
            List<AzureDevopsPipelineRunStageStep> jobs = ListUtils.emptyIfNull(stageSteps).stream()
                    .filter(s-> s.getParentId() != null && s.getParentId().equals(phase.getId()) && "Job".equals(s.getType()))
                    .collect(Collectors.toList());

            jobs.stream().forEach(job -> {
                List<AzureDevopsPipelineRunStageStep> steps = stageSteps.stream()
                        .filter(s-> s.getParentId() != null && s.getParentId().equals(job.getId()))
                        .collect(Collectors.toList());

                steps.add(job);
                steps = steps.stream().map(step -> {
                    if(step.getLog() == null || StringUtils.isEmpty(step.getLog().getUrl())){
                        return step;
                    }
                    List<String> stepLogs = new ArrayList<>();
                    try {
                        stepLogs = azureDevopsClient.getStepLogs(step.getLog().getUrl());
                    } catch (AzureDevopsClientException e) {
                        log.error("Failed to fetch step logs for step id: " + step.getId() + " reason: " + e);
                    }
                    String logs = String.join("\n", stepLogs);
                    log.debug("Get step log size: " + logs.length() + " for step id: " + step.getId());
                    return step.toBuilder()
                            .stepLogs(StringUtils.truncate(logs, MAX_LOG_SIZE))
                            .build();
                }).collect(Collectors.toList());
                allSteps.addAll(steps);
            });

        });
        return allSteps;
    }

    public static Stream<Pipeline> fetchPaginatedPipelines(AzureDevopsClient azureDevopsClient,
                                                           AzureDevopsIterativeScanQuery query, Project project,
                                                           AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(STARTING_CURSOR, continuationToken -> {
            if (continuationToken == null)
                return null;
            return getPipelineCursorPageData(azureDevopsClient, query, project, continuationToken, intermediateState);
        });
    }

    private static PaginationUtils.CursorPageData<Pipeline> getPipelineCursorPageData(AzureDevopsClient client,
                                                                                      AzureDevopsIterativeScanQuery query,
                                                                                      Project project,
                                                                                      String continuationToken,
                                                                                      AzureDevopsIntermediateState intermediateState) {
        PipelineResponse pipelineResponse;
        try {
            List<Pipeline> enrichedPipelineDetails;
            pipelineResponse = getPipelines(client, project.getOrganization(), project.getName(), continuationToken, intermediateState);
            if (pipelineResponse != null) {
                enrichedPipelineDetails = pipelineResponse.getPipelines().stream()
                        .map(pipeline -> {
                            Pipeline pipelineDetails = null;
                            try {
                                pipelineDetails = client.getPipeline(project.getOrganization(),
                                        project.getName(), pipeline.getId());
                            } catch (AzureDevopsClientException e) {
                                log.warn("Failed to fetch pipelines for project : " + project.getName() + "  " +
                                        "of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() + ", -----> " + e);
                                AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                        .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                        .resumeFromOrganization(project.getOrganization())
                                        .resumeFromProject(project.getName())
                                        .build();
                                ResumableIngestException build = ResumableIngestException.builder()
                                        .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                                        .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                        .customMessage("Failed to ingest pipelines for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                                        .error(e)
                                        .build();
                                throwException(build);
                            }
                            return pipeline.toBuilder()
                                    .project(project)
                                    .configuration(pipelineDetails.getConfiguration()).build();
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                String nextContinuationToken = pipelineResponse.getContinuationToken();
                return PaginationUtils.CursorPageData.<Pipeline>builder()
                        .data(enrichedPipelineDetails)
                        .cursor(nextContinuationToken)
                        .build();
            }
        } catch (RuntimeStreamException e) {
            log.error("Encountered AzureDevops client while fetching pipelines for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered error while fetching pipelines for integration key: " +
                    query.getIntegrationKey(), e);
        }
        return PaginationUtils.CursorPageData.<Pipeline>builder().build();
    }


    private Stream<Run> streamRuns(AzureDevopsClient azureDevopsClient, Pipeline pipeline,
                                   AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        Project project = pipeline.getProject();
        RunResponse runResponse = getRuns(azureDevopsClient, project.getOrganization(), project.getName(),
                pipeline.getId(), intermediateState);
        List<Run> enrichedRuns = new ArrayList<>();
        Optional<Run> reducedRuns = Optional.empty();
        if (CollectionUtils.isNotEmpty(runResponse.getRuns())) {
            reducedRuns = runResponse.getRuns().stream()
                    .takeWhile(run -> {
                        Instant createdAt = DateUtils.parseDateTime(run.getCreatedDate());
                        Instant finishedAt = DateUtils.parseDateTime(run.getFinishedDate());
                        return createdAt != null && createdAt.isAfter(DateUtils.toInstant(query.getFrom())) &&
                                finishedAt != null && finishedAt.isBefore(DateUtils.toInstant(query.getTo()));
                    })
                    .reduce((firstRun, secondRun) -> {
                        try {
                            firstRun = firstRun.toBuilder()
                                    .commitIds(azureDevopsClient.getBuildCommits(project.getOrganization(),
                                            project.getName(), firstRun.getId(), secondRun.getId())
                                            .stream().map(BuildChange::getId).collect(Collectors.toList()))
                                    .build();
                            enrichedRuns.add(firstRun);
                        } catch (AzureDevopsClientException e) {
                            log.warn("process: encountered client exception while fetching commits "
                                    + e.getMessage(), e);
                            throw new RuntimeStreamException("Encountered error while fetching commits for project: " +
                                    project, e);
                        }
                        return secondRun;
                    });
        }
        List<Run> runs = CollectionUtils.isEmpty(enrichedRuns) ?
                reducedRuns.stream().collect(Collectors.toList()) :
                ListUtils.union(enrichedRuns, reducedRuns.stream().collect(Collectors.toList()));
        return runs.stream();
    }

    private static PipelineResponse getPipelines(AzureDevopsClient azureDevopsClient, String organization, String project,
                                                 String continuationToken, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getPipelines(organization, project, continuationToken);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to fetch pipelines for project : " + project + "  " +
                    "of organization : " + project + " with completed stages : " + intermediateState.getCompletedStages() + ", -----> " + e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest pipelines for project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return PipelineResponse.builder().build();
    }

    private static RunResponse getRuns(AzureDevopsClient azureDevopsClient, String organization, String project,
                                       int pipelineId, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getRuns(organization, project, pipelineId);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to fetch runs for project : " + project + "  " +
                    "of organization : " + project + " with completed stages : " + intermediateState.getCompletedStages() +
                    ", -----> " + e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest runs for project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return RunResponse.builder().build();
    }
}