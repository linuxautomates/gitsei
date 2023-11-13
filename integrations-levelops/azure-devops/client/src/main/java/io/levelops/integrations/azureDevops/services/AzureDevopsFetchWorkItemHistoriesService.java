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
import io.levelops.integrations.azureDevops.models.WorkItemHistory;
import io.levelops.integrations.azureDevops.models.WorkItemQueryResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.throwException;

@Log4j2
public class AzureDevopsFetchWorkItemHistoriesService {

    private static final int INGESTION_BATCH_SIZE = 600;
    private static final int WORKITEM_BATCH_SIZE = 200;

    private final AzureDevopsProjectProviderService projectProviderService = new AzureDevopsProjectProviderService();

    public Stream<EnrichedProjectData> fetchWorkItemHistories(IngestionCachingService ingestionCachingService,
                                                              AzureDevopsClient azureDevopsClient,
                                                              AzureDevopsIterativeScanQuery query, AzureDevopsIntermediateState intermediateState) {
        Stream<ImmutablePair<Project, WorkItemHistory>> workItemStream = Objects.requireNonNull(projectProviderService
                        .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> {
                    log.info("fetchWorkItems: Fetching projects and workItems for org {}, project {}",
                            project.getOrganization(), project.getName());
                    return streamWorkItemHistories(azureDevopsClient, query, project, intermediateState)
                                .map(workItemHistory -> ImmutablePair.of(project, workItemHistory));
                });
        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(workItemStream, INGESTION_BATCH_SIZE)
                .flatMap(pairs -> {
                    log.info("fetchWorkItems: Adding pairs of type List<ImmutablePair<Project, WorkItem>>");
                    Map<Project, List<ImmutablePair<Project, WorkItemHistory>>> groupedBatch =
                            pairs.stream()
                                    .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .workItemHistories(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    public static Stream<WorkItemHistory> streamWorkItemHistories(AzureDevopsClient azureDevopsClient,
                                                                  AzureDevopsIterativeScanQuery query, Project project,
                                                                  AzureDevopsIntermediateState intermediateState) {
        long diffInMillis = Math.abs(query.getTo().getTime() - query.getFrom().getTime());
        long numOfDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        numOfDays = (numOfDays == 0) ? 1 : numOfDays + 1;
        LocalDateTime fromLocalDateTime = query.getFrom().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toLocalDateTime = query.getTo().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        log.info("streamWorkItemHistories query = {}, diffInMillis = {}, numOfDays = {}, fromLocalDateTime = {}, toLocalDateTime= {}", query, diffInMillis, numOfDays, fromLocalDateTime, toLocalDateTime);
        Stream<String> listStream = stream(fromLocalDateTime,
                toLocalDateTime,
                (int) Math.ceil(1.0 * numOfDays),
                1, (fromDt, toDt) -> {
                    Date from = Date.from(fromDt.atZone(ZoneId.systemDefault()).toInstant());
                    Date to = Date.from(toDt.atZone(ZoneId.systemDefault()).toInstant());
                    try {
                        log.info("streamWorkItemHistories fetching workitems for workitem histories from {}, to {} project {}", from, to, project.getName());
                        return azureDevopsClient.getWorkItemQuery(project.getOrganization(),
                                project.getName(), from, to).getWorkItems();
                    } catch (AzureDevopsClientException e) {
                        log.warn("streamWorkItemHistories: Encountered AzureDevopsClient client error " +
                                "while fetching workItems from WorkItemQuery for " + project.getName(), e);
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(project.getOrganization())
                                .resumeFromProject(project.getName())
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest WorkItem Histories for project =  : " + project.getName() + " of organization : " + project.getOrganization()
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    }
                    return List.of();
                })
                .map(WorkItemQueryResult.WorkItemReference::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .distinct();

        return StreamUtils.partition(listStream, WORKITEM_BATCH_SIZE)
                .flatMap(ids -> ids.stream()
                        .flatMap(((id -> {
                            try {
                                return azureDevopsClient.getWorkItemsUpdates(project.getOrganization(), project.getId(), id).stream()
                                        .filter(workItemHistory -> {
                                            // LEV-5297 assuming we are now only scanning work items that have been truely updated
                                            // let's scan for all the histories to make use we got everything
                                            return true;
//                                            Timestamp dateFromHistory = getChangedDateFromHistoryAsTimestamp(workItemHistory);
//                                            return dateFromHistory == null ||
//                                                    (dateFromHistory.after(query.getFrom()) &&
//                                                            dateFromHistory.before(query.getTo()));
                                        });
                            } catch (AzureDevopsClientException e) {
                                log.warn("streamWorkItemHistories: Encountered AzureDevopsClient client error " +
                                        "while fetching workItems from WorkItemQuery for " + project.getName(), e);
                                AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                        .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                        .resumeFromOrganization(project.getOrganization())
                                        .resumeFromProject(project.getName())
                                        .build();
                                List<ControllerIngestionResult> results = List.of();
                                ResumableIngestException build = ResumableIngestException.builder()
                                        .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                        .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                        .customMessage("Failed to ingest WorkItem Histories for project =  : " + project.getName() + " of organization : " + project.getOrganization()
                                                + " with completed stages : " + intermediateState.getCompletedStages())
                                        .error(e)
                                        .build();
                                throwException(build);
                            }
                            return Stream.empty();
                        }))));
    }

    public static <T> Stream<T> stream(LocalDateTime from, LocalDateTime to, long limit, int pageSizeInDays,
                                       BiFunction<LocalDateTime, LocalDateTime, List<T>> pageSupplier) {
        return Stream.iterate(from, date -> date.plusDays(pageSizeInDays))
                .map(date -> pageSupplier.apply(date, to.isBefore(date.plusDays(pageSizeInDays)) ? to :
                        date.plusDays(pageSizeInDays)))
                .limit(limit)
                .flatMap(Collection::stream);
    }
}
