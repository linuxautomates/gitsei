package io.levelops.commons.services.business_alignment.es.query_builder;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.IssueInheritanceMode;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.services.business_alignment.es.result_converter.BAESResultConverter;
import io.levelops.commons.services.business_alignment.es.result_converter.composite.BACompositeESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.result_converter.terms.BATermsESResultConverterFactory;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import io.levelops.faceted_search.querybuilders.ESRequest;
import io.levelops.faceted_search.querybuilders.workitems.EsJiraQueryBuilder;
import io.levelops.faceted_search.utils.ESAggResultUtils;
import io.levelops.faceted_search.utils.EsUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.IN_PROGRESS_STATUS_CATEGORY;

@Log4j2
@Service
public class BaJiraWIAggsESQueryBuilder {
    private static final Integer COMPOSITE_AGG_PAGE_SIZE = 10000;
    public static final Boolean USE_TERMS_AGG = false;
    public static final Boolean USE_COMPOSITE_AGG = true;
    private static final Boolean SKIP_INTEGRATION_TYPE_QUERY = true;
    private final BATermsESResultConverterFactory baTermsESResultConverterFactory;
    private final BACompositeESResultConverterFactory baCompositeESResultConverterFactory;

    @Autowired
    public BaJiraWIAggsESQueryBuilder(BATermsESResultConverterFactory baTermsESResultConverterFactory, BACompositeESResultConverterFactory baCompositeESResultConverterFactory) {
        this.baTermsESResultConverterFactory = baTermsESResultConverterFactory;
        this.baCompositeESResultConverterFactory = baCompositeESResultConverterFactory;
    }

    private boolean needsHistoricalAssignees(BaJiraOptions baJiraOptions) {
        return BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES.equals(baJiraOptions.getAttributionMode());
    }

    //region Aggs Common
    private Map<String, Aggregation> buildSumAggForHistAssigneeStatuses(BaJiraOptions baJiraOptions) {
        Map<String, Aggregation> sumTicketTime = EsUtils.getSumAgg("w_hist_assignee_statuses.hist_assignee_time");
        Map<String, Aggregation> sumTicketTimeIncludeInProgress = null;
        if (CollectionUtils.isNotEmpty(baJiraOptions.getInProgressStatuses())) {
            List<String> inProgressStatuses = baJiraOptions.getInProgressStatuses().stream()
                    .map(StringUtils::upperCase)
                    .collect(Collectors.toList());
            sumTicketTimeIncludeInProgress = EsUtils.getFilterAgg(sumTicketTime, "w_hist_assignee_statuses.issue_status", inProgressStatuses);
        } else {
            List<String> statusCategories;
            if (CollectionUtils.isNotEmpty(baJiraOptions.getInProgressStatusCategories())) {
                statusCategories = baJiraOptions.getInProgressStatusCategories().stream()
                        .map(StringUtils::upperCase)
                        .collect(Collectors.toList());
            } else {
                statusCategories = List.of(IN_PROGRESS_STATUS_CATEGORY.toUpperCase());
            }
            sumTicketTimeIncludeInProgress = EsUtils.getFilterAgg(sumTicketTime, "w_hist_assignee_statuses.issue_status_category", statusCategories);
        }
        return sumTicketTimeIncludeInProgress;
    }
    //endregion

    //region Term Aggs
    //region Term Aggs + Current
    private Map<String, Aggregation> buildAggForCurrentForTime(ESAggInterval esAggInterval, BaJiraOptions baJiraOptions) {
        Map<String, Aggregation> sumTicketTimeIncludeInProgress = buildSumAggForHistAssigneeStatuses(baJiraOptions);
        Map<String, Aggregation> nestedAggOnHistAssigneeStatuses = EsUtils.getNestedAgg(sumTicketTimeIncludeInProgress, "w_hist_assignee_statuses");
        Map<String, Aggregation> sumStoryPointsByMonth = EsUtils.getDateHistogramAgg(nestedAggOnHistAssigneeStatuses, "w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat());
        Map<String, Aggregation> sumStoryPointsByMonthByAssigneeOrReporterName = EsUtils.getTermsAgg(sumStoryPointsByMonth, "w_assignee.display_name");
        Map<String, Aggregation> sumStoryPointsByMonthByAssigneeOrReporterNameAndId = EsUtils.getTermsAgg(sumStoryPointsByMonthByAssigneeOrReporterName, "w_assignee.id");
        return sumStoryPointsByMonthByAssigneeOrReporterNameAndId;
    }
    private Map<String, Aggregation> buildAggForCurrentForCounts(ESAggInterval esAggInterval) {
        Map<String, Aggregation> sumStoryPoints = EsUtils.getSumAgg("w_story_points");
        Map<String, Aggregation> sumStoryPointsByMonth = EsUtils.getDateHistogramAgg(sumStoryPoints, "w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat());
        Map<String, Aggregation> sumStoryPointsByMonthByAssigneeOrReporterName = EsUtils.getTermsAgg(sumStoryPointsByMonth, "w_assignee.display_name");
        Map<String, Aggregation> sumStoryPointsByMonthByAssigneeOrReporterNameAndId = EsUtils.getTermsAgg(sumStoryPointsByMonthByAssigneeOrReporterName, "w_assignee.id");
        return sumStoryPointsByMonthByAssigneeOrReporterNameAndId;
    }
    private Map<String, Aggregation> buildAggForCurrent(Calculation calculation, ESAggInterval aggsInterval, BaJiraOptions baJiraOptions) {
        switch (calculation) {
            case TICKET_COUNT:
            case STORY_POINTS:
                return buildAggForCurrentForCounts(aggsInterval);
            case TICKET_TIME_SPENT:
                return buildAggForCurrentForTime(aggsInterval, baJiraOptions);
            default:
                throw new RuntimeException("Calculation " + calculation + " not supported!");
        }
    }
    //endregion

    //region Term Aggs + Hist
    private Map<String, Aggregation> buildAggForHistForCounts(ESAggInterval esAggInterval) {
        Map<String, Aggregation> sumStoryPoints = EsUtils.getSumAgg("w_story_points");
        Map<String, Aggregation> sumStoryPointsByMonth = EsUtils.getDateHistogramAgg(sumStoryPoints, "w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat());
        Map<String, Aggregation> sumStoryPointsByMonthReverse = EsUtils.getReverseNestedAgg(sumStoryPointsByMonth, "sum_story_points_by_month");
        Map<String, Aggregation> termsAggOnHistAssignee = EsUtils.getTermsAgg(sumStoryPointsByMonthReverse, "w_hist_assignee_statuses.historical_assignee");
        Map<String, Aggregation> termsAggOnHistAssigneeId = EsUtils.getTermsAgg(termsAggOnHistAssignee, "w_hist_assignee_statuses.historical_assignee_id");
        Map<String, Aggregation> nestedAggOnHistAssigneeStatuses = EsUtils.getNestedAgg(termsAggOnHistAssigneeId, "w_hist_assignee_statuses");
        return nestedAggOnHistAssigneeStatuses;
    }
    private Map<String, Aggregation> buildAggForHistForTime(ESAggInterval esAggInterval, BaJiraOptions baJiraOptions) {
        Map<String, Aggregation> sumTicketTimeIncludeInProgress = buildSumAggForHistAssigneeStatuses(baJiraOptions);
        Map<String, Aggregation> termsAggOnHistAssignee = EsUtils.getTermsAgg(sumTicketTimeIncludeInProgress, "w_hist_assignee_statuses.historical_assignee");
        Map<String, Aggregation> termsAggOnHistAssigneeId = EsUtils.getTermsAgg(termsAggOnHistAssignee, "w_hist_assignee_statuses.historical_assignee_id");
        Map<String, Aggregation> nestedAggOnHistAssigneeStatuses = EsUtils.getNestedAgg(termsAggOnHistAssigneeId, "w_hist_assignee_statuses");
        Map<String, Aggregation> sumTicketTimeByAssigneeByTimeInterval = EsUtils.getDateHistogramAgg(nestedAggOnHistAssigneeStatuses, "w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat());
        return sumTicketTimeByAssigneeByTimeInterval;
    }
    private Map<String, Aggregation> buildAggForHist(Calculation calculation, ESAggInterval aggsInterval, BaJiraOptions baJiraOptions) {
        switch (calculation) {
            case TICKET_COUNT:
            case STORY_POINTS:
                return buildAggForHistForCounts(aggsInterval);
            case TICKET_TIME_SPENT:
                return buildAggForHistForTime(aggsInterval, baJiraOptions);
            default:
                throw new RuntimeException("Calculation " + calculation + " not supported!");
        }
    }
    //endregion

    private Map<String, Aggregation> buildTermAgg(Calculation calculation, ESAggInterval esAggInterval, BaJiraOptions baJiraOptions) {
        if (BooleanUtils.isNotTrue(esAggInterval.getSupportsBA())) {
            throw new RuntimeException("Agg Interval " + esAggInterval + " is not supported for BA!");
        }
        if (needsHistoricalAssignees(baJiraOptions)) {
            return buildAggForHist(calculation, esAggInterval, baJiraOptions);
        } else {
            return buildAggForCurrent(calculation, esAggInterval, baJiraOptions);
        }
    }
    //endregion

    //region Composite Aggs
    //region Composite Aggs + Current
    private CompositeAggregation buildCompositeAggBaseForCurrent(ESAggInterval esAggInterval, boolean addInterval, Map<String, String> afterKey) {
        List<Map<String, CompositeAggregationSource>> compositeSources = new ArrayList<>();
        compositeSources.add(EsUtils.getTermCompositeSource("assignee_id", "w_assignee.id"));
        compositeSources.add(EsUtils.getTermCompositeSource("assignee_name", "w_assignee.display_name"));
        if (addInterval) {
            compositeSources.add(EsUtils.getDateHistoCompositeSource("interval", "w_resolved_at", esAggInterval.getCalendarInterval(), null));
            //compositeSources.add(EsUtils.getDateHistoCompositeSource("interval_as_string", "w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat()));
        }
        CompositeAggregation compositeAgg = EsUtils.getCompositeAgg(COMPOSITE_AGG_PAGE_SIZE, compositeSources, afterKey);
        return compositeAgg;
    }
    private Map<String, Aggregation> buildCompositeAggForCurrentForTime(ESAggInterval esAggInterval, BaJiraOptions baJiraOptions, Map<String, String> afterKey) {
        CompositeAggregation compositeAgg = buildCompositeAggBaseForCurrent(esAggInterval, true, afterKey);
        Map<String, Aggregation> sumTicketTimeIncludeInProgress = buildSumAggForHistAssigneeStatuses(baJiraOptions);
        Map<String, Aggregation> nestedAggOnHistAssigneeStatuses = EsUtils.getNestedAgg(sumTicketTimeIncludeInProgress, "w_hist_assignee_statuses");
        Map<String, Aggregation> baAggs = EsUtils.combineCompositeAndAgg("ba_agg", nestedAggOnHistAssigneeStatuses, compositeAgg);
        return baAggs;
    }
    private Map<String, Aggregation> buildCompositeAggForCurrentForCounts(ESAggInterval esAggInterval, Map<String, String> afterKey) {
        CompositeAggregation compositeAgg = buildCompositeAggBaseForCurrent(esAggInterval, true, afterKey);
        Map<String, Aggregation> sumStoryPoints = EsUtils.getSumAgg("w_story_points");
        Map<String, Aggregation> baAggs = EsUtils.combineCompositeAndAgg("ba_agg", sumStoryPoints, compositeAgg);
        return baAggs;
    }
    private Map<String, Aggregation> buildCompositeAggForCurrent(Calculation calculation, ESAggInterval aggsInterval, BaJiraOptions baJiraOptions, Map<String, String> afterKey) {
        switch (calculation) {
            case TICKET_COUNT:
            case STORY_POINTS:
                return buildCompositeAggForCurrentForCounts(aggsInterval, afterKey);
            case TICKET_TIME_SPENT:
                return buildCompositeAggForCurrentForTime(aggsInterval, baJiraOptions, afterKey);
            default:
                throw new RuntimeException("Calculation " + calculation + " not supported!");
        }
    }
    //endregion
    //region Composite Aggs + Hist
    private CompositeAggregation buildCompositeAggBaseForHist(ESAggInterval esAggInterval, boolean addInterval, Map<String, String> afterKey) {
        List<Map<String, CompositeAggregationSource>> compositeSources = new ArrayList<>();
        compositeSources.add(EsUtils.getTermCompositeSource("assignee_id", "w_hist_assignee_statuses.historical_assignee_id"));
        compositeSources.add(EsUtils.getTermCompositeSource("assignee_name", "w_hist_assignee_statuses.historical_assignee"));
        if (addInterval) {
            compositeSources.add(EsUtils.getDateHistoCompositeSource("interval", "w_hist_assignee_statuses.w_resolved_at", esAggInterval.getCalendarInterval(), null));
            //compositeSources.add(EsUtils.getDateHistoCompositeSource("interval_as_string", "w_hist_assignee_statuses.w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat()));
        }
        CompositeAggregation compositeAgg = EsUtils.getCompositeAgg(COMPOSITE_AGG_PAGE_SIZE, compositeSources, afterKey);
        return compositeAgg;
    }
    private Map<String, Aggregation> buildCompositeAggForHistForCounts(ESAggInterval esAggInterval, Map<String, String> afterKey) {
        CompositeAggregation compositeAgg = buildCompositeAggBaseForHist(esAggInterval, false, afterKey);
        Map<String, Aggregation> sumStoryPoints = EsUtils.getSumAgg("w_story_points");
        Map<String, Aggregation> sumStoryPointsByMonth = EsUtils.getDateHistogramAgg(sumStoryPoints, "w_resolved_at", esAggInterval.getCalendarInterval(), esAggInterval.getFormat());
        Map<String, Aggregation> sumStoryPointsByMonthReverse = EsUtils.getReverseNestedAgg(sumStoryPointsByMonth, "sum_story_points_by_month");
        Map<String, Aggregation> combined = EsUtils.combineCompositeAndAgg("combined", sumStoryPointsByMonthReverse, compositeAgg);
        Map<String, Aggregation> baAggs = EsUtils.getNestedAgg(combined, "w_hist_assignee_statuses");
        return baAggs;
    }
    private Map<String, Aggregation> buildCompositeAggForHistForTime(ESAggInterval esAggInterval, BaJiraOptions baJiraOptions, Map<String, String> afterKey) {
        CompositeAggregation compositeAgg = buildCompositeAggBaseForHist(esAggInterval, true, afterKey);
        Map<String, Aggregation> sumTicketTimeIncludeInProgress = buildSumAggForHistAssigneeStatuses(baJiraOptions);
        Map<String, Aggregation> combined = EsUtils.combineCompositeAndAgg("combined", sumTicketTimeIncludeInProgress, compositeAgg);
        Map<String, Aggregation> baAggs = EsUtils.getNestedAgg(combined, "w_hist_assignee_statuses");
        return baAggs;
    }
    private Map<String, Aggregation> buildCompositeAggForHist(Calculation calculation, ESAggInterval aggsInterval, BaJiraOptions baJiraOptions, Map<String, String> afterKey) {
        switch (calculation) {
            case TICKET_COUNT:
            case STORY_POINTS:
                return buildCompositeAggForHistForCounts(aggsInterval, afterKey);
            case TICKET_TIME_SPENT:
                return buildCompositeAggForHistForTime(aggsInterval, baJiraOptions, afterKey);
            default:
                throw new RuntimeException("Calculation " + calculation + " not supported!");
        }
    }
    //endregion

    private Map<String, Aggregation> buildCompositeAgg(Calculation calculation, ESAggInterval esAggInterval, BaJiraOptions baJiraOptions, Map<String, String> afterKey) {
        if (BooleanUtils.isNotTrue(esAggInterval.getSupportsBA())) {
            throw new RuntimeException("Agg Interval " + esAggInterval + " is not supported for BA!");
        }
        if (needsHistoricalAssignees(baJiraOptions)) {
            return buildCompositeAggForHist(calculation, esAggInterval, baJiraOptions, afterKey);
        } else {
            return buildCompositeAggForCurrent(calculation, esAggInterval, baJiraOptions, afterKey);
        }
    }
    //endregion

    //region Query
    private ESRequest buildQueryWithIssueInheritance(JiraIssuesFilter.TicketCategorizationFilter categoryFilter, List<DbJiraField> dbJiraFields) {
        ESRequest request = EsJiraQueryBuilder.buildESRequest(categoryFilter.getFilter().toBuilder().isActive(false).age(null).build(), null, null, null, dbJiraFields, false, null, null, null, null, null, SKIP_INTEGRATION_TYPE_QUERY);
        //ToDo: VA Fix.
//        if(categoryFilter.getIssueInheritanceMode() == IssueInheritanceMode.ALL_TICKETS) {
//            return request;
//        }
//        String path = categoryFilter.getIssueInheritanceMode().getPath();
        String path = "";
        Query nestedQ = NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(request.getMust())
                                .mustNot(ListUtils.emptyIfNull(request.getMustNot()))
                                .should(ListUtils.emptyIfNull(request.getShould()))
                        )
                )
        )._toQuery();
        return ESRequest.builder()
                .must(List.of(nestedQ))
                .mustNot(null)
                .should(null)
                .build();
    }

    private ESRequest buildQuery(JiraIssuesFilter filter, List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters, String categoryName, List<DbJiraField> dbJiraFields) {
        ESRequest esRequestForJiraFilter = EsJiraQueryBuilder.buildESRequest(filter, null, null, null, dbJiraFields, false, null,
                false, null, null, false);
        List<ESRequest> mustNotForNonMatchingCategories = new ArrayList<>();
        ESRequest mustForMatchingCategories = null;
        if (StringUtils.isNotBlank(categoryName)) {
            for (JiraIssuesFilter.TicketCategorizationFilter categoryFilter : ticketCategorizationFilters) {
                ESRequest request = EsJiraQueryBuilder.buildESRequest(categoryFilter.getFilter().toBuilder().isActive(false).age(null).build(), null, null, null, dbJiraFields, false, null, null, null, null, null, SKIP_INTEGRATION_TYPE_QUERY);
                //ESRequest request = buildQueryWithIssueInheritance(categoryFilter, dbJiraFields);
                if (categoryName.equals(categoryFilter.getName())) {
                    mustForMatchingCategories = request;
                    break;
                } else {
                    mustNotForNonMatchingCategories.add(request);
                }
            }
        }
        List<Query> must = new ArrayList<>();
        List<Query> mustNot = new ArrayList<>();
        List<Query> should = new ArrayList<>();

        //Process esRequestForJiraFilter
        must.addAll(esRequestForJiraFilter.getMust());
        mustNot.addAll(esRequestForJiraFilter.getMustNot());
        if (CollectionUtils.isNotEmpty(esRequestForJiraFilter.getShould())) {
            should.addAll(esRequestForJiraFilter.getShould());
        }

        //Process mustForMatchingCategories
        if (mustForMatchingCategories != null) {
            must.addAll(mustForMatchingCategories.getMust());
        }
        //Process mustNotForNonMatchingCategories
        mustNotForNonMatchingCategories.stream().forEach(r -> mustNot.addAll(r.getMust()));

        ESRequest.ESRequestBuilder bldr = ESRequest.builder();
        if(CollectionUtils.isNotEmpty(must)) {
            bldr.must(must);
        }
        if(CollectionUtils.isNotEmpty(mustNot)) {
            bldr.mustNot(mustNot);
        }
        if(CollectionUtils.isNotEmpty(should)) {
            bldr.should(should);
        }

        return bldr.build();
    }
    //endregion

    public SearchRequestAndConverter buildIssuesFTEQuery(String company, Calculation calculation, JiraIssuesFilter filter, OUConfiguration ouConfig, BaJiraOptions baJiraOptions, ESAggInterval esAggInterval,
                                                         List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters, String categoryName, List<DbJiraField> dbJiraFields, boolean useComposteAgg, Map<String, String> afterKey)  {

        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, filter.getIngestedAt());

        //Build the aggregation
        Map<String, Aggregation> aggs = (useComposteAgg) ? buildCompositeAgg(calculation, esAggInterval, baJiraOptions, afterKey)
                : buildTermAgg(calculation, esAggInterval, baJiraOptions);

        //Build the query
        ESRequest esRequest = buildQuery(filter, ticketCategorizationFilters, categoryName, dbJiraFields);

        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(indexNameOrAlias)
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(esRequest.getMust())
                                .mustNot(ListUtils.emptyIfNull(esRequest.getMustNot()))))))
                .aggregations(aggs);

        SearchRequest sr = builder.build();

        try {
            String queryString = ESAggResultUtils.getQueryString(sr);
            log.info("Index name {} and ES Query 2 : {} ", indexNameOrAlias, queryString);
        } catch (IOException e) {
            log.error("Error serializing query string!", e);
        }

        BaJiraOptions.AttributionMode attributionMode = baJiraOptions.getAttributionMode();
        BAESResultConverter converter = (useComposteAgg) ? baCompositeESResultConverterFactory.getConverter(attributionMode, calculation, esAggInterval, categoryName)
                : baTermsESResultConverterFactory.getConverter(attributionMode, calculation, esAggInterval, categoryName);

        return SearchRequestAndConverter.builder()
                .searchRequest(sr)
                .converter(converter)
                .build();
    }

    public SearchRequest buildIssuesListQuery(String company, JiraIssuesFilter filter, OUConfiguration ouConfig, Integer pageNumber, Integer pageSize,
                                             List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters, String categoryName, List<DbJiraField> dbJiraFields) {
        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, filter.getIngestedAt());
        Integer sanitizedPageSize = ObjectUtils.firstNonNull(pageSize, Integer.MAX_VALUE);

        //Build the query
        ESRequest esRequest = buildQuery(filter, ticketCategorizationFilters, categoryName, dbJiraFields);

        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(indexNameOrAlias)
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(esRequest.getMust())
                                .mustNot(ListUtils.emptyIfNull(esRequest.getMustNot()))))))
                .sort(List.of(
                        SortOptions.of(s -> s.field(f -> f.field("w_created_at").order(SortOrder.Desc))),
                        SortOptions.of(s -> s.field(f -> f.field("w_workitem_id").order(SortOrder.Asc)))
                ))
                .size(sanitizedPageSize);

        SearchRequest sr = builder.build();

        try {
            String queryString = ESAggResultUtils.getQueryString(sr);
            log.info("Index name {} and ES Query 2 : {} ", indexNameOrAlias, queryString);
        } catch (IOException e) {
            log.error("Error serializing query string!", e);
        }
        return sr;
    }
}
