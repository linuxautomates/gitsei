package io.levelops.integrations.azureDevops.services;

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
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchWorkItemFieldsService {

    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();
    private static final int INGESTION_BATCH_SIZE = 100;

    public Stream<EnrichedProjectData> fetchWorkItemsFields(IngestionCachingService ingestionCachingService,
                                                            AzureDevopsClient azureDevopsClient,
                                                            AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        Stream<ImmutablePair<Project, WorkItemField>> workItemsFieldsStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> streamWorkItemsFields(azureDevopsClient, project, intermediateState)
                        .map(workItem -> ImmutablePair.of(project, workItem))
                );
        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(workItemsFieldsStream, INGESTION_BATCH_SIZE)
                .flatMap(pairs -> {
                    log.info("fetchWorkItemsFields: Adding pairs of type List<ImmutablePair<Project, WorkItemFields>>");
                    Map<Project, List<ImmutablePair<Project, WorkItemField>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .workItemFields(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    public static Stream<WorkItemField> streamWorkItemsFields(AzureDevopsClient azureDevopsClient, Project project, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getFields(project.getOrganization(), project.getName()).stream();
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to ingest WorkItemFields Metadata for project : " + project.getName() + "  " +
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
                    .customMessage("Failed to ingest project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return Stream.empty();
    }
}

