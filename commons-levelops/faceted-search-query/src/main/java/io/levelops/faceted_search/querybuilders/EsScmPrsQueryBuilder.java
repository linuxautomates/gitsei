package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.AverageAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MedianAbsoluteDeviationAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.ScmPrSorting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.services.ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS;
import static io.levelops.commons.databases.services.ScmAggService.PRS_PARTIAL_MATCH_COLUMNS;
import static io.levelops.commons.databases.services.ScmQueryUtils.getScmSortOrder;
import static io.levelops.faceted_search.utils.EsUtils.createExistsQueryForMissingField;
import static io.levelops.faceted_search.utils.EsUtils.getNestedRangeTimeQuery;
import static io.levelops.faceted_search.utils.EsUtils.getQuery;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTimeinMills;
import static io.levelops.faceted_search.utils.EsUtils.getRegex;
import static io.levelops.faceted_search.utils.EsUtils.getTimeRangeInMillis;
import static java.util.stream.Collectors.toList;

public class EsScmPrsQueryBuilder {
    private static final int MAX_PAGE_SIZE = 1000;
    public static final String INCLUDE_CONDITIONS = "Include_Conditions";
    public static final String EXCLUDE_CONDITIONS = "Exclude_Conditions";
    public static final String PR_PREFIX = "pr_";

    public static final String PR_COMMENT_COUNT="pr_comment_count";

    public static Map<String, List<Query>> buildQueryConditionsForPrs(ScmPrFilter filter) {

        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();

        createPrsFilterIncludesCondition(filter, includesQueryConditions);
        createPrsFilterExcludesCondition(filter, excludesQueryConditions);
        createPrsFilterRangeCondition(filter, includesQueryConditions);

        if (MapUtils.isNotEmpty(filter.getMissingFields())) {
            filter.getMissingFields().forEach((field, shouldBeMissing) -> Optional.ofNullable(ScmPrFilter.MISSING_BUILTIN_FIELD.fromString(field))
                    .ifPresent(builtinField -> createExistsQueryForMissingField(field, shouldBeMissing, excludesQueryConditions, includesQueryConditions)));
        }

        return Map.of(INCLUDE_CONDITIONS, includesQueryConditions,
                EXCLUDE_CONDITIONS, excludesQueryConditions);
    }

    public static Map<String, Aggregation> buildAggsConditions(ScmPrFilter filter, Integer page, Integer pageSize) {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        ScmPrFilter.DISTINCT across = filter.getAcross();
        return getAcrossConditions(across, filter, new HashMap<>(), page, pageSize);
    }

    public static Map<String, Aggregation> buildAggsConditions(ScmPrFilter filter, ScmPrFilter.CALCULATION calculation, Integer page, Integer pageSize) {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");

        ScmPrFilter.DISTINCT across = filter.getAcross();

        Map<String, Aggregation> aggCalcConditions = getCalculationConditions(calculation, filter);
        return getAcrossConditions(across, filter, aggCalcConditions, page, pageSize);
    }

    public static Map<String, Aggregation> buildAggsConditionsFoCollab(ScmPrFilter filter, boolean stackValue) {

        Map<String, Aggregation> aggConditions = new HashMap<>();

        if (stackValue) {

            aggConditions.put("across_approvers", Aggregation.of(a -> a.terms(t -> t.field("pr_approver_ids").size(MAX_PAGE_SIZE))
            ));

        } else {
            String scriptString = "if(doc['pr_collab_state'].size() > 0) { def id = doc['pr_creator_id'].value; def name = doc['pr_creator'].value; def collab_state = doc['pr_collab_state'].value; return id+'#'+name+'#'+collab_state; } return ''; ";
            Script moduleScript = Script.of(s -> s.inline(i -> i
                    .source(scriptString)));
            aggConditions.put("across_creators", Aggregation.of(a -> a.terms(t -> t.script(moduleScript).size(MAX_PAGE_SIZE))
            ));
        }

        return aggConditions;
    }

    private static Map<String, Aggregation> getAcrossConditions(ScmPrFilter.DISTINCT across,
                                                                ScmPrFilter filter,
                                                                Map<String, Aggregation> stackAggregation,
                                                                Integer page, Integer pageSize) {
        if (across == null) {
            return Map.of();
        }
        String sortByKey = getPrsCountSortByKey(filter.getSort(), filter.getAcross().toString());
        SortingOrder sortingOrder = getScmSortOrder(filter.getSort());
        Pair<String, SortOrder> sortOrder = getSortConfig(Map.of(sortByKey, sortingOrder), "_key", ScmPrSorting.PR_SORTABLE);
        boolean termSorting = false;
        if (page != null) {
            Aggregation aggregation = BucketSortAggregation.of(a -> a
                    .from(page * pageSize)
                    .size(pageSize))._toAggregation();
            if (stackAggregation.containsKey(sortOrder.getKey())) {
                //Do Bucket sorting
                aggregation = BucketSortAggregation.of(a -> a
                        .from(page * pageSize)
                        .sort(List.of(SortOptions.of(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))))
                        .size(pageSize))._toAggregation();
            } else if (!MapUtils.isEmpty(filter.getSort())) {
                //Do Term sorting
                termSorting = true;
            }
            stackAggregation.put("bucket_pagination", aggregation);
        }
        Map<String, Aggregation> aggConditions = new HashMap<>();
        boolean usePlural = false;
        int pageLength = pageSize == null ? MAX_PAGE_SIZE : pageSize;
        switch (across) {

            case label:
            case project:
            case repo_id:
            case review_type:
            case collab_state:
            case branch:
            case source_branch:
            case target_branch:
            case state:
            case approver_count:
            case reviewer_count:
            case approval_status:
            case technology:
                Aggregation agg;
                if (termSorting) {
                    agg = Aggregation.of(a -> a.terms(t -> t.field(PR_PREFIX + across.name()).order(List.of(Map.of("_key", sortOrder.getRight()))).size(pageLength)).aggregations(stackAggregation));
                } else {
                    agg = Aggregation.of(a -> a.terms(t -> t.field(PR_PREFIX + across.name()).size(pageLength)).aggregations(stackAggregation));
                }
                aggConditions.put("across_" + across.name(), agg);
                break;
            case reviewer:
            case approver:
            case assignee:
                usePlural = true;
            case creator:
                boolean checkPlural = usePlural;
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(checkPlural ? PR_PREFIX + across.name() + "_ids" : PR_PREFIX + across.name() + "_id").size(pageLength))
                        .aggregations(stackAggregation)
                ));

                break;

            case code_change:
                int low = (filter.getCodeChangeSizeConfig() != null && filter.getCodeChangeSizeConfig().containsKey("small")) ? Integer.parseInt(filter.getCodeChangeSizeConfig().get("small")) : 100;
                int medium = (filter.getCodeChangeSizeConfig() != null && filter.getCodeChangeSizeConfig().containsKey("medium")) ? Integer.parseInt(filter.getCodeChangeSizeConfig().get("medium")) : 1000;

                String codeChangeUnit = "pr_changes";
                if("files".equals(filter.getCodeChangeUnit())){
                    codeChangeUnit = "pr_files_count";
                }
                String scriptStr = " if(doc['"+codeChangeUnit+"'].size() > 0) { def changes = doc['"+codeChangeUnit+"'].value; if (changes <=" + low + ") {return 'small';} if( changes > " + low + " && changes <=" + medium + ") {return 'medium';} return 'large';} return 'small';";
                Script script = Script.of(s -> s.inline(i -> i
                        .source(scriptStr)));

                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.script(script))
                        .aggregations(stackAggregation)
                ));

                break;

            case comment_density:
                int shallow = (filter.getCommentDensitySizeConfig() != null && filter.getCommentDensitySizeConfig().containsKey("shallow")) ? Integer.parseInt(filter.getCommentDensitySizeConfig().get("shallow")) : 100;
                int good = (filter.getCommentDensitySizeConfig() != null && filter.getCommentDensitySizeConfig().containsKey("good")) ? Integer.parseInt(filter.getCommentDensitySizeConfig().get("good")) : 1000;

                String densityScript = " if(doc['pr_comment_count'].size() > 0) { def changes = doc['pr_comment_count'].value; if (changes <=" + shallow + ") {return 'shallow';} if( changes > " + shallow + " && changes <=" + good + ") {return 'good';} return 'heavy';} return '';";
                Script density = Script.of(s -> s.inline(i -> i
                        .source(densityScript)));

                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.script(density))
                        .aggregations(stackAggregation)
                ));

                break;

            case pr_merged:
            case pr_closed:
            case pr_reviewed:
            case pr_created:
            case pr_updated:
                aggConditions = getTimeBasedAcross(across, filter, stackAggregation);
                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid across provided " + across);
        }
        return aggConditions;
    }

    private static Map<String, Aggregation> getTimeBasedAcross(ScmPrFilter.DISTINCT across, ScmPrFilter filter, Map<String, Aggregation> stackAggregation) {

        Map<String, Aggregation> aggConditions = new HashMap<>();
        final CalendarInterval finalCalendarInterval = getCalenderInterval(filter);

        switch (across) {
            case pr_merged:
            case pr_closed:
            case pr_reviewed:
            case pr_created:
            case pr_updated:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> dh
                                .field(PR_PREFIX+across + "_at")
                                .timeZone("UTC")
                                .calendarInterval(finalCalendarInterval)
                                //.offset(finalCalendarInterval.equals(CalendarInterval.Week) ? Time.of(t -> t.time("-1d")): null) // Removing offset for weekly PRs to resolve PROP-1085 - Will update it once FE fix is deployed
                                .minDocCount(1))
                        .aggregations(stackAggregation)
                ));
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time based across provided " + across);
        }
        return aggConditions;
    }

    private static CalendarInterval getCalenderInterval(ScmPrFilter filter) {

        String interval = filter.getAggInterval() != null ? filter.getAggInterval().name() : AGG_INTERVAL.month.name();

        switch (interval.toLowerCase()) {
            case "year":
                return CalendarInterval.Year;
            case "quarter":
                return CalendarInterval.Quarter;
            case "month":
                return CalendarInterval.Month;
            case "day_of_week":
            case "day":
                return CalendarInterval.Day;
            case "week":
                return CalendarInterval.Week;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided " + interval);
        }

    }

    private static Map<String, Aggregation> getCalculationConditions(ScmPrFilter.CALCULATION calculation, ScmPrFilter filter) {

        Map<String, Aggregation> stackAggs = new HashMap<>();

        switch (calculation) {

            case reviewer_approve_time:
            case reviewer_comment_time:
            case merge_time:
            case reviewer_response_time:
            case author_response_time:
                String column = "pr_avg_author_response_time";
                if (ScmPrFilter.CALCULATION.reviewer_response_time.equals(calculation)) {
                    column = "pr_avg_reviewer_response_time";
                }
                if (ScmPrFilter.CALCULATION.merge_time.equals(calculation)) {
                    column = "pr_cycle_time";
                }
                if (ScmPrFilter.CALCULATION.reviewer_approve_time.equals(calculation)) {
                    column = "pr_approval_time";
                }
                if (ScmPrFilter.CALCULATION.reviewer_comment_time.equals(calculation)) {
                    column = "pr_comment_time";
                }

                StatsAggregation statsAggregation = AggregationBuilders.stats().field(column).missing(0L).build();
                MedianAbsoluteDeviationAggregation medianResponseTime = AggregationBuilders.medianAbsoluteDeviation().field(column).build();
                AverageAggregation mean = AggregationBuilders.avg().field(column).build();
                stackAggs.put("stats", Aggregation.of(a -> a.stats(statsAggregation)));
                stackAggs.put("median", Aggregation.of(a -> a.medianAbsoluteDeviation(medianResponseTime)));
                stackAggs.put("pr_" + ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME, Aggregation.of(a -> a.avg(mean)));
                stackAggs.put("pr_" + ScmPrSorting.MEDIAN_AUTHOR_RESPONSE_TIME, Aggregation.of(a -> a.medianAbsoluteDeviation(medianResponseTime)));
                break;

            case count:

                String unit = filter.getCodeChangeUnit();
                String codeUnitColumn = "pr_files_count";
                if ("lines".equals(unit)) {
                    codeUnitColumn = "pr_files_count";
                }
                CardinalityAggregation count = AggregationBuilders.cardinality().field("id").build();

                SumAggregation addedCount = AggregationBuilders.sum().field("pr_additions").build();
                SumAggregation deletedCount = AggregationBuilders.sum().field("pr_deletions").build();
                SumAggregation changedCount = AggregationBuilders.sum().field("pr_changes").build();
                SumAggregation filesCount = AggregationBuilders.sum().field(codeUnitColumn).build();
                SumAggregation commentCount = AggregationBuilders.sum().field("pr_comment_count").build();

                AverageAggregation avgLineChanged = AggregationBuilders.avg().field("pr_changes").build();
                MedianAbsoluteDeviationAggregation medianLineChanged = AggregationBuilders.medianAbsoluteDeviation().field("pr_changes").build();

                AverageAggregation avgFilesChanged = AggregationBuilders.avg().field("pr_files_count").build();
                MedianAbsoluteDeviationAggregation medianFilesChanged = AggregationBuilders.medianAbsoluteDeviation().field("pr_files_count").build();

                stackAggs.put("count", Aggregation.of(a -> a.cardinality(count)));
                stackAggs.put("addition_ct", Aggregation.of(a -> a.sum(addedCount)));
                stackAggs.put("deletion_ct", Aggregation.of(a -> a.sum(deletedCount)));
                stackAggs.put("changes_ct", Aggregation.of(a -> a.sum(changedCount)));
                stackAggs.put("files_ct", Aggregation.of(a -> a.sum(filesCount)));
                stackAggs.put("comment_ct", Aggregation.of(a -> a.sum(commentCount)));

                stackAggs.put("avg_line_change", Aggregation.of(a -> a.avg(avgLineChanged)));
                stackAggs.put("avg_file_change", Aggregation.of(a -> a.avg(avgFilesChanged)));

                stackAggs.put("median_line", Aggregation.of(a -> a.medianAbsoluteDeviation(medianLineChanged)));
                stackAggs.put("median_file", Aggregation.of(a -> a.medianAbsoluteDeviation(medianFilesChanged)));

                break;
        }

        return stackAggs;
    }

    public static Map<String, Aggregation> buildAggsConditionsForTrend(ScmPrFilter filter, ScmPrFilter.CALCULATION calculation) throws IOException {

        ScmPrFilter.DISTINCT across = filter.getAcross();

        Map<String, Aggregation> aggCalcConditions = getCalculationConditionsForTrend(calculation, filter);
        return getTimeBasedAcross(across, filter, aggCalcConditions);
    }

    private static Map<String, Aggregation> getCalculationConditionsForTrend(ScmPrFilter.CALCULATION calculation, ScmPrFilter filter) throws IOException {

        Map<String, Aggregation> stackAggs = new HashMap<>();
        switch (calculation) {
            case merge_time:
            case first_review_to_merge_time:
            case first_review_time:
                String column = "pr_cycle_time";
                if (ScmPrFilter.CALCULATION.first_review_time.equals(calculation)) {
                    column = "pr_review_cycle_time";
                }
                if (ScmPrFilter.CALCULATION.first_review_to_merge_time.equals(calculation)) {
                    column = "pr_review_merge_cycle_time";
                }

                StatsAggregation statsAggregation = AggregationBuilders.stats().field(column).missing(0L).build();
                MedianAbsoluteDeviationAggregation medianResponseTime = AggregationBuilders.medianAbsoluteDeviation().field(column).build();
                stackAggs.put("stats", Aggregation.of(a -> a.stats(statsAggregation)));
                stackAggs.put("median", Aggregation.of(a -> a.medianAbsoluteDeviation(medianResponseTime)));

                break;
            default:
                throw new IOException("Invalid calculation field provided for this agg: " + calculation);
        }

        return stackAggs;
    }

    private static void getPartialMatch(Map<String, Map<String, String>> partialMatch, List<Query> includesQueryConditions) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");
            String regex = value.get("$regex");

            if (begins != null || ends != null || contains != null || regex != null) {
                if (PRS_PARTIAL_MATCH_COLUMNS.contains(key) || PRS_PARTIAL_MATCH_ARRAY_COLUMNS.contains(key)) {
                    switch (key) {
                        case "title":
                            key = "pr_title";
                            break;
                        case "state":
                            key = "pr_state";
                            break;
                        case "creator":
                            key = "pr_creator";
                            break;
                        case "source_branch":
                            key = "pr_source_branch";
                            break;
                        case "project":
                            key = "pr_project";
                            break;
                        case "target_branch":
                            key = "pr_target_branch";
                            break;
                        case "assignees":
                            key = "pr_assignees";
                            break;
                        case "labels":
                            key = "pr_label";
                            break;
                        case "repo_id":
                            key = "pr_repo_id";
                            break;
                        case "reviewer":
                            key = "pr_reviewer";
                            break;
                        case "aprrover":
                            key = "pr_approver";
                            break;
                    }
                    getRegex(begins, ends, contains, key, includesQueryConditions);
                }
            }
        }
    }

    private static void createPrsFilterIncludesCondition(ScmPrFilter filter, List<Query> includesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            includesQueryConditions.add(getQuery("pr_id", filter.getIds().stream().map(UUID::toString).collect(toList())));
        }
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            includesQueryConditions.add(getQuery("pr_repo_id", filter.getRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            includesQueryConditions.add(getQuery("pr_project", filter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCommenters())) {
            includesQueryConditions.add(getQuery("pr_commenter_ids", filter.getCommenters()));
        }
        if (CollectionUtils.isNotEmpty(filter.getSourceBranches())) {
            includesQueryConditions.add(getQuery("pr_source_branch", filter.getSourceBranches()));
        }
        if (CollectionUtils.isNotEmpty(filter.getTargetBranches())) {
            includesQueryConditions.add(getQuery("pr_target_branch", filter.getTargetBranches()));
        }
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            includesQueryConditions.add(getQuery("pr_state", filter.getStates()));
        }
        if (CollectionUtils.isNotEmpty(filter.getTitles())) {
            includesQueryConditions.add(getQuery("pr_title", filter.getTitles()));
        }
        if (CollectionUtils.isNotEmpty(filter.getLabels())) {
            includesQueryConditions.add(getQuery("pr_label", filter.getLabels()));
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            includesQueryConditions.add(getQuery("pr_integration_id", filter.getIntegrationIds()));
        }
        if (filter.getReviewerIds() != null) {
            includesQueryConditions.add(getQuery("pr_reviewer_ids", filter.getReviewerIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCollabStates())) {
            includesQueryConditions.add(getQuery("pr_collab_state", filter.getCollabStates()));
        }
        if (CollectionUtils.isNotEmpty(filter.getPrCreatedDaysOfWeek())) {
            includesQueryConditions.add(getQuery("pr_created_day", filter.getPrCreatedDaysOfWeek()));
        }
        if (CollectionUtils.isNotEmpty(filter.getPrMergedDaysOfWeek())) {
            includesQueryConditions.add(getQuery("pr_merged_day", filter.getPrMergedDaysOfWeek()));
        }
        if (CollectionUtils.isNotEmpty(filter.getPrClosedDaysOfWeek())) {
            includesQueryConditions.add(getQuery("pr_closed_day", filter.getPrClosedDaysOfWeek()));
        }
        if (CollectionUtils.isNotEmpty(filter.getReviewTypes())) {
            includesQueryConditions.add(getQuery("pr_review_type", filter.getReviewTypes()));
        }
        if (CollectionUtils.isNotEmpty(filter.getApprovalStatuses())) {
            includesQueryConditions.add(getQuery("pr_approval_status", filter.getApprovalStatuses()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCreators())) { // OU: creators
            includesQueryConditions.add(getQuery("pr_creator_id", filter.getCreators()));
        }
        if (CollectionUtils.isNotEmpty(filter.getAssignees())) { // OU: assignees
            includesQueryConditions.add(getQuery("pr_assignee_ids", filter.getAssignees()));
        }
        if (CollectionUtils.isNotEmpty(filter.getApprovers())) { // OU: approvers
            includesQueryConditions.add(getQuery("pr_approver_ids", filter.getApprovers()));
        }
        if (CollectionUtils.isNotEmpty(filter.getReviewers())) { // OU: reviewers
            includesQueryConditions.add(getQuery("pr_reviewer_ids", filter.getReviewers()));
        }
        if(filter.getCalculation() == ScmPrFilter.CALCULATION.merge_time || filter.getCalculation() == ScmPrFilter.CALCULATION.first_review_to_merge_time ||
            filter.getAcross() == ScmPrFilter.DISTINCT.pr_merged){
            includesQueryConditions.add(Query.of(q -> q.exists(e -> e.field("pr_merged"))));
        }
        if(filter.getCalculation() == ScmPrFilter.CALCULATION.first_review_to_merge_time ||
                filter.getCalculation() == ScmPrFilter.CALCULATION.first_review_time) {
            List<Query> queryList = new ArrayList<>();
            queryList.add(Query.of(q -> q.range(r -> r.field("pr_comment_count").gt(JsonData.of(0l)))));
            queryList.add(Query.of(q -> q.range(r -> r.field("pr_approver_count").gt(JsonData.of(0l)))));
            includesQueryConditions.add(Query.of(q -> q
                    .bool(BoolQuery.of(b -> b
                            .should(queryList)))));
        }
        if (CollectionUtils.isNotEmpty(filter.getCodeChanges())) {
            String codeChangeUnit = "pr_changes";
            if("files".equals(filter.getCodeChangeUnit())){
                codeChangeUnit = "pr_files_count";
            }
            createCodeChangeCondition(filter, includesQueryConditions, codeChangeUnit);
        }
        if (CollectionUtils.isNotEmpty(filter.getCommentDensities())) {
            createCommentDensityCondition(filter, includesQueryConditions);
        }
        if (StringUtils.isNotEmpty(filter.getHasIssueKeys()) && BooleanUtils.toBoolean(filter.getHasIssueKeys())) {
            includesQueryConditions.add(Query.of(q -> q.exists(e -> e.field("pr_workitem_ids"))));
        }

        if (filter.getLocRange() != null) {
            if (filter.getLocRange().getLeft() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("pr_loc")
                        .gt(JsonData.of(filter.getLocRange().getLeft()))
                        .build();

                includesQueryConditions.add(rangeQuery._toQuery());
            }
            if (filter.getLocRange().getRight() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("pr_loc")
                        .lt(JsonData.of(filter.getLocRange().getLeft()))
                        .build();
                includesQueryConditions.add(rangeQuery._toQuery());
            }
        }

        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            getPartialMatch(filter.getPartialMatch(), includesQueryConditions);
        }
    }

    private static void createPrsFilterExcludesCondition(ScmPrFilter filter, List<Query> excludesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getExcludeRepoIds())) {
            excludesQueryConditions.add(getQuery("pr_repo_id", filter.getExcludeRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCollabStates())) {
            excludesQueryConditions.add(getQuery("pr_collab_state", filter.getExcludeCollabStates()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeReviewTypes())) {
            excludesQueryConditions.add(getQuery("pr_review_type", filter.getExcludeReviewTypes()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            excludesQueryConditions.add(getQuery("pr_project", filter.getExcludeProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeApprovers())) {
            excludesQueryConditions.add(getQuery("pr_approver_ids", filter.getExcludeApprovers()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommenters())) {
            excludesQueryConditions.add(getQuery("pr_commenter_ids", filter.getExcludeCommenters()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCreators())) {
            excludesQueryConditions.add(getQuery("pr_creator_id", filter.getExcludeCreators()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeSourceBranches())) {
            excludesQueryConditions.add(getQuery("pr_source_branch", filter.getExcludeSourceBranches()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTargetBranches())) {
            excludesQueryConditions.add(getQuery("pr_target_branch", filter.getExcludeTargetBranches()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeStates())) {
            excludesQueryConditions.add(getQuery("pr_state", filter.getExcludeStates()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTitles())) {
            excludesQueryConditions.add(getQuery("pr_title", filter.getExcludeTitles()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAssignees())) {
            excludesQueryConditions.add(getQuery("pr_assignee_ids", filter.getExcludeAssignees()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeLabels())) {
            excludesQueryConditions.add(getQuery("pr_label", filter.getExcludeLabels()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeApprovalStatuses())) {
            excludesQueryConditions.add(getQuery("pr_approval_status", filter.getExcludeApprovalStatuses()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeReviewers())) {
            excludesQueryConditions.add(getQuery("pr_reviewer_ids", filter.getExcludeReviewers()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCodeChanges())) {
            String codeChangeUnit = "pr_changes";
            if("files".equals(filter.getCodeChangeUnit())){
                codeChangeUnit = "pr_files_count";
            }
            createCodeChangeCondition(filter, excludesQueryConditions, codeChangeUnit);
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommentDensities())) {
            createCommentDensityCondition(filter, excludesQueryConditions);
        }
        if (filter.getExcludeLocRange() != null) {
            if (filter.getExcludeLocRange().getLeft() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("pr_loc")
                        .gt(JsonData.of(filter.getExcludeLocRange().getLeft()))
                        .build();

                excludesQueryConditions.add(rangeQuery._toQuery());
            }
            if (filter.getExcludeLocRange().getRight() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("pr_loc")
                        .lt(JsonData.of(filter.getExcludeLocRange().getLeft()))
                        .build();
                excludesQueryConditions.add(rangeQuery._toQuery());
            }
        }
        if (StringUtils.isNotEmpty(filter.getHasIssueKeys()) && !BooleanUtils.toBoolean(filter.getHasIssueKeys())) {
            excludesQueryConditions.add(Query.of(q -> q.exists(e -> e.field("pr_workitem_ids"))));
        }
        if (MapUtils.isNotEmpty(filter.getExcludePartialMatch())) {
            getPartialMatch(filter.getExcludePartialMatch(), excludesQueryConditions);
        }
    }

    private static void createPrsFilterRangeCondition(ScmPrFilter filter, List<Query> includesQueryConditions) {

        if (filter.getPrCreatedRange() != null && (filter.getPrCreatedRange().getLeft() != null || filter.getPrCreatedRange().getRight() != null)) {
            ImmutablePair<Long, Long> range = getTimeRangeInMillis(filter.getPrCreatedRange().getLeft(), filter.getPrCreatedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("pr_pr_created_at", range));
        }
        if (filter.getPrClosedRange() != null && (filter.getPrClosedRange().getLeft() != null && filter.getPrClosedRange().getRight() != null)) {
            ImmutablePair<Long, Long> range = getTimeRangeInMillis(filter.getPrClosedRange().getLeft(), filter.getPrClosedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("pr_pr_closed_at", range));
        }
        if (filter.getPrUpdatedRange() != null && (filter.getPrUpdatedRange().getLeft() != null && filter.getPrUpdatedRange().getRight() != null)) {
            ImmutablePair<Long, Long> range = getTimeRangeInMillis(filter.getPrUpdatedRange().getLeft(), filter.getPrUpdatedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("pr_pr_updated_at", range));
        }
        if (filter.getPrMergedRange() != null && (filter.getPrMergedRange().getLeft() != null || filter.getPrMergedRange().getRight() != null)) {
            ImmutablePair<Long, Long> range = getTimeRangeInMillis(filter.getPrMergedRange().getLeft(), filter.getPrMergedRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("pr_pr_merged_at", range));
        }
        if (filter.getPrReviewedRange() != null && (filter.getPrReviewedRange().getLeft() != null || filter.getPrReviewedRange().getRight() != null)) {
            ImmutablePair<Long, Long> range = getTimeRangeInMillis(filter.getPrReviewedRange().getLeft(), filter.getPrReviewedRange().getRight());
            includesQueryConditions.add(getNestedRangeTimeQuery("pr_approvers_info", "reviewed_at", range));
        }

        if (filter.getApproverCount() != null && (filter.getApproverCount().getLeft() != null || filter.getApproverCount().getRight() != null)) {

            RangeQuery.Builder rangeQueryBuilder =  new RangeQuery.Builder().field("pr_approver_count");
            if (filter.getApproverCount().getLeft() != null) {
                rangeQueryBuilder.gt(JsonData.of(filter.getApproverCount().getLeft()));
            }
            if (filter.getApproverCount().getRight() != null) {
                rangeQueryBuilder.lt(JsonData.of(filter.getApproverCount().getRight()));
            }
            includesQueryConditions.add(rangeQueryBuilder.build()._toQuery());
        }

        if (filter.getReviewerCount() != null && (filter.getReviewerCount().getLeft() != null || filter.getReviewerCount().getRight() != null)) {
            RangeQuery.Builder rangeQueryBuilder =  new RangeQuery.Builder().field("pr_reviewer_count");
            if (filter.getReviewerCount().getLeft() != null) {
                rangeQueryBuilder.gt(JsonData.of(filter.getReviewerCount().getLeft()));
            }
            if (filter.getReviewerCount().getRight() != null) {
                rangeQueryBuilder.lt(JsonData.of(filter.getReviewerCount().getRight()));
            }
            includesQueryConditions.add(rangeQueryBuilder.build()._toQuery());
        }
    }

    private static void createCodeChangeCondition(ScmPrFilter filter, List<Query> includesQueryConditions, String codeChangeUnit) {

        if (CollectionUtils.isNotEmpty(filter.getCodeChanges()) || CollectionUtils.isNotEmpty(filter.getExcludeCodeChanges())) {

            int low = (filter.getCodeChangeSizeConfig() != null && filter.getCodeChangeSizeConfig().containsKey("small")) ? Integer.parseInt(filter.getCodeChangeSizeConfig().get("small")) : 100;
            int medium = (filter.getCodeChangeSizeConfig() != null && filter.getCodeChangeSizeConfig().containsKey("medium")) ? Integer.parseInt(filter.getCodeChangeSizeConfig().get("medium")) : 1000;

            List<String> codeChangeList = null;
            if(CollectionUtils.isNotEmpty(filter.getCodeChanges())) {
                codeChangeList = filter.getCodeChanges();
            }else{
                codeChangeList = filter.getExcludeCodeChanges();
            }

            List<Query> queryList = new ArrayList<>();

            if (codeChangeList.contains("small")) {
                Query rangeQuery =  new RangeQuery.Builder().field(codeChangeUnit)
                        .lte(JsonData.of(low))
                        .build()._toQuery();
                queryList.add(rangeQuery);
                queryList.add(Query.of(q -> q.bool( b-> b.mustNot( e1 -> e1.exists(e -> e.field(codeChangeUnit))))));
            }

            if (codeChangeList.contains("medium")) {
                Query rangeQuery =  new RangeQuery.Builder().field(codeChangeUnit)
                        .gt(JsonData.of(low))
                        .lte(JsonData.of(medium))
                        .build()._toQuery();
                queryList.add(rangeQuery);
            }

            if (codeChangeList.contains("large")) {
                Query rangeQuery =  new RangeQuery.Builder().field(codeChangeUnit)
                        .gt(JsonData.of(medium))
                        .build()._toQuery();
                queryList.add(rangeQuery);
            }

            includesQueryConditions.add(Query.of(q -> q
                    .bool(BoolQuery.of(b -> b
                            .should(queryList)))));
        }

    }

    private static void createCommentDensityCondition(ScmPrFilter filter, List<Query> queryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getCommentDensities())) {

            int shallow = (filter.getCommentDensitySizeConfig() != null && filter.getCommentDensitySizeConfig().containsKey("shallow")) ? Integer.parseInt(filter.getCommentDensitySizeConfig().get("shallow")) : 100;
            int good = (filter.getCommentDensitySizeConfig() != null && filter.getCommentDensitySizeConfig().containsKey("good")) ? Integer.parseInt(filter.getCommentDensitySizeConfig().get("good")) : 1000;

            List<String> commentDensities = filter.getCommentDensities();
            List<Query> queryList = new ArrayList<>();

            if (commentDensities.contains("shallow")) {
                Query rangeQuery =  new RangeQuery.Builder().field(PR_COMMENT_COUNT)
                        .lte(JsonData.of(shallow))
                        .build()._toQuery();
                queryList.add(rangeQuery);
            }

            if (commentDensities.contains("good")) {
                Query rangeQuery =  new RangeQuery.Builder().field(PR_COMMENT_COUNT)
                        .gt(JsonData.of(shallow))
                        .lte(JsonData.of(good))
                        .build()._toQuery();
                queryList.add(rangeQuery);
            }

            if (commentDensities.contains("heavy")) {
                Query rangeQuery =  new RangeQuery.Builder().field(PR_COMMENT_COUNT)
                        .gt(JsonData.of(good))
                        .build()._toQuery();
                queryList.add(rangeQuery);
            }

            queryConditions.add(Query.of(q -> q
                    .bool(BoolQuery.of(b -> b
                            .should(queryList)))));
        }
    }

    public static String getPrsCountSortByKey(Map<String, SortingOrder> sortBy, String across) {
        if (MapUtils.isEmpty(sortBy)) {
            return "_doc";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmPrFilter.DISTINCT.fromString(entry.getKey()) != null) {
                        if (!across.equals(entry.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return entry.getKey();
                    }
                    if (ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME.toString().equalsIgnoreCase(entry.getKey())) {
                        return ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME.toString();
                    }
                    if (ScmPrSorting.MEDIAN_AUTHOR_RESPONSE_TIME.toString().equalsIgnoreCase(entry.getKey())) {
                        return ScmPrSorting.MEDIAN_AUTHOR_RESPONSE_TIME.toString();
                    }
                    if (ScmPrFilter.CALCULATION.count.toString().equalsIgnoreCase(entry.getKey())) {
                        return "_doc";
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                })
                .orElse("_doc");
    }

    public static Pair<String, SortOrder> getSortConfig(Map<String, SortingOrder> sortBy, String defaultSort, Set<String> sortableColumns) {
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (sortableColumns.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return defaultSort;
                })
                .orElse(defaultSort);

        SortingOrder esSortOrderTmp = sortBy.values().stream().findFirst().orElse(SortingOrder.DESC);
        SortOrder esSortOrder = esSortOrderTmp.toString().equalsIgnoreCase(SortOrder.Asc.toString()) ? SortOrder.Asc : SortOrder.Desc;

        if (sortableColumns.contains(sortByKey)) {
            sortByKey = "pr_" + sortByKey;
        }
        return Pair.of(sortByKey, esSortOrder);
    }
}
