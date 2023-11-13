package io.levelops.faceted_search.converters;

import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.faceted_search.db.models.scm.EsScmIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.utils.EsUtils.getCalenderInterval;

public class EsScmIssuesConverter {

    public static List<ScmIssueFilter.DISTINCT> ACROSS_USERS = List.of(ScmIssueFilter.DISTINCT.creator);

    public static List<EsScmIssue> getESScmIssueList(List<DbScmIssue> dbScmIssueList){

        return dbScmIssueList.stream().map( d -> EsScmIssue.builder()
                .id(d.getId())
                .repoId(d.getRepoId())
                .project(d.getProject())
                .integrationId(d.getIntegrationId())
                .issueId(d.getIssueId())
                .number(d.getNumber())
                .creator(d.getCreator())
                .creatorInfo(d.getCreatorInfo())
                .assignees(d.getAssignees())
                .labels(d.getLabels())
                .state(d.getState())
                .title(d.getTitle())
                .url(d.getUrl())
                .numComments(d.getNumComments())
                .issueCreatedAt(d.getIssueCreatedAt())
                .issueUpdatedAt(d.getIssueUpdatedAt())
                .issueClosedAt(d.getIssueClosedAt())
                .firstCommentAt(d.getFirstCommentAt())
                .createdAt(d.getCreatedAt())
                .responseTime(d.getResponseTime())
                .solveTime(d.getSolveTime())
                .build()).collect(Collectors.toList());
    }

    public static List<DbAggregationResult> getAggResultFromSearchResponse(SearchResponse<Void> searchResponse, ScmIssueFilter filter) {

        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        ScmIssueFilter.DISTINCT across = filter.getAcross();
        ScmIssueFilter.CALCULATION calculation = filter.getCalculation() == null ? ScmIssueFilter.CALCULATION.count : filter.getCalculation();

        switch (across) {
            case repo_id:
            case project:
            case state:
            case creator:
            case label:
            case assignee:
                List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_" + across)
                        .sterms().buckets().array();
                termsBuckets.forEach(term -> {
                    if (!term.key().isEmpty()) {
                        DbAggregationResult res = getDbAggResponse(term, across, filter.getAggInterval(), calculation);
                        if (res != null) {
                            dbAggregationResults.add(res);
                        }
                    }
                });
                break;

            case issue_closed:
            case issue_created:
            case issue_updated:
            case first_comment:
                List<DateHistogramBucket> dateBucket = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateBucket.forEach(term -> {
                    DbAggregationResult res = getDbAggResponse(term, across, filter.getAggInterval(), calculation);
                    if (res != null) {
                        dbAggregationResults.add(res);
                    }
                });
                break;

        }

        return dbAggregationResults;
    }

    private static DbAggregationResult getDbAggResponse(MultiBucketBase term, ScmIssueFilter.DISTINCT across, AGG_INTERVAL aggInterval, ScmIssueFilter.CALCULATION calculation) {
        {

            String key;
            String additionalKey;

            switch (calculation) {

                case response_time:
                case resolution_time:
                    return getDbAggResponseForTrend(term, aggInterval, across, calculation);

                default:
                    key = null;
                    additionalKey = null;

                    if (term instanceof MultiTermsBucket) {
                        additionalKey = ((MultiTermsBucket) term).key().get(0);
                        key = ((MultiTermsBucket) term).key().get(1);
                    } else if (term instanceof StringTermsBucket) {
                        key = ((StringTermsBucket) term).key();

                        if (ACROSS_USERS.contains(across)) {
                            String temp = key;
                            key = temp.substring(0, temp.indexOf("#"));
                            additionalKey = temp.substring(temp.indexOf("#") + 1);
                        }

                    } else if (term instanceof DateHistogramBucket) {
                        long interval = (((DateHistogramBucket) term).key().toEpochMilli());
                        key = Long.toString(TimeUnit.MILLISECONDS.toSeconds(interval));
                        additionalKey = ((DateHistogramBucket) term).keyAsString();
                        additionalKey = getCalenderInterval(aggInterval.name(), additionalKey, interval);
                    }

                    long count = term.docCount();

                    return DbAggregationResult.builder()
                            .key(key)
                            .additionalKey(additionalKey)
                            .count(count)
                            .build();
            }
        }
    }

    private static DbAggregationResult getDbAggResponseForTrend(MultiBucketBase term, AGG_INTERVAL aggInterval, ScmIssueFilter.DISTINCT across, ScmIssueFilter.CALCULATION calculation) {

        switch (calculation) {

            default:
                String key = null;
                String additionalKey = null;
                if (term instanceof StringTermsBucket) {
                    key = ((StringTermsBucket) term).key();

                    if (ACROSS_USERS.contains(across)) {
                        key = key.substring(0, key.indexOf("#"));
                        additionalKey = key.substring(key.indexOf("#") + 1);
                    }
                } else if (term instanceof DateHistogramBucket) {
                    long interval = (((DateHistogramBucket) term).key().toEpochMilli());
                    key = Long.toString(TimeUnit.MILLISECONDS.toSeconds(interval));
                    additionalKey = ((DateHistogramBucket) term).keyAsString();
                    additionalKey = getCalenderInterval(aggInterval.name(), additionalKey, interval);
                }

                long ct = term.aggregations().get("stats").stats().count();
                if (ct == 0) {
                    return null;
                }
                long min = (long) term.aggregations().get("stats").stats().min();
                min = min == Long.MIN_VALUE ? 0 : min;
                long max = (long) term.aggregations().get("stats").stats().max();
                max = max == Long.MAX_VALUE ? 0 : max;
                long sum = (long) term.aggregations().get("stats").stats().sum();
                long medianResponseTime = (long) term.aggregations().get("median").medianAbsoluteDeviation().value();
                double avg = term.aggregations().get("stats").stats().avg();

                return DbAggregationResult.builder()
                        .key(key)
                        .additionalKey(additionalKey)
                        .count(ct)
                        .min(min)
                        .max(max)
                        .sum(sum)
                        .mean(avg)
                        .median(medianResponseTime)
                        .build();
        }
    }
}
