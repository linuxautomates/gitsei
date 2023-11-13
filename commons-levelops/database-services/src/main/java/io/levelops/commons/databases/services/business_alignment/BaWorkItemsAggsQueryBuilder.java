package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.Query;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.StringJoiner;
import io.levelops.ingestion.models.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class BaWorkItemsAggsQueryBuilder {

    private static final String WORKITEMS_TABLE = "issue_mgmt_workitems";
    private static final String MILESTONES_TABLE = "issue_mgmt_milestones";
    private static final String SPRINT_MAPPINGS_TABLE = "issue_mgmt_sprint_mappings";
    private static final String TIMELINE_TABLE = "issue_mgmt_workitems_timeline";
    private static final String STATUS_METADATA_TABLE = "issue_mgmt_status_metadata";
    private static final String PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";

    private static final List<Query.SelectField> MILESTONE_SELECT_FIELDS = List.of(
            Query.selectField("integration_id"),
            Query.selectField("parent_field_value"),
            Query.selectField("field_value"),
            Query.selectField("name"),
            Query.selectField("start_date"),
            Query.selectField("completed_at")
    );
    private static final List<Query.SelectField> SPRINT_MAPPING_SELECT_FIELDS = List.of(
            Query.selectField("sm.sprint_id", "sprint_mapping_sprint_id"),
            Query.selectField("sm.workitem_id", "sprint_mapping_workitem_id"),
            Query.selectField("sm.integration_id", "sprint_mapping_integration_id"),
            Query.selectField("smpj.name", "sprint_mapping_name"),
            Query.selectField("smpj.completed_at", "sprint_mapping_completed_at"),
            Query.selectField("smpj.start_date", "sprint_mapping_start_date"),
            Query.selectField("row_to_json(sm)", "sprint_mapping_json")
    );
    public static final List<String> DONE_STATUS_CATEGORIES = List.of("Done", "Resolved", "Closed", "Completed");
    public static final List<String> IN_PROGRESS_STATUS_CATEGORIES = List.of("InProgress", "Doing", "Active");
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    @Autowired
    public BaWorkItemsAggsQueryBuilder(WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    @Value
    @Builder
    public static class WorkItemsAggsQuery {
        String sql;
        String countSql;
        Map<String, Object> params;
        RowMapper<DbAggregationResult> rowMapper;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public enum WorkItemsAcross {
        ASSIGNEE,
        TICKET_CATEGORY,
        TREND("ingested_at"),
        WORKITEM_CREATED_AT,
        WORKITEM_UPDATED_AT,
        WORKITEM_RESOLVED_AT;

        String acrossColumnName;

        public String getAcrossColumnName() {
            return StringUtils.defaultIfEmpty(acrossColumnName, toString());
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @Nullable
        public static WorkItemsAcross fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(WorkItemsAcross.class, value);
        }

        public static WorkItemsAcross fromWorkItemsFilter(WorkItemsFilter filter) {
            WorkItemsFilter.DISTINCT requestAcross = MoreObjects.firstNonNull(filter.getAcross(), WorkItemsFilter.DISTINCT.ticket_category);
            switch (requestAcross) {
                case trend:
                    return TREND;
                case workitem_created_at:
                    return WORKITEM_CREATED_AT;
                case workitem_updated_at:
                    return WORKITEM_UPDATED_AT;
                case workitem_resolved_at:
                    return WORKITEM_RESOLVED_AT;
                case assignee:
                    return ASSIGNEE;
                default:
                case ticket_category:
                    return TICKET_CATEGORY;
            }
        }
    }

    public enum Calculation {
        TICKET_COUNT,
        STORY_POINTS,
        TICKET_TIME_SPENT
    }

    public static Function<String, RowMapper<DbAggregationResult>> FTE_ROW_MAPPER_BUILDER = (keyColumn) -> (rs, rowNumber) -> DbAggregationResult.builder()
            .key(rs.getString(keyColumn))
            .additionalKey("author".equalsIgnoreCase(keyColumn) ? rs.getString("author_id") : null)
            .fte(rs.getFloat("fte"))
            .effort(rs.getLong("effort"))
            .total(rs.getLong("total_effort"))
            .build();
    public static RowMapper<DbAggregationResult> TICKET_CAT_FTE_ROW_MAPPER = FTE_ROW_MAPPER_BUILDER.apply("ticket_category");
    public static BiFunction<String, String, RowMapper<DbAggregationResult>> ASSIGNEE_FTE_ROW_MAPPER = (keyColumn, additionalKey) -> (rs, rowNumber) -> DbAggregationResult.builder()
            .key(rs.getString(keyColumn))
            .additionalKey(rs.getString(additionalKey))
            .fte(rs.getFloat("fte"))
            .effort(rs.getLong("effort"))
            .total(rs.getLong("total_effort"))
            .build();

    public WorkItemsAggsQuery buildWorkItemFTEQuery(String company,
                                                    WorkItemsFilter filter,
                                                    WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                    OUConfiguration ouConfig,
                                                    WorkItemsAcross across,
                                                    BaWorkItemsAggsDatabaseService.BaWorkItemsOptions baWorkItemsOptions,
                                                    Calculation calculation) {
        Map<String, Object> params = new HashMap<>();
        boolean needsHistoricalAssignees = needsHistoricalAssignees(baWorkItemsOptions);
        String sql;
        RowMapper<DbAggregationResult> rowMapper;
        switch (across) {
            case TREND:
            case WORKITEM_CREATED_AT:
            case WORKITEM_UPDATED_AT:
            case WORKITEM_RESOLVED_AT: {
                String acrossName = across.toString();
                String columnName = across.getAcrossColumnName();
                AggTimeQueryHelper.AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                        .columnName(columnName)
                        .across(acrossName)
                        .interval(filter.getAggInterval())
                        .isBigInt(false)
                        .prefixWithComma(false)
                        .build());
                //  example:  {
                //      "select" : "EXTRACT(EPOCH FROM workitem_resolved_at_interval) as workitem_resolved_at, CONCAT(date_part('week',workitem_resolved_at_interval), '-' ,date_part('year',workitem_resolved_at_interval)) as interval",
                //      "group_by" : "workitem_resolved_at_interval",
                //      "order_by" : "workitem_resolved_at_interval DESC",
                //      "helper_column" : "date_trunc('week',to_timestamp(workitem_resolved_at)) as workitem_resolved_at_interval",
                //      "interval_key" : "interval"
                //    }
                if (across == WorkItemsAcross.TREND) {
                    aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                            .columnName(columnName)
                            .across(acrossName)
                            .interval(filter.getAggInterval())
                            .isBigInt(true)
                            .prefixWithComma(false)
                            .build());
                    filter = filter.toBuilder()
                            .ingestedAt(null) // for trend, we don't want ingested_at
                            .build();
                }
                String fteSql = generateFteSql(company, filter, workItemsMilestoneFilter, ouConfig, baWorkItemsOptions, calculation, params, aggTimeQuery.getHelperColumn(), aggTimeQuery.getGroupBy());
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
                String fteSql = generateFteSql(company, filter, workItemsMilestoneFilter, ouConfig, baWorkItemsOptions, calculation, params, null, assigneeColumn);
                sql = "select " + assigneeColumn + ", sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by " + assigneeColumn + "\n" +
                        "order by fte desc";
                if(needsHistoricalAssignees) {
                    rowMapper = FTE_ROW_MAPPER_BUILDER.apply(assigneeColumn);
                }else{
                    rowMapper = ASSIGNEE_FTE_ROW_MAPPER.apply("assignee", "assignee_id");
                }

                break;
            }
            case TICKET_CATEGORY:
            default: {
                String fteSql = generateFteSql(company, filter, workItemsMilestoneFilter, ouConfig, baWorkItemsOptions, calculation, params, null, null);
                sql = "select ticket_category, sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by ticket_category\n" +
                        "order by fte desc";
                rowMapper = TICKET_CAT_FTE_ROW_MAPPER;
                break;
            }
        }

        String limit = filter.getAcrossLimit() != null ? "\nlimit " + filter.getAcrossLimit() + "\n" : "";
        sql += limit;

        return WorkItemsAggsQuery.builder()
                .sql(sql)
                .params(params)
                .rowMapper(rowMapper)
                .build();
    }

    private String generateFteSql(String company,
                                  WorkItemsFilter filter,
                                  WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                  OUConfiguration ouConfig,
                                  BaWorkItemsAggsDatabaseService.BaWorkItemsOptions baWorkItemsOptions,
                                  Calculation calculation,
                                  Map<String, Object> params,
                                  String helperColumn,
                                  String groupByColumn) {
        String withSql = "";

        List<String> completedWorkStatusCategories = ListUtils.defaultIfEmpty(baWorkItemsOptions.getCompletedWorkStatusCategories(), DONE_STATUS_CATEGORIES);
        List<String> completedWorkStatuses = ListUtils.emptyIfNull(baWorkItemsOptions.getCompletedWorkStatuses());

        boolean needsHistoricalAssignees = needsHistoricalAssignees(baWorkItemsOptions);
        List<String> historicalAssigneesStatuses = baWorkItemsOptions.getHistoricalAssigneesStatuses();

        // -- conditions for basis
        WorkItemsFilter basisFilter = generateBasisWorkItemsFilter(filter, completedWorkStatusCategories, completedWorkStatuses);
        String workItemsBasisSql = generateWorkItemsSql(company, basisFilter, workItemsMilestoneFilter, params, "basis_", null, needsHistoricalAssignees, historicalAssigneesStatuses);

        // -- condition for filtered set
        WorkItemsFilter filteredSetFilter = generateFilteredSetIssueFilter(filter, completedWorkStatusCategories, completedWorkStatuses);
        String workItemsSql = generateWorkItemsSql(company, filteredSetFilter, workItemsMilestoneFilter, params, "filtered_set_", ouConfig, needsHistoricalAssignees, historicalAssigneesStatuses);

        // -- ticket time spent
        if (calculation == Calculation.TICKET_TIME_SPENT) {
            withSql += generateTicketTimeSpentSqlWith(baWorkItemsOptions, params);
            workItemsBasisSql = generateTicketTimeSpentSql(workItemsBasisSql);
            workItemsSql = generateTicketTimeSpentSql(workItemsSql);
        }

        // -- select
        String assigneeColumn = needsHistoricalAssignees ? "historical_assignee" : "assignee, assignee_id";

        String joinAttributes = groupByColumn;
        String joinColumn = assigneeColumn;
        if(!needsHistoricalAssignees){
            joinColumn = "assignee";
            joinAttributes = "assignee";
        }

        String effortCalculation = generateEffortCalculationSql(calculation) + " as effort";
        String select = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, "ticket_category", effortCalculation, helperColumn);
        String selectBasis = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, effortCalculation, helperColumn);

        // -- group by
        String groupBy = StringJoiner.dedupeAndJoin(", ", "ingested_at", assigneeColumn, "ticket_category", groupByColumn);
        String groupByBasis = StringJoiner.dedupeAndJoin(", ", "ingested_at",assigneeColumn, groupByColumn);

        // -- join
        String joinCondition = StringJoiner.dedupeStream(Stream.of(joinColumn, "ingested_at", joinAttributes))
                .map(str -> String.format("i1.%1$s = i2.%1$s", str))
                .collect(Collectors.joining(" and "));

        return "\n" + withSql +
                "  select i1.*, i1.effort as effort_for_cat, i2.effort as total_effort,\n" +
                "    coalesce(i1.effort::float / nullif(i2.effort, 0), 0)::numeric as fte \n" + // use nullif + coalesce to return 0 when dividing by 0
                "  from ( \n" +
                "    select " + select + "\n" +
                "    from (\n" + workItemsSql + ") as filtered_set\n" +
                "    group by " + groupBy +
                "  ) as i1 \n" +
                "  join ( \n" +
                "    select " + selectBasis + "\n" +
                "    from (\n" + workItemsBasisSql + ") as basis\n" +
                "    group by " + groupByBasis +
                "  ) as i2 \n" +
                "  on " + joinCondition + "\n";
    }

    private boolean needsHistoricalAssignees(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions baWorkItemsOptions) {
        return baWorkItemsOptions.getAttributionMode().equals(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES);
    }

    private String getAssigneeColumn(boolean needsHistoricalAssignees) {
        return needsHistoricalAssignees ? "historical_assignee" : "assignee, assignee_id";
    }

    private WorkItemsFilter generateBasisWorkItemsFilter(WorkItemsFilter filter, List<String> completedWorkStatusCategories, List<String> completedWorkStatuses) {
        List<String> basisStatuses;
        List<String> basisStatusCategories;
        if (!completedWorkStatuses.isEmpty()) {
            basisStatuses = completedWorkStatuses;
            basisStatusCategories = null;
        } else {
            basisStatuses = null;
            basisStatusCategories = completedWorkStatusCategories;
        }
        return WorkItemsFilter.builder()
                .ingestedAt(filter.getIngestedAt())
                .ingestedAtByIntegrationId(filter.getIngestedAtByIntegrationId())
                .integrationIds(filter.getIntegrationIds())
                .statusCategories(basisStatusCategories)
                .statuses(basisStatuses)
                .workItemResolvedRange(filter.getWorkItemResolvedRange())
                .build();
    }

    private WorkItemsFilter generateFilteredSetIssueFilter(WorkItemsFilter filter, List<String> completedWorkStatusCategories, List<String> completedWorkStatuses) {
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

    private String generateTicketTimeSpentSqlWith(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions baWorkItemsOptions,
                                                  Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(baWorkItemsOptions.getInProgressStatuses())) {
            conditions.add("status in (:statuses_agg_statuses)");
            params.put("statuses_agg_statuses", baWorkItemsOptions.getInProgressStatuses());
        } else {
            List<String> statusCategories;
            if (CollectionUtils.isNotEmpty(baWorkItemsOptions.getInProgressStatusCategories())) {
                statusCategories = baWorkItemsOptions.getInProgressStatusCategories();
            } else {
                statusCategories = IN_PROGRESS_STATUS_CATEGORIES;
            }
            conditions.add("status_category in (:statuses_agg_status_categories)");
            params.put("statuses_agg_status_categories", statusCategories);
        }
        return "" +
                "with statuses_agg as (\n" +
                "  select wit.workitem_id, wit.integration_id, \n" +
                "  sum( greatest(coalesce(extract(epoch FROM end_date),0)-coalesce(extract(epoch FROM start_date),0),0) ) as elapsed\n" +
                "  from ${company}." + TIMELINE_TABLE + " as wit\n" +
                "  where field_type = 'status' and field_value in (\n" +
                "    select distinct(status) from ${company}." + STATUS_METADATA_TABLE + "\n" +
                "    where " + String.join(" AND ", conditions) + "\n" +
                "  )\n" +
                "\tgroup by wit.workitem_id, wit.integration_id\n" +
                ")";
    }

    private String generateTicketTimeSpentSql(String workItemSql) {
        return "" +
                "  select workitems.*, statuses_agg.elapsed as elapsed from (\n" +
                "  " + workItemSql + "\n" +
                "  ) as workitems\n" +
                "  join statuses_agg\n" +
                "  on statuses_agg.workitem_id = workitems.workitem_id and statuses_agg.integration_id = workitems.integration_id\n";
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

    public String generateWorkItemsSql(String company,
                                       WorkItemsFilter filter,
                                       WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                       Map<String, Object> params,
                                       String paramPrefix,
                                       @Nullable OUConfiguration ouConfig,
                                       boolean needsHistoricalAssignees,
                                       @Nullable List<String> historicalAssigneeStatuses) {
        // special handling for assignees filter when using historical assignees
        List<String> originalAssigneesFilter = filter.getAssignees();
        if (needsHistoricalAssignees) {
            // - remove assignees filter
            filter = filter.toBuilder().assignees(null).build();
            // - disable OU assignees
            if (ouConfig != null) {
                ouConfig = ouConfig.toBuilder()
                        .ouExclusions(ListUtils.addIfNotPresent(ouConfig.getOuExclusions(), "assignees"))
                        .build();
            }
        }
        List<DbWorkItemField> workItemCustomFields = null;
        if (MapUtils.isNotEmpty(filter.getCustomFields())) {
            try {
                workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                        null, null, null, null, null, 0,
                        1000).getRecords();
            } catch (SQLException e) {
                log.error("Error while querying workitem field meta table. Reason: " + e.getMessage());
            }
        }
        Query workItemSelectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields, "", paramPrefix, ouConfig);
        String whereClause = "";
        if (CollectionUtils.isNotEmpty(workItemSelectionCriteria.getCriteria().getConditions())) {
            whereClause = " WHERE " + String.join(" AND ", workItemSelectionCriteria.getCriteria().getConditions());
        }

        String historicalAssigneesTableJoin = "";
        if (needsHistoricalAssignees) {
            historicalAssigneesTableJoin = generateHistoricalAssigneesJoin(company, ouConfig, paramPrefix, params, historicalAssigneeStatuses, originalAssigneesFilter);
        }

        String slaTableJoin = "";
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));
        if (needSlaTimeStuff) {
            workItemSelectionCriteria.getSelectFields().addAll(List.of(
                    Query.selectField("extract(epoch from(COALESCE(first_comment_at,now())-workitem_created_at))", "resp_time"),
                    Query.selectField("extract(epoch from(COALESCE(workitem_resolved_at,now())-workitem_created_at))", "solve_time"),
                    Query.selectField("solve_sla",null),
                    Query.selectField("resp_sla",null)
            ));
        }

        if(needSlaTimeStuff) {
            Query prioritySlaQuery = Query.builder()
                    .select(List.of(
                            Query.selectField("solve_sla"),
                            Query.selectField("resp_sla"),
                            Query.selectField("project", "proj"),
                            Query.selectField("workitem_type", "ttype"),
                            Query.selectField("priority", "prio"),
                            Query.selectField("integration_id", "integid")

                    ))
                    .from(Query.fromField(company + "." + PRIORITIES_SLA_TABLE))
                    .build();

            slaTableJoin = " inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                    + " p.proj = workitems.project AND p.prio = workitems.priority AND p.integid = workitems.integration_id AND p.ttype = workitems.workitem_type";
        }

        boolean needMilestonesJoin = workItemsMilestoneFilter.isSpecified();
        String sprintTableJoin = "";
        if (needMilestonesJoin) {
            Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(workItemsMilestoneFilter, paramPrefix);
            Query milestoneQuery = Query.builder().select(MILESTONE_SELECT_FIELDS)
                    .from(Query.fromField(company + "." + MILESTONES_TABLE))
                    .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND).build();
            Query sprintMappingQuery = Query.builder().select(SPRINT_MAPPING_SELECT_FIELDS)
                    .from(Query.fromField(company + "." + SPRINT_MAPPINGS_TABLE))
                    .build();

            workItemSelectionCriteria.getCriteria().getQueryParams().putAll(milestoneQuery.getCriteria().getQueryParams());

            String sprintMappingAndSprintQuery = sprintMappingQuery.toSql() + " as sm INNER JOIN (" + milestoneQuery.toSql() + ") as smpj " +
                    " ON sm.integration_id = smpj.integration_id  AND sm.sprint_id = smpj.parent_field_value || '\\' || smpj.name ";
            sprintTableJoin = " INNER JOIN (" + sprintMappingAndSprintQuery + ") smj " +
                    " ON workitems.integration_id = smj.sprint_mapping_integration_id  AND workitems.workitem_id = smj.sprint_mapping_workitem_id";
        }
        params.putAll(workItemSelectionCriteria.getCriteria().getQueryParams());

        String selectStmt = workItemSelectionCriteria.getSelectStmt(null, workItemSelectionCriteria.getSelectFields());
        if (StringUtils.isEmpty(selectStmt)) {
            selectStmt = "select workitems.*";
        } else {
            selectStmt += ", workitems.*";
        }
        selectStmt += (needsHistoricalAssignees ? ",historical_assignee" : StringUtils.EMPTY);

        return "    select * from (\n" +
                "      " + selectStmt + "\n" +
                "      from ${company}." + WORKITEMS_TABLE + " as workitems \n" +
                "      " + slaTableJoin + sprintTableJoin + historicalAssigneesTableJoin +
                "    ) as final_table \n" +
                whereClause;
    }

    //endregion

    public String generateTicketCategoryManyToOneSql(String company, TicketCategorizationScheme ticketCategorizationScheme, String categoriesColumn) {
        List<String> categories = ticketCategorizationScheme.retrieveCategories(WorkItemQueryCriteria.DEFAULT_TICKET_CATEGORY);
        if (categories.size() == 1) {
            return "'" + WorkItemQueryCriteria.DEFAULT_TICKET_CATEGORY + "'";
        }
        return "    ( case\n" + categories.stream()
                .map(category -> String.format("        when '%1$s' = any(%2$s) then '%1$s'", category, categoriesColumn))
                .collect(Collectors.joining("\n")) +
                "        else '" + WorkItemQueryCriteria.DEFAULT_TICKET_CATEGORY + "'\n" +
                "      end )";
    }

    public static String generateHistoricalAssigneesJoin(String company, OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, @Nullable List<String> historicalAssigneesStatuses, List<String> originalAssigneesFilter) {
        List<String> conditions = new ArrayList<>();
        // if we need to filter assignees by status, we need to join on the statuses table
        String statusJoin = "";
        if (CollectionUtils.isNotEmpty(historicalAssigneesStatuses)) {
            String statusListKey = paramPrefix + "historical_assignees_statuses";
            params.put(statusListKey, historicalAssigneesStatuses);
            statusJoin = "" +
                    "          join ${company}.issue_mgmt_workitems_timeline as s\n" +
                    "          on \n" +
                    "            a.integration_id = s.integration_id and\n" +
                    "            a.workitem_id = s.workitem_id and\n" +
                    "            ((s.start_date >= a.start_date and s.start_date <= a.end_date) OR\n" +
                    "             (s.end_date >= a.start_date and s.end_date <= a.end_date))\n";
            conditions.add("s.field_type = 'status'");
            conditions.add("s.field_value in (:" + statusListKey + ")");
        }
        // recreate assignee filter logic
        generateHistoricalAssigneesFilter(company, ouConfig, paramPrefix, params, originalAssigneesFilter).ifPresent(conditions::add);

        conditions.add("a.field_type = 'assignee'");
        String where = " where " + String.join(" and ", conditions) + "\n";
        return "" +
                "        join (\n" +
                "          select a.integration_id, a.workitem_id, a.field_value as historical_assignee\n" +
                "          from ${company}.issue_mgmt_workitems_timeline as a\n" +
                statusJoin +
                where +
                "          group by a.integration_id, a.workitem_id, a.field_value\n" +
                "        ) as historical_assignees\n" +
                "        on workitems.integration_id = historical_assignees.integration_id and\n" +
                "           workitems.workitem_id = historical_assignees.workitem_id";
    }

    public static Optional<String> generateHistoricalAssigneesFilter(String company, OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, List<String> originalAssigneesFilter) {
        if (OrgUnitHelper.doesOUConfigHaveWorkItemAssignees(ouConfig)) { // OU: assignee
            return generateOuAssigneeCondition(company, ouConfig, params, "a.");
        } else if (CollectionUtils.isNotEmpty(originalAssigneesFilter)) {
            List<String> integrationUsersConditions = new ArrayList<>();
            integrationUsersConditions.add("id::text in (:" + paramPrefix + "wi_assignees)");
            params.put(paramPrefix + "wi_assignees", originalAssigneesFilter);
            if (params.containsKey(paramPrefix + "wi_integration_ids")) {
                integrationUsersConditions.add("integration_id in (:" + paramPrefix + "wi_integration_ids)");
            }
            return Optional.of("a.field_value IN (select display_name from ${company}.integration_users " +
                    "where " + String.join(" and ", integrationUsersConditions) + ")");
        }
        return Optional.empty();
    }

    public static Optional<String> generateOuAssigneeCondition(String company, OUConfiguration ouConfig, Map<String, Object> params, String wiTblQualifier) {
        String usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.AZURE_DEVOPS);
        if (StringUtils.isBlank(usersSelect)) {
            return Optional.empty();
        }
        return Optional.of(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", wiTblQualifier + "field_value", usersSelect));
    }

}
