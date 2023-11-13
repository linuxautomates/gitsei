package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemTimelineQueryCriteria;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class WorkItemsFirstAssigneeReportService {

    private static final String TABLE_NAME = "issue_mgmt_workitems";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";

    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsFirstAssigneeReportService(DataSource dataSource,
                                               WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DbListResponse<DbAggregationResult> generateReport(String company, WorkItemsFilter filter,
                                                              WorkItemsTimelineFilter workItemsTimelineFilter,
                                                              WorkItemsMilestoneFilter milestoneFilter,
                                                              boolean valuesOnly,
                                                              final OUConfiguration ouConfig) throws SQLException {

        List<Query.SelectField> selectTimelineFields = new ArrayList<>(
                List.of(
                        Query.selectField("DISTINCT ON (integration_id, workitem_id) integration_id ", "tl_integration_id"),
                        Query.selectField("workitem_id ", "tl_workitem_id"),
                        Query.selectField("start_date "),
                        Query.selectField("field_value", "first_assignee"))
        );

        List<Query.SelectField> selectWorkItemFields = new ArrayList<>();
        selectWorkItemFields.add(Query.selectField("*"));
        Query groupByCriteria = WorkItemQueryCriteria.getGroupByCriteria(filter);

        WorkItemsFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");

        WorkItemsFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.assign_to_resolve;
        }
        Query.SortByField orderByField = filter.getOrderBy(filter.getSort(), across, calculation);
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

        Query calculationCriteria = filter.getCalculation(calculation);
        int limit = valuesOnly ? -1 : (filter.getAcrossLimit() != null) ? filter.getAcrossLimit() : 90;

        boolean needStagesJoin = across == WorkItemsFilter.DISTINCT.stage;
        boolean needMilestonesJoin = milestoneFilter.isSpecified() ||
                across == WorkItemsFilter.DISTINCT.sprint;

        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company,
                filter, workItemCustomFields, null, null, ouConfig);
        selectWorkItemFields.addAll(selectionCriteria.getSelectFields());
        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(workItemsTimelineFilter, null);

        selectWorkItemFields.addAll(groupByCriteria.getSelectFields());

        timelineSelectionCriteria.getCriteria().getConditions().add("field_value NOT IN (:exclude_field_values)");
        timelineSelectionCriteria.getCriteria().getQueryParams().put("exclude_field_values", "UNASSIGNED");
        timelineSelectionCriteria.getCriteria().getConditions().add("field_type IN (:fieldTypes)");
        timelineSelectionCriteria.getCriteria().getQueryParams().put("fieldTypes", "assignee");

        selectionCriteria.getCriteria().getQueryParams().putAll(timelineSelectionCriteria.getCriteria().getQueryParams());

        List<Query.SelectField> finalQuerySelect = getFieldNames(groupByCriteria.getGroupByFields());
        finalQuerySelect.addAll(calculationCriteria.getSelectFields());

        Query finalQuery;

        Query distinctOnTimelines = Query.builder()
                .select(selectTimelineFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, ""))
                .build();

        List<String> firstAssigneeSelectStatmenets = selectionCriteria.getCriteria().getConditions().stream().filter(
                whereClauses -> whereClauses.contains("first_assignee")
        ).collect(Collectors.toList());
        selectionCriteria.getCriteria().getConditions().removeIf(whereClauses -> whereClauses.contains("first_assignee"));

        String whereClauseForFirstAssignees = "";
        if (CollectionUtils.isNotEmpty(firstAssigneeSelectStatmenets)) {
            whereClauseForFirstAssignees = " WHERE " + String.join(" AND ", firstAssigneeSelectStatmenets);
        }

        if (needMilestonesJoin) {

            selectTimelineFields.add(Query.selectField("field_type ", "timeilne_field_type"));
            distinctOnTimelines = Query.builder()
                    .select(selectTimelineFields)
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, ""))
                    .build();

            Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
            Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
            selectionCriteria.getCriteria().getQueryParams().putAll(milestoneQueryConditions.getQueryParams());

            List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());

            Query.QueryBuilder fetchMilestonesBuilder = Query.builder()
                    .select(milestoneSelectFields)
                    .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                    .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);

            Query fetchWorkItemsBuilder = Query.builder()
                    .select(Query.selectField("*"))
                    .from(Query.fromField(company + "." + TABLE_NAME, "workitems"))
                    .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                    .build();

            if (milestoneFilter.getSprintCount() > 0) {
                fetchMilestonesBuilder = fetchMilestonesBuilder
                        .orderBy(Query.sortByField("end_date", "DESC", false))
                        .limit(milestoneFilter.getSprintCount());
            }
            Query fetchMilestones = fetchMilestonesBuilder.build();

            String whereClauseForTimelines = "";
            if (CollectionUtils.isNotEmpty(selectionCriteria.getCriteria().getConditions())) {
                whereClauseForTimelines = " WHERE " + String.join(" AND ", timelineSelectionCriteria.getCriteria().getConditions());
            }

            List<Query.SelectField> timelineQuerySelect = getFieldNames(groupByCriteria.getGroupByFields());
            timelineQuerySelect.add(Query.selectField("(EXTRACT(epoch FROM COALESCE(workitem_resolved_at,now())-start_date))", "assign"));

            List<Query.SelectField> milestoneQuerySelect = new ArrayList<>(List.of(
                    Query.selectField("milestone_full_name"),
                    Query.selectField("mt.workitem_id"),
                    Query.selectField("mt.integration_id"),
                    Query.selectField("start_date")
            ));
            if (groupByCriteria.getGroupByFields().get(0).getField().equals("first_assignee")) {
                milestoneQuerySelect.add(Query.selectField("first_assignee"));
            }

            String milestoneTimelineQuery = " (SELECT milestones.milestone_full_name, workitem_id, milestones.milestone_integration_id as integration_id " +
                    " FROM (SELECT * FROM " + company +".issue_mgmt_workitems_timeline) " +
                    "timelines INNER JOIN (" + fetchMilestones.toSql() + ")  milestones ON" +
                    " timelines.integration_id = milestones.milestone_integration_id AND" +
                    " timelines.field_value = milestones.milestone_full_name)";
            Query milestoneTimelineSqlQuery = Query.builder()
                    .select(milestoneQuerySelect)
                    .from(Query.fromField( milestoneTimelineQuery, "mt"))
                    .build();

            String secondJoinOnTimelineQuery = milestoneTimelineSqlQuery.toSql() + " INNER JOIN ( SELECT * FROM (" + distinctOnTimelines.toSql() +
                    whereClauseForTimelines + "ORDER  BY tl_integration_id, workitem_id, start_date ASC) timelines "  + whereClauseForFirstAssignees +") t" +
                    " ON mt.workitem_id = t.tl_workitem_id and mt.integration_id = t.tl_integration_id";

            Query secondJoinOnTimelineSqlQuery = Query.builder()
                    .select(timelineQuerySelect)
                    .from(Query.fromField("(" + secondJoinOnTimelineQuery+ ")", "mtt"))
                    .build();

            String finalJoinOnWorkitemsQuery = secondJoinOnTimelineSqlQuery.toSql() + " INNER JOIN (" + fetchWorkItemsBuilder.toSql() + ") " +
                    "workitems  ON mtt.workitem_id = workitems.workitem_id" +
                    " AND mtt.integration_id = workitems.integration_id";

            if (needStagesJoin) {
                finalJoinOnWorkitemsQuery += getStagesJoin(company);
            }

            finalQuery  = Query.builder()
                    .select(finalQuerySelect)
                    .from(Query.fromField("(" + finalJoinOnWorkitemsQuery + ")", "tii"))
                    .groupBy(groupByCriteria.getGroupByFields())
                    .orderBy(orderByField)
                    .build();

        } else {

            selectWorkItemFields.add(Query.selectField("integration_id", "wi_integration_id"));

            Query workItemsFetchQuery = Query.builder()
                    .select(selectWorkItemFields)
                    .from(Query.fromField(company + "." + TABLE_NAME))
                    .build();

            String whereClauseForWorkitems = "";
            if (CollectionUtils.isNotEmpty(selectionCriteria.getCriteria().getConditions())) {
                whereClauseForWorkitems = " WHERE " + String.join(" AND ", selectionCriteria.getCriteria().getConditions());
            }
            String whereClauseForTimelines = "";
            if (CollectionUtils.isNotEmpty(selectionCriteria.getCriteria().getConditions())) {
                whereClauseForTimelines = " WHERE " + String.join(" AND ", timelineSelectionCriteria.getCriteria().getConditions());
            }

            String workItemsAndTimelinesJoin = " (" + workItemsFetchQuery.toSql() + whereClauseForWorkitems + ") workitems INNER JOIN ( SELECT * FROM ("
                    + distinctOnTimelines.toSql() + whereClauseForTimelines + " ORDER BY tl_integration_id, workitem_id, start_date ASC) timelines " + whereClauseForFirstAssignees +") t"
                    + " ON t.tl_workitem_id = workitems.workitem_id AND"
                    + " t.tl_integration_id = workitems.integration_id ";

            if (needStagesJoin) {
                workItemsAndTimelinesJoin += getStagesJoin(company);
            }

            String finalSubQuery = Query.builder()
                    .select(List.of(
                            Query.selectField("*"),
                            Query.selectField("(EXTRACT(epoch FROM COALESCE(tii.workitem_resolved_at,now())-tii.start_date))", "assign")
                    ))
                    .from(Query.fromField("(" + workItemsAndTimelinesJoin + ")", "tii"))
                    .build().toSql();


            finalQuery = Query.builder()
                    .select(finalQuerySelect)
                    .from(Query.fromField("(" + finalSubQuery + ")", "tiii"))
                    .groupBy(groupByCriteria.getGroupByFields())
                    .orderBy(orderByField)
                    .limit(limit)
                    .build();
        }
        String query = finalQuery.toSql();

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", selectionCriteria.getCriteria().getQueryParams());

        String rowKey = WorkItemQueryCriteria.getGroupByKey(filter, across, false);

        List<DbAggregationResult> aggResults = template.query(query, selectionCriteria.getCriteria().getQueryParams(),
                DbWorkItemConverters.distinctRowMapper(rowKey, additionalKey, calculation));

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
                .where(Query.conditions(List.of("field_type IN ('status')")),
                        Query.Condition.AND)
                .build();
        return " INNER JOIN (" + fetchStages.toSql() + ") as stages ON "
                + "stages.stage_workitem_id = workitems.workitem_id AND "
                + "stages.stage_integration_id = workitems.integration_id AND "
                + "stages.stage = workitems.status ";
    }

    private List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }
}