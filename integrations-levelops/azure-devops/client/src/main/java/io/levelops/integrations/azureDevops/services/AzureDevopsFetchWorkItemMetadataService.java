package io.levelops.integrations.azureDevops.services;

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
import io.levelops.integrations.azureDevops.models.Metadata;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.WorkItemType;
import io.levelops.integrations.azureDevops.models.WorkItemTypeCategory;
import io.levelops.integrations.azureDevops.models.WorkItemTypeState;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchWorkItemMetadataService {

    private final AzureDevopsProjectProviderService projectProviderService = new AzureDevopsProjectProviderService();

    public Stream<EnrichedProjectData> fetchWorkItemsMetadata(IngestionCachingService ingestionCachingService,
                                                              AzureDevopsClient azureDevopsClient,
                                                              AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        Stream<EnrichedProjectData> workItemStream = Objects.requireNonNull(projectProviderService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .map(project -> {
                    EnrichedProjectData.EnrichedProjectDataBuilder builder = EnrichedProjectData.builder();
                    List<WorkItemTypeCategory> workItemTypeCategories = getWorkItemTypeCategories(azureDevopsClient, project, intermediateState);
                    List<WorkItemType> workItemTypes = getWorkItemTypes(azureDevopsClient, project, intermediateState);
                    List<WorkItemTypeState> workItemTypeStates = getWorkItemTypeStates(azureDevopsClient, project, intermediateState);
                    Metadata metadata = Metadata.builder()
                            .workItemTypeCategories(workItemTypeCategories)
                            .workItemTypes(workItemTypes)
                            .workItemTypeStates(workItemTypeStates)
                            .build();
                    return builder
                            .project(project)
                            .metadata(metadata)
                            .build();
                });
        return workItemStream;
    }

    List<WorkItemTypeCategory> getWorkItemTypeCategories(AzureDevopsClient azureDevopsClient,
                                                         Project project, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getWorkItemTypeCategories(project.getOrganization(), project.getId());
        } catch (AzureDevopsClientException e) {
            log.warn("getWorkItemTypeCategories: Encountered AzureDevopsClient client error " +
                    "while fetching workitem type categories for project " + project.getId(), e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest WorkItemTypeCategories for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

    List<WorkItemType> getWorkItemTypes(AzureDevopsClient azureDevopsClient, Project project, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getWorkItemTypes(project.getOrganization(), project.getId());
        } catch (AzureDevopsClientException e) {
            log.warn("getWorkItemTypes: Encountered AzureDevopsClient client error " +
                    "while fetching workitem types for project " + project.getId(), e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest WorkItemTypes for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

    List<WorkItemTypeState> getWorkItemTypeStates(AzureDevopsClient azureDevopsClient,
                                                  Project project,
                                                  AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getWorkItemTypes(project.getOrganization(), project.getId())
                    .stream()
                    .flatMap(
                            workItemType -> {
                                try {
                                    return azureDevopsClient.getWorkItemTypeStates(project.getOrganization(),
                                            project.getId(), workItemType.getName())
                                            .stream();
                                } catch (AzureDevopsClientException e) {
                                    log.warn("getWorkItemTypeStates: failed for workItems states for org " +
                                            project.getOrganization() + ", project {}" + project.getName(), e);
                                    AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                            .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                            .resumeFromOrganization(project.getOrganization())
                                            .resumeFromProject(project.getName())
                                            .build();
                                    List<ControllerIngestionResult> results = List.of();
                                    ResumableIngestException build = ResumableIngestException.builder()
                                            .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                            .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                            .customMessage("Failed to ingest WorkItemTypeStates for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                                            .error(e)
                                            .build();
                                    throwException(build);
                                }
                                return Stream.empty();
                            }

                    )
                    .collect(Collectors.toList());
        } catch (AzureDevopsClientException e) {
            log.warn("getWorkItemTypes: failed for workItems types for org " +
                    project.getOrganization() + ", project {}" + project.getName(), e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest WorkItemTypes for project : " + project.getName() + " of organization : " +
                            project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

}