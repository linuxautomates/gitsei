package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.PercentilesAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ReverseNestedAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmAggService.COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS;
import static io.levelops.commons.databases.services.ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS;
import static io.levelops.faceted_search.utils.EsUtils.getNestedQuery;
import static io.levelops.faceted_search.utils.EsUtils.getQuery;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForNumbers;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTimeinMills;
import static io.levelops.faceted_search.utils.EsUtils.getRegex;
import static io.levelops.faceted_search.utils.EsUtils.getTimeRangeInMillis;

public class EsScmCommitQueryBuilder {

    public static final String INCLUDE_CONDITIONS = "Include_Conditions";
    public static final String EXCLUDE_CONDITIONS = "Exclude_Conditions";
    public static final String COMMITS_PREFIX = "c_";
    public static final int MAX_PAGE_SIZE = 1000;

    public static Map<String, List<Query>> buildQueryConditions(ScmCommitFilter commitFilter) {

        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();

        createCommitFilterIncludesCondition(commitFilter, includesQueryConditions);
        createCommitFilterExcludesCondition(commitFilter, excludesQueryConditions);
        createCommitFilterRangeCondition(commitFilter, includesQueryConditions);

        if (MapUtils.isNotEmpty(commitFilter.getPartialMatch())) {
            getPartialMatch(commitFilter.getPartialMatch(), includesQueryConditions);
        }
        if (MapUtils.isNotEmpty(commitFilter.getExcludePartialMatch())) {
            getPartialMatch(commitFilter.getExcludePartialMatch(), excludesQueryConditions);
        }

        return Map.of(INCLUDE_CONDITIONS, includesQueryConditions,
                EXCLUDE_CONDITIONS, excludesQueryConditions);

    }

    public static Map<String, Aggregation> buildAggsConditions(ScmCommitFilter commitFilter) {

        Validate.notNull(commitFilter.getAcross(), "Across cant be missing for groupby query.");

        ScmCommitFilter.DISTINCT across = commitFilter.getAcross();
        return getAcrossConditions(across, commitFilter, Map.of(), null, null);
    }


    public static Map<String, Aggregation> buildAggsConditions(ScmCommitFilter commitFilter, ScmCommitFilter.CALCULATION calculation,
                                                               Integer page, Integer pageSize) {

        Validate.notNull(commitFilter.getAcross(), "Across cant be missing for groupby query.");

        ScmCommitFilter.DISTINCT across = commitFilter.getAcross();

        Map<String, Aggregation> aggCalcConditions = getCalculationConditions(calculation, commitFilter);
        return getAcrossConditions(across, commitFilter, aggCalcConditions, page, pageSize);
    }

    private static void createCommitFilterIncludesCondition(ScmCommitFilter commitFilter, List<Query> includesQueryConditions) {

        if (CollectionUtils.isNotEmpty(commitFilter.getIntegrationIds())) {
            includesQueryConditions.add(getQuery("c_integration_id", commitFilter.getIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getRepoIds())) {
            includesQueryConditions.add(getQuery("c_repo_id", commitFilter.getRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getProjects())) {
            includesQueryConditions.add(getQuery("c_project", commitFilter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getCommitShas())) {
            includesQueryConditions.add(getQuery("c_commit_sha", commitFilter.getCommitShas()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getCommitBranches())) {
            includesQueryConditions.add(getQuery("c_branch.keyword", commitFilter.getCommitBranches()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getDaysOfWeek())) {
            includesQueryConditions.add(getQuery("c_day", commitFilter.getDaysOfWeek()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getFileTypes())) {
            includesQueryConditions.add(getQuery("c_file_types", commitFilter.getFileTypes()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getTechnologies())) {
            includesQueryConditions.add(getNestedQuery("c_technologies", "c_technologies.name", commitFilter.getTechnologies()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getVcsTypes())) {
            includesQueryConditions.add(getQuery("c_vcs_type", commitFilter.getVcsTypes().stream().map(v -> v.toString()).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getAuthors())) {
            includesQueryConditions.add(getQuery("c_author_id", commitFilter.getAuthors()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getCommitters())) {
            includesQueryConditions.add(getQuery("c_committer_id", commitFilter.getCommitters()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getCodeChanges())) {
            createCodeChangeCondition(commitFilter, includesQueryConditions);
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getCommitTitles())) {
            includesQueryConditions.add(getQuery("c_message", commitFilter.getCommitTitles()));
        }
        if (commitFilter.getLocRange() != null) {
            if (commitFilter.getLocRange().getLeft() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .gt(JsonData.of(commitFilter.getLocRange().getLeft()))
                        .build();

                includesQueryConditions.add(rangeQuery._toQuery());
            }
            if (commitFilter.getLocRange().getRight() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .lt(JsonData.of(commitFilter.getLocRange().getRight()))
                        .build();
                includesQueryConditions.add(rangeQuery._toQuery());
            }
        }

    }

    private static void createCommitFilterExcludesCondition(ScmCommitFilter commitFilter,
                                                            List<Query> excludesQueryConditions) {

        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeRepoIds())) {
            excludesQueryConditions.add(getQuery("c_repo_id", commitFilter.getExcludeRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeProjects())) {
            excludesQueryConditions.add(getQuery("c_project", commitFilter.getExcludeProjects()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeCommitShas())) {
            excludesQueryConditions.add(getQuery("c_commit_sha", commitFilter.getExcludeCommitShas()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeCommitBranches())) {
            excludesQueryConditions.add(getQuery("c_branch.keyword", commitFilter.getExcludeCommitBranches()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeDaysOfWeek())) {
            excludesQueryConditions.add(getQuery("c_day", commitFilter.getExcludeDaysOfWeek()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeFileTypes())) {
            excludesQueryConditions.add(getQuery("c_file_types", commitFilter.getExcludeFileTypes()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeTechnologies())) {
            excludesQueryConditions.add(getNestedQuery("c_technologies", "technologies.name", commitFilter.getExcludeTechnologies()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeAuthors())) {
            excludesQueryConditions.add(getQuery("c_author_id", commitFilter.getExcludeAuthors()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeCommitters())) {
            excludesQueryConditions.add(getQuery("c_committer_id", commitFilter.getExcludeCommitters()));
        }
        if (CollectionUtils.isNotEmpty(commitFilter.getExcludeCommitTitles())) {
            excludesQueryConditions.add(getQuery("c_message", commitFilter.getExcludeCommitTitles()));
        }
        if (commitFilter.getExcludeLocRange() != null) {
            if (commitFilter.getExcludeLocRange().getLeft() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .gt(JsonData.of(commitFilter.getExcludeLocRange().getLeft()))
                        .build();
                excludesQueryConditions.add(rangeQuery._toQuery());
            }
            if (commitFilter.getExcludeLocRange().getRight() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .lt(JsonData.of(commitFilter.getExcludeLocRange().getRight()))
                        .build();
                excludesQueryConditions.add(rangeQuery._toQuery());
            }
        }
    }

    private static void createCommitFilterRangeCondition(ScmCommitFilter commitFilter,
                                                         List<Query> includesQueryConditions) {

        if (commitFilter.getCommittedAtRange() != null && (commitFilter.getCommittedAtRange().getLeft() != null ||
                commitFilter.getCommittedAtRange().getRight() != null)) {
            ImmutablePair<Long, Long> range = getTimeRangeInMillis(commitFilter.getCommittedAtRange().getLeft(), commitFilter.getCommittedAtRange().getRight());
            includesQueryConditions.add(getRangeQueryForTimeinMills("c_committed_at", range));
        }
    }

    private static Map<String, Aggregation> getAcrossConditions(ScmCommitFilter.DISTINCT across,
                                                                ScmCommitFilter commitFilter,
                                                                Map<String, Aggregation> stackAggregation,
                                                                Integer page, Integer pageSize) {
        if (across == null) {
            return Map.of();
        }
        if (page != null) {
            stackAggregation.put("bucket_pagination", BucketSortAggregation.of(a -> a
                    .from(page * pageSize)
                    .size(pageSize))._toAggregation());
        }
        int pageLength = pageSize == null ? MAX_PAGE_SIZE : pageSize;
        Map<String, Aggregation> aggConditions = new HashMap<>();
        switch (across) {
            case project:
            case repo_id:
            case vcs_type:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(COMMITS_PREFIX + across.name()).size(pageLength))
                        .aggregations(stackAggregation)));
                break;
            case commit_branch:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(COMMITS_PREFIX + "branch.keyword").size(pageLength))
                        .aggregations(stackAggregation)));
                break;

            case file_type:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(COMMITS_PREFIX + across.name() + "s").size(pageLength))
                        .aggregations(stackAggregation)));
                break;

            case committer:
            case author:

                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.field(COMMITS_PREFIX + across.name()+"_id").size(pageLength))
                        .aggregations(stackAggregation)));
                break;

            case trend:
                aggConditions = getTimeBasedAcross(across, commitFilter, stackAggregation);
                break;

            case technology:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.nested(n -> n.path("c_technologies"))
                        .aggregations(Map.of("technology", Aggregation.of(a1 -> a1.terms(t -> t.field("c_technologies.name").size(pageLength))))
                        )));
                break;

            case code_change:

                int low = (commitFilter.getCodeChangeSizeConfig() != null && commitFilter.getCodeChangeSizeConfig().containsKey("small")) ? Integer.parseInt(commitFilter.getCodeChangeSizeConfig().get("small")) : 100;
                int medium = (commitFilter.getCodeChangeSizeConfig() != null && commitFilter.getCodeChangeSizeConfig().containsKey("medium")) ? Integer.parseInt(commitFilter.getCodeChangeSizeConfig().get("medium")) : 1000;

                Map<String, Aggregation> stackAggs = getNestedLineChangeAggregations(true);
                Map<String, Aggregation> codeChangeAggs = getCodeChangeAggregations(commitFilter);

                NestedAggregation nestedAggs = AggregationBuilders.nested().path("c_files").build();

                aggConditions.put("small_code", Aggregation.of(a -> a.filter(f -> f.range(r -> r.field("c_changes").lt(JsonData.of(low))))
                        .aggregations(Map.of("code_change_filter", Aggregation.of(a1 -> a1.nested(nestedAggs)
                                .aggregations(codeChangeAggs)
                                .aggregations(stackAggs))))
                ));

                aggConditions.put("medium_code", Aggregation.of(a -> a.filter(f -> f.range(r -> r.field("c_changes").lt(JsonData.of(medium)).gte(JsonData.of(low))))
                        .aggregations(Map.of("code_change_filter", Aggregation.of(a1 -> a1.nested(nestedAggs)
                                .aggregations(codeChangeAggs)
                                .aggregations(stackAggs))))
                ));

                aggConditions.put("large_code", Aggregation.of(a -> a.filter(f -> f.range(r -> r.field("c_changes").gte(JsonData.of(medium))))
                        .aggregations(Map.of("code_change_filter", Aggregation.of(a1 -> a1.nested(nestedAggs)
                                .aggregations(codeChangeAggs)
                                .aggregations(stackAggs))))
                ));

                break;
            case code_category:

                long legacyTime = commitFilter.getLegacyCodeConfig() != null ? TimeUnit.SECONDS.toMillis(commitFilter.getLegacyCodeConfig()) :
                        TimeUnit.SECONDS.toMillis(Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());

                stackAggs = getNestedLineChangeAggregations(true);
                codeChangeAggs = getCodeChangeAggregations(commitFilter);

                aggConditions.put("total_legacy_refactored_lines", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                        .aggregations(
                                Map.of("code_change_filter", Aggregation.of(a1 -> a1.filter(f -> f.range(t -> t.field("c_files.previous_committed_at").gte(JsonData.of(legacyTime))))
                                        .aggregations(codeChangeAggs)
                                        .aggregations(stackAggs)
                                )))));

                aggConditions.put("total_refactored_lines", Aggregation.of(a -> a
                        .nested(n -> n.path("c_files"))
                        .aggregations(
                                Map.of("code_change_filter", Aggregation.of(a1 -> a1.filter(f -> f.range(t -> t.field("c_files.previous_committed_at").lte(JsonData.of(legacyTime))))
                                        .aggregations(codeChangeAggs)
                                        .aggregations(stackAggs)
                                )))));

                aggConditions.put("total_new_lines", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                        .aggregations(Map.of("code_change_filter", Aggregation.of(a1 -> a1
                                .filter(f -> f.bool(b -> b.mustNot(List.of(Query.of(q -> q.exists(e -> e.field("c_files.previous_committed_at")))))))
                                .aggregations(codeChangeAggs)
                                .aggregations(stackAggs)
                        )))));

                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid across provided " + across);
        }
        return aggConditions;
    }

    private static Map<String, Aggregation> getCalculationConditions(ScmCommitFilter.CALCULATION calculation,
                                                                     ScmCommitFilter commitFilter) {

        Map<String, Aggregation> aggConditions = new HashMap<>();

        switch (calculation) {

            case commit_days:

                Map<String, Aggregation> aggsMap = Map.of("coding_days", Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field("c_committed_at")
                                        .timeZone("UTC")
                                        //.calendarInterval(getCalenderInterval(commitFilter))
                                        .calendarInterval(CalendarInterval.Day) // will always use days for commit_days calculation
                                        .minDocCount(1))),
                        "commit_size_change", Aggregation.of(a1 -> a1.nested(n -> n.path("c_files"))
                                .aggregations(Map.of("commit_size", Aggregation.of(a2 -> a2.sum(t -> t.field("c_files.total_change"))),
                                        "median_percentile", Aggregation.of(a2 -> a2.percentiles(t -> t.field("c_files.total_change").percents(List.of(50d))))))));
                aggConditions.putAll(aggsMap);
                break;

            case count:
                aggConditions.putAll(getLineChangeAggregations(false));
                aggConditions.putAll(getNestedCodeChangeAggregation(commitFilter));
                break;

            case commit_count:
                aggConditions.put("coding_days", Aggregation.of(a -> a.terms(t -> t.field("c_day"))
                        .aggregations(Map.of("committed_at", Aggregation.of(a2 -> a2.terms(t -> t.field("c_committed_at")))))
                        .aggregations(Map.of("commit_size_change", Aggregation.of(a1 -> a1.nested(n -> n.path("c_files"))
                                .aggregations(Map.of("commit_size", Aggregation.of(a2 -> a2.sum(t -> t.field("c_files.total_change"))))))))));
                break;

            case commit_count_only:
                aggConditions.putAll(getLineChangeAggregations(false));
                break;
        }
        return aggConditions;
    }

    private static Map<String, Aggregation> getTimeBasedAcross(ScmCommitFilter.DISTINCT across, ScmCommitFilter commitFilter, Map<String, Aggregation> stackAggregation) {

        Map<String, Aggregation> aggConditions = new HashMap<>();
        final CalendarInterval finalCalendarInterval = getCalenderInterval(commitFilter);

        switch (across) {
            case trend:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> dh
                                .field("c_committed_at")
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

    private static CalendarInterval getCalenderInterval(ScmCommitFilter commitFilter) {

        String interval = commitFilter.getAggInterval() != null ? commitFilter.getAggInterval().name() : AGG_INTERVAL.month.name();
        CalendarInterval calendarInterval = null;

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
            case "day_of_week":
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

    private static Map<String, Aggregation> getLineChangeAggregations(boolean useReverseNested) {

        Map<String, Aggregation> stackAggs = new HashMap<>();

        SumAggregation count = AggregationBuilders.sum().field("c_files_ct").build();
        SumAggregation addedCount = AggregationBuilders.sum().field("c_tot_lines_added").build();
        SumAggregation deletedCount = AggregationBuilders.sum().field("c_tot_lines_removed").build();
        SumAggregation changedCount = AggregationBuilders.sum().field("c_tot_lines_changed").build();
        SumAggregation commitAddedCount = AggregationBuilders.sum().field("c_additions").build();
        SumAggregation commitDeletedCount = AggregationBuilders.sum().field("c_deletions").build();
        SumAggregation commitChangedCount = AggregationBuilders.sum().field("c_changes").build();
        PercentilesAggregation pct = AggregationBuilders.percentiles().field("c_tot_lines_changed").percents(List.of(50.0)).build();
        ReverseNestedAggregation r = AggregationBuilders.reverseNested().build();

        if (useReverseNested) {
            stackAggs.put("files_ct", Aggregation.of(z -> z.reverseNested(r)
                    .aggregations("files_ct", Aggregation.of(a1 -> a1.sum(count)))));
        } else {
            stackAggs.put("files_ct", Aggregation.of(a -> a.sum(count)));
        }
        stackAggs.put("addition_ct", Aggregation.of(a -> a.sum(addedCount)));
        stackAggs.put("deletion_ct", Aggregation.of(a -> a.sum(deletedCount)));
        stackAggs.put("changes_ct", Aggregation.of(a -> a.sum(changedCount)));
        stackAggs.put("commit_addition_ct", Aggregation.of(a -> a.sum(commitAddedCount)));
        stackAggs.put("commit_deletion_ct", Aggregation.of(a -> a.sum(commitDeletedCount)));
        stackAggs.put("commit_changes_ct", Aggregation.of(a -> a.sum(commitChangedCount)));
        stackAggs.put("pct", Aggregation.of(a -> a.percentiles(pct)));

        return stackAggs;
    }

    private static Map<String, Aggregation> getNestedCodeChangeAggregation(ScmCommitFilter commitFilter) {

        long legacyTime = commitFilter.getLegacyCodeConfig() != null ? TimeUnit.SECONDS.toMillis(commitFilter.getLegacyCodeConfig()) :
                TimeUnit.SECONDS.toMillis(Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());

        return Map.of("total_legacy_refactored_lines", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                        .aggregations(Map.of("legacy_filter", Aggregation.of(a1 -> a1.
                                filter(f -> f.range(t -> t.field("c_files.previous_committed_at").gte(JsonData.of(legacyTime))))
                                .aggregations(Map.of("legacy_sum", Aggregation.of(a3 -> a3.sum(t -> t.field("c_files.total_change"))))))))),
                "total_refactored_lines", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                        .aggregations(Map.of("legacy_filter", Aggregation.of(a1 -> a1
                                .filter(f -> f.range(t -> t.field("c_files.previous_committed_at").lte(JsonData.of(legacyTime))))
                                .aggregations(Map.of("refactored_sum", Aggregation.of(a3 -> a3.sum(t -> t.field("c_files.total_change"))))))))),
                "total_new_lines", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                        .aggregations(Map.of("legacy_filter", Aggregation.of(a1 -> a1
                                .filter(f -> f.bool(b -> b.mustNot(List.of(Query.of(q -> q.exists(e -> e.field("c_files.previous_committed_at")))))))
                                .aggregations(Map.of("new_lines", Aggregation.of(a3 -> a3.sum(t -> t.field("c_files.total_change"))))))))),
                "total_lines_changed", Aggregation.of(a -> a.nested(n -> n.path("c_files"))
                        .aggregations(Map.of("total_lines", Aggregation.of(a3 -> a3.sum(t -> t.field("c_files.total_change"))))))
        );
    }

    private static Map<String, Aggregation> getNestedLineChangeAggregations(boolean useReverseNested) {

        Map<String, Aggregation> stackAggs = new HashMap<>();

        SumAggregation count = AggregationBuilders.sum().field("c_files_ct").build();
        SumAggregation addedCount = AggregationBuilders.sum().field("c_files.addition").build();
        SumAggregation deletedCount = AggregationBuilders.sum().field("c_files.deletion").build();
        SumAggregation changedCount = AggregationBuilders.sum().field("c_files.change").build();
        PercentilesAggregation pct = AggregationBuilders.percentiles().field("c_files.change").percents(List.of(50.0)).build();
        ReverseNestedAggregation r = AggregationBuilders.reverseNested().build();

        if (useReverseNested) {
            stackAggs.put("files_ct", Aggregation.of(z -> z.reverseNested(r)
                    .aggregations("files_ct", Aggregation.of(a1 -> a1.sum(count)))));
        } else {
            stackAggs.put("files_ct", Aggregation.of(a -> a.sum(count)));
        }
        stackAggs.put("addition_ct", Aggregation.of(a -> a.sum(addedCount)));
        stackAggs.put("deletion_ct", Aggregation.of(a -> a.sum(deletedCount)));
        stackAggs.put("changes_ct", Aggregation.of(a -> a.sum(changedCount)));
        stackAggs.put("pct", Aggregation.of(a -> a.percentiles(pct)));

        return stackAggs;
    }

    private static Map<String, Aggregation> getCodeChangeAggregations(ScmCommitFilter commitFilter) {

        long legacyTime = commitFilter.getLegacyCodeConfig() != null ? TimeUnit.SECONDS.toMillis(commitFilter.getLegacyCodeConfig()) :
                TimeUnit.SECONDS.toMillis(Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());

        return Map.of("total_legacy_line", Aggregation.of(a2 -> a2.filter(f -> f.range(t -> t.field("c_files.previous_committed_at").gte(JsonData.of(legacyTime))))
                        .aggregations(Map.of("total_legacy_refactored_lines", Aggregation.of(a12 -> a12.sum(t -> t.field("c_files.total_change")))))),
                "total_refactored_line", Aggregation.of(a3 -> a3.filter(f -> f.range(t -> t.field("c_files.previous_committed_at").lte(JsonData.of(legacyTime))))
                        .aggregations(Map.of("total_refactored_lines", Aggregation.of(a4 -> a4.sum(t -> t.field("c_files.total_change")))))),
                "total_new_line", Aggregation.of(a5 -> a5.filter(f -> f.bool(b -> b.mustNot(List.of(Query.of(q -> q.exists(e -> e.field("c_files.previous_committed_at")))))))
                        .aggregations(Map.of("total_new_lines", Aggregation.of(a6 -> a6.sum(t -> t.field("c_files.total_change")))))),
                "total_lines", Aggregation.of(a7 -> a7.sum(t -> t.field("c_files.total_change")))
        );
    }

    private static void createCodeChangeCondition(ScmCommitFilter filter, List<Query> includesQueryConditions) {

        int low = (filter.getCodeChangeSizeConfig() != null && filter.getCodeChangeSizeConfig().containsKey("small")) ? Integer.parseInt(filter.getCodeChangeSizeConfig().get("small")) : 100;
        int medium = (filter.getCodeChangeSizeConfig() != null && filter.getCodeChangeSizeConfig().containsKey("medium")) ? Integer.parseInt(filter.getCodeChangeSizeConfig().get("medium")) : 1000;

        List<String> codeChangeList = filter.getCodeChanges();
        List<Query> queryList = new ArrayList<>();

        if (codeChangeList.contains("small")) {
            queryList.add(getRangeQueryForNumbers("c_changes", JsonData.of(low), null));
        }

        if (codeChangeList.contains("medium")) {
            queryList.add(getRangeQueryForNumbers("c_changes", JsonData.of(low), JsonData.of(medium)));
        }

        if (codeChangeList.contains("large")) {
            queryList.add(getRangeQueryForNumbers("c_changes", null, JsonData.of(medium)));
        }

        includesQueryConditions.add(Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .should(queryList)))));

    }

    private static void getPartialMatch(Map<String, Map<String, String>> partialMatch, List<Query> queries) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS.contains(key) || COMMITS_PARTIAL_MATCH_COLUMNS.contains(key)) {
                switch (key) {
                    case "author":
                        key = "c_author";
                        break;
                    case "committer":
                        key = "c_committer";
                        break;
                    case "commit_branch":
                        key = "c_branch.keyword";
                        break;
                    case "project":
                        key = "c_project";
                        break;
                    case "commit_title":
                    case "message":
                        key = "c_message";
                        break;
                    case "repo_id":
                        key = "c_repo_id";
                        break;
                }
                getRegex(begins, ends, contains, key, queries);
            }
        }
    }
}
