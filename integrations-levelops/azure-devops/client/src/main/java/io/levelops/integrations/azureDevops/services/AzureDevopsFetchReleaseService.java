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
import io.levelops.integrations.azureDevops.models.AzureDevopsRelease;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseDefinition;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseEnvironment;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseStep;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.ReleaseResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;
import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class AzureDevopsFetchReleaseService {
    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();
    private static final String STARTING_CURSOR = StringUtils.EMPTY;
    private static final int BATCH_SIZE = 5;

    public Stream<EnrichedProjectData> fetchReleases(IngestionCachingService ingestionCachingService,
                                                     AzureDevopsClient azureDevopsClient,
                                                     AzureDevopsIterativeScanQuery query,
                                                     AzureDevopsIntermediateState intermediateState) {
        Stream<ImmutablePair<AzureDevopsReleaseDefinition, AzureDevopsRelease>> definitionReleaseStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> {
                    Stream <ImmutablePair<AzureDevopsReleaseDefinition, AzureDevopsRelease>> releases = Stream.empty();
                    try {
                        releases = fetchPaginatedReleases(azureDevopsClient, project, query, intermediateState)
                                .map(release -> getReleaseDetails(azureDevopsClient, project, release, intermediateState))
                                .map(release -> release.toBuilder()
                                        .definition(release.getDefinition()
                                                .toBuilder().project(project)
                                                .build())
                                        .build())
                                .map(release-> ImmutablePair.of(release.getDefinition(), release));
                        return releases;
                    } catch (Exception e) {
                        log.warn("Failed to ingest releases for project : " + project.getName() + "  " +
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
                                .customMessage("Failed to ingest releases for project : " + project.getName() + " of organization : " + project.getOrganization()
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    return releases;
                });
            Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(definitionReleaseStream, BATCH_SIZE)
                    .flatMap(pairs -> {
                        Map<AzureDevopsReleaseDefinition, List<ImmutablePair<AzureDevopsReleaseDefinition, AzureDevopsRelease>>> groupedBatch =
                                pairs.stream()
                                        .collect(groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                        Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                                .map(entry -> EnrichedProjectData.builder()
                                        .project(entry.getKey().getProject())
                                        .definition(entry.getKey())
                                        .releases(entry.getValue().stream()
                                                .map(ImmutablePair::getRight)
                                                .collect(Collectors.toList()))
                                        .build());
                        return projectDataStream;
                    });
            return enrichedProjectDataStream;
    }

    public static Stream<AzureDevopsRelease> fetchPaginatedReleases(AzureDevopsClient azureDevopsClient, Project project,
                                                         AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(STARTING_CURSOR, continuationToken -> {
            if (continuationToken == null)
                return null;
            return getReleaseCursorPageData(azureDevopsClient, project, query, intermediateState, continuationToken);
        });
    }

    private static PaginationUtils.CursorPageData<AzureDevopsRelease> getReleaseCursorPageData(AzureDevopsClient azureDevopsClient, Project project,
                                           AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState,
                                           String continuationToken) {
        ReleaseResponse releaseResponse;
        try {
            List<AzureDevopsRelease> enrichedReleasesDetails;
            releaseResponse = getReleases(azureDevopsClient, project.getOrganization(),
                    project.getName(), intermediateState, continuationToken);
            if (CollectionUtils.isNotEmpty(releaseResponse.getReleases())) {
                enrichedReleasesDetails = releaseResponse.getReleases().stream()
                        .filter(release -> {
                            Instant createdOn = DateUtils.parseDateTime(release.getCreatedOn());
                            return createdOn != null && createdOn.isAfter(DateUtils.toInstant(query.getFrom())) &&
                                    createdOn.isBefore(DateUtils.toInstant(query.getTo()));
                        })
                        .collect(Collectors.toList());
                String nextContinuationToken = releaseResponse.getContinuationToken();
                return PaginationUtils.CursorPageData.<AzureDevopsRelease>builder()
                        .data(enrichedReleasesDetails)
                        .cursor(nextContinuationToken)
                        .build();
            }
        }
        catch (RuntimeStreamException e) {
            log.error("Encountered AzureDevops client while fetching releases for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered error while fetching releases for integration key: " +
                    query.getIntegrationKey(), e);
        }
        return PaginationUtils.CursorPageData.<AzureDevopsRelease>builder().build();
    }

    private static ReleaseResponse getReleases(AzureDevopsClient azureDevopsClient, String organization,
                                               String project, AzureDevopsIntermediateState intermediateState,
                                               String continuationToken) {
        try {
            return azureDevopsClient.getReleases(organization, project, continuationToken);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to fetch releases for project : " + project + "  " +
                    "of organization : " + project + " with completed stages : " + intermediateState.getCompletedStages() +
                    ", -----> " + e);
            throwException(e);
        }
        return ReleaseResponse.builder().build();
    }

    private AzureDevopsRelease getReleaseDetails(AzureDevopsClient azureDevopsClient, Project project, AzureDevopsRelease release, AzureDevopsIntermediateState intermediateState) {
        try{
            AzureDevopsRelease releaseDetails = azureDevopsClient.getRelease(project.getOrganization(), project.getName(), release.getId());
            List<AzureDevopsReleaseEnvironment> stages = enrichStageSteps(azureDevopsClient, releaseDetails);

            String startTime = null;
            String finishTime = null;
            if(stages.size() > 0) {
                if(stages.get(0).getSteps().size() > 0) {
                    startTime = stages.get(0).getSteps().get(0).getStartTime();
                    List<AzureDevopsReleaseStep> steps = stages.get(stages.size()-1).getSteps();
                    if(steps.size() > 0) {
                        finishTime = steps.get(steps.size()-1).getFinishTime();
                    }
                }
            }

            return release.toBuilder()
                    .startTime(startTime)
                    .finishTime(finishTime)
                    .variables(releaseDetails.getVariables())
                    .variableGroups(releaseDetails.getVariableGroups())
                    .artifacts(releaseDetails.getArtifacts())
                    .stages(stages)
                    .build();
        } catch(AzureDevopsClientException e) {
            log.warn("Failed to fetch release details for project : " + project.getName() + "  " +
                    "of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() +
                    ", -----> " + e);
            throwException(e);
        }
        return release;
    }

    private List<AzureDevopsReleaseEnvironment> enrichStageSteps(AzureDevopsClient azureDevopsClient, AzureDevopsRelease release){
        if(CollectionUtils.isEmpty(release.getEnvironments())){
            return new ArrayList<>();
        }
        List<AzureDevopsReleaseEnvironment> stages = new ArrayList<>();
        for(AzureDevopsReleaseEnvironment environment : release.getEnvironments()){
            if(StringUtils.isNotEmpty(environment.getCreatedOn())) {
                List<AzureDevopsReleaseStep> steps = enrichSteps(azureDevopsClient, environment);
                environment = environment.toBuilder().deploySteps(List.of()).steps(steps).build();
                stages.add(environment);
            }
        }
        return stages.stream()
                .sorted(Comparator.comparing(AzureDevopsReleaseEnvironment::getModifiedOn))
                .collect(Collectors.toList());
    }

    private List<AzureDevopsReleaseStep> enrichSteps(AzureDevopsClient azureDevopsClient, AzureDevopsReleaseEnvironment environment) {
        List<AzureDevopsReleaseStep> steps = new ArrayList<>();
        if (CollectionUtils.isEmpty(environment.getDeploySteps())) {
            return new ArrayList<>();
        }
        for (AzureDevopsReleaseEnvironment.DeployStep deployStep : environment.getDeploySteps()) {
            for (var releaseDeployPhase : ListUtils.emptyIfNull(deployStep.getReleaseDeployPhases())) {
                for (var deploymentJob : ListUtils.emptyIfNull(releaseDeployPhase.getDeploymentJobs())) {
                    if (deploymentJob.getJob() != null) {
                        steps.add(deploymentJob.getJob());
                    }
                    steps.addAll(ListUtils.emptyIfNull(deploymentJob.getTasks())
                            .stream()
                            .filter(Objects::nonNull)
                            .map(step -> {
                                        try {
                                            if (StringUtils.isNotEmpty(step.getLogUrl())) {
                                                return step.toBuilder()
                                                        .stepLogs(azureDevopsClient.getReleaseStepLogs(step.getLogUrl()))
                                                        .build();
                                            }
                                        } catch (AzureDevopsClientException e) {
                                            log.error("Failed to get step logs of id:" + step.getId() + " name:" + step.getName() + " reason: " + e);
                                        }
                                        return step;
                                    }
                            ).collect(Collectors.toList())
                    );
                }
            }
        }
        return steps;
    }
}
