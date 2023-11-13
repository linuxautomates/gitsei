package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.AggTimeQueryHelper.AggTimeQuery;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.StringJoiner;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.database.TicketCategorizationScheme.DEFAULT_TICKET_CATEGORY;
import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.INNER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;

@Log4j2
@Service
public class BaJiraAggsQueryBuilder {

    public static final String DONE_STATUS_CATEGORY = "Done";
    public static final String IN_PROGRESS_STATUS_CATEGORY = "In Progress"; // case sensitive for issues, but upper case for statuses table
    private final JiraIssueQueryBuilder jiraIssueQueryBuilder;
    private final JiraConditionsBuilder jiraConditionsBuilder;
    private final boolean ignoreOuForHistoricalAttribution;

    @lombok.Value
    @Builder
    public static class Query {
        String sql;
        String countSql;
        Map<String, Object> params;
        RowMapper<DbAggregationResult> rowMapper;
    }

    public static Function<String, RowMapper<DbAggregationResult>> FTE_ROW_MAPPER_BUILDER = (keyColumn) -> (rs, rowNumber) -> DbAggregationResult.builder()
            .key(rs.getString(keyColumn))
            .additionalKey("author".equalsIgnoreCase(keyColumn) ? rs.getString("author_id") : null)
            .fte(rs.getFloat("fte"))
            .effort(rs.getLong("effort"))
            .total(rs.getLong("total_effort"))
            .build();
    public static RowMapper<DbAggregationResult> TICKET_CAT_FTE_ROW_MAPPER = FTE_ROW_MAPPER_BUILDER.apply("ticket_category");
    public static RowMapper<DbAggregationResult> ASSIGNEE_FTE_ROW_MAPPER = FTE_ROW_MAPPER_BUILDER.apply("assignee");

    @Autowired
    public BaJiraAggsQueryBuilder(JiraIssueQueryBuilder jiraIssueQueryBuilder,
                                  JiraConditionsBuilder jiraConditionsBuilder,
                                  @Value("${BA_IGNORE_OU_FOR_HISTORICAL_ATTRIBUTION:false}") boolean ignoreOuForHistoricalAttribution) {
        this.jiraIssueQueryBuilder = jiraIssueQueryBuilder;
        this.jiraConditionsBuilder = jiraConditionsBuilder;
        this.ignoreOuForHistoricalAttribution = ignoreOuForHistoricalAttribution;
    }

    public Query buildIssueFTEQuery(String company, JiraIssuesFilter filter, OUConfiguration ouConfig, JiraAcross across, BaJiraOptions baJiraOptions, Calculation calculation,
                                    Integer page, Integer pageSize) throws BadRequestException {
        long currentTime = Instant.now().getEpochSecond();
        Map<String, Object> params = new HashMap<>();

        boolean needsHistoricalAssignees = needsHistoricalAssignees(baJiraOptions);

        String sql;
        RowMapper<DbAggregationResult> rowMapper;
        switch (across) {
            case TREND:
            case ISSUE_CREATED_AT:
            case ISSUE_UPDATED_AT:
            case ISSUE_RESOLVED_AT: {
                String acrossName = across.toString();
                String columnName = across.getAcrossColumnName();
                AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                        .columnName(columnName)
                        .across(acrossName)
                        .interval(filter.getAggInterval())
                        .isBigInt(true)
                        .prefixWithComma(false)
                        .build());
                //  example:  {
                //      "select" : "EXTRACT(EPOCH FROM issue_resolved_at_interval) as issue_resolved_at, CONCAT(date_part('week',issue_resolved_at_interval), '-' ,date_part('year',issue_resolved_at_interval)) as interval",
                //      "group_by" : "issue_resolved_at_interval",
                //      "order_by" : "issue_resolved_at_interval DESC",
                //      "helper_column" : "date_trunc('week',to_timestamp(issue_resolved_at)) as issue_resolved_at_interval",
                //      "interval_key" : "interval"
                //    }
                if (across == JiraAcross.TREND) {
                    filter = filter.toBuilder()
                            .ingestedAt(null) // for trend, we don't want ingested_at
                            .build();
                }
                String fteSql = generateFteSql(company, filter, ouConfig, baJiraOptions, calculation, params, currentTime, aggTimeQuery.getHelperColumn(), aggTimeQuery.getGroupBy());
                sql = "" +
                        "select " + aggTimeQuery.getSelect() + ", fte, effort, total_effort from (\n" +
                        "  select " + aggTimeQuery.getGroupBy() + ", sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        "  ) as interval_table \n" +
                        "  group by " + aggTimeQuery.getGroupBy() + "\n" +
                        "  order by " + aggTimeQuery.getOrderBy() + "\n" +
                        ") as t";
                rowMapper = (rs, rowNumber) -> DbAggregationResult.builder()
                        .key(String.valueOf(rs.getInt(acrossName)))
                        .additionalKey(rs.getString("interval"))
                        .fte(rs.getFloat("fte"))
                        .effort(rs.getLong("effort"))
                        .total(rs.getLong("total_effort"))
                        .build();
                break;
            }
            case ASSIGNEE: {
                String assigneeColumn = getAssigneeColumn(needsHistoricalAssignees);
                String fteSql = generateFteSql(company, filter, ouConfig, baJiraOptions, calculation, params, currentTime, null, assigneeColumn);
                sql = "select " + assigneeColumn + ", sum(effort_for_cat) as effort, sum(total_effort) as total_effort, sum(effort_for_cat) / sum(total_effort) as fte from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by " + assigneeColumn + "\n" +
                        "order by fte desc, " + assigneeColumn + " asc";
                rowMapper = FTE_ROW_MAPPER_BUILDER.apply(assigneeColumn);
                break;
            }
            case TICKET_CATEGORY:
            default: {
                String fteSql = generateFteSql(company, filter, ouConfig, baJiraOptions, calculation, params, currentTime, null, null);
                sql = "select ticket_category, sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by ticket_category\n" +
                        "order by fte desc";
                rowMapper = TICKET_CAT_FTE_ROW_MAPPER;
                break;
            }
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

        return Query.builder()
                .sql(sql)
                .params(params)
                .rowMapper(rowMapper)
                .countSql(countSql)
                .build();
    }

    private String generateFteSql(String company, JiraIssuesFilter filter, OUConfiguration ouConfig, BaJiraOptions baJiraOptions, Calculation calculation, Map<String, Object> params, long currentTime, String helperColumn, String groupByColumn) {
        String ticketCategorySql = generateTicketCategorySql(company, filter, params, currentTime);
        String withSql = "";

        List<String> completedWorkStatusCategories = ListUtils.defaultIfEmpty(baJiraOptions.getCompletedWorkStatusCategories(), List.of(DONE_STATUS_CATEGORY));
        List<String> completedWorkStatuses = ListUtils.emptyIfNull(baJiraOptions.getCompletedWorkStatuses());

        boolean needsHistoricalAssignees = needsHistoricalAssignees(baJiraOptions);
        List<String> historicalAssigneesStatuses = baJiraOptions.getHistoricalAssigneesStatuses();

        // -- conditions for basis
        JiraIssuesFilter basisFilter = generateBasisIssueFilter(filter, completedWorkStatusCategories, completedWorkStatuses);
        String jiraIssuesBasisSql = generateJiraIssuesSql(company, basisFilter, null  /* OU does not impact the basis */, "basis_", params, currentTime, ticketCategorySql, needsHistoricalAssignees, historicalAssigneesStatuses);

        // -- condition for filtered set
        JiraIssuesFilter filteredSetFilter = generateFilteredSetIssueFilter(filter, completedWorkStatusCategories, completedWorkStatuses);
        String jiraIssuesSql = generateJiraIssuesSql(company, filteredSetFilter, ouConfig, "filtered_set_", params, currentTime, ticketCategorySql, needsHistoricalAssignees, historicalAssigneesStatuses);

        // -- ticket time spent
        if (calculation == Calculation.TICKET_TIME_SPENT) {
            withSql += generateTicketTimeSpentSqlWith(baJiraOptions, params);
            jiraIssuesBasisSql = generateTicketTimeSpentSql(jiraIssuesBasisSql);
            jiraIssuesSql = generateTicketTimeSpentSql(jiraIssuesSql);
        }

        // -- select
        String assigneeColumn = needsHistoricalAssignees ? "historical_assignee" : "assignee";
        String effortCalculation = generateEffortCalculationSql(calculation) + " as effort";
        String select = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, "ticket_category", effortCalculation, helperColumn);
        String selectBasis = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, effortCalculation, helperColumn);

        // -- group by
        String groupBy = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, "ticket_category", groupByColumn);
        String groupByBasis = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, groupByColumn);

        // -- join
        String joinCondition = StringJoiner.dedupeStream(Stream.of(assigneeColumn, "ingested_at", groupByColumn))
                .map(str -> String.format("i1.%1$s = i2.%1$s", str))
                .collect(Collectors.joining(" and "));

        return "\n" + withSql +
                "  select i1.*, i1.effort as effort_for_cat, i2.effort as total_effort,\n" +
                "    coalesce(i1.effort::float / nullif(i2.effort, 0), 0)::numeric as fte \n" + // use nullif + coalesce to return 0 when dividing by 0
                "  from ( \n" +
                "    select " + select + "\n" +
                "    from (\n" + jiraIssuesSql + ") as filtered_set\n" +
                "    group by " + groupBy +
                "  ) as i1 \n" +
                "  join ( \n" +
                "    select " + selectBasis + "\n" +
                "    from (\n" + jiraIssuesBasisSql + ") as basis\n" +
                "    group by " + groupByBasis +
                "  ) as i2 \n" +
                "  on " + joinCondition + "\n";
    }

    private boolean needsHistoricalAssignees(BaJiraOptions baJiraOptions) {
        return baJiraOptions.getAttributionMode().equals(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES);
    }

    private String getAssigneeColumn(boolean needsHistoricalAssignees) {
        return needsHistoricalAssignees ? "historical_assignee" : "assignee";
    }

    private JiraIssuesFilter generateBasisIssueFilter(JiraIssuesFilter filter, List<String> completedWorkStatusCategories, List<String> completedWorkStatuses) {
        List<String> basisStatuses;
        List<String> basisStatusCategories;
        if (!completedWorkStatuses.isEmpty()) {
            basisStatuses = completedWorkStatuses;
            basisStatusCategories = null;
        } else {
            basisStatuses = null;
            basisStatusCategories = completedWorkStatusCategories;
        }
        return JiraIssuesFilter.builder()
                .ingestedAt(filter.getIngestedAt())
                .ingestedAtByIntegrationId(filter.getIngestedAtByIntegrationId())
                .isActive(filter.getIsActive())
                .integrationIds(filter.getIntegrationIds())
                .statusCategories(basisStatusCategories)
                .statuses(basisStatuses)
                .issueResolutionRange(filter.getIssueResolutionRange())
                .ticketCategorizationFilters(filter.getTicketCategorizationFilters())
                .build();
    }

    private JiraIssuesFilter generateFilteredSetIssueFilter(JiraIssuesFilter filter, List<String> completedWorkStatusCategories, List<String> completedWorkStatuses) {
        List<String> filteredSetStatuses;
        List<String> filteredSetStatusCategories;
        if (!completedWorkStatuses.isEmpty()) {
            filteredSetStatuses = ListUtils.intersectionIgnoringEmpty(filter.getStatuses(), completedWorkStatuses);
            filteredSetStatusCategories = filter.getStatusCategories();
        } else {
            filteredSetStatuses = filter.getStatuses();
            filteredSetStatusCategories = ListUtils.intersectionIgnoringEmpty(filter.getStatusCategories(), completedWorkStatusCategories);
        }
        return filter.toBuilder()
                .statusCategories(filteredSetStatusCategories)
                .statuses(filteredSetStatuses)
                .build();
    }

    private String generateTicketTimeSpentSqlWith(BaJiraOptions baJiraOptions, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(baJiraOptions.getInProgressStatuses())) {
            conditions.add("upper(status) in (:statuses_agg_statuses)");
            params.put("statuses_agg_statuses", baJiraOptions.getInProgressStatuses().stream()
                    .map(StringUtils::upperCase)
                    .collect(Collectors.toList()));
        } else {
            List<String> statusCategories;
            if (CollectionUtils.isNotEmpty(baJiraOptions.getInProgressStatusCategories())) {
                statusCategories = baJiraOptions.getInProgressStatusCategories().stream()
                        .map(StringUtils::upperCase)
                        .collect(Collectors.toList());
            } else {
                statusCategories = List.of(IN_PROGRESS_STATUS_CATEGORY.toUpperCase());
            }
            conditions.add("upper(status_category) in (:statuses_agg_status_categories)");
            params.put("statuses_agg_status_categories", statusCategories);
        }
        return "" +
                "with statuses_agg as (\n" +
                "  select jis.issue_key, jis.integration_id, sum( greatest(coalesce(end_time,0)-coalesce(start_time,0),0) ) as elapsed\n" +
                "  from ${company}.jira_issue_statuses as jis\n" +
                "  where status in (\n" +
                "    select distinct(status) from ${company}.jira_status_metadata\n" +
                "    where " + String.join(" AND ", conditions) + "\n" +
                "  )\n" +
                "\tgroup by jis.issue_key, jis.integration_id\n" +
                ")";
    }

    private String generateTicketTimeSpentSql(String issueSql) {
        return "" +
                "  select issues.*, statuses_agg.elapsed as elapsed from (\n" +
                "  " + issueSql + "\n" +
                "  ) as issues\n" +
                "  join statuses_agg\n" +
                "  on statuses_agg.issue_key = issues.key and statuses_agg.integration_id = issues.integration_id\n";
    }

    public static String generateEffortCalculationSql(Calculation calculation) {
        switch (calculation) {
            case STORY_POINTS:
                return "coalesce(sum(story_points), 0)";
            case TICKET_COUNT:
                return "count(*)";
            case TICKET_TIME_SPENT:
                return "sum(elapsed)";
            default:
                throw new IllegalArgumentException("Calculation not support: " + calculation);
        }
    }

    public String generateTicketCategorySql(String company, JiraIssuesFilter jiraIssuesFilter, Map<String, Object> params, long currentTime) {
        String ticketCategorySql = jiraIssueQueryBuilder.generateTicketCategorySql(company, params, jiraIssuesFilter, currentTime);
        String ticketCategoryColumn = String.format(" (%s) as ticket_category ", ticketCategorySql);
        log.debug("ticketCategoryColumn={}", ticketCategoryColumn);
        return ticketCategoryColumn;
    }

    public String generateTicketCategoryManyToOneSql(String company, TicketCategorizationScheme ticketCategorizationScheme, String categoriesColumn) {
        List<String> categories = ticketCategorizationScheme.retrieveCategories();
        if (categories.size() == 1) {
            return "'" + DEFAULT_TICKET_CATEGORY + "'";
        }
        return "    ( case\n" + categories.stream()
                .map(category -> String.format("        when '%1$s' = any(%2$s) then '%1$s'", category, categoriesColumn))
                .collect(Collectors.joining("\n")) +
                "        else '" + DEFAULT_TICKET_CATEGORY + "'\n" +
                "      end )";
    }

    // region jira issues query builder
    public String generateJiraIssuesSql(String company, JiraIssuesFilter filter, @Nullable OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, long currentTime, String ticketCategorySql,
                                        boolean needsHistoricalAssignees, @Nullable List<String> historicalAssigneeStatuses) {
        // special handling for assignees filter when using historical assignees
        List<String> originalAssigneesFilter = filter.getAssignees();
        if (needsHistoricalAssignees) {
            // - remove assignees filter
            filter = filter.toBuilder().assignees(null).build();
            // - disable OU assignees
            // NOTE: disabling this behavior by default as per SEI-2044 (see comments)
            if (ouConfig != null && ignoreOuForHistoricalAttribution) {
                ouConfig = ouConfig.toBuilder()
                        .ouExclusions(ListUtils.addIfNotPresent(ouConfig.getOuExclusions(), "assignees"))
                        .build();
            }
        }

        Map<String, List<String>> conditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params, paramPrefix, filter, currentTime, filter.getIngestedAt(), "issues.", ouConfig);

        String historicalAssigneesTableJoin = "";
        String historicalAssigneesSelect = null;
        if (needsHistoricalAssignees) {
            historicalAssigneesTableJoin = generateHistoricalAssigneesJoin(company, ouConfig, paramPrefix, params, historicalAssigneeStatuses, originalAssigneesFilter);
            historicalAssigneesSelect = "historical_assignee";
        }

        String issuesWhere = generateWhereFromCondition(conditions, ISSUES_TABLE);
        String finalTableWhere = generateWhereFromCondition(conditions, FINAL_TABLE);

        String sprintWhere = generateWhereFromCondition(conditions, JIRA_ISSUE_SPRINTS);
        String sprintTableJoin = !JiraIssueReadUtils.isSprintTblJoinRequired(filter) ? "" :
                JiraIssueReadUtils.getSprintJoinStmt(company, filter, INNER_JOIN, sprintWhere, "issues") + "\n";

        String parentIssueKeyJoin = "";
        String parentIssueKeySelect = "";
        if (JiraIssueReadUtils.needParentIssueTypeJoin(filter)) {
            parentIssueKeySelect = "parent_issue_type";
            parentIssueKeyJoin = JiraIssueReadUtils.getParentIssueTypeJoinStmt(company, filter, "issues");
        }

        String select = StringJoiner.dedupeAndJoin(", ", "issues.*", historicalAssigneesSelect, parentIssueKeySelect, ticketCategorySql);

        return "    select * from (\n" +
                "      select " + select + "\n" +
                "      from ${company}.jira_issues as issues \n" +
                sprintTableJoin +
                historicalAssigneesTableJoin +
                parentIssueKeyJoin +
                "      " + issuesWhere + "\n" +
                "    ) as final_table " + finalTableWhere + "\n";
    }

    public static String generateHistoricalAssigneesJoin(String company, OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, @Nullable List<String> historicalAssigneesStatuses, List<String> originalAssigneesFilter) {
        List<String> conditions = new ArrayList<>();
        // if we need to filter assignees by status, we need to join on the statuses table
        String statusJoin = "";
        if (CollectionUtils.isNotEmpty(historicalAssigneesStatuses)) {
            String statusListKey = paramPrefix + "historical_assignees_statuses";
            params.put(statusListKey, historicalAssigneesStatuses);
            statusJoin = "" +
                    "          join ${company}.jira_issue_statuses as s\n" +
                    "          on \n" +
                    "            a.integration_id = s.integration_id and\n" +
                    "            a.issue_key = s.issue_key and\n" +
                    "            ((s.start_time >= a.start_time and s.start_time <= a.end_time) OR\n" +
                    "             (s.end_time >= a.start_time and s.end_time <= a.end_time))\n";
            conditions.add("s.status in (:" + statusListKey + ")");
        }
        // recreate assignee filter logic
        generateHistoricalAssigneesFilter(company, ouConfig, paramPrefix, params, originalAssigneesFilter).ifPresent(conditions::add);

        String where = conditions.isEmpty() ? "" : "          where " + String.join(" and ", conditions) + "\n";
        return "" +
                "        join (\n" +
                "          select a.integration_id, a.issue_key, assignee as historical_assignee\n" +
                "          from ${company}.jira_issue_assignees as a\n" +
                statusJoin +
                where +
                "          group by a.integration_id, a.issue_key, assignee\n" +
                "        ) as historical_assignees\n" +
                "        on issues.integration_id = historical_assignees.integration_id and\n" +
                "           issues.key = historical_assignees.issue_key";
    }

    public static Optional<String> generateHistoricalAssigneesFilter(String company, OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, List<String> originalAssigneesFilter) {
        if (OrgUnitHelper.doesOUConfigHaveJiraAssignees(ouConfig)) { // OU: assignee
            return JiraConditionsBuilder.generateOuAssigneeCondition(company, ouConfig, params, "a.");
        } else if (CollectionUtils.isNotEmpty(originalAssigneesFilter)) {
            List<String> integrationUsersConditions = new ArrayList<>();
            integrationUsersConditions.add("id::text in (:" + paramPrefix + "jira_assignees)");
            params.put(paramPrefix + "jira_assignees", originalAssigneesFilter);
            if (params.containsKey(paramPrefix + "jira_integration_ids")) {
                integrationUsersConditions.add("integration_id in (:" + paramPrefix + "jira_integration_ids)");
            }
            return Optional.of("a.assignee IN (select display_name from ${company}.integration_users " +
                    "where " + String.join(" and ", integrationUsersConditions) + ")");
        }
        return Optional.empty();
    }

    public static String generateWhereFromCondition(Map<String, List<String>> conditions, String table) {
        return conditions.get(table).isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions.get(table));
    }

    //endregion
}
