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
import io.levelops.integrations.azureDevops.models.Build;
import io.levelops.integrations.azureDevops.models.BuildResponse;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchBuildsService {
    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();

    private static final String STARTING_CURSOR = StringUtils.EMPTY;
    private static final int BATCH_SIZE = 100;

    public Stream<EnrichedProjectData> fetchBuilds(IngestionCachingService ingestionCachingService,
                                                   AzureDevopsClient azureDevopsClient,
                                                   AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) throws NoSuchElementException {
        Stream<ImmutablePair<Project, Build>> buildStream = projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState)
                .flatMap(project -> fetchPaginatedBuilds(azureDevopsClient, query, project, intermediateState)
                        .map(build -> ImmutablePair.of(project, build)));

        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(buildStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Project, List<ImmutablePair<Project, Build>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .builds(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    public static Stream<Build> fetchPaginatedBuilds(AzureDevopsClient azureDevopsClient,
                                                     AzureDevopsIterativeScanQuery query, Project project, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(STARTING_CURSOR, continuationToken -> {
            if (continuationToken == null)
                return null;
            return getBuildCursorPageData(azureDevopsClient, query, project, continuationToken, intermediateState);
        });
    }

    private static PaginationUtils.CursorPageData<Build> getBuildCursorPageData(AzureDevopsClient azureDevopsClient,
                                                                                AzureDevopsIterativeScanQuery query,
                                                                                Project project,
                                                                                String continuationToken,
                                                                                AzureDevopsIntermediateState intermediateState) {
        BuildResponse buildResponse = BuildResponse.builder().build();
        try {
            buildResponse = azureDevopsClient.getBuilds(project.getOrganization(), project.getName(),
                    query.getFrom(), query.getTo(), continuationToken);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to ingest builds for project : " + project.getName() + "  " +
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
                    .customMessage("Failed to ingest Builds for project : " + project.getName() + " of organization : " + project.getOrganization()
                            + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        String nextContinuationToken = buildResponse.getContinuationToken();
        return PaginationUtils.CursorPageData.<Build>builder()
                .data(buildResponse.getBuilds().parallelStream()
                        .collect(Collectors.toList()))
                .cursor(nextContinuationToken)
                .build();
    }
}
