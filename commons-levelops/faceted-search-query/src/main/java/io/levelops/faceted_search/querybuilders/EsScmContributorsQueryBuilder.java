package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.databases.services.ScmAggService.CONTRIBUTORS_PARTIAL_MATCH_COLUMNS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.COMMITS_PREFIX;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.EXCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.INCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.MAX_PAGE_SIZE;
import static io.levelops.faceted_search.utils.EsUtils.getQuery;
import static io.levelops.faceted_search.utils.EsUtils.getRangeQueryForTimeinMills;
import static io.levelops.faceted_search.utils.EsUtils.getRegex;

public class EsScmContributorsQueryBuilder {

    public static Map<String, List<Query>> buildQueryConditionsForContributors(ScmContributorsFilter filter) {

        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();

        createFilterIncludesCondition(filter, includesQueryConditions);
        createFilterExcludesCondition(filter, excludesQueryConditions);
        createfilterRangeCondition(filter, includesQueryConditions);

        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            getPartialMatch(filter.getPartialMatch(), includesQueryConditions);
        }
        if (MapUtils.isNotEmpty(filter.getExcludePartialMatch())) {
            getPartialMatch(filter.getExcludePartialMatch(), excludesQueryConditions);
        }

        return Map.of(INCLUDE_CONDITIONS, includesQueryConditions,
                EXCLUDE_CONDITIONS, excludesQueryConditions);

    }

    public static Map<String, Aggregation> buildAggsConditionsForContributor(ScmContributorsFilter filter, Pair<String, SortOrder> sortOrder, Integer pageNumber, Integer pageSize) {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");

        ScmContributorsFilter.DISTINCT across = filter.getAcross();
        Map<String, Aggregation> aggCalcConditions = getCalculationConditions(filter);
        Map<String, Aggregation> aggConditions = getAcrossConditions(across, filter, aggCalcConditions, sortOrder, pageNumber, pageSize);

        return aggConditions;
    }

    private static Map<String, Aggregation> getCalculationConditions(ScmContributorsFilter filter) {

        Map<String, Aggregation> calcMap = Map.of("num_commits", Aggregation.of( t -> t.cardinality( a -> a.field("c_id"))),
                "repo_count", Aggregation.of( t -> t.cardinality( a -> a.field("c_repo_id"))),
                "num_prs", Aggregation.of( t -> t.sum( a -> a.field("c_committer_pr_count"))),
                "repos", Aggregation.of( t -> t.terms( a -> a.field("c_repo_id").size(MAX_PAGE_SIZE))),
                "file_types", Aggregation.of( t -> t.terms( a -> a.field("c_file_types").size(MAX_PAGE_SIZE))),
                "num_workitems", Aggregation.of( t -> t.valueCount( a -> a.field("c_workitem_ids"))),
               // "num_jiraissues", Aggregation.of(t -> t.valueCount(a -> a.field("c_issue_keys"))), //error for >10 entries
                "additions", Aggregation.of( t -> t.sum( a -> a.field("c_additions"))),
                "deletions", Aggregation.of( t -> t.sum( a -> a.field("c_deletions"))),
                "changes", Aggregation.of( t -> t.sum( a -> a.field("c_changes"))),
                "technology_breadth", Aggregation.of(a -> a.nested(n -> n.path("c_technologies"))
                        .aggregations(Map.of("technologies", Aggregation.of(a1 -> a1.terms(t -> t.field("c_technologies.name").size(MAX_PAGE_SIZE))))
                        ))
        );

        Map<String, Aggregation> map = new HashMap<>(calcMap);
        map.put("num_jiraissues", Aggregation.of(t -> t.valueCount(a -> a.field("c_issue_keys"))));

        return map;
    }

    private static void createFilterIncludesCondition(ScmContributorsFilter filter, List<Query> includesQueryConditions) {

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
        if (CollectionUtils.isNotEmpty(filter.getCommitTitles())) {
            includesQueryConditions.add(getQuery("c_message", filter.getCommitTitles()));
        }
        if (filter.getLocRange() != null) {
            if (filter.getLocRange().getLeft() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .gt(JsonData.of(filter.getLocRange().getLeft()))
                        .build();

                includesQueryConditions.add(rangeQuery._toQuery());
            }
            if (filter.getLocRange().getRight() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .lt(JsonData.of(filter.getLocRange().getRight()))
                        .build();
                includesQueryConditions.add(rangeQuery._toQuery());
            }
        }

    }

    private static void createFilterExcludesCondition(ScmContributorsFilter filter,
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
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommitTitles())) {
            excludesQueryConditions.add(getQuery("c_message", filter.getExcludeCommitTitles()));
        }
        if (filter.getExcludeLocRange() != null) {
            if (filter.getExcludeLocRange().getLeft() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .gt(JsonData.of(filter.getExcludeLocRange().getLeft()))
                        .build();
                excludesQueryConditions.add(rangeQuery._toQuery());
            }
            if (filter.getExcludeLocRange().getRight() != null) {
                RangeQuery rangeQuery =  new RangeQuery.Builder().field("c_loc")
                        .lt(JsonData.of(filter.getExcludeLocRange().getRight()))
                        .build();
                excludesQueryConditions.add(rangeQuery._toQuery());
            }
        }
    }

    private static void createfilterRangeCondition(ScmContributorsFilter filter,
                                                   List<Query> includesQueryConditions) {

        if (filter.getDataTimeRange() != null && filter.getDataTimeRange().getLeft() != null &&
                filter.getDataTimeRange().getRight() != null) {
            ImmutablePair<Long, Long> range = ImmutablePair.of(TimeUnit.SECONDS.toMillis(filter.getDataTimeRange().getLeft()),
                    TimeUnit.SECONDS.toMillis(filter.getDataTimeRange().getRight()));
            includesQueryConditions.add(getRangeQueryForTimeinMills("c_committed_at", range));
        }
    }

    private static Map<String, Aggregation> getAcrossConditions(ScmContributorsFilter.DISTINCT across,
                                                                ScmContributorsFilter filter,
                                                                Map<String, Aggregation> stackAggregation,
                                                                Pair<String, SortOrder> sortOrder, Integer page, Integer pageSize) {
        if (across == null) {
            return Map.of();
        }
        if (page != null) {
            stackAggregation.put("bucket_pagination", BucketSortAggregation.of(a -> a
                    .from(page * pageSize)
                    .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                    .size(pageSize))
                    ._toAggregation());
        }
        Map<String, Aggregation> aggConditions = new HashMap<>();
        switch (across) {
            case committer:
            case author:
                String scriptString = "if( doc['" + COMMITS_PREFIX+across.name() + "_id'].size() > 0) { def id = doc['" + COMMITS_PREFIX+across.name() + "_id'].value; def name = doc['" + COMMITS_PREFIX+across.name() + "'].value; return id+'#'+name; } return '';";
                Script moduleScript = Script.of(s -> s.inline(i -> i
                        .source(scriptString)));
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a.terms(t -> t.script(moduleScript).size(MAX_PAGE_SIZE))
                        .aggregations(stackAggregation)
                ));
                aggConditions.put("total_count", Aggregation.of( a1 -> a1.cardinality(f -> f.field(COMMITS_PREFIX+across.name()+"_id"))));
                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid across provided " + across);
        }
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
                        key = "c_author";
                        break;
                    case "committer":
                        key = "c_committer";
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
