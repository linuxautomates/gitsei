package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Label;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.AZURE_FAILED_RESPONSE_PREDICATE;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchLabelsService {


    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int BATCH_SIZE = 100;

    public Stream<EnrichedProjectData> fetchTfvcLabels(IngestionCachingService ingestionCachingService,
                                                       AzureDevopsClient azureDevopsClient,
                                                       AzureDevopsIterativeScanQuery query,
                                                       AzureDevopsIntermediateState intermediateState) throws NoSuchElementException {
        Stream<ImmutablePair<Project, Label>> labelsStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .filter(Project::getTfvcEnabled)
                .flatMap(project -> fetchPaginatedLabels(azureDevopsClient, query, project, intermediateState)
                            .map(label -> ImmutablePair.of(project, label))
                );
        
        return StreamUtils.partition(labelsStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Project, List<ImmutablePair<Project, Label>>> groupedBatch = pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .labels(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }

    private Stream<Label> fetchPaginatedLabels(AzureDevopsClient azureDevopsClient,
                                               AzureDevopsIterativeScanQuery query, Project project,
                                               AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset ->
                getLabels(azureDevopsClient, project.getOrganization(), project.getName(), offset, intermediateState));
    }

    public List<Label> getLabels(AzureDevopsClient azureDevopsClient,
                                 String organization,
                                 String project, int offset,
                                 AzureDevopsIntermediateState intermediateState) {
        List<Label> labels = new ArrayList<>();
        try {
            return azureDevopsClient.getLabels(organization, project, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (Exception e) {
            log.warn("Failed to ingest TfvcLabels for project : " + project + "  " +
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
                    .customMessage("Failed to ingest TfvcLabels for project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return labels;
    }
}
