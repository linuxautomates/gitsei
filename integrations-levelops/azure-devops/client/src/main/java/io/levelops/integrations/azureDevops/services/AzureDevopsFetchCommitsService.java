package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.Change;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.Repository;
import lombok.extern.log4j.Log4j2;
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
public class AzureDevopsFetchCommitsService {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int BATCH_SIZE = 100;

    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();


    public Stream<EnrichedProjectData> fetchCommits(IngestionCachingService ingestionCachingService,
                                                    AzureDevopsClient azureDevopsClient,
                                                    AzureDevopsIterativeScanQuery query,
                                                    AzureDevopsIntermediateState intermediateState) throws NoSuchElementException {
        Stream<ImmutablePair<Repository, Commit>> dataStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .filter(Project::getGitEnabled)
                .flatMap(project -> streamRepositories(azureDevopsClient, project)
                            .map(repository -> ImmutablePair.of(project, repository)))
                .flatMap(pair -> streamCommits(azureDevopsClient,
                            pair.getLeft(), pair.getRight(), query, intermediateState)
                            .map(commit -> enrichCommitChanges(azureDevopsClient, pair.getLeft(),
                                    pair.getRight(), query, commit, intermediateState))
                            .map(commit -> ImmutablePair.of(pair.getRight(), commit)));
        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(dataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Repository, List<ImmutablePair<Repository, Commit>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .repository(entry.getKey())
                                    .commits(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    public Stream<Repository> streamRepositories(AzureDevopsClient azureDevopsClient, Project project)  {
        try {
            return azureDevopsClient.getRepositories(project.getOrganization(), project.getId()).stream();
        } catch (Exception e) {
            log.warn("streamRepositories: Encountered AzureDevopsClient client error " +
                    "while fetching repositories for project " + project.getId(), e);
            throw new RuntimeStreamException("Encountered error while fetching repositories for project: " +
                    project.getOrganization(), e);
        }
    }

    private static Stream<Commit> streamCommits(AzureDevopsClient azureDevopsClient, Project project,
                                                Repository repository, AzureDevopsIterativeScanQuery query,
                                                AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset -> getCommits(azureDevopsClient,
                project.getOrganization(), repository.getProject().getName(), repository.getName(),
                repository.getId(), query.getFrom(), query.getTo(), offset, intermediateState));
    }

    private Commit enrichCommitChanges(AzureDevopsClient client,
                                       Project project,
                                       Repository repository,
                                       AzureDevopsIterativeScanQuery query, Commit commit,
                                       AzureDevopsIntermediateState intermediateState) {
        return commit.toBuilder()
                .changes(streamCommitChanges(client, project, query, repository, commit, intermediateState)
                        .collect(Collectors.toList())).build();
    }

    private static Stream<Change> streamCommitChanges(AzureDevopsClient client, Project project, AzureDevopsIterativeScanQuery query,
                                                      Repository repository, Commit commit, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, (offset) -> {
            try {
                return client.getChanges(project.getOrganization(), repository.getProject().getId(),
                        repository.getId(), commit.getCommitId(), offset);
            } catch (AzureDevopsClientException e) {
                log.warn("streamCommitChanges: Encountered AzureDevopsClient client error " +
                        "while fetching Commit Changes for " + query, e);
                AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                        .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                        .resumeFromOrganization(project.getOrganization())
                        .resumeFromProject(project.getName())
                        .build();
                ResumableIngestException build = ResumableIngestException.builder()
                        .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                        .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                        .customMessage("Failed to ingest streamCommitChanges for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                        .error(e)
                        .build();
                throwException(build);
            }
            return List.of();
        });
    }

    private static List<Commit> getCommits(AzureDevopsClient client, String organization, String project, String repositoryName,
                                           String repositoryId, Date from, Date to, int offset, AzureDevopsIntermediateState intermediateState) {
        try {
            return client.getCommits(organization, project, repositoryName, repositoryId, from, to, offset);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest commits for project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }
}
