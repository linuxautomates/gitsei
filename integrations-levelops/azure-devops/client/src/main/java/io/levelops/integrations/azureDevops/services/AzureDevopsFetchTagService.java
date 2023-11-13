package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
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
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.azureDevops.models.Tag;
import io.levelops.integrations.azureDevops.models.TagResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchTagService {

    private static final String STARTING_CURSOR = StringUtils.EMPTY;
    private static final int BATCH_SIZE = 100;

    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();

    public Stream<EnrichedProjectData> fetchTags(IngestionCachingService ingestionCachingService,
                                                 AzureDevopsClient azureDevopsClient,
                                                 AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) throws NoSuchElementException {
        Stream<ImmutablePair<Repository, Tag>> dataStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .filter(Project::getGitEnabled)
                .flatMap(project -> {
                    Stream<ImmutablePair<Project, Repository>> collectRepositories = streamRepositories(azureDevopsClient, project)
                                .map(repository -> ImmutablePair.of(project, repository));
                    return collectRepositories;
                })
                .flatMap(pair -> {
                    Stream<ImmutablePair<Repository, Tag>> enrichedTags = null;
                    try {
                        enrichedTags = streamTags(azureDevopsClient,
                                pair.getLeft(), pair.getRight(), intermediateState)
                                .map(tag -> enrichTags(azureDevopsClient, pair.getLeft(),
                                        pair.getRight(), tag, intermediateState))
                                .map(tag -> ImmutablePair.of(pair.getRight(), tag));
                    } catch (Exception e) {
                        log.warn("Failed to ingest Tags for project : " + pair.getLeft().getName() + "  " +
                                "of organization : " + pair.getLeft().getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() +
                                "-----> " + e);
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(pair.getLeft().getOrganization())
                                .resumeFromProject(pair.getLeft().getName())
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest tags of project : " + pair.getLeft().getName() + " of organization : "
                                        + pair.getLeft().getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    return enrichedTags;
                });

        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(dataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Repository, List<ImmutablePair<Repository, Tag>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .repository(entry.getKey())
                                    .tags(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    private Stream<Repository> streamRepositories(AzureDevopsClient azureDevopsClient, Project project) {
        try {
            return azureDevopsClient.getRepositories(project.getOrganization(), project.getId()).stream();
        } catch (AzureDevopsClientException e) {
            log.warn("streamRepositories: Encountered AzureDevopsClient client error " +
                    "while fetching repositories for project " + project.getId(), e);
            throw new RuntimeStreamException("Encountered error while fetching repositories for project: " +
                    project.getOrganization(), e);
        }
    }

    public static Stream<Tag> streamTags(AzureDevopsClient azureDevopsClient, Project project, Repository repository,
                                         AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(STARTING_CURSOR, continuationToken -> {
            if (continuationToken == null)
                return PaginationUtils.CursorPageData.<Tag>builder()
                        .data(List.of())
                        .cursor(null)
                        .build();
            return getTagCursorPageData(azureDevopsClient, project, repository, continuationToken, intermediateState);
        });
    }

    private static Tag enrichTags(AzureDevopsClient client, Project project, Repository repository, Tag tag,
                                  AzureDevopsIntermediateState intermediateState) {
        Optional<Tag> annotatedTag = Optional.empty();
        try {
            annotatedTag = annotatedTagInfo(client, project, repository, tag);
        } catch (AzureDevopsClientException e) {
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest AnnotatedTags of project : " + project.getName() + " of organization : "
                            + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        Tag.GitUserDate taggedBy = annotatedTag .isPresent() ? annotatedTag.get().getTaggedBy() : Tag.GitUserDate.builder().build();
        Tag.GitTaggedObject taggedObject = annotatedTag.isPresent() ? annotatedTag.get().getTaggedObject() : Tag.GitTaggedObject.builder().build();
        return tag.toBuilder()
                .taggedBy(taggedBy)
                .taggedObject(taggedObject)
                .build();
    }

    private static Optional<Tag> annotatedTagInfo(AzureDevopsClient client, Project project,
                                                  Repository repository, Tag tag) throws AzureDevopsClientException {
        return client.getAnnotatedTags(project.getOrganization(), repository.getProject().getId(),
                repository.getId(), tag.getObjectId());
    }

    private static PaginationUtils.CursorPageData<Tag> getTagCursorPageData(AzureDevopsClient azureDevopsClient,
                                                                            Project project,
                                                                            Repository repository,
                                                                            String continuationToken, AzureDevopsIntermediateState intermediateState) {
        TagResponse tagResponse = TagResponse.builder().build();
        try {
            tagResponse = azureDevopsClient.getTags(project.getOrganization(),
                    repository.getProject().getName(), repository.getId(), continuationToken);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to ingest Tags for project : " + project.getName() + "  " +
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
                    .customMessage("Failed to ingest tags of project : " + project.getName() + " of organization : "
                            + project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        String nextContinuationToken = tagResponse != null ? tagResponse.getContinuationToken() : StringUtils.EMPTY;
        return PaginationUtils.CursorPageData.<Tag>builder()
                .data(ListUtils.emptyIfNull(tagResponse != null ? tagResponse.getTags() : List.of()))
                .cursor(nextContinuationToken)
                .build();
    }
}