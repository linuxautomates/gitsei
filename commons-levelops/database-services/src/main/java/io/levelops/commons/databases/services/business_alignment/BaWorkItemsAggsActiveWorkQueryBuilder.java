package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.business_alignment.BaActiveWorkScoreUtils.Scores;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.StringJoiner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.business_alignment.BaWorkItemsAggsQueryBuilder.DONE_STATUS_CATEGORIES;
import static io.levelops.commons.databases.services.business_alignment.BaWorkItemsAggsQueryBuilder.IN_PROGRESS_STATUS_CATEGORIES;

@Log4j2
@Service
public class BaWorkItemsAggsActiveWorkQueryBuilder {

    public static final String UNASSIGNED = "_UNASSIGNED_";
    public static final String CURRENT_STATE = "current";
    private final BaWorkItemsAggsQueryBuilder workItemsAggsQueryBuilder;

    @AllArgsConstructor
    @Getter
    public enum AlignmentScore {
        POOR(1),
        FAIR(2),
        GOOD(3);
        int score;
    }

    public BaWorkItemsAggsActiveWorkQueryBuilder(BaWorkItemsAggsQueryBuilder workItemsAggsQueryBuilder) {
        this.workItemsAggsQueryBuilder = workItemsAggsQueryBuilder;
    }

    public BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery buildActiveWorkSql(String company,
                                                                             WorkItemsFilter filter,
                                                                             WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                             OUConfiguration ouConfig,
                                                                             BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                             TicketCategorizationScheme scheme,
                                                                             BaWorkItemsAggsQueryBuilder.Calculation calculation) {
        Map<String, Object> params = new HashMap<>();
        TicketCategorizationScheme.IssuesActiveWork activeWorkOptions = scheme.getConfig().getActiveWork().getIssues();

        String sql;
        String key;
        switch (across) {
            case ASSIGNEE: {
                sql = generateActiveWorkSql(company, filter, workItemsMilestoneFilter, ouConfig, calculation, activeWorkOptions, params, "assignee");
                key = "assignee";
                break;
            }
            case TICKET_CATEGORY: {
                sql = generateActiveWorkSql(company, filter, workItemsMilestoneFilter, ouConfig, calculation, activeWorkOptions, params, null);
                key = null;
                break;
            }
            default:
                throw new UnsupportedOperationException("Active work across " + across + " is not supported");
        }

        String limit = filter.getAcrossLimit() != null ? "\nlimit " + filter.getAcrossLimit() + "\n" : "";
        sql += limit;

        return BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery.builder()
                .sql(sql)
                .params(params)
                .rowMapper((rs, rowNumber) -> {
                    List<Map<String, Object>> effortArray = ParsingUtils.parseJsonList(DefaultObjectMapper.get(), "allocations", rs.getString("effort_array"));
                    Scores allocations = BaActiveWorkScoreUtils.processActiveWorkEffortArray(effortArray, scheme);
                    return DbAggregationResult.builder()
                            .key(key != null ? rs.getString(key) : null)
                            .alignmentScore(allocations.getAlignmentScore())
                            .percentageScore(allocations.getPercentageScore())
                            .categoryAllocations(allocations.getCategoryAllocation())
                            .build();
                })
                .build();
    }

    private String generateActiveWorkSql(String company,
                                         WorkItemsFilter filter,
                                         WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                         OUConfiguration ouConfig,
                                         BaWorkItemsAggsQueryBuilder.Calculation calculation,
                                         TicketCategorizationScheme.IssuesActiveWork activeWorkOptions,
                                         Map<String, Object> params,
                                         String groupByColumn) {

        // -- conditions for completed work
        WorkItemsFilter completedWorkFilter = filter.toBuilder()
                .statusCategories(DONE_STATUS_CATEGORIES)
                .build();
        String completedWorkSql = workItemsAggsQueryBuilder.generateWorkItemsSql(company, completedWorkFilter, workItemsMilestoneFilter, params, "completed_work_", ouConfig, false, null);

        // -- conditions for active work
        var filterBuilder = filter.toBuilder();
        if (BooleanUtils.isTrue(activeWorkOptions.getInProgress())) {
            filterBuilder.statusCategories(IN_PROGRESS_STATUS_CATEGORIES);
        }
        if (BooleanUtils.isTrue(activeWorkOptions.getAssigned())) {
            filterBuilder.excludeAssignees(List.of(UNASSIGNED));
        }
        if (BooleanUtils.isTrue(activeWorkOptions.getActiveSprints())) {
            workItemsMilestoneFilter = workItemsMilestoneFilter.toBuilder()
                    .states(List.of(CURRENT_STATE))
                    .build();
        }
        WorkItemsFilter activeWorkFilter = filterBuilder
                .workItemResolvedRange(null) // active work is by definition not resolved
                .build();
        String activeWorkSql = workItemsAggsQueryBuilder.generateWorkItemsSql(company, activeWorkFilter, workItemsMilestoneFilter, params, "active_work_", ouConfig, false, null);

        // -- select
        String effortCalculation = BaWorkItemsAggsQueryBuilder.generateEffortCalculationSql(calculation) + " as effort";
        String select = StringJoiner.dedupeAndJoin(", ", groupByColumn, "json_agg(json_build_object('ticket_category', ticket_category, 'effort', effort)) as effort_array");
        String innerSelect = StringJoiner.dedupeAndJoin(", ", groupByColumn, "ticket_category", effortCalculation);

        // -- group by
        String innerGroupBy = StringJoiner.dedupeAndJoin(", ", groupByColumn, "rollup(ticket_category)"); // "assignee, rollup(ticket_category)"
        String groupBy = StringJoiner.prefixIfNotBlank("group by ", StringJoiner.dedupeAndJoin(", ", groupByColumn)); // "group by assignee"

        return "\n" +
                "  with filtered_workitems as ( \n" +
                "    select * from (\n" + completedWorkSql + ") as completed_work\n" +
                "    union (\n" + activeWorkSql + ")\n" +
                "  )\n" +
                "  select " + select + "\n" +
                "  from (\n" +
                "    select " + innerSelect + "\n" +
                "    from filtered_workitems\n" +
                "    group by " + innerGroupBy + "\n" +
                "  ) a\n" +
                "  " + groupBy + "\n";
    }
}