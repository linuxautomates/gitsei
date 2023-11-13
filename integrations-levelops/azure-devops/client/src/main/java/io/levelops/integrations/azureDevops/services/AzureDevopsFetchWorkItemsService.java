package io.levelops.integrations.azureDevops.services;

import com.google.common.base.Stopwatch;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.Comment;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.models.WorkItemQueryResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
public class AzureDevopsFetchWorkItemsService {

    private static final int INGESTION_BATCH_SIZE = 100;
    private static final String FETCH_WORKITEM_COMMENTS = "fetch_work_items_comments";

    private final AzureDevopsProjectProviderService projectProviderService = new AzureDevopsProjectProviderService();

    public Stream<EnrichedProjectData> fetchWorkItems(IngestionCachingService ingestionCachingService,
                                                      AzureDevopsClient azureDevopsClient,
                                                      AzureDevopsIterativeScanQuery query,
                                                      AzureDevopsIntermediateState intermediateState) {
        boolean fetchComments = BooleanUtils.isNotFalse((Boolean) MapUtils.emptyIfNull(query.getIngestionFlags()).get(FETCH_WORKITEM_COMMENTS));
        log.info("fetch_work_items_comments={}", fetchComments);
        Stream<ImmutablePair<Project, WorkItem>> workItemStream = Objects.requireNonNull(projectProviderService
                        .streamProjects(ingestionCachingService, azureDevopsClient, query, intermediateState))
                .flatMap(project -> {
                    log.info("fetchWorkItems: fetching workItems for org='{}', project='{}'",
                            project.getOrganization(), project.getName());
                    Stream<ImmutablePair<Project, WorkItem>> workItems;
                    workItems = streamWorkItems(azureDevopsClient, query, project, intermediateState)
                            .map(workItem -> fetchComments
                                    ? enrichComments(azureDevopsClient, project, query, workItem, intermediateState)
                                    : workItem)
                            .map(workItem -> ImmutablePair.of(project, workItem));
                    return workItems;
                });
        MutableInt nbBatches = new MutableInt(0);
        Stream<EnrichedProjectData> enrichedProjectDataStream = StreamUtils.partition(workItemStream, INGESTION_BATCH_SIZE)
                .flatMap(batch -> {
                    log.info("fetchWorkItems: batch_count={}", nbBatches.incrementAndGet());
                    Map<Project, List<ImmutablePair<Project, WorkItem>>> groupedBatch =
                            batch.stream().collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    Stream<EnrichedProjectData> projectDataStream = groupedBatch.entrySet().stream()
                            .map(entry -> EnrichedProjectData.builder()
                                    .project(entry.getKey())
                                    .workItems(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                    return projectDataStream;
                });
        return enrichedProjectDataStream;
    }

    public static Stream<WorkItem> streamWorkItems(AzureDevopsClient azureDevopsClient, AzureDevopsIterativeScanQuery query,
                                                   Project project, AzureDevopsIntermediateState intermediateState) {
        long diffInMillis = Math.abs(query.getTo().getTime() - query.getFrom().getTime());
        long numOfDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        numOfDays = (numOfDays == 0) ? 1 : numOfDays;
        LocalDateTime fromLocalDateTime = query.getFrom().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toLocalDateTime = query.getTo().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        log.info("streamWorkItems query = {}, diffInMillis = {}, numOfDays = {}, fromLocalDateTime = {}, toLocalDateTime= {}", query, diffInMillis, numOfDays, fromLocalDateTime, toLocalDateTime);
        Stream<String> listStream = stream(fromLocalDateTime, toLocalDateTime,
                (int) Math.ceil(1.0 * numOfDays),
                1, (fromDt, toDt) -> {
                    Date from = Date.from(fromDt.atZone(ZoneId.systemDefault()).toInstant());
                    Date to = Date.from(toDt.atZone(ZoneId.systemDefault()).toInstant());
                    Stopwatch sw = Stopwatch.createStarted();
                    try {
                        log.info("streamWorkItems fetching workitems fromDt = {}, toDt = {}, from {}, to {} project {}", fromDt, toDt, from, to, project.getName());
                        List<WorkItemQueryResult.WorkItemReference> workItems = azureDevopsClient.getWorkItemQuery(project.getOrganization(),
                                project.getName(), from, to).getWorkItems();
                        log.info("azure ingestion timing getWorkItemQuery = {}, org = {}, project = {}, workItems.size() = {}", sw.elapsed(TimeUnit.SECONDS), project.getOrganization(), project.getId(), CollectionUtils.emptyIfNull(workItems).size());
                        return workItems;
                    } catch (AzureDevopsClientException e) {
                        log.warn("azure ingestion timing getWorkItemQuery = {}, org = {}, project = {}, failed", sw.elapsed(TimeUnit.SECONDS), project.getOrganization(), project.getId());
                        log.warn("streamWorkItems: Encountered AzureDevopsClient client error " +
                                "while fetching workItems from WorkItemQuery for " + project.getName(), e);
                        log.warn("azure ingestion timing getWorkItems = {}, org = {}, project = {}, failed", sw.elapsed(TimeUnit.SECONDS), project.getOrganization(), project.getId());
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(project.getOrganization())
                                .resumeFromProject(project.getName())
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest WorkItems for project : " + project.getName() + " of organization : " + project.getOrganization()
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    } finally {
                        sw.stop();
                    }
                    return List.of();
                })
                .map(WorkItemQueryResult.WorkItemReference::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .distinct();
        return StreamUtils.partition(listStream, INGESTION_BATCH_SIZE)
                .flatMap(ids -> {
                    if (CollectionUtils.isEmpty(ids)) {
                        return Stream.empty();
                    }
                    Stopwatch sw = Stopwatch.createStarted();
                    try {
                        log.info("streamWorkItems: Fetching work items for org {}, project {}, ids {}",
                                project.getOrganization(), project.getName(), ids);
                        List<WorkItem> workItems = azureDevopsClient.getWorkItems(project.getOrganization(), project.getId(), new ArrayList<>(ids));
                        log.info("azure ingestion timing getWorkItems = {}, org = {}, project = {}, workItems.size() = {}", sw.elapsed(TimeUnit.SECONDS), project.getOrganization(), project.getId(), CollectionUtils.emptyIfNull(workItems).size());
                        return workItems.stream();
                    } catch (AzureDevopsClientException e) {
                        log.info("azure ingestion timing getWorkItems = {}, org = {}, project = {}, failed", sw.elapsed(TimeUnit.SECONDS), project.getOrganization(), project.getId());
                        log.warn("streamWorkItems: failed for getWorkItems for org " +
                                project.getOrganization() + ", project " + project.getName(), e);
                        AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                                .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                                .resumeFromOrganization(project.getOrganization())
                                .resumeFromProject(project.getName())
                                .build();
                        List<ControllerIngestionResult> results = List.of();
                        ResumableIngestException build = ResumableIngestException.builder()
                                .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                                .customMessage("Failed to ingest WorkItems for project : " + project.getName() + " of organization : " + project.getOrganization()
                                        + " with completed stages : " + intermediateState.getCompletedStages())
                                .error(e)
                                .build();
                        throwException(build);
                    } finally {
                        sw.stop();
                    }
                    return Stream.empty();
                });
    }

    public static <T> Stream<T> stream(LocalDateTime from, LocalDateTime to, long limit, int pageSizeInDays,
                                       BiFunction<LocalDateTime, LocalDateTime, List<T>> pageSupplier) {
        return Stream.iterate(from, date -> date.plusDays(pageSizeInDays))
                .map(date -> pageSupplier.apply(date, to.isBefore(date.plusDays(pageSizeInDays)) ? to :
                        date.plusDays(pageSizeInDays)))
                .limit(limit)
                .flatMap(Collection::stream);
    }

    private WorkItem enrichComments(AzureDevopsClient client,
                                    Project project, AzureDevopsIterativeScanQuery query,
                                    WorkItem workItem, AzureDevopsIntermediateState intermediateState) {
        return workItem.toBuilder()
                .comments(getComments(client, project, query, workItem, intermediateState)).build();
    }

    private static List<Comment> getComments(AzureDevopsClient client, Project project, AzureDevopsIterativeScanQuery query,
                                             WorkItem workItem, AzureDevopsIntermediateState intermediateState) {
        Stopwatch sw = Stopwatch.createStarted();
        if (client.shouldIngestWorkitemComments()) {
            List<Comment> comments = List.of();
            try {
                comments = client.getComments(project.getOrganization(), project.getName(),
                                workItem.getId()).stream().filter(comment ->
                                DateUtils.parseDateTime(comment.getCreatedDate()).isAfter(query.getFrom().toInstant()) &&
                                        DateUtils.parseDateTime(comment.getCreatedDate()).isBefore(query.getTo().toInstant()))
                        .collect(Collectors.toList());
                log.info("azure ingestion timing getComments = {}, comments.size() = {}, org = {}, project = {}, wi_id = {}",
                        sw.stop().elapsed(TimeUnit.SECONDS), CollectionUtils.emptyIfNull(comments).size(),
                        project.getOrganization(), project.getName(), workItem.getId());
            } catch (AzureDevopsClientException e) {
                log.warn("fetchIterations: failed for getComments for org " +
                        project.getOrganization() + ", project " + project.getName(), e);
                AzureDevopsIntermediateState azureDevopsIntermediateState = intermediateState.toBuilder()
                        .completedStages(intermediateState.getCompletedStages() != null ? intermediateState.getCompletedStages() : List.of())
                        .resumeFromOrganization(project.getOrganization())
                        .resumeFromProject(project.getName())
                        .build();
                List<ControllerIngestionResult> results = List.of();
                ResumableIngestException build = ResumableIngestException.builder()
                        .result(new ControllerIngestionResultList(StorageResultsListMergingStrategy.NAME, results))
                        .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), azureDevopsIntermediateState))
                        .customMessage("Failed to ingest getComments for project : " + project.getName() + " of organization : " + project.getOrganization()
                                + " with completed stages : " + intermediateState.getCompletedStages())
                        .error(e)
                        .build();
                throwException(build);
            }
            return comments;
        }
        return List.of();
    }

}
