package io.levelops.faceted_search.converters;

import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.faceted_search.db.models.scm.EsScmPullRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.utils.EsUtils.getCalenderInterval;

public class EsScmPRsConverter {

    public static List<ScmPrFilter.DISTINCT> ACROSS_USERS = List.of(ScmPrFilter.DISTINCT.creator, ScmPrFilter.DISTINCT.reviewer, ScmPrFilter.DISTINCT.approver, ScmPrFilter.DISTINCT.assignee);

    public static List<EsScmPullRequest> getEsScmPullRequestsList(List<DbScmPullRequest> dbScmPullRequestList){

        return dbScmPullRequestList.stream().map( p -> EsScmPullRequest.builder()
                .additions(p.getAdditions())
                .approvers(p.getApprovers())
                .approverIds(p.getApproverIds())
                .approverInfo(p.getApproverInfo())
                .approverCount(p.getApproverCount())
                .approvalStatus(p.getApprovalStatus())
                .approvalTime(p.getApprovalTime())
                .assigneesInfo(p.getAssigneesInfo())
                .assignees(p.getAssignees())
                .assigneeIds(p.getAssigneeIds())
                .avgAuthorResponseTime(p.getAvgAuthorResponseTime())
                .avgReviewerResponseTime(p.getAvgReviewerResponseTime())
                .commitShas(p.getCommitShas())
                .change(p.getChange())
                .collabState(p.getCollabState())
                .closedDay(p.getClosedDay())
                .codeChange(p.getCodeChange())
                .commentCount(p.getCommentCount())
                .commentDensity(p.getCommentDensity())
                .commenterIds(p.getCommenterIds())
                .commenterInfo(p.getCommenterInfo())
                .commenters(p.getCommenters())
                .creator(p.getCreator())
                .creatorId(p.getCreatorId())
                .creatorInfo(p.getCreatorInfo())
                .createdAt(p.getCreatedAt())
                .commitStats(p.getCommitStats())
                .commentTime(p.getCommentTime())
                .cycleTime(p.getCycleTime())
                .createdDay(p.getCreatedDay())
                .deletions(p.getDeletions())
                .filesChanged(p.getFilesChanged())
                .filesCount(p.getFilesCount())
                .firstCommittedAt(p.getFirstCommittedAt())
                .hasIssueKeys(p.getHasIssueKeys())
                .id(p.getId())
                .integrationId(p.getIntegrationId())
                .issueKeys(p.getIssueKeys())
                .labels(p.getLabels())
                .linesAdded(p.getLinesAdded())
                .linesChanged(p.getLinesChanged())
                .linesDeleted(p.getLinesDeleted())
                .merged(p.getMerged())
                .mergedDay(p.getMergedDay())
                .mergeSha(p.getMergeSha())
                .number(p.getNumber())
                .project(p.getProject())
                .prLabels(p.getPrLabels())
                .prClosedAt(p.getPrClosedAt())
                .prCreatedAt(p.getPrCreatedAt())
                .prMergedAt(p.getPrMergedAt())
                .prUpdatedAt(p.getPrUpdatedAt())
                .repoIds(p.getRepoIds())
                .reviews(p.getReviews())
                .reviewCycleTime(p.getReviewCycleTime())
                .reviewerCount(p.getReviewerCount())
                .reviewerIds(p.getReviewerIds())
                .reviewers(p.getReviewers())
                .reviewerInfo(p.getReviewerInfo())
                .reviewMergeCycleTime(p.getReviewMergeCycleTime())
                .reviewType(p.getReviewType())
                .sourceBranch(p.getSourceBranch())
                .state(p.getState())
                .title(p.getTitle())
                .targetBranch(p.getTargetBranch())
                .technology(p.getTechnology())
                .workitemIds(p.getWorkitemIds())
                .loc(p.getLoc())
        .build()).collect(Collectors.toList());
    }

    public static List<DbAggregationResult> getAggResultFromSearchResponse(SearchResponse<Void> searchResponse, ScmPrFilter.DISTINCT across, ScmPrFilter.CALCULATION calculation, AGG_INTERVAL aggInterval, boolean valuesOnly, Map<String, String> userIdMap) {

        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        switch (across) {
            case creator:
            case reviewer:
            case approver:
            case assignee:
            case repo_id:
            case project:
            case review_type:
            case branch:
            case source_branch:
            case target_branch:
            case state:
            case collab_state:
            case label:
            case approval_status:
            case technology:
            case code_change:
            case comment_density:
                List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_" + across)
                        .sterms().buckets().array();
                termsBuckets.forEach(term -> {
                    if (!term.key().isEmpty()) {
                        DbAggregationResult result = getDbAggResponse(term, across, aggInterval, valuesOnly, calculation, userIdMap);
                        if (result != null) {
                            dbAggregationResults.add(result);
                        }
                    }
                });
                break;

            case approver_count:
            case reviewer_count:
                List<LongTermsBucket> ltermsBuckets = searchResponse.aggregations().get("across_" + across)
                        .lterms().buckets().array();
                ltermsBuckets.forEach(term -> {
                    DbAggregationResult result = getDbAggResponse(term, across, aggInterval, valuesOnly, calculation, userIdMap);
                    if (result != null) {
                        dbAggregationResults.add(result);
                    }
                });
                break;
            case pr_merged:
            case pr_closed:
            case pr_reviewed:
            case pr_created:
            case pr_updated:
                List<DateHistogramBucket> dateBucket = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateBucket.forEach(term -> {
                    DbAggregationResult result = getDbAggResponse(term, across, aggInterval, valuesOnly, calculation, userIdMap);
                    if (result != null) {
                        dbAggregationResults.add(result);
                    }
                });
                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid across provided " + across);
        }
        return dbAggregationResults;
    }

    private static DbAggregationResult getDbAggResponse(MultiBucketBase term, ScmPrFilter.DISTINCT across, AGG_INTERVAL aggInterval, boolean valuesOnly, ScmPrFilter.CALCULATION calculation, Map<String, String> userIdMap) {
        String key = null;
        String additionalKey = null;

        switch (calculation) {

            case reviewer_approve_time:
            case reviewer_comment_time:
            case merge_time:
            case reviewer_response_time:
            case author_response_time:
                return getDbAggResponseForTrend(term, aggInterval, across, calculation, userIdMap);

            default:
                key = null;
                additionalKey = null;

                if (term instanceof MultiTermsBucket) {
                    additionalKey = ((MultiTermsBucket) term).key().get(0);
                    key = ((MultiTermsBucket) term).key().get(1);
                } else if (term instanceof StringTermsBucket) {
                    key = ((StringTermsBucket) term).key();

                    if (ACROSS_USERS.contains(across)) {
                        additionalKey = userIdMap.getOrDefault(key, "NONE");
                    }

                } else if (term instanceof DateHistogramBucket) {
                    long interval = (((DateHistogramBucket) term).key().toEpochMilli());
                    key = Long.toString(TimeUnit.MILLISECONDS.toSeconds(interval));
                    additionalKey = ((DateHistogramBucket) term).keyAsString();
                    additionalKey = getCalenderInterval(aggInterval.name(), additionalKey, interval);
                }else if (term instanceof LongTermsBucket) {
                    long temp = ((LongTermsBucket) term).key();
                    key = String.valueOf(temp);
                }

                long count = term.docCount();

                if (valuesOnly) {
                    return DbAggregationResult.builder()
                            .key(key)
                            .additionalKey(additionalKey)
                            .count(count)
                            .build();
                }

                long files_ct = (long) term.aggregations().get("files_ct").sum().value();
                long addition_ct = (long) term.aggregations().get("addition_ct").sum().value();
                long deletion_ct = (long) term.aggregations().get("deletion_ct").sum().value();
                long changes_ct = (long) term.aggregations().get("changes_ct").sum().value();
                long comment_ct = (long) term.aggregations().get("comment_ct").sum().value();

                double avgLineChange = term.aggregations().get("avg_line_change").avg().value();
                double avgFileChange = term.aggregations().get("avg_file_change").avg().value();

                if(count != 0) {
                    avgLineChange = changes_ct /(double) count;
                    avgFileChange = files_ct /(double) count;
                }

                double medianLine = term.aggregations().get("median_line").medianAbsoluteDeviation().value();
                double medianFile = term.aggregations().get("median_file").medianAbsoluteDeviation().value();

                return DbAggregationResult.builder()
                        .key(key)
                        .additionalKey(additionalKey)
                        .count(count)
                        .filesChangedCount(files_ct)
                        .linesAddedCount(addition_ct)
                        .linesRemovedCount(deletion_ct)
                        .totalComments(comment_ct)
                        .linesChangedCount(changes_ct)
                        .filesChangedCount(files_ct)
                        .avgLinesChanged(Double.valueOf(String.format("%.2f", avgLineChange)))
                        .avgFilesChanged(Double.valueOf(String.format("%.2f", avgFileChange)))
                        .medianLinesChanged(Double.valueOf(String.format("%.2f", medianLine)))
                        .medianFilesChanged(Double.valueOf(String.format("%.2f", medianFile)))
                        .build();
        }
    }

    public static List<DbAggregationResult> getAggResultFromSearchResponseForTrend(SearchResponse<Void> searchResponse, ScmPrFilter.DISTINCT across, ScmPrFilter.CALCULATION calculation, AGG_INTERVAL aggInterval, Map<String, String> userIdMap) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();

        switch (across) {
            case pr_merged:
            case pr_closed:
            case pr_reviewed:
            case pr_created:
            case pr_updated:
                List<DateHistogramBucket> dateBucket = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateBucket.forEach(term -> {
                    DbAggregationResult result = getDbAggResponseForTrend(term, aggInterval, across, calculation, userIdMap);
                    if (result != null) {
                        dbAggregationResults.add(result);
                    }
                });
                break;

        }
        return dbAggregationResults;
    }

    private static DbAggregationResult getDbAggResponseForTrend(MultiBucketBase term, AGG_INTERVAL aggInterval, ScmPrFilter.DISTINCT across, ScmPrFilter.CALCULATION calculation, Map<String, String> userIdMap) {

        switch (calculation) {

            default:

                String key = null;
                String additionalKey = null;
                if (term instanceof StringTermsBucket) {
                    key = ((StringTermsBucket) term).key();
                    if (ACROSS_USERS.contains(across)) {
                        additionalKey = userIdMap.getOrDefault(key, "NONE");
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
                double mean = term.aggregations().get("stats").stats().avg();
                long median =   (long) mean;
                return DbAggregationResult.builder()
                        .key(key)
                        .additionalKey(additionalKey)
                        .count(ct)
                        .min(min)
                        .max(max)
                        .sum(sum)
                        .median(median)
                        .mean(mean)
                        .build();
        }
    }

    public static List<DbAggregationResult> getAggResultForCollabReport(SearchResponse<Void> searchResponse, boolean stackValue, Map<String, String> userIdMap) {

        List<StringTermsBucket> termsBuckets;

        if(stackValue) {
            termsBuckets = searchResponse.aggregations().get("across_approvers").sterms().buckets().array();
        }else {
            termsBuckets = searchResponse.aggregations().get("across_creators").sterms().buckets().array();
        }

        List<DbAggregationResult> list = new ArrayList<>();
        termsBuckets.forEach(term -> {
            String key = null;
            String additionalKey = null;
            String collabState = null;
            if(!stackValue) {
                key = term.key().substring(0, term.key().indexOf("#"));
                collabState = term.key().substring(term.key().lastIndexOf("#") + 1);
                additionalKey = term.key().substring(term.key().indexOf("#") + 1, term.key().lastIndexOf("#"));
            }else{
                key = term.key();
                additionalKey = userIdMap.getOrDefault(key, "NONE");
            }
            long count = term.docCount();

            DbAggregationResult res = DbAggregationResult.builder()
                    .key(key)
                    .additionalKey(additionalKey)
                    .collabState(collabState)
                    .count(count)
                    .build();

            list.add(res);
        });

        return list;
    }
}
