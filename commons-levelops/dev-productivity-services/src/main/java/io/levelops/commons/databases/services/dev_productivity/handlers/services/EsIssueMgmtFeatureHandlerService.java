package io.levelops.commons.databases.services.dev_productivity.handlers.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.faceted_search.db.models.workitems.EsDevProdWorkItemResponse;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.faceted_search.converters.EsDevProdWorkItemConverter;
import io.levelops.faceted_search.converters.EsJiraIssueConverter;
import io.levelops.faceted_search.converters.EsWorkItemConverter;
import io.levelops.faceted_search.querybuilders.workitems.EsJiraQueryBuilder;
import io.levelops.faceted_search.querybuilders.workitems.EsWorkItemQueryBuilder;
import io.levelops.faceted_search.utils.EsUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.utils.ESAggResultUtils.getQueryString;

@Log4j2
@Service
public class EsIssueMgmtFeatureHandlerService {

    private final ESClientFactory esClientFactory;
    private final Set<String> issuePartialCreditJiraTenants;
    private final Set<String> issuePartialCreditWITenants;

    @Autowired
    public EsIssueMgmtFeatureHandlerService(ESClientFactory esClientFactory,
                                            @Qualifier("issuePartialCreditJiraTenants") Set<String> issuePartialCreditJiraTenants,
                                            @Qualifier("issuePartialCreditWITenants") Set<String> issuePartialCreditWITenants) {
        this.esClientFactory = esClientFactory;
        this.issuePartialCreditJiraTenants = issuePartialCreditJiraTenants;
        this.issuePartialCreditWITenants = issuePartialCreditWITenants;
    }

    public List<DbAggregationResult> getJiraFeatureResponse(String company,
                                                            JiraIssuesFilter filter,
                                                            List<String> developmentStages,
                                                            DevProductivityProfile.FeatureType featureType) throws IOException {
        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, filter.getIngestedAt());

        if (CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters()) && CollectionUtils.isNotEmpty(filter.getTicketCategories())) {

            for (JiraIssuesFilter.TicketCategorizationFilter ticketCategorizationFilter : filter.getTicketCategorizationFilters()) {
                if (ticketCategorizationFilter.getFilter() != null) {
                    filter = mergeFilters(filter, ticketCategorizationFilter.getFilter());
                }
            }
        }
        boolean useIssuePartialCredit = issuePartialCreditJiraTenants.contains(company);
        List<String> historicalAssignees = filter.getAssignees();
        ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories = ImmutablePair.of(historicalAssignees, List.of("IN PROGRESS"));
        filter = filter.toBuilder().assignees(null).across(null).calculation(null).acrossLimit(10000).build();

        SearchRequest.Builder builder2 = null;
        if (useIssuePartialCredit) {
            builder2 = EsJiraQueryBuilder.buildSearchRequest(filter, developmentStages, null, histAssigneesAndHistStatusCategories, List.of(), false, null, indexNameOrAlias, false, null, null, false);
        } else {
            builder2 = EsJiraQueryBuilder.buildSearchRequest(filter, developmentStages, historicalAssignees, null, List.of(), false, null, indexNameOrAlias, false, null, null, false);
        }

        Map<String, Aggregation> outerAgg2 = getAggForWorkItemDevProd(historicalAssignees, developmentStages, useIssuePartialCredit);
        SearchRequest searchRequest2 = builder2.aggregations(outerAgg2)
                .build();
        String queryString = getQueryString(searchRequest2);
        log.info("Index name {} and ES Query 2 : {} ", indexNameOrAlias, queryString);

        ElasticsearchClient esClient = esClientFactory.getESClient(company);
        SearchResponse<Void> searchResponse2 = esClient.search(searchRequest2, Void.class);
        List<EsDevProdWorkItemResponse> responses = EsDevProdWorkItemConverter.convert(searchResponse2, CollectionUtils.isNotEmpty(developmentStages), historicalAssignees, useIssuePartialCredit);
        return getDbAggregationResults(featureType, responses);
    }

    public List<DbAggregationResult> getWorkItemFeatureResponse(String company,
                                                                WorkItemsFilter filter,
                                                                List<String> developmentStages,
                                                                DevProductivityProfile.FeatureType featureType) throws IOException {
        ElasticsearchClient esClient = esClientFactory.getESClient(company);
        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, filter.getIngestedAt());

        boolean useIssuePartialCredit = issuePartialCreditWITenants.contains(company);
        List<String> historicalAssignees = filter.getAssignees();
        ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories = ImmutablePair.of(historicalAssignees, List.of("InProgress", "Resolved"));
        filter = filter.toBuilder().assignees(null).across(null).calculation(null).acrossLimit(10000).build();

        SearchRequest.Builder builder2 = null;
        if (useIssuePartialCredit) {
            builder2 = EsWorkItemQueryBuilder.buildSearchRequest(filter, WorkItemsMilestoneFilter.builder().build(),
                    developmentStages, null, histAssigneesAndHistStatusCategories, List.of(), false, null, null, indexNameOrAlias, false, null, null, false);
        } else {
            builder2 = EsWorkItemQueryBuilder.buildSearchRequest(filter, WorkItemsMilestoneFilter.builder().build(),
                    developmentStages, historicalAssignees, null, List.of(), false, null, null, indexNameOrAlias, false, null, null, false);
        }
        Map<String, Aggregation> outerAgg2 = getAggForWorkItemDevProd(historicalAssignees, developmentStages, useIssuePartialCredit);
        SearchRequest searchRequest2 = builder2.aggregations(outerAgg2)
                .build();

        String queryString = getQueryString(searchRequest2);
        log.info("Index name {} and ES WI Query 2 : {} ", indexNameOrAlias, queryString);

        SearchResponse<Void> searchResponse2 = esClient.search(searchRequest2, Void.class);
        List<EsDevProdWorkItemResponse> responses = EsDevProdWorkItemConverter.convert(searchResponse2, CollectionUtils.isNotEmpty(developmentStages), historicalAssignees, useIssuePartialCredit);
        return getDbAggregationResults(featureType, responses);
    }

    public DbListResponse<DbJiraIssue> getJiraFeatureBreakDown(String company,
                                                               JiraIssuesFilter filter,
                                                               List<String> developmentStages,
                                                               DevProductivityProfile.FeatureType featureType,
                                                               Map<String, SortingOrder> sortBy,
                                                               Integer pageNumber,
                                                               Integer pageSize) throws IOException {

        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, filter.getIngestedAt());

        if (CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters()) && CollectionUtils.isNotEmpty(filter.getTicketCategories())) {

            for (JiraIssuesFilter.TicketCategorizationFilter ticketCategorizationFilter : filter.getTicketCategorizationFilters()) {
                if (ticketCategorizationFilter.getFilter() != null) {
                    filter = mergeFilters(filter, ticketCategorizationFilter.getFilter());
                }
            }
        }

        boolean useIssuePartialCredit = issuePartialCreditJiraTenants.contains(company);

        List<String> historicalAssignees = filter.getAssignees();
        ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories = ImmutablePair.of(historicalAssignees, List.of("IN PROGRESS"));
        filter = filter.toBuilder().assignees(null).across(null).calculation(null).acrossLimit(10000).build();
        boolean needMissingStoryPointsFilter = featureType == DevProductivityProfile.FeatureType.NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH;

        SearchRequest.Builder builder2 = null;
        if (useIssuePartialCredit) {
            builder2 = EsJiraQueryBuilder.buildSearchRequest(filter, developmentStages, null, histAssigneesAndHistStatusCategories, List.of(), needMissingStoryPointsFilter, null, indexNameOrAlias, false, null, null, false);
        } else {
            builder2 = EsJiraQueryBuilder.buildSearchRequest(filter, developmentStages, historicalAssignees, null, List.of(), needMissingStoryPointsFilter, null, indexNameOrAlias, false, null, null, false);
        }

        Map<String, Aggregation> outerAgg2 = getAggForWorkItemDevProd(historicalAssignees, developmentStages, useIssuePartialCredit);
        SearchRequest searchRequest2 = builder2.aggregations(outerAgg2)
                .build();
        String queryString = getQueryString(searchRequest2);
        log.info("Index name {} and ES Query 2 : {} ", indexNameOrAlias, queryString);

        ElasticsearchClient esClient = esClientFactory.getESClient(company);
        SearchResponse<Void> searchResponse2 = esClient.search(searchRequest2, Void.class);

        List<EsDevProdWorkItemResponse> responses = EsDevProdWorkItemConverter.convert(searchResponse2, CollectionUtils.isNotEmpty(developmentStages), historicalAssignees, useIssuePartialCredit);
        responses = getEnrichedEsDevProdWorkItemResponses(responses);

        SearchRequest.Builder builder = null;
        if (useIssuePartialCredit) {
            builder = EsJiraQueryBuilder.buildSearchRequest(filter, developmentStages, null, histAssigneesAndHistStatusCategories, List.of(), needMissingStoryPointsFilter, null, indexNameOrAlias, false, null, null, false);
        } else {
            builder = EsJiraQueryBuilder.buildSearchRequest(filter, developmentStages, historicalAssignees, null, List.of(), needMissingStoryPointsFilter, null, indexNameOrAlias, false, null, null, false);
        }


        SearchRequest searchRequest = builder.size(pageSize)
                .from(pageNumber * pageSize)
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();
        SearchResponse<EsWorkItem> esWorkItemSearchResponse = esClient.search(searchRequest, EsWorkItem.class);
        List<DbJiraIssue> issuesList = new ArrayList<>();
        List<EsWorkItem> esWorkItems = getEsWorkItems(esWorkItemSearchResponse);
        List<String> workItemIds = getWorkItemIds(responses);
        List<EsDevProdWorkItemResponse> finalResponses = responses;
        esWorkItems.stream()
                .filter(esWorkItem -> workItemIds.contains(esWorkItem.getWorkitemId()))
                .forEach(esWorkItem -> {
                    Optional<EsDevProdWorkItemResponse> first = finalResponses.stream()
                            .filter(response -> response.getWorkitemId().equalsIgnoreCase(esWorkItem.getWorkitemId()))
                            .findFirst();

                    esWorkItem.toBuilder()
                            .summary(esWorkItem.getSummary() == null ? StringUtils.EMPTY : esWorkItem.getSummary())
                            .build();

                    issuesList.add(EsJiraIssueConverter.getIssueFromEsWorkItem(esWorkItem, List.of(), first.get(), true, true, true, null));

                });
        return DbListResponse.of(issuesList, (int) esWorkItemSearchResponse.hits().total().value());
    }

    public DbListResponse<DbWorkItem> getWorkItemFeatureBreakDown(String company,
                                                                  WorkItemsFilter filter,
                                                                  List<String> developmentStages,
                                                                  DevProductivityProfile.FeatureType featureType,
                                                                  Map<String, SortingOrder> sortBy,
                                                                  Integer pageNumber,
                                                                  Integer pageSize) throws IOException {
        ElasticsearchClient esClient = esClientFactory.getESClient(company);
        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, filter.getIngestedAt());

        boolean useIssuePartialCredit = issuePartialCreditWITenants.contains(company);
        List<String> historicalAssignees = filter.getAssignees();
        ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories = ImmutablePair.of(historicalAssignees, List.of("InProgress", "Resolved"));
        filter = filter.toBuilder().assignees(null).across(null).calculation(null).acrossLimit(10000).build();
        boolean needMissingStoryPointsFilter = featureType == DevProductivityProfile.FeatureType.NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH;

        SearchRequest.Builder builder2 = null;
        if (useIssuePartialCredit) {
            builder2 = EsWorkItemQueryBuilder.buildSearchRequest(filter, WorkItemsMilestoneFilter.builder().build(),
                    developmentStages, null, histAssigneesAndHistStatusCategories, List.of(), needMissingStoryPointsFilter, null, null, indexNameOrAlias, false, null, null, false);
        } else {
            builder2 = EsWorkItemQueryBuilder.buildSearchRequest(filter, WorkItemsMilestoneFilter.builder().build(),
                    developmentStages, historicalAssignees, null, List.of(), needMissingStoryPointsFilter, null, null, indexNameOrAlias, false, null, null, false);
        }
        Map<String, Aggregation> outerAgg2 = getAggForWorkItemDevProd(historicalAssignees, developmentStages, useIssuePartialCredit);
        SearchRequest searchRequest2 = builder2.aggregations(outerAgg2)
                .build();

        String queryString = getQueryString(searchRequest2);
        log.info("Index name {} and ES WI Query 1 : {} ", indexNameOrAlias, queryString);

        SearchResponse<Void> searchResponse2 = esClient.search(searchRequest2, Void.class);

        List<EsDevProdWorkItemResponse> responses = EsDevProdWorkItemConverter.convert(searchResponse2, CollectionUtils.isNotEmpty(developmentStages), historicalAssignees, useIssuePartialCredit);
        responses = getEnrichedEsDevProdWorkItemResponses(responses);

        SearchRequest.Builder builder = null;
        if (useIssuePartialCredit) {
            builder = EsWorkItemQueryBuilder.buildSearchRequest(filter, WorkItemsMilestoneFilter.builder().build(),  developmentStages, null, histAssigneesAndHistStatusCategories, List.of(), needMissingStoryPointsFilter, null, null, indexNameOrAlias, false, null, null, false);
        } else {
            builder = EsWorkItemQueryBuilder.buildSearchRequest(filter, WorkItemsMilestoneFilter.builder().build(),  developmentStages, historicalAssignees, null, List.of(), needMissingStoryPointsFilter, null, null, indexNameOrAlias, false, null, null, false);
        }

        SearchRequest searchRequest = builder.size(pageSize)
                .from(pageNumber * pageSize)
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();
        queryString = getQueryString(searchRequest2);
        log.info("Index name {} and ES WI Query 2 : {} ", indexNameOrAlias, queryString);

        SearchResponse<EsWorkItem> esWorkItemSearchResponse = esClient.search(searchRequest, EsWorkItem.class);
        List<EsWorkItem> esWorkItems = getEsWorkItems(esWorkItemSearchResponse);
        List<String> workItemIds = getWorkItemIds(responses);
        List<EsDevProdWorkItemResponse> finalResponses = responses.stream()
                .collect(Collectors.toList());
        List<DbWorkItem> workItemsList = new ArrayList<>();
        esWorkItems.stream()
                .filter(esWorkItem -> workItemIds.contains(esWorkItem.getWorkitemId()))
                .forEach(esWorkItem -> {
                    Optional<EsDevProdWorkItemResponse> first = finalResponses.stream()
                            .filter(response -> response.getWorkitemId().equalsIgnoreCase(esWorkItem.getWorkitemId()))
                            .findFirst();
                    esWorkItem.toBuilder()
                            .summary(esWorkItem.getSummary() == null ? StringUtils.EMPTY : esWorkItem.getSummary())
                            .build();

                    workItemsList.add(EsWorkItemConverter.getWorkItemsFromEsWorkItem(esWorkItem, List.of(), first.get(), true, true, true));

                });
        return DbListResponse.of(workItemsList, (int) esWorkItemSearchResponse.hits().total().value());
    }

    @NotNull
    private List<String> getWorkItemIds(List<EsDevProdWorkItemResponse> responses) {
        return responses.stream()
                .map(EsDevProdWorkItemResponse::getWorkitemId)
                .collect(Collectors.toList());
    }

    @NotNull
    private List<EsWorkItem> getEsWorkItems(SearchResponse<EsWorkItem> esWorkItemSearchResponse) {
        return esWorkItemSearchResponse.hits().hits().stream().filter(hit -> hit.source() != null)
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    public List<DbAggregationResult> getDbAggregationResults(DevProductivityProfile.FeatureType featureType,
                                                             List<EsDevProdWorkItemResponse> responses) {
        if (featureType != DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME) {
            responses = getEnrichedEsDevProdWorkItemResponses(responses);
        }

        List<DbAggregationResult> results = new ArrayList<>();
        Map<String, List<EsDevProdWorkItemResponse>> dataByHistAssignees = responses.stream()
                .collect(Collectors.groupingBy(EsDevProdWorkItemResponse::getHistoricalAssigneeId));
        if (featureType == DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME) {
            dataByHistAssignees.forEach((assignee, esDevProdWorkItemResponses) -> {
                double assigneeTimeSum = esDevProdWorkItemResponses.stream().map(EsDevProdWorkItemResponse::getAssigneeTime)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                double timeSpentPerTicket = assigneeTimeSum / esDevProdWorkItemResponses.size();
                results.add(DbAggregationResult.builder()
                        .timeSpentPerTicket(Math.round(timeSpentPerTicket))
                        .build());
            });
        } else {
            dataByHistAssignees.forEach((assignee, esDevProdWorkItemResponses) -> {
                double storyPoints = esDevProdWorkItemResponses.stream().map(EsDevProdWorkItemResponse::getStoryPointsPortion)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                double ticketCount = esDevProdWorkItemResponses.stream().map(EsDevProdWorkItemResponse::getTicketPortion)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                results.add(DbAggregationResult.builder()
                        .totalTickets(Math.round(ticketCount))
                        .totalStoryPoints(Math.round(storyPoints))
                        .build());
            });
        }
        return results;
    }

    private List<EsDevProdWorkItemResponse> getEnrichedEsDevProdWorkItemResponses(List<EsDevProdWorkItemResponse> responses) {
        responses = responses.stream().map(response -> {
            EsDevProdWorkItemResponse.EsDevProdWorkItemResponseBuilder toBuilder = response.toBuilder();
            Double timeInStatuses = response.getTimeInStatuses();
            Double assigneeTime = response.getAssigneeTime();
            Double ticketPortion = assigneeTime / timeInStatuses;
            Double storyPoints = response.getStoryPoints() == null ? 0d : response.getStoryPoints();
            Double storyPointsPortion = storyPoints * ticketPortion;
            ticketPortion = Math.round(ticketPortion * 100.0)/100.0;
            storyPointsPortion = Math.round(storyPointsPortion * 100.0)/100.0;
            toBuilder = toBuilder.timeInStatuses(timeInStatuses)
                    .storyPointsPortion(storyPointsPortion)
                    .ticketPortion(ticketPortion);
            return toBuilder.build();
        }).collect(Collectors.toList());
        return responses;
    }

    @NotNull
    public static Map<String, Aggregation> getAggForWorkItemDevProd(List<String> assignees, List<String> developmentStages, boolean useIssuePartialCredit) {
        Map<String, Aggregation> sumAggOnHistAssigneeTime = (useIssuePartialCredit) ? EsUtils.getSumAgg("w_hist_assignee_statuses.hist_assignee_time_excluding_resolution") :  EsUtils.getSumAgg("w_hist_assignee_statuses.hist_assignee_time");
        Map<String, Aggregation> filterAggOnHistAssignee = EsUtils.getFilterExcludeAgg(sumAggOnHistAssigneeTime, "w_hist_assignee_statuses.issue_status", List.of("UNASSIGNED", "_UNASSIGNED_", "unassigned", "_unassigned_"));
        Map<String, Aggregation> termsAggOnHistAssignee;
        if (CollectionUtils.isEmpty(developmentStages)) {
            termsAggOnHistAssignee = EsUtils.getTermsAgg(filterAggOnHistAssignee, "w_hist_assignee_statuses.historical_assignee_id");
        } else {
            Map<String, Aggregation> filterAggOnHistStatus = EsUtils.getFilterAgg(filterAggOnHistAssignee, "w_hist_assignee_statuses.issue_status", developmentStages);
            termsAggOnHistAssignee = EsUtils.getTermsAgg(filterAggOnHistStatus, "w_hist_assignee_statuses.historical_assignee_id");
        }
        Map<String, Aggregation> nestedAggOnHistAssigneeStatuses = EsUtils.getNestedAgg(termsAggOnHistAssignee, "w_hist_assignee_statuses");
        Map<String, Aggregation> termsAggOnStoryPoints = EsUtils.getTermsAggWithMissing(nestedAggOnHistAssigneeStatuses, "w_story_points");
        return EsUtils.getTermsAgg(termsAggOnStoryPoints, "w_workitem_id");
    }

    @NotNull
    public static Map<String, Aggregation> getAggForWorkItemDevProd(List<String> developmentStages) {
        Map<String, Aggregation> sumAggOnTimeInStatuses = EsUtils.getSumAgg("w_hist_statuses.time_spent");
        Map<String, Aggregation> nestedAggOnHistStatuses;
        if (CollectionUtils.isEmpty(developmentStages)) {
            nestedAggOnHistStatuses = EsUtils.getNestedAgg(sumAggOnTimeInStatuses, "w_hist_statuses");
        } else {
            Map<String, Aggregation> filterAggOnHistStatuses = EsUtils.getFilterAgg(sumAggOnTimeInStatuses, "w_hist_statuses.status", developmentStages);
            nestedAggOnHistStatuses = EsUtils.getNestedAgg(filterAggOnHistStatuses, "w_hist_statuses");
        }
        return EsUtils.getTermsAgg(nestedAggOnHistStatuses, "w_workitem_id");
    }

    public JiraIssuesFilter mergeFilters(JiraIssuesFilter filter, JiraIssuesFilter ticketFilter) {

        return mergeJiraFilters(filter, ticketFilter.getExtraCriteria(),
                ticketFilter.getKeys(), ticketFilter.getPriorities(), ticketFilter.getStatuses(), ticketFilter.getAssignees(),
                ticketFilter.getIssueTypes(), ticketFilter.getIntegrationIds(), ticketFilter.getProjects(), ticketFilter.getComponents(),
                ticketFilter.getReporters(), ticketFilter.getLabels(), ticketFilter.getFixVersions(), ticketFilter.getVersions(), ticketFilter.getStages(), ticketFilter.getVelocityStages(),
                ticketFilter.getEpics(), ticketFilter.getParentKeys(), ticketFilter.getFirstAssignees(), ticketFilter.getLinks(), ticketFilter.getCustomFields(),
                ticketFilter.getHygieneCriteriaSpecs(), ticketFilter.getMissingFields(), ticketFilter.getIssueCreatedRange(), ticketFilter.getIssueDueRange(),
                ticketFilter.getIssueUpdatedRange(), ticketFilter.getIssueResolutionRange(), ticketFilter.getParentStoryPoints(),
                ticketFilter.getStoryPoints(), ticketFilter.getSummary(), ticketFilter.getFieldSize(), ticketFilter.getPartialMatch(),
                ticketFilter.getAge(), ticketFilter.getSprintCount(), ticketFilter.getSprintIds(), ticketFilter.getSprintNames(), ticketFilter.getSprintFullNames(), ticketFilter.getSprintStates(),
                ticketFilter.getResolutions(), ticketFilter.getStatusCategories(), ticketFilter.getTicketCategories(),
                ticketFilter.getAssigneesDateRange(), ticketFilter.getExcludeStatusCategories(), ticketFilter.getExcludeResolutions(),
                ticketFilter.getExcludeStages(), ticketFilter.getExcludeVelocityStages(), ticketFilter.getExcludeKeys(), ticketFilter.getExcludePriorities(),
                ticketFilter.getExcludeStatuses(), ticketFilter.getExcludeAssignees(), ticketFilter.getExcludeReporters(),
                ticketFilter.getExcludeIssueTypes(), ticketFilter.getExcludeFixVersions(), ticketFilter.getExcludeVersions(),
                ticketFilter.getExcludeIntegrationIds(), ticketFilter.getExcludeProjects(), ticketFilter.getExcludeComponents(),
                ticketFilter.getExcludeLabels(), ticketFilter.getExcludeEpics(), ticketFilter.getExcludeParentKeys(), ticketFilter.getExcludeLinks(),
                ticketFilter.getExcludeSprintIds(), ticketFilter.getExcludeSprintNames(), ticketFilter.getExcludeSprintFullNames(), ticketFilter.getExcludeSprintStates(),
                ticketFilter.getExcludeCustomFields(), ticketFilter.getSnapshotRange(), ticketFilter.getFilterByLastSprint(), BooleanUtils.isNotFalse(ticketFilter.getIsActive()),
                (ticketFilter.getIgnoreOU() != null ? ticketFilter.getIgnoreOU() : false), ticketFilter.getCustomStacks(), ticketFilter.getUnAssigned(), ticketFilter.getAssigneeDisplayNames(), ticketFilter.getHistoricalAssignees(), ticketFilter.getIds());
    }

    public JiraIssuesFilter mergeJiraFilters(JiraIssuesFilter jiraIssuesFilter,
                                             List<JiraIssuesFilter.EXTRA_CRITERIA> extraCriteria,
                                             List<String> keys,
                                             List<String> priorities,
                                             List<String> statuses,
                                             List<String> assignees,
                                             List<String> issueTypes,
                                             List<String> integrationIds,
                                             List<String> projects,
                                             List<String> components,
                                             List<String> reporters,
                                             List<String> labels,
                                             List<String> fixVersions,
                                             List<String> versions,
                                             List<String> stages,
                                             List<String> velocityStages,
                                             List<String> epics,
                                             List<String> parentKeys,
                                             List<String> firstAssignees,
                                             List<String> links,
                                             Map<String, Object> customFields,
                                             Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> hygieneSpecs,
                                             Map<String, Boolean> missingFields,
                                             ImmutablePair<Long, Long> issueCreatedRange,
                                             ImmutablePair<Long, Long> issueDueRange,
                                             ImmutablePair<Long, Long> issueUpdatedRange,
                                             ImmutablePair<Long, Long> issueResolutionRange,
                                             Map<String, String> parentStoryPoints,
                                             Map<String, String> storyPoints,
                                             String summary,
                                             Map<String, Map<String, String>> fieldSize,
                                             Map<String, Map<String, String>> partialMatch,
                                             ImmutablePair<Long, Long> age,
                                             Integer sprintCount,
                                             List<String> sprintIds,
                                             List<String> sprintNames,
                                             List<String> sprintFullNames,
                                             List<String> sprintStates,
                                             List<String> resolutions,
                                             List<String> statusCategories,
                                             List<String> ticketCategories,
                                             ImmutablePair<Long, Long> assigneesDateRange,
                                             List<String> excludeStatusCategories,
                                             List<String> excludeResolutions,
                                             List<String> excludeStages,
                                             List<String> excludeVelocityStages,
                                             List<String> excludeKeys,
                                             List<String> excludePriorities,
                                             List<String> excludeStatuses,
                                             List<String> excludeAssignees,
                                             List<String> excludeReporters,
                                             List<String> excludeIssueTypes,
                                             List<String> excludeFixVersions,
                                             List<String> excludeVersions,
                                             List<String> excludeIntegrationIds,
                                             List<String> excludeProjects,
                                             List<String> excludeComponents,
                                             List<String> excludeLabels,
                                             List<String> excludeEpics,
                                             List<String> excludeParentKeys,
                                             List<String> excludeLinks,
                                             List<String> excludeSprintIds,
                                             List<String> excludeSprintNames,
                                             List<String> excludeSprintFullNames,
                                             List<String> excludeSprintStates,
                                             Map<String, Object> excludeCustomFields,
                                             ImmutablePair<Long, Long> snapshotRange,
                                             Boolean filterByLastSprint,
                                             Boolean isActive,
                                             Boolean ignoreOU,
                                             List<String> customStacks,
                                             Boolean unAssigned, List<String> assigneeDisplayNames,
                                             List<String> historicalAssignees, List<UUID> ids) {

        if (CollectionUtils.isNotEmpty(excludeSprintIds)) {
            if (jiraIssuesFilter.getExcludeSprintIds() != null) {
                excludeSprintIds.addAll(jiraIssuesFilter.getExcludeSprintIds());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeSprintIds(excludeSprintIds)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeSprintNames)) {
            if (jiraIssuesFilter.getExcludeSprintNames() != null) {
                excludeSprintNames.addAll(jiraIssuesFilter.getExcludeSprintNames());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeSprintNames(excludeSprintNames)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeSprintFullNames)) {
            if (jiraIssuesFilter.getExcludeSprintFullNames() != null) {
                excludeSprintFullNames.addAll(jiraIssuesFilter.getExcludeSprintFullNames());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeSprintFullNames(excludeSprintFullNames)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeSprintStates)) {
            if (jiraIssuesFilter.getExcludeSprintStates() != null) {
                excludeSprintStates.addAll(jiraIssuesFilter.getExcludeSprintStates());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeSprintStates(excludeSprintStates)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeLinks)) {
            if (jiraIssuesFilter.getExcludeSprintStates() != null) {
                excludeLinks.addAll(jiraIssuesFilter.getExcludeLinks());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeLinks(excludeLinks)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeVelocityStages)) {
            if (jiraIssuesFilter.getStages() != null) {
                excludeVelocityStages.addAll(jiraIssuesFilter.getExcludeVelocityStages());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeVelocityStages(excludeVelocityStages)
                    .build();
        }

        if (MapUtils.isNotEmpty(customFields)) {
            if (jiraIssuesFilter.getCustomFields() != null) {
                customFields.putAll(jiraIssuesFilter.getCustomFields());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .customFields(customFields)
                    .build();
        }

        if (CollectionUtils.isNotEmpty(customStacks)) {
            if (jiraIssuesFilter.getCustomStacks() != null) {
                customStacks.addAll(jiraIssuesFilter.getCustomStacks());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .customStacks(customStacks)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(stages)) {
            if (jiraIssuesFilter.getStages() != null) {
                stages.addAll(jiraIssuesFilter.getStages());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .stages(stages)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(velocityStages)) {
            if (jiraIssuesFilter.getStages() != null) {
                velocityStages.addAll(jiraIssuesFilter.getVelocityStages());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .velocityStages(velocityStages)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(links)) {
            if (jiraIssuesFilter.getLinks() != null) {
                links.addAll(jiraIssuesFilter.getLinks());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .links(links)
                    .build();
        }

        if (CollectionUtils.isNotEmpty(sprintIds)) {
            if (jiraIssuesFilter.getSprintIds() != null) {
                sprintIds.addAll(jiraIssuesFilter.getSprintIds());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .sprintIds(sprintIds)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(sprintNames)) {
            if (jiraIssuesFilter.getSprintNames() != null) {
                sprintNames.addAll(jiraIssuesFilter.getSprintNames());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .sprintNames(sprintNames)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(sprintFullNames)) {
            if (jiraIssuesFilter.getSprintFullNames() != null) {
                sprintFullNames.addAll(jiraIssuesFilter.getSprintFullNames());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .sprintFullNames(sprintFullNames)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(sprintStates)) {
            if (jiraIssuesFilter.getSprintStates() != null) {
                sprintStates.addAll(jiraIssuesFilter.getSprintStates());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .sprintStates(sprintStates)
                    .build();
        }

        if (CollectionUtils.isNotEmpty(integrationIds)) {
            if (jiraIssuesFilter.getIntegrationIds() != null) {
                integrationIds.addAll(jiraIssuesFilter.getIntegrationIds());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .integrationIds(integrationIds)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(assignees)) {
            if (jiraIssuesFilter.getAssignees() != null) {
                assignees.addAll(jiraIssuesFilter.getAssignees());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .assignees(assignees)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(reporters)) {
            if (jiraIssuesFilter.getReporters() != null) {
                reporters.addAll(jiraIssuesFilter.getReporters());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .reporters(reporters)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeKeys)) {
            if (jiraIssuesFilter.getExcludeKeys() != null) {
                excludeKeys.addAll(jiraIssuesFilter.getExcludeKeys());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeKeys(excludeKeys)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludePriorities)) {
            if (jiraIssuesFilter.getExcludePriorities() != null) {
                excludePriorities.addAll(jiraIssuesFilter.getExcludePriorities());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludePriorities(excludePriorities)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeStatuses)) {
            if (jiraIssuesFilter.getExcludeStatuses() != null) {
                excludeStatuses.addAll(jiraIssuesFilter.getExcludeStatuses());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeStatuses(excludeStatuses)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeAssignees)) {
            if (jiraIssuesFilter.getExcludeAssignees() != null) {
                excludeAssignees.addAll(jiraIssuesFilter.getExcludeAssignees());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeAssignees(excludeAssignees)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeReporters)) {
            if (jiraIssuesFilter.getExcludeReporters() != null) {
                excludeReporters.addAll(jiraIssuesFilter.getExcludeReporters());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeReporters(excludeReporters)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeIssueTypes)) {
            if (jiraIssuesFilter.getExcludeIssueTypes() != null) {
                excludeIssueTypes.addAll(jiraIssuesFilter.getExcludeIssueTypes());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeIssueTypes(excludeIssueTypes)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeFixVersions)) {
            if (jiraIssuesFilter.getExcludeFixVersions() != null) {
                excludeFixVersions.addAll(jiraIssuesFilter.getExcludeFixVersions());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeFixVersions(excludeFixVersions)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeVersions)) {
            if (jiraIssuesFilter.getExcludeVersions() != null) {
                excludeVersions.addAll(jiraIssuesFilter.getExcludeVersions());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeVersions(excludeVersions)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeIntegrationIds)) {
            if (jiraIssuesFilter.getExcludeIntegrationIds() != null) {
                excludeIntegrationIds.addAll(jiraIssuesFilter.getExcludeIntegrationIds());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeIntegrationIds(excludeIntegrationIds)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            if (jiraIssuesFilter.getExcludeProjects() != null) {
                excludeProjects.addAll(jiraIssuesFilter.getExcludeProjects());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeProjects(excludeProjects)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeComponents)) {
            if (jiraIssuesFilter.getExcludeComponents() != null) {
                excludeComponents.addAll(jiraIssuesFilter.getExcludeComponents());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeComponents(excludeComponents)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeLabels)) {
            if (jiraIssuesFilter.getExcludeLabels() != null) {
                excludeLabels.addAll(jiraIssuesFilter.getExcludeLabels());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeLabels(excludeLabels)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeEpics)) {
            if (jiraIssuesFilter.getExcludeEpics() != null) {
                excludeEpics.addAll(jiraIssuesFilter.getExcludeEpics());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeEpics(excludeEpics)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeParentKeys)) {
            if (jiraIssuesFilter.getExcludeParentKeys() != null) {
                excludeParentKeys.addAll(jiraIssuesFilter.getExcludeParentKeys());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeParentKeys(excludeParentKeys)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeResolutions)) {
            if (jiraIssuesFilter.getExcludeResolutions() != null) {
                excludeResolutions.addAll(jiraIssuesFilter.getExcludeResolutions());
            }
            jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                    .excludeResolutions(excludeResolutions)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeStatusCategories)) {
            if (jiraIssuesFilter.getExcludeStatusCategories() != null) {
                excludeStatusCategories.addAll(jiraIssuesFilter.getExcludeStatusCategories());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .excludeStatusCategories(excludeStatusCategories)
                    .build();
        }
        if (MapUtils.isNotEmpty(excludeCustomFields)) {
            if (jiraIssuesFilter.getExcludeCustomFields() != null) {
                excludeCustomFields.putAll(jiraIssuesFilter.getExcludeCustomFields());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .excludeCustomFields(excludeCustomFields)
                    .build();

        }
        if (MapUtils.isNotEmpty(missingFields)) {
            if (jiraIssuesFilter.getMissingFields() != null) {
                missingFields.putAll(jiraIssuesFilter.getMissingFields());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .missingFields(missingFields)
                    .build();

        }
        if (MapUtils.isNotEmpty(hygieneSpecs)) {
            if (jiraIssuesFilter.getMissingFields() != null) {
                hygieneSpecs.putAll(jiraIssuesFilter.getHygieneCriteriaSpecs());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .hygieneCriteriaSpecs(hygieneSpecs)
                    .build();

        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            if (jiraIssuesFilter.getPriorities() != null) {
                priorities.addAll(jiraIssuesFilter.getPriorities());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .priorities(priorities)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(keys)) {
            if (jiraIssuesFilter.getKeys() != null) {
                keys.addAll(jiraIssuesFilter.getKeys());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .keys(keys)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            if (jiraIssuesFilter.getStatuses() != null) {
                statuses.addAll(jiraIssuesFilter.getStatuses());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .statuses(statuses)
                    .build();
        }

        if (CollectionUtils.isNotEmpty(assigneeDisplayNames)) {
            if (jiraIssuesFilter.getAssigneeDisplayNames() != null) {
                assigneeDisplayNames.addAll(jiraIssuesFilter.getAssigneeDisplayNames());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .assigneeDisplayNames(assigneeDisplayNames)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(historicalAssignees)) {
            if (jiraIssuesFilter.getHistoricalAssignees() != null) {
                historicalAssignees.addAll(jiraIssuesFilter.getHistoricalAssignees());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .historicalAssignees(historicalAssignees)
                    .build();
        }
        if (unAssigned != null && unAssigned) {
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .unAssigned(unAssigned)
                    .build();
        }
        if (isActive != null && isActive) {
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .isActive(isActive)
                    .build();
        }
        if (filterByLastSprint != null && filterByLastSprint) {
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .filterByLastSprint(filterByLastSprint)
                    .build();
        }
        if (sprintCount != null && sprintCount > 0) {
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .sprintCount(sprintCount)
                    .build();
        }

        if (CollectionUtils.isNotEmpty(issueTypes)) {
            if (jiraIssuesFilter.getIssueTypes() != null) {
                issueTypes.addAll(jiraIssuesFilter.getIssueTypes());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .issueTypes(issueTypes)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            if (jiraIssuesFilter.getProjects() != null) {
                projects.addAll(jiraIssuesFilter.getProjects());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .projects(projects)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(epics)) {
            if (jiraIssuesFilter.getEpics() != null) {
                epics.addAll(jiraIssuesFilter.getEpics());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .epics(epics)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(parentKeys)) {
            if (jiraIssuesFilter.getParentKeys() != null) {
                parentKeys.addAll(jiraIssuesFilter.getParentKeys());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .parentKeys(parentKeys)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(resolutions)) {
            if (jiraIssuesFilter.getResolutions() != null) {
                resolutions.addAll(jiraIssuesFilter.getResolutions());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .resolutions(resolutions)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(statusCategories)) {
            if (jiraIssuesFilter.getStatusCategories() != null) {
                statusCategories.addAll(jiraIssuesFilter.getStatusCategories());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .statusCategories(statusCategories)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            if (jiraIssuesFilter.getStatusCategories() != null) {
                extraCriteria.addAll(jiraIssuesFilter.getExtraCriteria());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .extraCriteria(extraCriteria)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(excludeStages)) {
            if (jiraIssuesFilter.getExcludeStages() != null) {
                excludeStages.addAll(jiraIssuesFilter.getExcludeStages());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .excludeStages(excludeStages)
                    .build();
        }
        if (issueCreatedRange != null) {
            if (jiraIssuesFilter.getIssueCreatedRange() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .issueCreatedRange(issueCreatedRange)
                        .build();
            }
        }
        if (issueUpdatedRange != null) {
            if (jiraIssuesFilter.getIssueUpdatedRange() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .issueUpdatedRange(issueUpdatedRange)
                        .build();
            }
        }
        if (issueDueRange != null) {
            if (jiraIssuesFilter.getIssueDueRange() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .issueDueRange(issueDueRange)
                        .build();
            }
        }
        if (issueResolutionRange != null) {
            if (jiraIssuesFilter.getIssueUpdatedRange() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .issueUpdatedRange(issueUpdatedRange)
                        .build();
            }
        }
        if (snapshotRange != null) {
            if (jiraIssuesFilter.getSnapshotRange() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .snapshotRange(snapshotRange)
                        .build();
            }
        }
        if (assigneesDateRange != null) {
            if (jiraIssuesFilter.getAssigneesDateRange() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .assigneesDateRange(assigneesDateRange)
                        .build();
            }
        }
        if (age != null) {
            if (jiraIssuesFilter.getAge() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .age(age)
                        .build();
            }
        }
        if (CollectionUtils.isNotEmpty(firstAssignees)) {
            if (jiraIssuesFilter.getFirstAssignees() != null) {
                firstAssignees.addAll(jiraIssuesFilter.getFirstAssignees());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .firstAssignees(firstAssignees)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            if (jiraIssuesFilter.getLabels() != null) {
                labels.addAll(jiraIssuesFilter.getLabels());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .labels(labels)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(ticketCategories)) {
            if (jiraIssuesFilter.getTicketCategories() != null) {
                ticketCategories.addAll(jiraIssuesFilter.getTicketCategories());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .ticketCategories(ticketCategories)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(versions)) {
            if (jiraIssuesFilter.getVersions() != null) {
                versions.addAll(jiraIssuesFilter.getVersions());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .versions(versions)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(fixVersions)) {
            if (jiraIssuesFilter.getFixVersions() != null) {
                fixVersions.addAll(jiraIssuesFilter.getFixVersions());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .fixVersions(fixVersions)
                    .build();
        }
        if (CollectionUtils.isNotEmpty(components)) {
            if (jiraIssuesFilter.getComponents() != null) {
                components.addAll(jiraIssuesFilter.getComponents());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .components(components)
                    .build();
        }
        if (StringUtils.isNotEmpty(summary)) {
            if (jiraIssuesFilter.getSummary() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .summary(summary)
                        .build();
            }
        }
        if (MapUtils.isNotEmpty(storyPoints)) {
            if (jiraIssuesFilter.getStoryPoints() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .storyPoints(storyPoints)
                        .build();
            }
        }

        if (MapUtils.isNotEmpty(fieldSize)) {
            if (jiraIssuesFilter.getFieldSize() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .fieldSize(fieldSize)
                        .build();
            }
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            if (jiraIssuesFilter.getPartialMatch() == null) {
                jiraIssuesFilter = jiraIssuesFilter
                        .toBuilder()
                        .partialMatch(partialMatch)
                        .build();
            }
        }
        if (MapUtils.isNotEmpty(parentStoryPoints)) {
            if (jiraIssuesFilter.getParentStoryPoints() == null) {
                jiraIssuesFilter = jiraIssuesFilter.toBuilder()
                        .parentStoryPoints(parentStoryPoints)
                        .build();
            }
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            if (jiraIssuesFilter.getIds() != null) {
                ids.addAll(jiraIssuesFilter.getIds());
            }
            jiraIssuesFilter = jiraIssuesFilter
                    .toBuilder()
                    .ids(ids)
                    .build();
        }
        return jiraIssuesFilter;

    }
}
