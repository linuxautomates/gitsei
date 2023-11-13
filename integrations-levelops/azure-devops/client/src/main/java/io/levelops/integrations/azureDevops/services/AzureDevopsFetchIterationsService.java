package io.levelops.integrations.azureDevops.services;

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
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Iteration;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.Team;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchIterationsService {

    private final AzureDevopsProjectProviderService projectProviderService = new AzureDevopsProjectProviderService();
    private static final int INGESTION_BATCH_SIZE = 500;
    private static final int DEFAULT_PAGE_SIZE = 25;

    public Stream<EnrichedProjectData> fetchIterations(IngestionCachingService ingestionCachingService,
                                                       AzureDevopsClient azureDevopsClient,
                                                       AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        boolean fetchOnlyCurrent = BooleanUtils.isFalse(query.getFetchAllIterations());
        Stream<ImmutablePair<Project, Iteration>> iterationStream = Objects.requireNonNull(projectProviderService
                        .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> {
                    Stream<ImmutablePair<Project, Iteration>> iterations = null;
                    try {
                        iterations = streamIterations(azureDevopsClient, project, fetchOnlyCurrent, intermediateState)
                                .map(iteration -> ImmutablePair.of(project, iteration));
                    } catch (Exception e) {
                        log.warn("fetchIterations: failed for projects and iterations for org " +
                                project.getOrganization() + ", project " + project.getName(), e);
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(project.getOrganization())
                                .resumeFromProject(project.getName())
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest Iterations for project : " + project.getName() + " of organization : " + project.getOrganization()
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    log.info("fetchIterations: Fetched projects and iterations for org {}, project {}",
                            project.getOrganization(), project.getName());
                    return iterations;
                });
        return StreamUtils.partition(iterationStream, INGESTION_BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Project, List<ImmutablePair<Project, Iteration>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .iterations(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }

    public static Stream<Iteration> streamIterations(AzureDevopsClient azureDevopsClient,
                                                     Project project,
                                                     boolean fetchOnlyCurrent, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset ->
                listIterations(azureDevopsClient, project.getOrganization(), project.getName(), offset, fetchOnlyCurrent, intermediateState));
    }

    public static List<Iteration> listIterations(AzureDevopsClient azureDevopsClient,
                                                 String organization,
                                                 String project,
                                                 int offset,
                                                 boolean fetchOnlyCurrent,
                                                 AzureDevopsIntermediateState intermediateState) {
        List<Team> projectTeams = List.of();
        try {
            projectTeams = azureDevopsClient.getTeams(organization, project, offset);
        } catch (AzureDevopsClientException e) {
            log.warn("fetchIterations: failed for teams for org " +
                    organization + ", project " + project, e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest teams for project : " + project + " of organization : " + organization
                            + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }

        return projectTeams.stream()
                .flatMap(team -> {
                    List<Iteration> teamIterations = List.of();
                    try {
                        teamIterations = azureDevopsClient.getIterations(organization, project, team.getId(), fetchOnlyCurrent);
                    } catch (AzureDevopsClientException e) {
                        log.warn("fetchIterations: failed for Iterations for org " +
                                organization + ", project " + project, e);
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(organization)
                                .resumeFromProject(project)
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest Iterations for project : " + project + " of organization : " + organization
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    return teamIterations.stream();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
