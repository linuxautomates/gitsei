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
public class WorkItemsAgeReportService {

    private static final String TABLE_NAME = "issue_mgmt_workitems";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    private static final String WORKITEM_TIMELINES_TBL_QUALIFIER = "timelines.";
    private static final String WORKITEM_TIMELINES_TBL_ALIAS = "timelines";
    private static final String INTEGRATION_TRACKER_TABLE_NAME = "integration_tracker";

    public static final List<String> fieldsToExcludeInGroupBy = List.of("rank");

    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsAgeReportService(DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
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
            calculation = WorkItemsFilter.CALCULATION.age;
        }
        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            additionalKey = Optional.of("interval");
        } else if (WorkItemsFilter.isAcrossUsers(across)) {
            additionalKey = Optional.of(across.toString());
        }

        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(WORKITEM_TIMELINES_TBL_QUALIFIER,WorkItemsTimelineFilter.builder().build(), null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();

        List<DbWorkItemField> workItemCustomFields = null;
        if(MapUtils.isNotEmpty(filter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }
        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields, null,null, ouConfig);
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

        List<Query.SelectField> selectFields = new ArrayList<>();
        selectFields.addAll(List.of(
                Query.selectField("(ingested_at-(extract(epoch FROM workitem_created_at)))/86400", "age"),
                Query.selectField("COALESCE(story_points, 0)", "story_point")
        ));
        selectFields.addAll(selectionCriteria.getSelectFields());
        selectFields.addAll(groupByCriteria.getSelectFields());
        //PROP-1772 : Add all columns instead of * for de-duping
        selectFields.addAll(WorkItemQueryCriteria.getAllWorkItemsFields());

        if(needStagesJoin){
            selectFields.addAll(List.of(Query.selectField("stage_id"),
                    Query.selectField("stage", null)));
        }

        List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(timelineSelectionCriteria.getSelectFields());
        timelineSelectFields.addAll(WorkItemQueryCriteria.getFieldNames(milestoneSelectFields));
        if(isSprintFilterOn){
            timelineSelectFields.add(Query.selectField("latest_ingested_at"));
        }

        if(needMilestonesJoin){
            selectFields.addAll(milestoneSelectFields.stream().map(f -> f.getAliasOrField())
                    .map(f -> Query.selectField(f))
                    .collect(Collectors.toList()));
        }

        selectionCriteria.getCriteria().getConditions().addAll(groupByCriteria.getCriteria().getConditions());

        Query workItemsFetchQuery = Query.builder().select(selectFields)
                .from(Query.fromField(company + "." + TABLE_NAME))
                .build();

        Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(milestoneSelectFields)
                .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
        if (milestoneFilter.getSprintCount() > 0) {
            fetchMilestonesBuilder = fetchMilestonesBuilder
                    .orderBy(Query.sortByField("end_date", "DESC", false))
                    .limit(milestoneFilter.getSprintCount());
        }
        Query fetchMilestones = fetchMilestonesBuilder.build();

        Query fetchTimelines = Query.builder().select(timelineSelectFields)
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

        String workItemsFetchSql = workItemsFetchQuery.toSql() + " AS wi";
        String milestonesSql = "";
        if (needMilestonesJoin)
            milestonesSql = " INNER JOIN (" + intermediateSQL + ") tmp"
                    + " ON tmp.timeline_workitem_id = wi.workitem_id AND"
                    + " tmp.timeline_integration_id = wi.integration_id ";
        workItemsFetchSql += milestonesSql;

        String stagesSql = "";
        if (needStagesJoin) {
            List<Query.SelectField> timelineFieldsForStages = new ArrayList<>(
                    List.of(
                            Query.selectField("id", "stage_id"),
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
            stagesSql = " INNER JOIN (" + fetchStages.toSql() + ") as stages ON "
                    + "stages.stage_workitem_id = wi.workitem_id AND "
                    + "stages.stage_integration_id = wi.integration_id ";
        }
        workItemsFetchSql += stagesSql;
        String workItemGroupBy = StringJoiner.dedupeAndJoin(", " , selectFields.stream().map(f ->
                f.getAliasOrField()).filter(f -> !fieldsToExcludeInGroupBy.contains(f)).collect(Collectors.toList()));
        workItemsFetchSql += " group by "+workItemGroupBy;

        List<Query.SelectField> finalQuerySelect = getFieldNames(groupByCriteria.getGroupByFields());
        finalQuerySelect.addAll(calculationCriteria.getSelectFields());

        if (across == WorkItemsFilter.DISTINCT.sprint &&
                (orderByField.getField().contains("milestone_start_date") ||
                        orderByField.getField().contains("milestone_end_date"))) {
            updateQueryForSortFields(groupByCriteria, orderByField, finalQuerySelect);
        }
        Query finalQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField("(" + workItemsFetchSql + ")", "wi"))
                .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(groupByCriteria.getGroupByFields())
                .orderBy(orderByField)
                .limit(limit)
                .build();

        String query = finalQuery.toSql();
        if (needMilestonesJoin)
            selectionCriteria.getCriteria().getQueryParams().putAll(milestoneQueryConditions.getQueryParams());

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", selectionCriteria.getCriteria().getQueryParams());

        String rowKey = WorkItemQueryCriteria.getGroupByKey(filter, across, false);
        List<DbAggregationResult> aggResults = template.query(query, selectionCriteria.getCriteria().getQueryParams(),
                DbWorkItemConverters.distinctRowMapper(rowKey, additionalKey, calculation));

        if(stack != null && stack != across) {
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
