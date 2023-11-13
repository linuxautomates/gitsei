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
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.Team;
import io.levelops.integrations.azureDevops.models.TeamSetting;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.AZURE_FAILED_RESPONSE_PREDICATE;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchTeamsService {

    private final AzureDevopsProjectProviderService projectEnrichmentService = new AzureDevopsProjectProviderService();
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int BATCH_SIZE = 100;

    public Stream<EnrichedProjectData> fetchTeams(IngestionCachingService ingestionCachingService,
                                                  AzureDevopsClient azureDevopsClient,
                                                  AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        Stream<ImmutablePair<Project, Team>> dataStream = Objects.requireNonNull(projectEnrichmentService
                .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> Objects
                        .requireNonNull(streamTeams(azureDevopsClient, project, intermediateState))
                        .map(team -> ImmutablePair.of(project, team)));

        return StreamUtils.partition(dataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<Project, List<ImmutablePair<Project, Team>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatch.entrySet().stream()
                            .map(entry -> {
                                EnrichedProjectData.EnrichedProjectDataBuilder builder = EnrichedProjectData.builder();
                                try {
                                    builder
                                            .project(entry.getKey())
                                            .teams(entry.getValue().stream()
                                                    .map(ImmutablePair::getRight)
                                                    .collect(Collectors.toList()))
                                            .codeAreas(azureDevopsClient.getCodeAreas(entry.getKey().getOrganization(),
                                                    entry.getKey().getName()))
                                            .build();
                                    return builder.build();
                                } catch (Exception e) {
                                    log.warn("Failed to ingest Code Areas for project : " + entry.getKey().getName() + "  " +
                                            "of organization : " + entry.getKey().getOrganization() + " with completed stages : " + intermediateState.getCompletedStages() +
                                            "-----> " + e);
                                    AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                            .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                            .resumeFromOrganization(entry.getKey().getOrganization())
                                            .resumeFromProject(entry.getKey().getName())
                                            .build();
                                    List<ControllerIngestionResult> results = List.of();
                                    ResumableIngestException build = ResumableIngestException.builder()
                                            .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                            .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                            .customMessage("Failed to ingest project : " + entry.getKey().getName() + " of organization : " + entry.getKey().getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                                            .error(e)
                                            .build();
                                    throwException(build);
                                }
                                return builder.build();
                            });
                });
    }

    private Stream<Team> streamTeams(AzureDevopsClient azureDevopsClient, Project project, AzureDevopsIntermediateState intermediateState) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, offset -> {
            try {
                return getTeams(azureDevopsClient, project.getOrganization(), project.getName(), offset, intermediateState)
                        .stream()
                        .map(team -> {
                            TeamSetting teamSetting;
                            teamSetting = getTeamSettings(azureDevopsClient, project, team.getName(), intermediateState);
                            return team.toBuilder()
                                    .teamSetting(teamSetting)
                                    .build();
                        })
                        .collect(Collectors.toList());
            } catch (AzureDevopsClientException e) {
                log.warn("Failed to fetch Teams for organization :" +
                        project.getOrganization() + " project :" + project.getName(), e);
                throw new RuntimeStreamException("Encountered error while fetching teams for project: " +
                        project.getOrganization(), e);
            }
        });
    }

    private TeamSetting getTeamSettings(AzureDevopsClient azureDevopsClient, Project project, String team,
                                        AzureDevopsIntermediateState intermediateState) {
        TeamSetting teamSetting = TeamSetting.builder().build();
        try {
            teamSetting = azureDevopsClient.getTeamSettings(project.getOrganization(), project.getName(), team);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to fetch Team settings for organization :" +
                    project.getOrganization() + " project :" + project.getName() + " team :" + team, e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(project.getOrganization())
                    .resumeFromProject(project.getName())
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest teams project : " + project.getName() + " of organization : " +
                            project.getOrganization() + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return teamSetting;
    }

    public List<Team> getTeams(AzureDevopsClient azureDevopsClient, String organization, String project,
                               int skip, AzureDevopsIntermediateState intermediateState) throws AzureDevopsClientException {
        try {
            return azureDevopsClient.getTeams(organization, project, skip, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Failed to ingest teams for project : " + project + "  " +
                    "of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages() +
                    "-----> " + e);
            AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                    .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                    .resumeFromOrganization(organization)
                    .resumeFromProject(project)
                    .build();
            List<ControllerIngestionResult> results = List.of();
            ResumableIngestException build = ResumableIngestException.builder()
                    .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                    .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                    .customMessage("Failed to ingest teams project : " + project + " of organization : " + organization + " with completed stages : " + intermediateState.getCompletedStages())
                    .error(e)
                    .build();
            throwException(build);
        }
        return List.of();
    }
}
