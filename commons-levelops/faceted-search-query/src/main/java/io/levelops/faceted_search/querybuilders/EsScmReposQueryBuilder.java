package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.databases.services.ScmAggService.CONTRIBUTORS_PARTIAL_MATCH_COLUMNS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.EXCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.INCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.MAX_PAGE_SIZE;
import static io.levelops.faceted_search.utils.EsUtils.getQuery;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTimeinMills;
import static io.levelops.faceted_search.utils.EsUtils.getRegex;

public class EsScmReposQueryBuilder {

    public static Map<String, List<Query>> buildQueryConditionsForRepos(ScmReposFilter filter) {

        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();

        createFilterIncludesCondition(filter, includesQueryConditions);
        createFilterExcludesCondition(filter, excludesQueryConditions);
        createfilterRangeCondition(filter, includesQueryConditions);

        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            getPartialMatch(filter.getPartialMatch(), includesQueryConditions);
        }

        return Map.of(INCLUDE_CONDITIONS, includesQueryConditions,
                EXCLUDE_CONDITIONS, excludesQueryConditions);

    }

    public static Map<String, Aggregation> buildAggsConditionsForRepos(ScmReposFilter filter, Pair<String, SortOrder> sortOrder, Integer pageNumber, Integer pageSize) {

        Map<String, Aggregation> aggCalcConditions = getCalculationConditions();
        return getAcrossConditions(aggCalcConditions, sortOrder, pageNumber, pageSize);
    }

    private static void createFilterIncludesCondition(ScmReposFilter filter, List<Query> includesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            includesQueryConditions.add(getQuery("c_integration_id", filter.getIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            includesQueryConditions.add(getQuery("c_repo_id", filter.getRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            includesQueryConditions.add(getQuery("c_project", filter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getAuthors())) {
            includesQueryConditions.add(getQuery("c_author_id", filter.getAuthors()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCommitters())) {
            includesQueryConditions.add(getQuery("c_committer_id", filter.getCommitters()));
        }

    }

    private static void createFilterExcludesCondition(ScmReposFilter filter,
                                                      List<Query> excludesQueryConditions) {

        if (CollectionUtils.isNotEmpty(filter.getExcludeRepoIds())) {
            excludesQueryConditions.add(getQuery("c_repo_id", filter.getExcludeRepoIds()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            excludesQueryConditions.add(getQuery("c_project", filter.getExcludeProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAuthors())) {
            excludesQueryConditions.add(getQuery("c_author_id", filter.getExcludeAuthors()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommitters())) {
            excludesQueryConditions.add(getQuery("c_committer_id", filter.getExcludeCommitters()));
        }
    }

    private static void createfilterRangeCondition(ScmReposFilter filter,
                                                   List<Query> includesQueryConditions) {

        if (filter.getDataTimeRange() != null && filter.getDataTimeRange().getLeft() != null &&
                filter.getDataTimeRange().getRight() != null) {
            ImmutablePair<Long, Long> range = ImmutablePair.of(TimeUnit.SECONDS.toMillis(filter.getDataTimeRange().getLeft()),
                    TimeUnit.SECONDS.toMillis(filter.getDataTimeRange().getRight()));
            includesQueryConditions.add(getRangeQueryForTimeinMills("c_committed_at", range));
        }
    }

    private static Map<String, Aggregation> getCalculationConditions() {

        return new HashMap<>( Map.of("num_commits", Aggregation.of(t -> t.cardinality(a -> a.field("c_id"))),
                "num_workitems", Aggregation.of(t -> t.valueCount(a -> a.field("c_workitem_ids"))),
                "num_prs", Aggregation.of(t -> t.sum(a -> a.field("c_committer_pr_count"))),
                "additions", Aggregation.of(t -> t.sum(a -> a.field("c_additions"))),
                "deletions", Aggregation.of(t -> t.sum(a -> a.field("c_deletions"))),
                "changes", Aggregation.of(t -> t.sum(a -> a.field("c_changes")))
        ));
    }

    private static Map<String, Aggregation> getAcrossConditions(Map<String, Aggregation> stackAggregation, Pair<String, SortOrder> sortOrder, Integer page, Integer pageSize) {

        Map<String, Aggregation> aggConditions = new HashMap<>();

        if (page != null) {
            stackAggregation.put("bucket_pagination", BucketSortAggregation.of(a -> a
                    .from(page * pageSize)
                    .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                    .size(pageSize))
                    ._toAggregation());
        }
        aggConditions.put("file_type_agg", Aggregation.of(a1 -> a1.terms(t -> t.field("c_file_types").size(MAX_PAGE_SIZE))
                .aggregations(stackAggregation)
                ));
        aggConditions.put("total_count", Aggregation.of(a1 -> a1.cardinality(t -> t.field("c_file_types"))));

        return aggConditions;
    }


    private static void getPartialMatch(Map<String, Map<String, String>> partialMatch, List<Query> queries) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (CONTRIBUTORS_PARTIAL_MATCH_COLUMNS.contains(key)) {
                switch (key) {
                    case "author":
                        key = "c_author_id";
                        break;
                    case "committer":
                        key = "c_committer_id";
                        break;
                }
                getRegex(begins, ends, contains, key, queries);
            }
        }
    }

}
