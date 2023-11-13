package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.dates.DateUtils;
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
import io.levelops.integrations.azureDevops.models.Branch;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchBranchService {


    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();

    private static final int BATCH_SIZE = 100;

    public Stream<EnrichedProjectData> fetchTfvcBranches(IngestionCachingService ingestionCachingService,
                                                         AzureDevopsClient azureDevopsClient,
                                                         AzureDevopsIterativeScanQuery query,
                                                         AzureDevopsIntermediateState intermediateState) throws NoSuchElementException, AzureDevopsClientException {
        Stream<ImmutablePair<Project, Branch>> branchesStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .filter(Project::getTfvcEnabled)
                .flatMap(project -> fetchBranches(azureDevopsClient, query, project, intermediateState)
                        .map(branch -> ImmutablePair.of(project, branch)));
        return StreamUtils.partition(branchesStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Project, List<ImmutablePair<Project, Branch>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .branches(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }

    private Stream<Branch> fetchBranches(AzureDevopsClient azureDevopsClient,
                                         AzureDevopsIterativeScanQuery query, Project project,
                                         AzureDevopsIntermediateState intermediateState) {
        Instant from = query.getFrom().toInstant();
        Instant to = query.getTo().toInstant();
        try {
            return CollectionUtils.emptyIfNull(azureDevopsClient.getBranches(project.getOrganization(), project.getName())).stream()
                    .filter(branch -> {
                        Instant createdDate = DateUtils.parseDateTime(branch.getCreatedDate());
                        return DateUtils.isBetween(createdDate, from, true, to, true);
                    });
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
                    .customMessage("Failed to ingest TfvcBranches for project =" + project.getName() + " of organization : " +
                            project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return Stream.empty();
    }
}
