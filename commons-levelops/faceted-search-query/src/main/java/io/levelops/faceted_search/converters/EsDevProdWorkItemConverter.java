package io.levelops.faceted_search.converters;

import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.faceted_search.db.models.workitems.EsDevProdWorkItemResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EsDevProdWorkItemConverter {

    public static List<EsDevProdWorkItemResponse> convert(SearchResponse<Void> searchResponse2,
                                                          Boolean hasDevStages, List<String> historicalAssignees, boolean useIssuePartialCredit) {


        List<EsDevProdWorkItemResponse> responses = new ArrayList<>();
        HashSet<String> historicalAssigneeSet = new HashSet<>(historicalAssignees);

        searchResponse2.aggregations().get("across_w_workitem_id").sterms().buckets().array().forEach(term2 -> {
            EsDevProdWorkItemResponse item = getHistAssigneeTimeSum(term2, hasDevStages, historicalAssigneeSet, useIssuePartialCredit);
            if(item.getAssigneeTime() != 0) {
                responses.add(item);
            }
        });
        return responses;
    }

    private static EsDevProdWorkItemResponse getHistAssigneeTimeSum(StringTermsBucket term, Boolean hasDevStages, Set<String> historicalAssignees, boolean useIssuePartialCredit) {

        List<DoubleTermsBucket> storyPointBucket = term.aggregations().get("across_w_story_points").dterms().buckets().array();
        String key = "";
        Double storyPoint = 0d;
        Double assigneeTime = 0d;
        Double totalTime = 0d;

        for (DoubleTermsBucket storyPoints : storyPointBucket) {
            storyPoint = storyPoints.key();
            SumAggregate totalTimeAggregarte = null;
            SumAggregate assigneeTimeAggregarte = null;

            if(storyPoint != null && storyPoint < 0){
                storyPoint = 0d;
            }

            List<StringTermsBucket> assigneeBuckets = storyPoints.aggregations().get("nested_w_hist_assignee_statuses").nested().aggregations()
                    .get("across_w_hist_assignee_statuses.historical_assignee_id").sterms().buckets().array();

            String assigneeTimeKey = (useIssuePartialCredit) ? "sum_w_hist_assignee_statuses.hist_assignee_time_excluding_resolution" : "sum_w_hist_assignee_statuses.hist_assignee_time";
            for (StringTermsBucket assignees : assigneeBuckets) {
                if (hasDevStages) {
                    if (historicalAssignees.contains(assignees.key())) {
                        key = assignees.key();
                        assigneeTimeAggregarte = assignees.aggregations().get("filter_w_hist_assignee_statuses.issue_status").filter().aggregations()
                                .get("filter_w_hist_assignee_statuses.issue_status").filter().aggregations()
                                .get(assigneeTimeKey).sum();
                        assigneeTime += assigneeTimeAggregarte.value();
                    }
                    totalTimeAggregarte = assignees.aggregations().get("filter_w_hist_assignee_statuses.issue_status").filter().aggregations()
                            .get("filter_w_hist_assignee_statuses.issue_status").filter().aggregations()
                            .get(assigneeTimeKey).sum();
                    totalTime += totalTimeAggregarte.value();
                } else {
                    if (historicalAssignees.contains(assignees.key())) {
                        key = assignees.key();
                        assigneeTimeAggregarte = assignees.aggregations().get("filter_w_hist_assignee_statuses.issue_status")
                                .filter().aggregations().get(assigneeTimeKey).sum();
                        assigneeTime += assigneeTimeAggregarte.value();
                    }
                    totalTimeAggregarte = assignees.aggregations().get("filter_w_hist_assignee_statuses.issue_status")
                            .filter().aggregations().get(assigneeTimeKey).sum();
                    totalTime += totalTimeAggregarte.value();
                }
            }
        }

        return EsDevProdWorkItemResponse.builder()
                .workitemId(term.key())
                .historicalAssigneeId(key)
                .storyPoints(storyPoint)
                .timeInStatuses(totalTime)
                .assigneeTime(assigneeTime)
                .build();

    }

}
