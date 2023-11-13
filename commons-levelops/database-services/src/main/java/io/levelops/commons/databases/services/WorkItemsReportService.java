package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbStackedAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemTimelineQueryCriteria;
import io.levelops.commons.databases.utils.WorkItemsReportUtil;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import io.levelops.commons.utils.StringJoiner;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria.updateQueryForSortFields;

@Log4j2
@Service
public class WorkItemsReportService {
    private static final String TABLE_NAME = "issue_mgmt_workitems";
    private static final String PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    public static final String STATUS_METADATA = "issue_mgmt_status_metadata";

    public static final List<String> fieldsToExcludeInGroupBy = List.of("rank");
    private static final String WORKITEM_TIMELINES_TBL_QUALIFIER = "timelines.";
    private static final String WORKITEM_TIMELINES_TBL_ALIAS = "timelines";
    private static final String INTEGRATION_TRACKER_TABLE_NAME = "integration_tracker";

    private final NamedParameterJdbcTemplate template;

    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsReportService(DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DbListResponse<DbAggregationResult> generateReport(String company, WorkItemsFilter filter,
                                                              WorkItemsMilestoneFilter milestoneFilter,
                                                              WorkItemsFilter.DISTINCT stack,
                                                              boolean valuesOnly,
                                                              final OUConfiguration ouConfig)
            throws SQLException {
        WorkItemsFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        WorkItemsFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.issue_count;
        }
        //PROP-1269: This block of code is meant to return all statuses from issue_mgmt_status_metadata table for provided integration ids.
        //It will ignore rest of the filters since values endpoint is currently invoked without filters (except integration id filter).
        List<DbAggregationResult> aggResultsFromStatusMetadata = List.of();
        if (valuesOnly && across == WorkItemsFilter.DISTINCT.status) {
            aggResultsFromStatusMetadata = getResultsForAcrossStatusValues(company, filter);
        }
        //endregion
        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            additionalKey = Optional.of("interval");
        } else if (WorkItemsFilter.isAcrossUsers(across)) {
            additionalKey = Optional.of(across.toString());
        }

        List<DbWorkItemField> workItemCustomFields = null;
        if (MapUtils.isNotEmpty(filter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }

        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields,
                stack, null, null, false, ouConfig);
        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(WORKITEM_TIMELINES_TBL_QUALIFIER,WorkItemsTimelineFilter.builder().build(), null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        Query groupByCriteria = WorkItemQueryCriteria.getGroupByCriteria(filter);
        Query calculationCriteria = filter.getCalculation(calculation);
        Query.SortByField orderByField = filter.getOrderBy(filter.getSort(), across, calculation);
        int limit = valuesOnly ? -1 : (filter.getAcrossLimit() != null) ? filter.getAcrossLimit() : 90;

        if (across != WorkItemsFilter.DISTINCT.none && (CollectionUtils.isEmpty(groupByCriteria.getGroupByFields()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }

        boolean needStagesJoin = across == WorkItemsFilter.DISTINCT.stage || stack == WorkItemsFilter.DISTINCT.stage;
        boolean isSprintFilterOn = milestoneFilter.isSpecified();
        boolean needMilestonesJoin = isSprintFilterOn || across == WorkItemsFilter.DISTINCT.sprint
                || stack == WorkItemsFilter.DISTINCT.sprint;
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));
        boolean needParentWorkItemTypeJoin = CollectionUtils.isNotEmpty(filter.getParentWorkItemTypes())
                || CollectionUtils.isNotEmpty(filter.getExcludeParentWorkItemTypes());

        List<Query.SelectField> selectFields = new ArrayList<>();

        selectFields.addAll(selectionCriteria.getSelectFields());
        selectFields.addAll(groupByCriteria.getSelectFields());
        //PROP-1772 : Add all columns instead of * for de-duping
        selectFields.addAll(WorkItemQueryCriteria.getAllWorkItemsFields());

        if(needStagesJoin){
            selectFields.addAll(List.of(Query.selectField("stage_id"),
                    Query.selectField("stage", null)));
        }

        if(needSlaTimeStuff){
            selectFields.addAll(List.of(Query.selectField("solve_sla"),
                    Query.selectField("resp_sla")));
        }
        if(needParentWorkItemTypeJoin){
            selectFields.addAll(List.of(Query.selectField("parent_id"),
                    Query.selectField("parent_workitem_type")));
        }

        Query workItemsFetchQuery = Query.builder().select(selectFields)
                .from(Query.fromField(company + "." + TABLE_NAME))
                .build();

        // --- milestones
        List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
        selectionCriteria.getCriteria().getConditions().addAll(groupByCriteria.getCriteria().getConditions());
        if(needMilestonesJoin){
            selectFields.addAll(milestoneSelectFields.stream().map(f -> f.getAliasOrField())
                    .map(f -> Query.selectField(f))
                    .collect(Collectors.toList()));
        }

        Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(milestoneSelectFields)
                .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
        if (milestoneFilter.getSprintCount() > 0) {
            fetchMilestonesBuilder = fetchMilestonesBuilder
                    .orderBy(Query.sortByField("end_date", "DESC", false))
                    .limit(milestoneFilter.getSprintCount());
        }
        Query fetchMilestones = fetchMilestonesBuilder.build();

        // --- timeline
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(timelineSelectionCriteria.getSelectFields());
        timelineSelectFields.addAll(WorkItemQueryCriteria.getFieldNames(milestoneSelectFields));
        if(needMilestonesJoin){
            timelineSelectFields.add(Query.selectField("latest_ingested_at"));
        }
        Query fetchTimelines = Query.builder().select(timelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, WORKITEM_TIMELINES_TBL_ALIAS))
                .build();

        String integrationTrackerJoinSQL = "";
        if(needMilestonesJoin){
            integrationTrackerJoinSQL = " INNER JOIN "+company+"."+INTEGRATION_TRACKER_TABLE_NAME+" it ON timelines.integration_id = it.integration_id";
        }

        // --- intermediate query with both milestones and timelines
        String intermediateSQL = fetchTimelines.toSql() + integrationTrackerJoinSQL + " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                + "milestones.milestone_integration_id = timelines.integration_id ";
        if(needMilestonesJoin){
            intermediateSQL += "AND timelines.start_date <= to_timestamp(latest_ingested_at) AND timelines.end_date >= to_timestamp(latest_ingested_at) ";
        }

        String workItemsFetchSql = workItemsFetchQuery.toSql() + " AS wi";
        String milestonesSql = "";

        if (needMilestonesJoin) {
            milestonesSql = " INNER JOIN (" + intermediateSQL + ") tmp"
                    + " ON tmp.timeline_workitem_id = wi.workitem_id AND"
                    + " tmp.timeline_integration_id = wi.integration_id ";
            selectionCriteria.getCriteria().getQueryParams().putAll(milestoneQueryConditions.getQueryParams());
        }
        workItemsFetchSql += milestonesSql;

        String stagesSql = "";

        if (needStagesJoin) {
            Query fetchStages = Query.builder()
                    .select(Query.selectField("id", "stage_id"),
                            Query.selectField("integration_id", "stage_integration_id"),
                            Query.selectField("workitem_id", "stage_workitem_id"),
                            Query.selectField("field_value", "stage"),
                            Query.selectField("field_type", "stage_field_type"))
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "stages"))
                    .where(Query.conditions(List.of("field_type IN ('status')")),
                            Query.Condition.AND)
                    .build();
            stagesSql = " INNER JOIN (" + fetchStages.toSql() + ") as stages ON "
                    + "stages.stage_workitem_id = wi.workitem_id AND "
                    + "stages.stage_integration_id = wi.integration_id ";
        }
        workItemsFetchSql += stagesSql;

        workItemsFetchSql += generateParentWorkItemTypeJoinSql(company, filter, needParentWorkItemTypeJoin);

        List<Query.SelectField> finalQuerySelect = getFieldNames(groupByCriteria.getGroupByFields());
        finalQuerySelect.addAll(calculationCriteria.getSelectFields());

        if (needSlaTimeStuff) {
            workItemsFetchQuery.getSelectFields().addAll(List.of(
                    Query.selectField("extract(epoch from(COALESCE(first_comment_at,now())-workitem_created_at))", "resp_time"),
                    Query.selectField("extract(epoch from(COALESCE(workitem_resolved_at,now())-workitem_created_at))", "solve_time")
            ));

            Query prioritySlaQuery = Query.builder()
                    .select(List.of(
                            Query.selectField("solve_sla"),
                            Query.selectField("resp_sla"),
                            Query.selectField("project", "proj"),
                            Query.selectField("workitem_type", "ttype"),
                            Query.selectField("priority", "prio"),
                            Query.selectField("integration_id", "integid")

                    ))
                    .from(Query.fromField(
                            company + "." + PRIORITIES_SLA_TABLE, null))
                    .build();

            if (!needMilestonesJoin) {
                workItemsFetchSql = workItemsFetchQuery.toSql() + " as wi inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                        + " p.proj = wi.project AND p.prio = wi.priority AND p.integid = wi.integration_id AND p.ttype = wi.workitem_type";
            } else {
                workItemsFetchSql = workItemsFetchQuery.toSql() + " as wi inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                        + " p.proj = wi.project AND p.prio = wi.priority AND p.integid = wi.integration_id AND p.ttype = wi.workitem_type"
                        + " INNER JOIN (" + intermediateSQL + ") tmp ON tmp.timeline_workitem_id = wi.workitem_id AND"
                        + " tmp.timeline_integration_id = wi.integration_id ";
            }
        }
        String workItemGroupBy = StringJoiner.dedupeAndJoin(", " , selectFields.stream().map(f ->
                f.getAliasOrField()).filter(f -> !fieldsToExcludeInGroupBy.contains(f)).collect(Collectors.toList()));
        workItemsFetchSql += " group by "+workItemGroupBy;

        if (across == WorkItemsFilter.DISTINCT.sprint &&
                (orderByField.getField().contains("milestone_start_date") ||
                        orderByField.getField().contains("milestone_end_date"))) {
            updateQueryForSortFields(groupByCriteria, orderByField, finalQuerySelect);
        }

        Query acrossQuery = new Query.QueryBuilder().select(finalQuerySelect)
                .from(List.of(new Query.FromField("(" + workItemsFetchSql + ")", "wi")))
                .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(groupByCriteria.getGroupByFields())
                .orderBy(List.of(orderByField))
                .limit(limit)
                .build();

        String acrossQuerySql = acrossQuery.toSql();

        log.info("sql = " + acrossQuerySql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", selectionCriteria.getCriteria().getQueryParams());

        String acrossKey = WorkItemQueryCriteria.getGroupByKey(filter, across, false);
        List<DbAggregationResult> aggResults = template.query(acrossQuerySql,
                selectionCriteria.getCriteria().getQueryParams(),
                DbWorkItemConverters.distinctRowMapper(acrossKey, additionalKey, calculation));

        if (stack != null && stack != across) {
            Optional<String> stackAdditionalKey = Optional.empty();
            if (WorkItemsFilter.isAcrossUsers(stack)) {
                stackAdditionalKey = Optional.of(stack.toString());
            }
            String stackKey = WorkItemQueryCriteria.getGroupByKey(filter, stack, true);
            String stackQuerySql = WorkItemsReportUtil.getStackQuery(acrossQuery, filter, stack);
            List<DbStackedAggregationResult> stackAggResults = template.query(stackQuerySql,
                    acrossQuery.getCriteria().getQueryParams(),
                    DbWorkItemConverters.distinctRowMapperForStacks(stackKey, stackAdditionalKey, acrossKey, additionalKey, calculation));

            aggResults = WorkItemsReportUtil.mergeResult(aggResults, stackAggResults);
        }
        //PROP-1269: This block of code is meant to return all statuses from issue_mgmt_status_metadata table for provided integration ids.
        //sanitizeResultsForAcrossStatusValues: it will sanitize the response for tickets count.
        if (valuesOnly && across == WorkItemsFilter.DISTINCT.status) {
            aggResultsFromStatusMetadata = sanitizeResultsForAcrossStatusValues(aggResults, aggResultsFromStatusMetadata);
            return DbListResponse.of(aggResultsFromStatusMetadata, aggResultsFromStatusMetadata.size());
        }
        //endregion
        return DbListResponse.of(aggResults, aggResults.size());
    }

    public static String generateParentWorkItemTypeJoinSql(String company, WorkItemsFilter filter, boolean needParentWorkItemTypeJoin) {
        if (!needParentWorkItemTypeJoin) {
            return "";
        }

        Query parentWorkItemsQuery = Query.builder()
                .select(
                        Query.selectField("integration_id", "parent_integration_id"),
                        Query.selectField("workitem_id", "parent_id"),
                        Query.selectField("ingested_at", "parent_ingested_at"),
                        Query.selectField("workitem_type", "parent_workitem_type"))
                .from(Query.fromField(company + "." + TABLE_NAME))
                .build();

        return "" +
                " INNER JOIN (" + parentWorkItemsQuery.toSql() + ") AS parent_workitems " +
                " ON parent_integration_id = integration_id " +
                " AND parent_ingested_at = ingested_at " +
                " AND parent_id = parent_workitem_id ";
    }

    private List<DbAggregationResult> sanitizeResultsForAcrossStatusValues(List<DbAggregationResult> aggResults,
                                                                           List<DbAggregationResult> aggResultsFromStatusMetadata) {
        Map<String, DbAggregationResult> map = aggResults.stream()
                .collect(Collectors.toMap(DbAggregationResult::getKey, Function.identity()));
        List<DbAggregationResult> results = new ArrayList<>();
        aggResultsFromStatusMetadata.forEach(dbAggregationResult -> {
            if (map.containsKey(dbAggregationResult.getKey())) {
                results.add(dbAggregationResult.toBuilder().totalTickets(map.get(dbAggregationResult.getKey()).getTotalTickets()).build());
            } else {
                results.add(dbAggregationResult.toBuilder().totalTickets(0L).build());
            }
        });
        return results;
    }

    private List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }

    private List<DbAggregationResult> getResultsForAcrossStatusValues(String company, WorkItemsFilter filter) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            conditions.add("integration_id IN (:workitem_integration_ids)");
            params.put("workitem_integration_ids",
                    filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        String whereClause = "";
        if (conditions.size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        String sql = "SELECT DISTINCT status FROM " + company + "." + STATUS_METADATA + whereClause + " ORDER BY status ASC";
        return template.query(sql, params, DbWorkItemConverters.statusMetadataRowMapperForValues());
    }
}
