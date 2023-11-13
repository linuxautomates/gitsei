package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.business_alignment.BaActiveWorkScoreUtils.Scores;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.commons.utils.StringJoiner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.DONE_STATUS_CATEGORY;
import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.IN_PROGRESS_STATUS_CATEGORY;

@Log4j2
@Service
public class BaJiraAggsActiveWorkQueryBuilder {

    public static final String ACTIVE_SPRINT_STATE = "ACTIVE";
    public static final String UNASSIGNED = "_UNASSIGNED_";

    private final BaJiraAggsQueryBuilder jiraAggsQueryBuilder;

    @AllArgsConstructor
    @Getter
    public enum AlignmentScore {
        POOR(1),
        FAIR(2),
        GOOD(3);
        int score;
    }

    public BaJiraAggsActiveWorkQueryBuilder(BaJiraAggsQueryBuilder jiraAggsQueryBuilder) {
        this.jiraAggsQueryBuilder = jiraAggsQueryBuilder;
    }

    public BaJiraAggsQueryBuilder.Query buildActiveWorkSql(String company, JiraIssuesFilter filter, OUConfiguration ouConfig, JiraAcross across, TicketCategorizationScheme scheme, Calculation calculation,
                                                           Integer page, Integer pageSize) {
        long currentTime = Instant.now().getEpochSecond();
        Map<String, Object> params = new HashMap<>();

        TicketCategorizationScheme.IssuesActiveWork activeWorkOptions = scheme.getConfig().getActiveWork().getIssues();

        String sql;
        String key;
        switch (across) {
            case ASSIGNEE: {
                sql = generateActiveWorkSql(company, filter, ouConfig, calculation, activeWorkOptions, params, currentTime, "assignee");
                key = "assignee";
                break;
            }
            case TICKET_CATEGORY: {
                sql = generateActiveWorkSql(company, filter, ouConfig, calculation, activeWorkOptions, params, currentTime, null);
                key = null;
                break;
            }
            default:
                throw new UnsupportedOperationException("Active work across " + across + " is not supported");
        }

        String limit = "";
        String offset = "";

        if (filter.getAcrossLimit() != null && filter.getAcrossLimit() != DefaultListRequest.DEFAULT_ACROSS_LIMIT) {
            limit = "\nlimit " + filter.getAcrossLimit() + "\n";
        } else {
            Integer skip = page * pageSize;
            limit = "\nlimit " + pageSize + "\n";
            offset = "\noffset " + skip + "\n";
        }

        String countSql = "SELECT COUNT(*) FROM ( " + sql + " ) i";
        sql += limit + offset;

        return BaJiraAggsQueryBuilder.Query.builder()
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
                .countSql(countSql)
                .build();
    }


    private String generateActiveWorkSql(String company, JiraIssuesFilter filter, OUConfiguration ouConfig, Calculation calculation, TicketCategorizationScheme.IssuesActiveWork activeWorkOptions, Map<String, Object> params, long currentTime, String groupByColumn) {
        String ticketCategorySql = jiraAggsQueryBuilder.generateTicketCategorySql(company, filter, params, currentTime);

        // -- conditions for completed work
        JiraIssuesFilter completedWorkFilter = filter.toBuilder()
                .statusCategories(List.of(DONE_STATUS_CATEGORY))
                .build();
        String completedWorkSql = jiraAggsQueryBuilder.generateJiraIssuesSql(company, completedWorkFilter, ouConfig, "completed_work_", params, currentTime, ticketCategorySql, false, null);

        // -- conditions for active work
        var filterBuilder = filter.toBuilder();
        if (BooleanUtils.isTrue(activeWorkOptions.getInProgress())) {
            filterBuilder.statusCategories(List.of(IN_PROGRESS_STATUS_CATEGORY));
        }
        if (BooleanUtils.isTrue(activeWorkOptions.getActiveSprints())) {
            filterBuilder.sprintStates(List.of(ACTIVE_SPRINT_STATE)); // TODO implement filter
        }
        if (BooleanUtils.isTrue(activeWorkOptions.getAssigned())) {
            filterBuilder.missingFields(Map.of("assignee", false));
        }
        JiraIssuesFilter activeWorkFilter = filterBuilder
                .issueResolutionRange(null) // active work is by definition not resolved
                .build();
        String activeWorkSql = jiraAggsQueryBuilder.generateJiraIssuesSql(company, activeWorkFilter, ouConfig, "active_work_", params, currentTime, ticketCategorySql, false, null);

        // -- select
        String effortCalculation = BaJiraAggsQueryBuilder.generateEffortCalculationSql(calculation) + " as effort";
        String select = StringJoiner.dedupeAndJoin(", ", groupByColumn, "json_agg(json_build_object('ticket_category', ticket_category, 'effort', effort)) as effort_array");
        String innerSelect = StringJoiner.dedupeAndJoin(", ", groupByColumn, "ticket_category", effortCalculation);

        // -- group by
        String innerGroupBy = StringJoiner.dedupeAndJoin(", ", groupByColumn, "rollup(ticket_category)"); // "assignee, rollup(ticket_category)"
        String groupBy = StringJoiner.prefixIfNotBlank("group by ", StringJoiner.dedupeAndJoin(", ", groupByColumn)); // "group by assignee"

        return "\n" +
                "  with filtered_issues as ( \n" +
                "    select * from (\n" + completedWorkSql + ") as completed_work\n" +
                "    union (\n" + activeWorkSql + ")\n" +
                "  )\n" +
                "  select " + select + "\n" +
                "  from (\n" +
                "    select " + innerSelect + "\n" +
                "    from filtered_issues\n" +
                "    group by " + innerGroupBy + "\n" +
                "  ) as i1\n" +
                "  " + groupBy + "\n";
    }

}
