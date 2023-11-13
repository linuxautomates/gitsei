package io.levelops.commons.service.dora;

import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.converters.DbWorkItemHistoryConverters;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemTimelineQueryCriteria;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ADODoraService {

    public static final String TABLE_NAME = "issue_mgmt_workitems";
    public static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    public static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    public static final String PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";
    public static final List<String> TIME_BASED_SORTABLE_COLUMNS = List.of("workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "workitem_due_at");
    private final NamedParameterJdbcTemplate template;

    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    private final DataSource dataSource;

    @Autowired
    public ADODoraService(final DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.dataSource = dataSource;
        template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DoraResponseDTO getTimeSeriesDataForDeployment(String company, WorkItemsFilter filter,
                                                          WorkItemsMilestoneFilter milestoneFilter,
                                                          WorkItemsFilter reqFilter,
                                                          WorkItemsMilestoneFilter reqMilestoneFilter,
                                                          final OUConfiguration ouConfig,
                                                          String calculationField) throws SQLException, BadRequestException {
        ImmutablePair<Long, Long> calculationTimeRange;
        if ("workitem_resolved_at".equals(calculationField)) {
            calculationTimeRange = reqFilter.getWorkItemResolvedRange();
        } else {
            calculationTimeRange = reqFilter.getWorkItemUpdatedRange();
        }
        if (calculationTimeRange == null || calculationTimeRange.getLeft() == null || calculationTimeRange.getRight() == null) {
            throw new BadRequestException("Error computing DORA metric, please provide Workitem Resolved range.");
        }
        List<Query.SelectField> timeSeriesFields = List.of(
                Query.selectField("EXTRACT(EPOCH FROM trend_interval)", "trend"),
                Query.selectField("CONCAT((date_part('day', trend_interval)),'-',(date_part('month', trend_interval)),'-',(date_part('year', trend_interval)))", "interval")
        );
        List<DbWorkItemField> workItemCustomFields = null;
        if (MapUtils.isNotEmpty(filter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }

        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields,
                null, null, null, false, ouConfig);

        List<Query.SelectField> filterSelectFields = selectionCriteria.getSelectFields();
        List<String> filterConditions = selectionCriteria.getCriteria().getConditions();
        Map<String, Object> filterParams = selectionCriteria.getCriteria().getQueryParams();

        Query reqSelectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, reqFilter, workItemCustomFields,
                null, null, "req_", false, ouConfig);

        // Merge request filters and workflow profile filters
        filterSelectFields.addAll(reqSelectionCriteria.getSelectFields());
        filterConditions.addAll(reqSelectionCriteria.getCriteria().getConditions());
        filterParams.putAll(reqSelectionCriteria.getCriteria().getQueryParams());

        selectionCriteria = Query.builder().select(filterSelectFields).where(Query.conditions(filterConditions, filterParams), Query.Condition.AND).build();

        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);

        List<Query.SelectField> milstoneFilterSelectFields = milestoneSelectionCriteria.getSelectFields();
        List<String> milstoneFilterConditions = milestoneSelectionCriteria.getCriteria().getConditions();
        Map<String, Object> milstoneFilterParams = milestoneSelectionCriteria.getCriteria().getQueryParams();

        Query reqMilestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(reqMilestoneFilter, "req_");

        // Merge request filters and workflow profile filters for Milestone
        milstoneFilterSelectFields.addAll(reqMilestoneSelectionCriteria.getSelectFields());
        milstoneFilterConditions.addAll(reqMilestoneSelectionCriteria.getCriteria().getConditions());
        milstoneFilterParams.putAll(reqMilestoneSelectionCriteria.getCriteria().getQueryParams());

        milestoneSelectionCriteria = Query.builder().select(milstoneFilterSelectFields).where(Query.conditions(milstoneFilterConditions, milstoneFilterParams), Query.Condition.AND).build();

        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        Query calculationCriteria = filter.getCalculation(WorkItemsFilter.CALCULATION.issue_count);

        List<Query.SelectField> selectFields = new ArrayList<>();
        selectFields.add(Query.selectField("*"));
        selectFields.addAll(selectionCriteria.getSelectFields());

        List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(WorkItemTimelineQueryCriteria
                .getSelectionCriteria(WorkItemsTimelineFilter.builder().build(), null).getSelectFields());
        timelineSelectFields.addAll(WorkItemQueryCriteria.getFieldNames(milestoneSelectFields));

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

        String intermediateSQL = fetchTimelines.toSql() + " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                + "milestones.milestone_integration_id = timelines.integration_id ";

        String workItemsFetchSql = workItemsFetchQuery.toSql() + " AS wi";
        String milestonesSql = "";

        if (milestoneFilter.isSpecified() || reqMilestoneFilter.isSpecified()) {
            milestonesSql = " INNER JOIN (" + intermediateSQL + ") tmp"
                    + " ON tmp.timeline_workitem_id = wi.workitem_id AND"
                    + " tmp.timeline_integration_id = wi.integration_id ";
            selectionCriteria.getCriteria().getQueryParams().putAll(milestoneQueryConditions.getQueryParams());
        }
        workItemsFetchSql += milestonesSql;

        List<Query.SelectField> finalQuerySelect = new ArrayList<>();
        finalQuerySelect.addAll(calculationCriteria.getSelectFields());
        finalQuerySelect.addAll(timeSeriesFields);

        Query dedupeQuery = Query.builder()
                .select(List.of(Query.selectField("*"), Query.selectField("DATE_TRUNC('day', " + calculationField + "::TIMESTAMP WITHOUT TIME ZONE)", "trend_interval")))
                .distinctOn(List.of("workitem_id", "integration_id"))
                .from(Query.fromField("(" + workItemsFetchSql + ")", "wi"))
                .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                .build();

        Query acrossQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField("(" + dedupeQuery.toSql() + ")", "dedupe"))
                .groupBy(List.of(Query.groupByField("trend_interval")))
                .build();
        // copying the query params over from the inner dedupe query, as this is re-used by the stacks
        acrossQuery.setCriteria(Query.conditions(new ArrayList<>(), dedupeQuery.getCriteria().getQueryParams()));

        String acrossQuerySql = acrossQuery.toSql();

        List<DoraTimeSeriesDTO.TimeSeriesData> tempResults = template.query(acrossQuerySql, selectionCriteria.getCriteria().getQueryParams(), DoraCalculationUtils.getTimeSeries());

        log.info(" ADO Deployment Frequency SQL" + acrossQuerySql);
        log.info("params = {}", selectionCriteria.getCriteria().getQueryParams());

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesByDayFailedDeployment = DoraCalculationUtils.fillRemainingDates(
                calculationTimeRange.getLeft(),
                calculationTimeRange.getRight(),
                tempResults
        );

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries("day", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries("week", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries("month", filledTimeSeriesByDayFailedDeployment);
        Integer total = CollectionUtils.emptyIfNull(filledTimeSeriesByDayFailedDeployment).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);

        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder().day(timeSeriesByDay).month(timeSeriesByMonth).week(timeSeriesByWeek).build();
        DoraSingleStateDTO stats = DoraSingleStateDTO.builder().totalDeployment(total).build();

        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats).build();
    }

    public DbListResponse<DbWorkItem> getDrillDownData(String company, WorkItemsFilter filter,
                                                   WorkItemsMilestoneFilter milestoneFilter,
                                                   WorkItemsFilter reqFilter,
                                                   WorkItemsMilestoneFilter reqMilestoneFilter,
                                                   final OUConfiguration ouConfig,
                                                   Integer pageNumber,
                                                   Integer pageSize,
                                                   boolean needSlaColumns) throws SQLException {
        log.info("getDrillDownData: API Being hit for {}, pageNumber-{} and pageSize-{}", company, pageNumber, pageSize);
        Instant now = Instant.now();
        Map<String, SortingOrder> sortBy = filter.getSort();
        List<DbWorkItemField> workItemCustomFields = null;
        if (MapUtils.isNotEmpty(filter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }
        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields, null, null, null, true, ouConfig);

        List<Query.SelectField> filterSelectFields = selectionCriteria.getSelectFields();
        List<String> filterConditions = selectionCriteria.getCriteria().getConditions();
        Map<String, Object> filterParams = selectionCriteria.getCriteria().getQueryParams();

        Query reqSelectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, reqFilter, workItemCustomFields,
                null, null, "req_", true, ouConfig);

        // Merge request filters and workflow profile filters
        filterSelectFields.addAll(reqSelectionCriteria.getSelectFields());
        filterConditions.addAll(reqSelectionCriteria.getCriteria().getConditions());
        filterParams.putAll(reqSelectionCriteria.getCriteria().getQueryParams());

        selectionCriteria = Query.builder().select(filterSelectFields).where(Query.conditions(filterConditions, filterParams), Query.Condition.AND).build();

        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);

        List<Query.SelectField> milstoneFilterSelectFields = milestoneSelectionCriteria.getSelectFields();
        List<String> milstoneFilterConditions = milestoneSelectionCriteria.getCriteria().getConditions();
        Map<String, Object> milstoneFilterParams = milestoneSelectionCriteria.getCriteria().getQueryParams();

        Query reqMilestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(reqMilestoneFilter, "req_");

        // Merge request filters and workflow profile filters for Milestone
        milstoneFilterSelectFields.addAll(reqMilestoneSelectionCriteria.getSelectFields());
        milstoneFilterConditions.addAll(reqMilestoneSelectionCriteria.getCriteria().getConditions());
        milstoneFilterParams.putAll(reqMilestoneSelectionCriteria.getCriteria().getQueryParams());

        milestoneSelectionCriteria = Query.builder().select(milstoneFilterSelectFields).where(Query.conditions(milstoneFilterConditions, milstoneFilterParams), Query.Condition.AND).build();

        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(WorkItemsTimelineFilter.builder().build(), null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        Query.QueryConditions queryConditions = selectionCriteria.getCriteria();

        if (filter.getAcross() != null && filter.getAcross().equals(WorkItemsFilter.DISTINCT.trend)
                && filter.getAggInterval() != null && !AGG_INTERVAL.day.name().equalsIgnoreCase(filter.getAggInterval())) {
            Query trendEpochForInterval = AggTimeQueryHelper.getAggTimeQueryForTimestampForList(filter.getAggInterval(), true);
            selectionCriteria.getSelectFields().addAll(trendEpochForInterval.getSelectFields());
            Query trendEpochForIntervalFilter = AggTimeQueryHelper.getAggTimeQueryForTimestampForFilter(filter.getAggInterval(), true);
            selectionCriteria.getCriteria().getConditions().add("trend_epoch = " + trendEpochForIntervalFilter.getSelectFields().get(0).getField());
            queryConditions.getQueryParams().put("wi_ingested_at_for_trend", filter.getIngestedAt());
            selectionCriteria.getCriteria().getConditions().remove("ingested_at = (:ingested_at)");
        }

        String whereClause = (CollectionUtils.isNotEmpty(queryConditions.getConditions()))
                ? " WHERE " + String.join(" AND ", queryConditions.getConditions()) : "";

        String orderByString = "";
        if (MapUtils.isNotEmpty(sortBy)) {
            String orderByField = sortBy.keySet().stream().findFirst().get();
            SortingOrder sortOrder = sortBy.values().stream().findFirst().get();
            if ("milestone_start_date".equals(orderByField) || "milestone_end_date".equals(orderByField) || TIME_BASED_SORTABLE_COLUMNS.contains(orderByField)) {
                orderByString = orderByField + " " + sortOrder + " NULLS LAST, workitem_id ASC ";
            } else {
                orderByString = "LOWER(" + orderByField + ") " + sortOrder + " NULLS LAST, workitem_id ASC ";
            }
        }
        boolean needMilestonesJoin = milestoneFilter.isSpecified() || reqMilestoneFilter.isSpecified();
        List<Query.SelectField> selectFields = new ArrayList<>();
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));
        needSlaTimeStuff = needSlaColumns || needSlaTimeStuff;
        boolean needStageTimelinesJoin = CollectionUtils.isNotEmpty(filter.getStages()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeStages());
        if (needMilestonesJoin) {
            selectFields.add(Query.selectField("workitems.*"));
            selectFields.add(Query.selectField("milestone_start_date"));
            selectFields.add(Query.selectField("milestone_end_date"));
            if (needSlaTimeStuff) {
                selectFields.add(Query.selectField("p.*"));
            }
        } else {
            selectFields.add(Query.selectField("*"));
        }
        selectFields.addAll(selectionCriteria.getSelectFields());

        List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(timelineSelectionCriteria.getSelectFields());

        Query fetchWorkitems;
        if (needMilestonesJoin || needSlaTimeStuff) {
            fetchWorkitems = Query.builder().select(selectFields)
                    .from(Query.fromField(company + "." + TABLE_NAME))
                    .build();
        } else {
            fetchWorkitems = Query.builder().select(selectFields)
                    .from(Query.fromField(company + "." + TABLE_NAME))
                    .build();
        }

        Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(milestoneSelectFields)
                .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
        if (milestoneFilter.getSprintCount() > 0) {
            fetchMilestonesBuilder = fetchMilestonesBuilder
                    .orderBy(Query.sortByField("end_date", "DESC", false))
                    .limit(milestoneFilter.getSprintCount());
        }else if(reqMilestoneFilter.getSprintCount() > 0){
            fetchMilestonesBuilder = fetchMilestonesBuilder
                    .orderBy(Query.sortByField("end_date", "DESC", false))
                    .limit(reqMilestoneFilter.getSprintCount());
        }
        Query fetchMilestones = fetchMilestonesBuilder.build();
        if (needMilestonesJoin) {
            timelineSelectFields.add(Query.selectField("milestone_start_date"));
            timelineSelectFields.add(Query.selectField("milestone_end_date"));
        }
        Query fetchTimelines = Query.builder().select(timelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "timelines"))
                .build();

        String sql;
        String countSQL;
        String slaQuery = "";
        String innerTimelineJoin = "";
        if (needSlaTimeStuff || Boolean.TRUE.equals(filter.getIncludeSolveTime())) {
            fetchWorkitems.getSelectFields().addAll(List.of(
                    Query.selectField("extract(epoch from(COALESCE(first_comment_at,now())-workitem_created_at))", "resp_time")
            ));
            boolean needExcludeTime = CollectionUtils.isNotEmpty(filter.getExcludeStages());
            String solveTimeString = "extract(epoch from coalesce(workitem_resolved_at, now())) - extract(epoch from  workitem_created_at)";
            fetchWorkitems.getSelectFields().addAll(List.of(
                    needExcludeTime
                            ? Query.selectField("greatest(" + solveTimeString + " - COALESCE(exclude_time, 0), 0)", "solve_time")
                            : Query.selectField(solveTimeString, "solve_time")
            ));

            if (needExcludeTime) {
                Query timelineQueryCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                        WorkItemsTimelineFilter.builder()
                                .fieldTypes(List.of("status"))
                                .fieldValues(filter.getExcludeStages())
                                .build(), "timeline_");
                selectionCriteria.getCriteria().getQueryParams().putAll(timelineQueryCriteria.getCriteria().getQueryParams());
                List<Query.SelectField> innerTimelineSelectFields = new ArrayList<>(
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
                Query timelineQuery = Query.builder().select(innerTimelineSelectFields)
                        .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                        .where(timelineQueryCriteria.getCriteria(), Query.Condition.AND)
                        .groupBy(timelineGroupByFields)
                        .build();
                String wiAlias = needMilestonesJoin ? "workitems" : "wi";
                innerTimelineJoin = " left join ( " + timelineQuery.toSql() + " ) wtm ON "
                        + wiAlias + ".integration_id = wtm.timeline_integration_id AND " + wiAlias + ".workitem_id = wtm.timeline_workitem_id ";
            }
        }
        if (needSlaTimeStuff) {
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
            slaQuery = " inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                    + " p.proj = workitems.project AND p.prio = workitems.priority AND p.integid = workitems.integration_id AND p.ttype = workitems.workitem_type";
        }
        String stageTimelineQuery = "";
        if (needStageTimelinesJoin) {
            stageTimelineQuery = getStageTimelineQuery(company, filter, selectionCriteria);
        }
        boolean needFirstAssigneeJoin = MapUtils.isNotEmpty(filter.getMissingFields())
                && filter.getMissingFields().containsKey(WorkItemsFilter.MISSING_BUILTIN_FIELD.first_assignee.toString())
                && filter.getMissingFields().get(WorkItemsFilter.MISSING_BUILTIN_FIELD.first_assignee.toString());
        String firstAssigneeQuery = "";
        if (needFirstAssigneeJoin) {
            Query firstAssigneeSelectionQuery = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                    WorkItemsTimelineFilter.builder()
                            .integrationIds(filter.getIntegrationIds())
                            .fieldTypes(List.of("assignee"))
                            .excludeFieldValues(List.of("UNASSIGNED"))
                            .build(), "fa_timeline_");
            selectionCriteria.getCriteria().getQueryParams().putAll(firstAssigneeSelectionQuery.getCriteria().getQueryParams());
            List<Query.SelectField> firstAssigneeTimelineSelectFields = new ArrayList<>(
                    List.of(
                            Query.selectField("DISTINCT ON (integration_id, workitem_id) integration_id", "tl_integration_id"),
                            Query.selectField("workitem_id", "tl_workitem_id")
                    ));
            List<Query.SortByField> firstAssigneeTimelineOrderByFields = new ArrayList<>(
                    List.of(
                            Query.sortByField("tl_integration_id", "ASC", true),
                            Query.sortByField("workitem_id", "ASC", true),
                            Query.sortByField("start_date", "ASC", true)
                    ));
            Query firstAssigneeTimelineQuery = Query.builder().select(firstAssigneeTimelineSelectFields)
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                    .where(firstAssigneeSelectionQuery.getCriteria(), Query.Condition.AND)
                    .orderBy(firstAssigneeTimelineOrderByFields)
                    .build();
            firstAssigneeQuery = " inner join ( " + firstAssigneeTimelineQuery.toSql() + " ) t ON " +
                    "t.tl_workitem_id = workitems.workitem_id AND t.tl_integration_id = workitems.integration_id";
        }
        if (needMilestonesJoin) {
            String intermediateSQL = fetchTimelines.toSql() + " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                    + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                    + "milestones.milestone_integration_id = timelines.integration_id ";

            String tempSQL = fetchWorkitems.toSql() + " as workitems "
                    + innerTimelineJoin
                    + stageTimelineQuery
                    + slaQuery
                    + firstAssigneeQuery
                    + " INNER JOIN (" + intermediateSQL + ") tmp"
                    + " ON tmp.timeline_workitem_id = workitem_id AND"
                    + " tmp.timeline_integration_id = integration_id ";

            String finalQuerySelectFields = "*";
            if (BooleanUtils.isTrue(filter.getIncludeSprintFullNames())) {
                finalQuerySelectFields += ", " + generateSprintFullNamesArraySql(company, "final");
            }
            sql = "select distinct on (workitem_id, integration_id) " + finalQuerySelectFields
                    + "from ( "
                    + tempSQL
                    + " ORDER BY " + orderByString
                    + " ) final "
                    + whereClause
                    + " order by workitem_id, integration_id";
            countSQL = "SELECT COUNT(*) FROM (" + sql + ") as ct";
            sql += "  LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
            queryConditions.getQueryParams().putAll(milestoneQueryConditions.getQueryParams());
        } else {
            String finalQuerySelectFields = "*";
            if (BooleanUtils.isTrue(filter.getIncludeSprintFullNames())) {
                finalQuerySelectFields += ", " + generateSprintFullNamesArraySql(company, "workitems");
            }
            sql = "select distinct on (workitem_id, integration_id) " + finalQuerySelectFields
                    + "from ( "
                    + fetchWorkitems.toSql() + " as wi " + innerTimelineJoin
                    + " ) as workitems "
                    + stageTimelineQuery
                    + slaQuery
                    + firstAssigneeQuery
                    + whereClause
                    + " order by workitem_id, integration_id" + (orderByString.equals("") ? "" : ", " + orderByString);
            countSQL = "SELECT COUNT(*) FROM (" + sql + ") as ct";
            sql += " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        }
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", queryConditions.getQueryParams());
        log.info("list: Parsing DbListResponse");
        List<DbWorkItem> aggResults = template.query(sql, queryConditions.getQueryParams(), DbWorkItemConverters.listRowMapper());
        aggResults = aggResults.stream()
                .map(dbWorkItem -> {
                    List<DbWorkItemHistory> workItemHistories = template.query(
                            "SELECT * FROM " + company + "." + TIMELINES_TABLE_NAME
                                    + " WHERE workitem_id = :workitemId AND integration_id = :integrationId"
                                    + " ORDER BY start_date DESC",
                            Map.of("workitemId", dbWorkItem.getWorkItemId(),
                                    "integrationId", Integer.parseInt(dbWorkItem.getIntegrationId())),
                            DbWorkItemHistoryConverters.listRowMapper());

                    List<DbWorkItemHistory> statusList = DbWorkItemHistoryConverters.sanitizeEventList(workItemHistories.stream()
                            .filter(dbWorkItemHistory -> "status".equalsIgnoreCase(dbWorkItemHistory.getFieldType()))
                            .collect(Collectors.toList()), now);
                    List<DbWorkItemHistory> assigneeList = DbWorkItemHistoryConverters.sanitizeEventList(workItemHistories.stream()
                            .filter(dbWorkItemHistory -> "assignee".equalsIgnoreCase(dbWorkItemHistory.getFieldType()))
                            .collect(Collectors.toList()), now);

                    return dbWorkItem.toBuilder()
                            .statusList(statusList)
                            .assigneeList(assigneeList)
                            .build();
                })
                .collect(Collectors.toList());
        log.info("countSQL = {}", countSQL);
        Integer totalCount = template.queryForObject(countSQL, queryConditions.getQueryParams(), Integer.class);
        return DbListResponse.of(aggResults, totalCount);
    }

    @NotNull
    private String getStageTimelineQuery(String company, WorkItemsFilter filter, Query selectionCriteria) {
        Query timelineQueryCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                WorkItemsTimelineFilter.builder()
                        .fieldTypes(List.of("status"))
                        .fieldValues(filter.getStages())
                        .excludeFieldValues(filter.getExcludeStages())
                        .build(), "timeline_");
        selectionCriteria.getCriteria().getQueryParams().putAll(timelineQueryCriteria.getCriteria().getQueryParams());
        List<Query.SelectField> stageTimelineSelectFields = new ArrayList<>(
                List.of(
                        Query.selectField("integration_id", "timeline_integration_id"),
                        Query.selectField("workitem_id", "timeline_workitem_id"),
                        Query.selectField("count(*)", "count")
                ));
        List<Query.GroupByField> timelineGroupByFields = new ArrayList<>(
                List.of(
                        Query.groupByField("timeline_integration_id"),
                        Query.groupByField("timeline_workitem_id")
                ));
        Query timelineQuery = Query.builder()
                .select(stageTimelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                .where(timelineQueryCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(timelineGroupByFields)
                .build();

        return " INNER JOIN ( " + timelineQuery.toSql() + " ) wt ON "
                + " workitems.integration_id = wt.timeline_integration_id AND workitems.workitem_id = wt.timeline_workitem_id ";
    }

    private String generateSprintFullNamesArraySql(String company, String workItemTableAlias) {
        return "  array(" +
                " select t.field_value as sprint_full_name " +
                " from " + company + ".issue_mgmt_workitems_timeline as t " +
                " join " + company + ".issue_mgmt_milestones as m " +
                " on " +
                "   m.field_type = t.field_type " +
                "   and m.integration_id = t.integration_id " +
                "   and m.parent_field_value || '\\' || m.name = t.field_value  " +
                " where " +
                "   t.workitem_id = " + workItemTableAlias + ".workitem_id and " +
                "   t.integration_id = " + workItemTableAlias + ".integration_id and " +
                "   t.field_type='sprint' " +
                " order by m.start_date asc " +
                " ) as sprint_full_names ";
    }

}
