package io.levelops.commons.faceted_search.db.utils;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AverageAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AverageBucketAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.PercentilesAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.PercentilesBucketAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.utils.MapUtils;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;

public class EsWorkItemCalculation {

    public static Map<String, Aggregation> getCalculation(CALCULATION calculation,
                                                          JiraIssuesFilter filter, Integer page, Integer pageSize) {

        switch (calculation) {
            case issue_count:
                return Map.of(calculation.name(), CardinalityAggregation.of(a -> a
                        .field("w_id"))._toAggregation());
            case ticket_count:
                return Map.of("total_story_points", SumAggregation.of(a ->
                                a.field("w_story_points"))._toAggregation(),
                        "mean_story_points", AverageAggregation.of(a ->
                                        a.field("w_story_points").missing(0L))
                                ._toAggregation());
            case hops:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_hops").missing(0L))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_hops").percents(List.of(50.0)))._toAggregation());
            case bounces:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_bounces").missing(0L))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_bounces").percents(List.of(50.0)))._toAggregation());
            case age:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_age").missing(0L))._toAggregation(),
                        "total_story_points", SumAggregation.of(a ->
                                a.field("w_story_points"))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_age").percents(List.of(90.0, 50.0)))._toAggregation());
            case story_points:
            case story_point_report:
                return Map.of(calculation.name(), ValueCountAggregation.of(a -> a
                                .field("w_id").missing(0L))._toAggregation(),
                        "total_story_points", SumAggregation.of(a ->
                                a.field("w_story_points"))._toAggregation(),
                        "total_unestimated_tickets", SumAggregation.of(a ->
                                        a.field("w_unestimated_ticket"))
                                ._toAggregation());
            case effort_report:
                return Map.of(calculation.name(), ValueCountAggregation.of(a -> a
                                .field("w_id").missing(0L))._toAggregation(),
                        "total_effort", SumAggregation.of(a ->
                                a.field("w_effort"))._toAggregation(),
                        "total_unestimated_tickets", SumAggregation.of(a ->
                                        a.field("w_unestimated_ticket"))
                                ._toAggregation());
            case assign_to_resolve:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_assign").missing(0L))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_assign").percents(List.of(50.0)))._toAggregation());
            case response_time:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_response_time").missing(0L))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_response_time").percents(List.of(50.0)))._toAggregation());
            case resolution_time:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_solve_time").missing(0L))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_solve_time").percents(90.0, 50.0))._toAggregation());
            case stage_times_report:
                return Map.of(calculation.name(), StatsAggregation.of(a ->
                                a.field("w_hist_statuses.time_spent").missing(0L))._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesAggregation.of(a ->
                                a.field("w_hist_statuses.time_spent").percents(List.of(50.0, 90.0, 95.0)))._toAggregation());
            case stage_bounce_report:
                return Map.of(calculation.name(), MultiTermsAggregation.of(mt -> mt
                                .terms(List.of(MultiTermLookup.of(l -> l.field("w_integration_id")),
                                        MultiTermLookup.of(l -> l.field("w_workitem_id")),
                                        MultiTermLookup.of(l -> l.field("w_hist_statuses.status"))))
                        )._toAggregation(),
                        calculation.name() + "_mean", AverageBucketAggregation.of(avg -> avg
                                .bucketsPath(p -> p.single(calculation.name() + ">_count"))
                        )._toAggregation(),
                        calculation.name() + "_percentiles", PercentilesBucketAggregation.of(avg -> avg
                                .bucketsPath(p -> p.single(calculation.name() + ">_count"))
                                .percents(50.0)
                        )._toAggregation());
            case state_transition_time:
                if (filter == null || filter.getToState() == null || filter.getFromState() == null) {
                    return Map.of();
                }
                return Map.of(calculation.name(), Aggregation.of(a -> a
                        .nested(NestedAggregation.of(n -> n
                                .path("w_hist_state_transitions")))
                        .aggregations(Map.of("state_transition", Aggregation.of(a1 -> a1
                                .filter(f -> f
                                        .bool(b -> b
                                                .filter(List.of(TermQuery.of(t1 -> t1
                                                        .field("w_hist_state_transitions.from_status")
                                                        .value(filter.getFromState()))._toQuery(), TermQuery.of(t2 -> t2
                                                        .field("w_hist_state_transitions.to_status")
                                                        .value(filter.getToState()))._toQuery()))))
                                .aggregations(Map.of("stats", StatsAggregation.of(sa -> sa
                                                .field("w_hist_state_transitions.state_transition_time").missing(0L))._toAggregation(),
                                        "percentile", PercentilesAggregation.of(p -> p
                                                .field("w_hist_state_transitions.state_transition_time")
                                                .percents(List.of(50.0)))._toAggregation())))))));
            case sprint_mapping:
                return Map.of(calculation.name(), Aggregation.of(a -> a
                                .composite(ca -> ca
                                        .sources(List.of(Map.of("integration_id", CompositeAggregationSource.of(cas -> cas
                                                        .terms(t -> t
                                                                .field("w_integration_id")
                                                        )
                                                )), Map.of("sprint_mappings.id", CompositeAggregationSource.of(cas -> cas
                                                        .terms(t -> t
                                                                .field("w_sprint_mappings.id")
                                                        )
                                                ))
                                        ))
                                        .size(pageSize)
                                )
                                .aggregations(Map.of(calculation.name() + "_nested", Aggregation.of(a0 -> a0
                                        .nested(n -> n
                                                .path("w_milestones")
                                        )
                                        .aggregations(Map.of(calculation.name() + "_name", Aggregation.of(a1 -> a1
                                                .terms(t1 -> t1
                                                        .field("w_milestones.name")
                                                        .size(Integer.MAX_VALUE)
                                                )
                                                .aggregations(Map.of(calculation.name() + "_start_time", Aggregation.of(a2 -> a2
                                                        .terms(t2 -> t2
                                                                .field("w_milestones.start_time")
                                                                .size(Integer.MAX_VALUE)
                                                        )
                                                        .aggregations(Map.of(calculation.name() + "_completed_at", Aggregation.of(a3 -> a3
                                                                .terms(t3 -> t3
                                                                        .field("w_milestones.completed_at")
                                                                        .size(Integer.MAX_VALUE)
                                                                )
                                                        )))
                                                )))
                                        )))
                                )))
                ));
            case sprint_mapping_count:
            case assignees:
                return Map.of(calculation.name(), TermsAggregation.of(t -> {
                    SortOrder esSortOrder = getSortOrder(filter);
                    return t
                                    .field("w_hist_assignees.assignee.display_name")
                                    .order(List.of(Map.of("_key", esSortOrder)));
                        }
                )._toAggregation());
            case priority:
            default:
                return Map.of();
        }
    }

    public static SortOrder getSortOrder(JiraIssuesFilter filter) {
        if (filter == null || MapUtils.isEmpty(filter.getSort()))
            return SortOrder.Desc;
        SortingOrder sortingOrder = filter.getSort().values().stream().findFirst().orElse(SortingOrder.DESC);
        return sortingOrder.name().equalsIgnoreCase("DESC") ? SortOrder.Desc : SortOrder.Asc;
    }

    public enum CALCULATION {
        age,
        hops,
        bounces,
        ticket_count,
        issue_count,
        resolution_time,
        stage_times_report,
        stage_bounce_report,
        story_points,
        story_point_report,
        effort_report,
        response_time,
        sprint_mapping,
        sprint_mapping_count,
        assign_to_resolve,
        assignees,
        state_transition_time,
        priority;

        public static EsWorkItemCalculation.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(EsWorkItemCalculation.CALCULATION.class, st);
        }
    }
}
