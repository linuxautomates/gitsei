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
import java.util.stream.Collectors;

import static io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria.updateQueryForSortFields;

@Log4j2
@Service
public class WorkItemsStageBounceReportService {

    private static final String TABLE_NAME = "issue_mgmt_workitems";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    private static final String WORKITEM_TIMELINES_TBL_QUALIFIER = "timelines.";
    private static final String WORKITEM_TIMELINES_TBL_ALIAS = "timelines";
    private static final String INTEGRATION_TRACKER_TABLE_NAME = "integration_tracker";

    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsStageBounceReportService(DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
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
            calculation = WorkItemsFilter.CALCULATION.stage_bounce_report;
        }
        if (CollectionUtils.isEmpty(filter.getStages()) && CollectionUtils.isEmpty(filter.getExcludeStages())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Stage Bounce Report must need stages filter. Missing or empty value of 'workitem_stages'.");
        }
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
        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields, null, null, ouConfig);
        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(WORKITEM_TIMELINES_TBL_QUALIFIER,WorkItemsTimelineFilter.builder().build(), null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        Query groupByCriteria = WorkItemQueryCriteria.getGroupByCriteria(filter);
        groupByCriteria.getGroupByFields().add(Query.groupByField("stage"));
        Query calculationCriteria = filter.getCalculation(calculation);
        Query.SortByField orderByField = filter.getOrderBy(filter.getSort(), across, calculation);
        int limit = valuesOnly ? -1 : (filter.getAcrossLimit() != null) ? filter.getAcrossLimit() : 90;

        if (across != WorkItemsFilter.DISTINCT.none && (CollectionUtils.isEmpty(groupByCriteria.getGroupByFields()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }
        boolean isSprintFilterOn = milestoneFilter.isSpecified();
        boolean needMilestonesJoin = isSprintFilterOn || across == WorkItemsFilter.DISTINCT.sprint
                || stack == WorkItemsFilter.DISTINCT.sprint;

        List<Query.SelectField> selectFields = new ArrayList<>();
        selectFields.addAll(List.of(
                Query.selectField("*")
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
        Query timelineQueryCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                WorkItemsTimelineFilter.builder()
                        .fieldTypes(List.of("status"))
                        .fieldValues(filter.getStages())
                        .excludeFieldValues(filter.getExcludeStages())
                        .build(), "timeline_");
        selectionCriteria.getCriteria().getQueryParams().putAll(timelineQueryCriteria.getCriteria().getQueryParams());
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(
                List.of(
                        Query.selectField("integration_id", "timeline_integration_id"),
                        Query.selectField("workitem_id", "timeline_workitem_id"),
                        Query.selectField("field_value", "stage"),
                        Query.selectField("count(*)", "count")
                ));
        List<Query.GroupByField> timelineGroupByFields = new ArrayList<>(
                List.of(
                        Query.groupByField("timeline_integration_id"),
                        Query.groupByField("timeline_workitem_id"),
                        Query.groupByField("stage")
                ));
        Query timelineQuery = Query.builder()
                .select(timelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                .where(timelineQueryCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(timelineGroupByFields)
                .build();

        finalSql = workItemsFetchQuery.toSql() + " AS wi INNER JOIN ( " + timelineQuery.toSql() + " ) wt ON "
                + " wi.integration_id = wt.timeline_integration_id AND wi.workitem_id = wt.timeline_workitem_id ";

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

            String intermediateSQL = fetchTimelines.toSql() + integrationTrackerJoinSQL + " INNER JOIN (" + fetchMilestones.toSql() + ") AS milestones ON "
                    + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                    + "milestones.milestone_integration_id = timelines.integration_id ";

            boolean isCurrentSprintFilterOn = CollectionUtils.emptyIfNull(milestoneFilter.getStates()).contains("current");

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
        Query finalQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField("(" + finalSql + ")", "final"))
                .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(groupByCriteria.getGroupByFields())
                .orderBy(orderByField)
                .limit(limit)
                .build();
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
            aggResults = mergeResult(aggResults, stackAggResults);
        }
        return DbListResponse.of(aggResults, aggResults.size());
    }

    public static List<DbAggregationResult> mergeResult(List<DbAggregationResult> aggResults,
                                                        List<DbStackedAggregationResult> stackAggResults) {
        Map<String, List<DbAggregationResult>> acrossKeyToStacks = new HashMap<>();
        for (DbStackedAggregationResult stackAggResult : stackAggResults) {
            if (stackAggResult.getStackedAggResult() != null) {
                String key = String.join("_", stackAggResult.getRowKey(), stackAggResult.getRowAdditionalKey(),
                        stackAggResult.getStackedAggResult().getStage());
                if (acrossKeyToStacks.containsKey(key)) {
                    List<DbAggregationResult> dbStackedAggregationResults = new ArrayList<>(acrossKeyToStacks.get(key));
                    dbStackedAggregationResults.add(stackAggResult.getStackedAggResult());
                    acrossKeyToStacks.put(key, dbStackedAggregationResults);
                } else {
                    acrossKeyToStacks.put(key, List.of(stackAggResult.getStackedAggResult()));
                }
            }
        }
        List<DbAggregationResult> results = new ArrayList<>();
        aggResults.forEach(aggResult ->
                results.add(aggResult.toBuilder().stacks(acrossKeyToStacks.getOrDefault(String.join("_",
                        aggResult.getKey(), aggResult.getAdditionalKey(), aggResult.getStage()), null)).build()));
        return results;
    }

    private List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();
        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }
}
