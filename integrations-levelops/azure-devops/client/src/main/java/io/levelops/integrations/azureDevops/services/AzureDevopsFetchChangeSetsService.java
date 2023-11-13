package io.levelops.integrations.azureDevops.services;

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
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.ChangeSetChange;
import io.levelops.integrations.azureDevops.models.ChangeSetWorkitem;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchChangeSetsService {
    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int BATCH_SIZE = 100;

    public Stream<EnrichedProjectData> fetchChangesets(IngestionCachingService ingestionCachingService,
                                                       AzureDevopsClient azureDevopsClient,
                                                       AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) throws NoSuchElementException {
        Stream<ImmutablePair<Project, ChangeSet>> changesetStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .filter(Project::getTfvcEnabled)
                .flatMap(project -> {
                    Stream<ImmutablePair<Project, ChangeSet>> immutablePairStream = Stream.empty();
                    try {
                        immutablePairStream = fetchPaginatedChangesets(azureDevopsClient, query, project, intermediateState)
                                .map(changeSet -> ImmutablePair.of(project, changeSet));
                    } catch (Exception e) {
                        log.warn("Failed to ingest Changesets Metadata for project : " + project.getName() + "  " +
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
                                .customMessage("Failed to ingest Changesets for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    return immutablePairStream;
                });

        return StreamUtils.partition(changesetStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Project, List<ImmutablePair<Project, ChangeSet>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .changeSets(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }

    private Stream<ChangeSet> fetchPaginatedChangesets(AzureDevopsClient azureDevopsClient,
                                                       AzureDevopsIterativeScanQuery query,
                                                       Project project, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset -> {
            try {
                return fetchEnrichedChangeset(azureDevopsClient, query, project, offset, intermediateState);
            } catch (AzureDevopsClientException e) {
                log.warn("Failed to enrich changesets for project " + project + " organization " + project.getOrganization(), e);
                throw new RuntimeStreamException("Encountered error while fetching changesset for integration key: " +
                        query.getIntegrationKey(), e);
            }
        });

    }

    private List<ChangeSet> fetchEnrichedChangeset(AzureDevopsClient azureDevopsClient,
                                                   AzureDevopsIterativeScanQuery query,
                                                   Project project, int offset, AzureDevopsIntermediateState intermediateState) throws AzureDevopsClientException {
        List<ChangeSet> changeSets = getChangesets(azureDevopsClient, project.getOrganization(), project.getName(),
                query.getFrom(), offset, intermediateState);
        return CollectionUtils.emptyIfNull(changeSets).stream()
                .map(changeSet -> {
                    List<ChangeSetChange> changesetChanges;
                    List<ChangeSetWorkitem> changeSetWorkitems;
                    log.info("Fetching changeset changes and workitems for changeset id {} project {}", changeSet.getChangesetId(), project.getName());
                    changesetChanges = fetchPaginatedChangesetChanges(azureDevopsClient, project, changeSet, intermediateState)
                            .collect(Collectors.toList());
                    changeSetWorkitems = getChangesetWorkitems(azureDevopsClient, project.getName(), project.getOrganization(),
                            String.valueOf(changeSet.getChangesetId()), intermediateState);
                    return changeSet.toBuilder()
                            .changeSetChanges(changesetChanges)
                            .changeSetWorkitems(changeSetWorkitems)
                            .build();
                }).collect(Collectors.toList());
    }

    private Stream<ChangeSetChange> fetchPaginatedChangesetChanges(AzureDevopsClient azureDevopsClient,
                                                                   Project project,
                                                                   ChangeSet changeSet, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset -> getChangesetChanges(azureDevopsClient, project.getName(), project.getOrganization(),
                String.valueOf(changeSet.getChangesetId()), offset, intermediateState));
    }

    public List<ChangeSetChange> getChangesetChanges(AzureDevopsClient azureDevopsClient, String project, String organization,
                                                     String changesetId, int offset,
                                                     AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getChangesetChanges(organization, changesetId, offset);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest ChangesetChanges for project : " + project + " of organization : "
                            + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

    public List<ChangeSetWorkitem> getChangesetWorkitems(AzureDevopsClient azureDevopsClient,
                                                         String project, String organization, String changeSetId,
                                                         AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getChangesetWorkitems(organization, changeSetId);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest ChangesetWorkitems for project : " + project + " of organization : "
                            + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

    public List<ChangeSet> getChangesets(AzureDevopsClient azureDevopsClient, String organization, String project,
                                         Date from, int offset, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getChangesets(organization, project, from, offset);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest Changesets for project : " + project + " of organization : "
                            + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

}
