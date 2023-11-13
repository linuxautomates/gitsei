package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.dates.DateUtils;
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
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.PullRequestHistory;
import io.levelops.integrations.azureDevops.models.Repository;
import lombok.extern.log4j.Log4j2;
import okhttp3.Response;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchPullRequestService {

    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int BATCH_SIZE = 100;
    private static final Predicate<Response> AZURE_FAILED_RESPONSE_PREDICATE = response -> !response.isSuccessful() || (response.code() == 203);

    public Stream<EnrichedProjectData> fetchPullRequests(IngestionCachingService ingestionCachingService,
                                                         AzureDevopsClient azureDevopsClient,
                                                         AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        Stream<ImmutablePair<Repository, PullRequest>> dataStream = projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState)
                .filter(Project::getGitEnabled)
                .flatMap(project -> {
                    try {
                        return streamRepositories(azureDevopsClient, project)
                                .map(repository -> ImmutablePair.of(project, repository));
                    } catch (IOException e) {
                        throw new RuntimeStreamException(e);
                    }
                })
                .flatMap(pair -> {
                    Stream<PullRequest> pullRequestStream  = streamPullRequest(azureDevopsClient,
                            pair.getLeft(), pair.getRight(), query, intermediateState);
                    return pullRequestStream
                            .map(pullRequest -> enrichPullRequests(azureDevopsClient, pair.getLeft(), pair.getRight(),
                                    pullRequest, intermediateState))
                            .map(pullRequest -> ImmutablePair.of(pair.getRight(), pullRequest));
                });

        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(dataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Repository, List<ImmutablePair<Repository, PullRequest>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .repository(entry.getKey())
                                    .pullRequests(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    private PullRequest enrichPullRequests(AzureDevopsClient azureDevopsClient, Project project, Repository repository,
                                           PullRequest pullRequest, AzureDevopsIntermediateState intermediateState) {
        PullRequest pullRequestInfo;
        try {
            pullRequestInfo = azureDevopsClient.getPullRequestsWithCommiterInfo(project.getOrganization(),
                    project.getName(), repository.getId(), String.valueOf(pullRequest.getPullRequestId()));
            return pullRequest.toBuilder()
                    .lastMergeCommit(pullRequestInfo.getLastMergeCommit() != null ?pullRequestInfo.getLastMergeCommit() : null)
                    .commits(ListUtils.emptyIfNull(pullRequestInfo.getCommits()))
                    .labels(azureDevopsClient.getPullRequestsWithLabelInfo(project.getOrganization(),
                            project.getName(), repository.getId(), String.valueOf(pullRequest.getPullRequestId())))
                    .build();
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest PRs for project : " + project.getName() + " of organization : " + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return PullRequest.builder().build();
    }

    private Stream<Repository> streamRepositories(AzureDevopsClient azureDevopsClient, Project project) throws IOException {
        try {
            return azureDevopsClient.getRepositories(project.getOrganization(), project.getId()).stream();
        } catch (AzureDevopsClientException e) {
            throw new IOException("streamRepositories: Encountered AzureDevopsClient client error " +
                    "while fetching repositories for project " + project.getId(), e);
        }
    }

    public static Stream<PullRequest> streamPullRequest(AzureDevopsClient azureDevopsClient, Project project,
                                                        Repository repository, AzureDevopsIterativeScanQuery query,
                                                        AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset -> getPullRequests(azureDevopsClient, project.getOrganization(),
                repository.getProject().getId(), repository.getId(), offset, intermediateState)
                .stream()
                .filter(pullRequest -> pullRequest.getClosedDate() == null ||
                        ((DateUtils.parseDateTime(pullRequest.getClosedDate()).compareTo(query.getTo().toInstant()) < 0) &&
                                (DateUtils.parseDateTime(pullRequest.getClosedDate()).compareTo(query.getFrom().toInstant())) > 0))
                .map(pullRequest -> {
                    List<PullRequestHistory> pullRequestHistories = getPullRequestsHistories(azureDevopsClient,
                            project.getOrganization(), project.getName(),
                            repository.getId(), String.valueOf(pullRequest.getPullRequestId()), intermediateState);
                    pullRequest = pullRequest.toBuilder()
                            .pullRequestHistories(pullRequestHistories)
                            .build();
                    return pullRequest;
                })
                .collect(Collectors.toList()));
    }

    private static List<PullRequest> getPullRequests(AzureDevopsClient azureDevopsClient,
                                                     String organization, String project, String repositoryId,
                                                     int offset, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getPullRequests(organization, project, repositoryId, offset);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest PRs for project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

    public static List<PullRequestHistory> getPullRequestsHistories(AzureDevopsClient azureDevopsClient,
                                                                    String organization, String project, String repositoryId,
                                                                    String pullRequestId, AzureDevopsIntermediateState intermediateState) {
        try {
            return azureDevopsClient.getPullRequestsHistories(organization, project, repositoryId, pullRequestId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, List.of()))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest PullRequestsHistories for project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }

}