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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

import static io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria.updateQueryForSortFields;

@Log4j2
@Service
public class WorkItemsResolutionTimeReportService {
    private static final String TABLE_NAME = "issue_mgmt_workitems";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";
    private static final String WORKITEM_TIMELINES_TBL_QUALIFIER = "timelines.";
    private static final String WORKITEM_TIMELINES_TBL_ALIAS = "timelines";
    private static final String INTEGRATION_TRACKER_TABLE_NAME = "integration_tracker";

    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsResolutionTimeReportService(DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
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
        List<String> metrics = new ArrayList<>();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        WorkItemsFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.resolution_time;
        }
        if (WorkItemsFilter.CALCULATION.resolution_time.equals(calculation) && ouConfig != null &&
                ouConfig.getRequest() != null &&
                ouConfig.getRequest().getFilter() != null &&
                !ouConfig.getRequest().getFilter().isEmpty() &&
                ouConfig.getRequest().getFilter().containsKey("metric") &&
                ouConfig.getRequest().getFilter().get("metric") instanceof List<?> &&
                !((List<?>) ouConfig.getRequest().getFilter().get("metric")).isEmpty()) {
            metrics = (List<String>) ouConfig.getRequest().getFilter().get("metric");
        }
        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            additionalKey = Optional.of("interval");
        } else if (WorkItemsFilter.isAcrossUsers(across)) {
            additionalKey = Optional.of(across.toString());
        }

        List<DbWorkItemField> workItemCustomFields = null;
        if(MapUtils.isNotEmpty(filter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }
        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields,
                null, null, ouConfig);
        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(WORKITEM_TIMELINES_TBL_QUALIFIER,WorkItemsTimelineFilter.builder().build(), null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        Query groupByCriteria = WorkItemQueryCriteria.getGroupByCriteria(filter);
        Query calculationCriteria = filter.getCalculation(calculation);
        Query.SortByField orderByField = filter.getOrderBy(metrics, filter.getCustomAcross(), filter.getSort(), across, calculation);
        int limit = valuesOnly ? -1 : (filter.getAcrossLimit() != null) ? filter.getAcrossLimit() : 90;

        if (across != WorkItemsFilter.DISTINCT.none && (CollectionUtils.isEmpty(groupByCriteria.getGroupByFields()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }
        boolean needStagesJoin = across == WorkItemsFilter.DISTINCT.stage || stack == WorkItemsFilter.DISTINCT.stage;
        boolean isSprintFilterOn = milestoneFilter.isSpecified();
        boolean needMilestonesJoin = isSprintFilterOn || across == WorkItemsFilter.DISTINCT.sprint
                || stack == WorkItemsFilter.DISTINCT.sprint;

        boolean needExcludeTime = CollectionUtils.isNotEmpty(filter.getExcludeStages());
        String solveTimeString = "extract(epoch from coalesce(workitem_resolved_at, now())) - extract(epoch from  workitem_created_at)";

        List<Query.SelectField> selectFields = new ArrayList<>();
        selectFields.addAll(List.of(
                Query.selectField("*"),
                needExcludeTime
                        ? Query.selectField("greatest(" + solveTimeString + " - COALESCE(exclude_time, 0), 0)", "solve_time")
                        : Query.selectField(solveTimeString, "solve_time")
        ));
        selectFields.addAll(selectionCriteria.getSelectFields());
        selectFields.addAll(groupByCriteria.getSelectFields());
        selectionCriteria.getCriteria().getConditions().addAll(groupByCriteria.getCriteria().getConditions());

        Query workItemsFetchQuery = Query.builder().select(selectFields)
                .from(Query.fromField(company + "." + TABLE_NAME))
                .build();

        List<Query.SelectField> finalQuerySelect = getFieldNames(groupByCriteria.getGroupByFields());
        finalQuerySelect.addAll(calculationCriteria.getSelectFields());

        String finalSql;
        String workItemsFetchQuerySql = workItemsFetchQuery.toSql();
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));
        if (needSlaTimeStuff) {
            workItemsFetchQuery.getSelectFields().addAll(List.of(
                    Query.selectField("extract(epoch from(COALESCE(first_comment_at,now())-workitem_created_at))", "resp_time")
            ));
        }

        if (needExcludeTime) {
            Query timelineQueryCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                    WorkItemsTimelineFilter.builder()
                            .fieldTypes(List.of("status"))
                            .fieldValues(filter.getExcludeStages())
                            .build(), "timeline_");
            selectionCriteria.getCriteria().getQueryParams().putAll(timelineQueryCriteria.getCriteria().getQueryParams());
            List<Query.SelectField> timelineSelectFields = new ArrayList<>(
                    List.of(
                            Query.selectField("integration_id", "timeline_integration_id"),
                            Query.selectField("workitem_id", "timeline_workitem_id"),
                            Query.selectField("COALESCE( SUM(EXTRACT(EPOCH FROM COALESCE(end_date, now())) - EXTRACT(EPOCH FROM start_date)) , 0)",
                                    "exclude_time")
                    ));
            List<Query.GroupByField> timelineGroupByFields = new ArrayList<>(
                    List.of(
                            Query.groupByField("timeline_integration_id"),
                            Query.groupByField("timeline_workitem_id")
                    ));
            Query timelineQuery = Query.builder().select(timelineSelectFields)
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                    .where(timelineQueryCriteria.getCriteria(), Query.Condition.AND)
                    .groupBy(timelineGroupByFields)
                    .build();
            finalSql = workItemsFetchQuery.toSql() + " as wi left join ( " + timelineQuery.toSql() + " ) wt ON "
                    + " wi.integration_id = wt.timeline_integration_id AND wi.workitem_id = wt.timeline_workitem_id ";
        } else {
            finalSql = workItemsFetchQuery.toSql() + " as wi";
        }

        if (needStagesJoin) {
            List<Query.SelectField> timelineFieldsForStages = new ArrayList<>(
                    List.of(
                            Query.selectField("integration_id", "stage_integration_id"),
                            Query.selectField("workitem_id", "stage_workitem_id"),
                            Query.selectField("field_value", "stage"),
                            Query.selectField("field_type", "stage_field_type")
                    ));
            Query fetchStages = Query.builder().select(timelineFieldsForStages)
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "stages"))
                    .where(Query.conditions(List.of("field_type IN ('status')")),
                            Query.Condition.AND)
                    .build();
            finalSql += " INNER JOIN (" + fetchStages.toSql() + ") as stages ON "
                    + "stages.stage_workitem_id = wi.workitem_id AND "
                    + "stages.stage_integration_id = wi.integration_id ";
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
                    .from(Query.fromField(
                            company + "." + PRIORITIES_SLA_TABLE, null))
                    .build();

            finalSql += " inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                    + " p.proj = wi.project AND p.prio = wi.priority AND p.integid = wi.integration_id AND p.ttype = wi.workitem_type";
        }

        if (needMilestonesJoin) {
            Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(milestoneSelectionCriteria.getSelectFields())
                    .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                    .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
            if (milestoneFilter.getSprintCount() > 0) {
                fetchMilestonesBuilder = fetchMilestonesBuilder
                        .orderBy(Query.sortByField("end_date", "DESC", false))
                        .limit(milestoneFilter.getSprintCount());
            }
            Query fetchMilestones = fetchMilestonesBuilder.build();

            timelineSelectionCriteria.getSelectFields().addAll(WorkItemQueryCriteria.getFieldNames(milestoneSelectionCriteria.getSelectFields()));
            if(isSprintFilterOn){
                timelineSelectionCriteria.getSelectFields().add(Query.selectField("latest_ingested_at"));
            }
            Query fetchTimelines = Query.builder().select(timelineSelectionCriteria.getSelectFields())
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "timelines"))
                    .build();

            String integrationTrackerJoinSQL = "";
            if(isSprintFilterOn){
                integrationTrackerJoinSQL = " INNER JOIN "+company+"."+INTEGRATION_TRACKER_TABLE_NAME+" it ON timelines.integration_id = it.integration_id";
            }

            String intermediateSQL = fetchTimelines.toSql() + integrationTrackerJoinSQL + " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                    + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                    + "milestones.milestone_integration_id = timelines.integration_id ";

            if(isSprintFilterOn){
                intermediateSQL += "AND timelines.start_date <= to_timestamp(latest_ingested_at) AND timelines.end_date >= to_timestamp(latest_ingested_at) ";
            }

            finalSql = "(" + finalSql + ") fwi INNER JOIN (" + intermediateSQL + ") tmp"
                    + " ON tmp.timeline_workitem_id = fwi.workitem_id AND"
                    + " tmp.timeline_integration_id = fwi.integration_id ";

            selectionCriteria.getCriteria().getQueryParams().putAll(milestoneQueryConditions.getQueryParams());
        }

        if (across == WorkItemsFilter.DISTINCT.sprint &&
                (orderByField.getField().contains("milestone_start_date") ||
                        orderByField.getField().contains("milestone_end_date"))) {
            updateQueryForSortFields(groupByCriteria, orderByField, finalQuerySelect);
        }

        // PROP-1718 (also LEV-4993)
        // If workItemsFetchSql is joining with other table, contrary to Jira where we use group by in the inner query,
        // we still need to dedupe the work items. Here we are using "select distinct on".
        // No where condition must be set on the outer, the filtering must happen before deduping!
        Query dedupeQuery = Query.builder()
                .select(Query.selectField("*"))
                .distinctOn("integration_id", "workitem_id")
                .from(Query.fromField("(" + finalSql + ")", "final"))
                .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                .build();

        Query finalQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField("(" + dedupeQuery.toSql() + ")", "dedupe"))
                .groupBy(groupByCriteria.getGroupByFields())
                .orderBy(orderByField)
                .limit(limit)
                .build();
        // copying the query params over from the inner dedupe query, as this is re-used by the stacks
        finalQuery.setCriteria(Query.conditions(new ArrayList<>(), dedupeQuery.getCriteria().getQueryParams()));

        String query = finalQuery.toSql();

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", selectionCriteria.getCriteria().getQueryParams());

        String rowKey = WorkItemQueryCriteria.getGroupByKey(filter, across, false);
        List<DbAggregationResult> aggResults = template.query(query, selectionCriteria.getCriteria().getQueryParams(),
                DbWorkItemConverters.distinctRowMapper(rowKey, additionalKey, calculation));

        if (stack != null && stack != across) {
            Optional<String> stackAdditionalKey = Optional.empty();
            if (WorkItemsFilter.isAcrossUsers(stack)) {
                stackAdditionalKey = Optional.of(stack.toString());
            }
            String stackKey = WorkItemQueryCriteria.getGroupByKey(filter, stack, true);
            String stackQuerySql = WorkItemsReportUtil.getStackQuery(finalQuery, filter, stack);
            List<DbStackedAggregationResult> stackAggResults = template.query(stackQuerySql,
                    finalQuery.getCriteria().getQueryParams(),
                    DbWorkItemConverters.distinctRowMapperForStacks(stackKey, stackAdditionalKey, rowKey, additionalKey, calculation));

            aggResults = WorkItemsReportUtil.mergeResult(aggResults, stackAggResults);
        }

        return DbListResponse.of(aggResults, aggResults.size());
    }

    private List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }
}
