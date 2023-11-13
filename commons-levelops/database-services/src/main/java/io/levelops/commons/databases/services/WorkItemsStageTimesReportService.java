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
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria.updateQueryForSortFields;

@Service
@Log4j2
public class WorkItemsStageTimesReportService {
    private static final String WORKITEMS = "issue_mgmt_workitems";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    private static final String PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";

    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsStageTimesReportService(DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DbListResponse<DbAggregationResult> generateReport(String company, WorkItemsFilter workItemsFilter,
                                                              WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                              WorkItemsTimelineFilter timelineFilter,
                                                              WorkItemsFilter.DISTINCT stack,
                                                              boolean valuesOnly,
                                                              final OUConfiguration ouConfig)
            throws SQLException {
        WorkItemsFilter.DISTINCT across = workItemsFilter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        WorkItemsFilter.CALCULATION calculation = workItemsFilter.getCalculation();
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.stage_times_report;
        }
        if (CollectionUtils.isEmpty(timelineFilter.getFieldTypes())) {
            timelineFilter = timelineFilter.toBuilder().fieldTypes(List.of("status")).build();
        } else {
            timelineFilter.getFieldTypes().add("status");
        }
        boolean needMilestonesJoin = workItemsMilestoneFilter.isSpecified() || across == WorkItemsFilter.DISTINCT.sprint
                || stack == WorkItemsFilter.DISTINCT.sprint;

        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            additionalKey = Optional.of("interval");
        } else if (WorkItemsFilter.isAcrossUsers(across)) {
            additionalKey = Optional.of(across.toString());
        }
        String rowKey;

        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(workItemsMilestoneFilter, null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        List<DbWorkItemField> workItemCustomFields = null;
        if (MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, workItemsFilter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }
        Query workItemSelectionCriteria =
                WorkItemQueryCriteria.getSelectionCriteria(company, workItemsFilter, workItemCustomFields, null, null, ouConfig);
        Query workItemGroupByCriteria =
                WorkItemQueryCriteria.getGroupByCriteria(workItemsFilter);
        Query workItemCalculationCriteria =
                workItemsFilter.getCalculation(calculation);
        Query.SortByField orderByField =
                workItemsFilter.getOrderBy(workItemsFilter.getSort(), across, calculation);
        int limit = valuesOnly ? -1 :
                (workItemsFilter.getAcrossLimit() != null) ? workItemsFilter.getAcrossLimit() : 90;

        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(timelineFilter, null);
        workItemSelectionCriteria.getCriteria().getQueryParams()
                .putAll(timelineSelectionCriteria.getCriteria().getQueryParams());

        if (across == WorkItemsFilter.DISTINCT.status) {
            workItemGroupByCriteria.getGroupByFields().clear();
            workItemGroupByCriteria.getGroupByFields().add(Query.groupByField("state"));
        }

        if (across != WorkItemsFilter.DISTINCT.none && CollectionUtils.isEmpty(workItemGroupByCriteria.getGroupByFields())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }
        List<Query.SelectField> timelineSelectFields = new ArrayList<>();
        timelineSelectFields.add(Query.selectField("extract(epoch from (end_date-start_date))", "time_spent"));
        timelineSelectFields.add(Query.selectField("field_value", "state"));
        timelineSelectFields.addAll(WorkItemTimelineQueryCriteria.getSelectionCriteria(timelineFilter, null).getSelectFields());
        boolean needExcludeTime = CollectionUtils.isNotEmpty(workItemsFilter.getExcludeStages());
        String solveTimeString = "extract(epoch from coalesce(workitem_resolved_at, now())) - extract(epoch from workitem_created_at)";
        String respTimeString = "extract(epoch from coalesce(first_comment_at,now())) - extract(epoch from workitem_created_at)";

        List<Query.SelectField> workSelectFields = new ArrayList<>();
        workSelectFields.add(Query.selectField("*"));
        if(needExcludeTime) {
            workSelectFields.add(Query.selectField("greatest(" + solveTimeString + " - COALESCE(exclude_time, 0), 0)", "solve_time"));
            workSelectFields.add(Query.selectField("greatest(" + respTimeString + " - COALESCE(exclude_time, 0), 0)", "resp_time"));
        } else {
            workSelectFields.add(Query.selectField(solveTimeString, "solve_time"));
            workSelectFields.add(Query.selectField(respTimeString, "resp_time"));
        }

        workSelectFields.addAll(workItemSelectionCriteria.getSelectFields());
        workSelectFields.addAll(workItemGroupByCriteria.getSelectFields());

        Query workItemQuery = Query.builder().select(workSelectFields)
                .from(Query.fromField(company + "." + WORKITEMS))
                .build();

        boolean needSlaTimeStuff = workItemsFilter.getExtraCriteria() != null &&
                (workItemsFilter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || workItemsFilter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));

        Query timelineQuery = Query.builder().select(timelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                .where(timelineSelectionCriteria.getCriteria(), Query.Condition.AND)
                .build();

        workItemSelectionCriteria.getCriteria().getConditions()
                .addAll(workItemGroupByCriteria.getCriteria().getConditions());
        String outerSubQuery;
        String intermediateQuery;
        String slaSubQuery = "";

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

            slaSubQuery = " inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                    + " p.proj = workitems.project AND p.prio = workitems.priority AND p.integid = workitems.integration_id AND p.ttype = workitems.workitem_type";
        }

        if (needMilestonesJoin) {
            List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
            Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(milestoneSelectFields)
                    .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                    .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
            if (workItemsMilestoneFilter.getSprintCount() > 0) {
                fetchMilestonesBuilder = fetchMilestonesBuilder
                        .orderBy(Query.sortByField("end_date", "DESC", false))
                        .limit(workItemsMilestoneFilter.getSprintCount());
            }
            Query fetchMilestones = fetchMilestonesBuilder.build();
            List<Query.SelectField> secondTimelineCriteria = new ArrayList<>();
            secondTimelineCriteria.add(Query.selectField("field_value"));
            secondTimelineCriteria.add(Query.selectField("integration_id", "final_integ_id"));
            secondTimelineCriteria.add(Query.selectField("field_type"));
            secondTimelineCriteria.add(Query.selectField("workitem_id"));

            Query secondJoinTimelineQuery = Query.builder().select(secondTimelineCriteria)
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                    .build();

            intermediateQuery = timelineQuery.toSql() + ") timeline ON timeline.timeline_integration_id " +
                    " = workitems.integration_id AND timeline.timeline_workitem_id = workitems.workitem_id AND" +
                    " timeline.timeline_field_type='sprint' " +
                    " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                    + "milestones.milestone_parent_field_value = split_part(timeline.timeline_field_value, '\\', 1) AND "
                    + "milestones.milestone_name = split_part(timeline.timeline_field_value, '\\', 2) AND "
                    + "milestones.milestone_integration_id = timeline.timeline_integration_id AND milestones.milestone_field_type='sprint' ";

            String secondTimelineJoin = " INNER JOIN (" + secondJoinTimelineQuery.toSql() + ") final ";
            String stagesJoin = getStagesJoin(company);

            intermediateQuery = needSlaTimeStuff ? intermediateQuery + slaSubQuery : intermediateQuery;

            outerSubQuery = "( " + workItemQuery.toSql() + " as workitems " + stagesJoin + " INNER JOIN (" + intermediateQuery + ") tl_ms " + secondTimelineJoin +
                    " ON tl_ms.timeline_integration_id = final.final_integ_id AND" +
                    " tl_ms.timeline_workitem_id = final.workitem_id AND final.field_type = 'status' ";
        } else {
            intermediateQuery = timelineQuery.toSql();
            String stagesJoin = getStagesJoin(company);
            String finalSubquery = workItemQuery.toSql() + " as workitems " + stagesJoin + " INNER JOIN (" + intermediateQuery + ") final " +
                    " ON final.timeline_integration_id = workitems.integration_id AND" +
                    " final.timeline_workitem_id = workitems.workitem_id ";
            finalSubquery = needSlaTimeStuff ? finalSubquery + slaSubQuery : finalSubquery;
            outerSubQuery =  " ( " + finalSubquery + ") a";
        }

        List<Query.SelectField> finalQuerySelect = getFieldNames(workItemGroupByCriteria.getGroupByFields());
        finalQuerySelect.addAll(workItemCalculationCriteria.getSelectFields());
        finalQuerySelect.add(Query.selectField("timeline_field_value", "state"));
        workItemGroupByCriteria.getGroupByFields().add(Query.groupByField("timeline_field_value"));

        if (across == WorkItemsFilter.DISTINCT.sprint &&
                (orderByField.getField().contains("milestone_start_date") ||
                        orderByField.getField().contains("milestone_end_date"))) {
            updateQueryForSortFields(workItemGroupByCriteria, orderByField, finalQuerySelect);
        }

        Query finalQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField(outerSubQuery))
                .where(workItemSelectionCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(workItemGroupByCriteria.getGroupByFields())
                .orderBy(orderByField)
                .limit(limit)
                .build();

        String query = finalQuery.toSql();

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", workItemSelectionCriteria.getCriteria().getQueryParams());
        List<DbAggregationResult> aggResults;

        rowKey = WorkItemQueryCriteria.getGroupByKey(workItemsFilter, across, false);
        if (across == WorkItemsFilter.DISTINCT.status) {
            rowKey = "state";
        }
        if (needMilestonesJoin)
            workItemSelectionCriteria.getCriteria().getQueryParams().putAll(milestoneQueryConditions.getQueryParams());

        aggResults = template.query(query, workItemSelectionCriteria.getCriteria().getQueryParams(),
                DbWorkItemConverters.distinctRowMapper(rowKey, additionalKey, calculation));

        if (stack != null && stack != across) {
            Optional<String> stackAdditionalKey = Optional.empty();
            if (WorkItemsFilter.isAcrossUsers(stack)) {
                stackAdditionalKey = Optional.of(stack.toString());
            }
            String stackKey = WorkItemQueryCriteria.getGroupByKey(workItemsFilter, stack, true);
            finalQuery.setCriteria(workItemSelectionCriteria.getCriteria());
            String stackQuerySql = WorkItemsReportUtil.getStackQuery(finalQuery, workItemsFilter, stack);
            log.info("stackQuerySql = {}", stackQuerySql);
            List<DbStackedAggregationResult> stackAggResults = template.query(stackQuerySql,
                    finalQuery.getCriteria().getQueryParams(),
                    DbWorkItemConverters.distinctRowMapperForStacks(stackKey, stackAdditionalKey, rowKey, additionalKey, calculation));

            aggResults = WorkItemsReportUtil.mergeResult(aggResults, stackAggResults);
        }

        return DbListResponse.of(aggResults, aggResults.size());
    }

    @NotNull
    private String getStagesJoin(String company) {
        List<Query.SelectField> timelineFieldsForStages = new ArrayList<>(
                List.of(
                        Query.selectField("integration_id", "stage_integration_id"),
                        Query.selectField("workitem_id", "stage_workitem_id"),
                        Query.selectField("field_value", "stage"),
                        Query.selectField("field_type", "stage_field_type")
                ));
        Query fetchStages = Query.builder().select(timelineFieldsForStages)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "stages"))
                .where(Query.conditions(List.of("field_type IN ('status')")), Query.Condition.AND)
                .build();
        return " INNER JOIN (" + fetchStages.toSql() + ") as stages ON "
                + "stages.stage_workitem_id = workitems.workitem_id AND "
                + "stages.stage_integration_id = workitems.integration_id ";
    }

    private List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(
                Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }
}
