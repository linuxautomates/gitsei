package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.MedianAbsoluteDeviationAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmAggService.ISSUES_PARTIAL_MATCH_COLUMNS;
import static io.levelops.faceted_search.utils.EsUtils.getQuery;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTime;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTimeinMills;
import static io.levelops.faceted_search.utils.EsUtils.getRegex;
import static io.levelops.faceted_search.utils.EsUtils.getTimeRangeInMillis;

public class EsScmIssuesQueryBuilder {

    public static final String INCLUDE_CONDITIONS = "Include_Conditions";
    public static final String EXCLUDE_CONDITIONS = "Exclude_Conditions";
    public static final int MAX_PAGE_SIZE = 1000;

    private static final String ISSUE_PREFIX = "i_";

    public static Map<String, List<Query>> buildQueryConditionsForIssues(ScmIssueFilter filter) {
        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();

        createIssuesFilterIncludesCondition(filter, includesQueryConditions);
        createIssuesFilterExcludesCondition(filter, excludesQueryConditions);
        createIssuesFilterRangeCondition(filter, includesQueryConditions);

        return Map.of(INCLUDE_CONDITIONS, includesQueryConditions,
                EXCLUDE_CONDITIONS, excludesQueryConditions);
    }

    public static Map<String, Aggregation> buildAggsConditions(ScmIssueFilter filter, ScmIssueFilter.CALCULATION calculation, int pageNumber, int pageSize) {

        if(calculation == null){
            calculation = ScmIssueFilter.CALCULATION.count;
        }

        Map<String, Aggregation> aggCalcConditions = getCalculationConditions(calculation);
        return getAcrossConditions(filter.getAcross(), filter, aggCalcConditions, pageNumber, pageSize);
    }

    private static Map<String, Aggregation> getAcrossConditions(ScmIssueFilter.DISTINCT across, ScmIssueFilter filter, Map<String, Aggregation> stackAggregation, Integer pageNumber, Integer pageSize) {

        if (across == null) {
            return Map.of();
        }
        if (pageNumber != null) {
            stackAggregation.put("bucket_pagination", BucketSortAggregation.of(a -> a
                    .from(pageNumber * pageSize)
                    .size(pageSize))._toAggregation());
        }
        Map<String, Aggregation> aggConditions = new HashMap<>();
        int pageLength = pageSize == null ? MAX_PAGE_SIZE : pageSize;
        switch (across) {
            case repo_id:
            case project:
            case state:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(ISSUE_PREFIX+across.name()).size(pageLength))
                        .aggregations(stackAggregation)
                ));
                break;

            case label:
            case assignee:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(ISSUE_PREFIX+across.name()+"s").size(pageLength))
                        .aggregations(stackAggregation)
                ));
                break;

            case creator:
                String scriptString = "def id = doc['" + ISSUE_PREFIX+across.name() + "_id'].value; def name = doc['" + ISSUE_PREFIX+across.name() + "'].value; return id+'#'+name";
                Script moduleScript = Script.of(s -> s.inline(i -> i
                        .source(scriptString)));
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.script(moduleScript).size(pageLength))
                        .aggregations(stackAggregation)
                ));

                break;

            case issue_closed:
            case issue_created:
            case issue_updated:
            case first_comment:
                aggConditions = getTimeBasedAcross(across, filter, stackAggregation);
                break;

            default:
                Validate.notNull(null, "Invalid across field provided.");
        }

        return aggConditions;
    }

    private static Map<String, Aggregation> getTimeBasedAcross(ScmIssueFilter.DISTINCT across, ScmIssueFilter filter, Map<String, Aggregation> stackAggregation) {

        Map<String, Aggregation> aggConditions = new HashMap<>();
        final CalendarInterval finalCalendarInterval = getCalenderInterval(filter);

        switch (across) {
            case issue_created:
            case issue_updated:
            case issue_closed:
            case first_comment:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> dh
                                .field(ISSUE_PREFIX+across + "_at")
                                .timeZone("UTC")
                                .calendarInterval(finalCalendarInterval)
                                .minDocCount(1))
                        .aggregations(stackAggregation)
                ));
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time based across provided " + across);
        }
        return aggConditions;
    }

    private static CalendarInterval getCalenderInterval(ScmIssueFilter filter) {

        String interval = filter.getAggInterval() != null ? filter.getAggInterval().name() : AGG_INTERVAL.month.name();
        CalendarInterval calendarInterval;

        switch (interval.toLowerCase()) {
            case "year":
                calendarInterval = CalendarInterval.Year;
                break;
            case "quarter":
                calendarInterval = CalendarInterval.Quarter;
                break;
            case "month":
                calendarInterval = CalendarInterval.Month;
                break;
            case "day":
                calendarInterval = CalendarInterval.Day;
                break;
            case "week":
                calendarInterval = CalendarInterval.Week;
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided " + interval);
        }

        return calendarInterval;
    }


    private static Map<String, Aggregation> getCalculationConditions(ScmIssueFilter.CALCULATION calculation) {

        Map<String, Aggregation> aggConditions = new HashMap<>();

        switch (calculation) {
            case response_time:
            case resolution_time:
                String column = "i_response_time";
                if (ScmIssueFilter.CALCULATION.resolution_time.equals(calculation)) {
                    column = "i_solve_time";
                }

                StatsAggregation statsAggregation = AggregationBuilders.stats().field(column).build();
                MedianAbsoluteDeviationAggregation medianResponseTime = AggregationBuilders.medianAbsoluteDeviation().field(column).build();
                aggConditions.put("stats", Aggregation.of(a -> a.stats(statsAggregation)));
                aggConditions.put("median", Aggregation.of(a -> a.medianAbsoluteDeviation(medianResponseTime)));
                break;
        }

        return aggConditions;
    }

    private static void createIssuesFilterIncludesCondition(ScmIssueFilter filter, List<Query> includesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            includesQueryConditions.add(getQuery("i_repo_id", filter.getRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            includesQueryConditions.add(getQuery("i_project", filter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCreators())) {
            includesQueryConditions.add(getQuery("i_creator_id", filter.getCreators()));
        }
        if (CollectionUtils.isNotEmpty(filter.getAssignees())) {
            includesQueryConditions.add(getQuery("i_assignees", filter.getAssignees()));
        }
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            includesQueryConditions.add(getQuery("i_state", filter.getStates()));
        }
        if (CollectionUtils.isNotEmpty(filter.getLabels())) {
            includesQueryConditions.add(getQuery("i_labels", filter.getLabels()));
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            includesQueryConditions.add(getQuery("i_integration_id", filter.getIntegrationIds()));
        }
        if (StringUtils.isNotEmpty(filter.getTitle())) {
            includesQueryConditions.add(getQuery("i_title", List.of(filter.getTitle())));
        }
        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            getPartialMatch(filter.getPartialMatch(), includesQueryConditions);
        }

        long currentTime = (new Date()).toInstant().toEpochMilli();
        for (ScmIssueFilter.EXTRA_CRITERIA criterion : filter.getExtraCriteria()) {
            switch (criterion) {
                case no_assignees:
                    includesQueryConditions.add(Query.of(q -> q.exists(e -> e.field("i_assignees"))));
                    break;
                case idle:
                    long idle7days = LocalDateTime.ofEpochSecond(
                            (currentTime - 7 * 86400), 0, ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli();
                    includesQueryConditions.add(getRangeQueryForTime("i_issue_updated_at", ImmutablePair.of(null, idle7days)));
                    break;
                case no_labels:
                    includesQueryConditions.add(Query.of(q -> q.exists(e -> e.field("i_labels"))));
                    break;
                case no_response:
                    includesQueryConditions.add(Query.of(q -> q.exists(e -> e.field("i_first_comment_at"))));
                    break;
                case missed_response_time:
                    includesQueryConditions.add(getRangeQueryForTime("i_response_time", ImmutablePair.of(null, 86400000l)));
                    break;
            }
        }
    }

    private static void createIssuesFilterExcludesCondition(ScmIssueFilter filter, List<Query> excludesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getExcludeRepoIds())) {
            excludesQueryConditions.add(getQuery("i_repo_id", filter.getExcludeRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            excludesQueryConditions.add(getQuery("i_project", filter.getExcludeProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCreators())) {
            excludesQueryConditions.add(getQuery("i_creator", filter.getCreators()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAssignees())) {
            excludesQueryConditions.add(getQuery("i_assignees", filter.getExcludeAssignees()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeStates())) {
            excludesQueryConditions.add(getQuery("i_state", filter.getExcludeStates()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeLabels())) {
            excludesQueryConditions.add(getQuery("i_label", filter.getExcludeLabels()));
        }
    }

    private static void createIssuesFilterRangeCondition(ScmIssueFilter filter, List<Query> includesQueryConditions) {

        if (filter.getIssueCreatedRange() != null && (filter.getIssueCreatedRange().getLeft() != null || filter.getIssueCreatedRange().getRight() != null )) {
            ImmutablePair range = getTimeRangeInMillis(filter.getIssueCreatedRange().getLeft(), filter.getIssueCreatedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("i_issue_created_at", range));
        }
        if (filter.getIssueClosedRange() != null && (filter.getIssueClosedRange().getLeft() != null || filter.getIssueClosedRange().getRight() != null )) {
            ImmutablePair range = getTimeRangeInMillis(filter.getIssueClosedRange().getLeft(), filter.getIssueClosedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("i_issue_closed_at", range));
        }
        if (filter.getIssueUpdatedRange() != null && (filter.getIssueUpdatedRange().getLeft() != null || filter.getIssueUpdatedRange().getRight() != null )) {
            ImmutablePair range = getTimeRangeInMillis(filter.getIssueUpdatedRange().getLeft(), filter.getIssueUpdatedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("i_issue_updated_at", range));
        }
        if (filter.getFirstCommentAtRange() != null && (filter.getFirstCommentAtRange().getLeft() != null || filter.getFirstCommentAtRange().getRight() != null )) {
            ImmutablePair range = getTimeRangeInMillis(filter.getFirstCommentAtRange().getLeft(), filter.getFirstCommentAtRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("i_first_comment_at", range));
        }
    }

    private static void getPartialMatch(Map<String, Map<String, String>> partialMatch, List<Query> queries) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (ISSUES_PARTIAL_MATCH_COLUMNS.contains(key)) {
                switch (key) {
                    case "title":
                        key = "i_title";
                        break;
                    case "issue_id":
                        key = "i_issue_id";
                        break;
                    case "creator":
                        key = "i_creator";
                        break;
                    case "state":
                        key = "i_state";
                        break;
                    case "project":
                        key = "i_project";
                        break;
                }
                getRegex(begins, ends, contains, key, queries);
            }
        }
    }
}