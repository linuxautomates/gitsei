package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbStackedAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.utils.WorkItemsReportUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class WorkItemsAssigneeAllocationReportService {
    private static final String TABLE_NAME = "issue_mgmt_workitems";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";

    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsAssigneeAllocationReportService(DataSource dataSource,
                                                    WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DbListResponse<DbAggregationResult> generateReport(String company, WorkItemsFilter filter,
                                                              WorkItemsFilter.DISTINCT stack, boolean valuesOnly,
                                                              final OUConfiguration ouConfig,
                                                              int page, int pageSize)
            throws SQLException, BadRequestException {
        WorkItemsFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        WorkItemsFilter.CALCULATION calculation = WorkItemsFilter.CALCULATION.assignees;

        DbListResponse<DbAggregationResult> aggResults = null;
        if(across != WorkItemsFilter.DISTINCT.trend) {
            aggResults = getReportForNonTrend(company, filter, across, stack,
                    calculation, valuesOnly, ouConfig);
        } else {
            List<DbAggregationResult> reportFromTrend = getReportFromTrend(company, filter, ouConfig, page, pageSize);
            aggResults = DbListResponse.of(reportFromTrend, reportFromTrend.size());
        }

        return aggResults;
    }

    private List<DbAggregationResult> getReportFromTrend(String company, WorkItemsFilter filter, final OUConfiguration ouConfig, int page, int pageSize) throws BadRequestException {
        String interval = StringUtils.defaultString(filter.getAggInterval()).trim().toLowerCase();
        ImmutablePair<Long, Long> assigneesRange = filter.getAssigneesDateRange();
        Instant lowerBound = DateUtils.fromEpochSecond(assigneesRange.getLeft());
        Instant upperBound = DateUtils.fromEpochSecond(assigneesRange.getRight());
        if (lowerBound == null || upperBound == null) {
            throw new BadRequestException("assignees_range required");
        }

        List<ImmutablePair<Long, Long>> timePartition;
        switch (interval) {
            case "week":
                timePartition = DateUtils.getWeeklyPartition(lowerBound, upperBound);
                break;
            case "month":
                timePartition = DateUtils.getMonthlyPartition(lowerBound, upperBound);
                break;
            default:
                throw new BadRequestException("Interval not supported: " + interval);
        }

        return getAssigneeAllocationForTimePartition(company, filter, timePartition, ouConfig, page, pageSize);
    }

    private List<DbAggregationResult> getAssigneeAllocationForTimePartition(String company, WorkItemsFilter filter,
                                                                            List<ImmutablePair<Long, Long>> timePartition,
                                                                            final OUConfiguration ouConfig,
                                                                            int page, int pageSize) {
        return timePartition.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(pair -> {
                    try {
                        return getAssigneeAllocationForDateRange(company, filter, pair.getLeft(), pair.getRight(), ouConfig);
                    } catch (SQLException exception) {
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private DbAggregationResult getAssigneeAllocationForDateRange(String company, WorkItemsFilter filter, Long left, Long right, final OUConfiguration ouConfig) throws SQLException {
        ImmutablePair<Long, Long> assigneeRange = ImmutablePair.of(left, right);
        WorkItemsFilter updatedFilter = filter.toBuilder().assigneesDateRange(assigneeRange).across(WorkItemsFilter.DISTINCT.none).build();
        DbListResponse<DbAggregationResult> results = getReportForNonTrend(company, updatedFilter,
                WorkItemsFilter.DISTINCT.none, null, WorkItemsFilter.CALCULATION.assignees, false, ouConfig);
        List<DbAggregationResult> records = results.getRecords();
        List<String> assignees = null;
        if(CollectionUtils.isNotEmpty(records)) {
            assignees = IterableUtils.getFirst(records).map(DbAggregationResult::getAssignees).orElse(List.of());
        }
        return DbAggregationResult.builder()
                .key(String.valueOf(left))
                .assignees(assignees)
                .total((long) CollectionUtils.size(assignees))
                .build();
    }

    private DbListResponse<DbAggregationResult> getReportForNonTrend(String company, WorkItemsFilter filter,
                                                                     WorkItemsFilter.DISTINCT across,
                                                                     WorkItemsFilter.DISTINCT stack,
                                                                     WorkItemsFilter.CALCULATION calculation,
                                                                     boolean valuesOnly,
                                                                     final OUConfiguration ouConfig) throws SQLException {
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

        if(across != WorkItemsFilter.DISTINCT.none && (CollectionUtils.isEmpty(groupByCriteria.getGroupByFields()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }
        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossUsers(across)) {
            additionalKey = Optional.of(across.toString());
        }
        boolean needStagesJoin = across == WorkItemsFilter.DISTINCT.stage || stack == WorkItemsFilter.DISTINCT.stage;

        List<Query.SelectField> selectFields = new ArrayList<>();
        selectFields.add(Query.selectField("*"));
        selectFields.addAll(selectionCriteria.getSelectFields());
        selectFields.addAll(groupByCriteria.getSelectFields());

        Query workItemsFetchQuery = Query.builder().select(selectFields)
                .from(Query.fromField(company + "." + TABLE_NAME))
                .build();

        Query.QueryConditions assigneeQueryCondition = getAssigneeQueryCondition(filter, "");
        selectionCriteria.getCriteria().getQueryParams().putAll(assigneeQueryCondition.getQueryParams());

        Query assigneeQuery = Query.builder()
                .select(List.of(
                        Query.selectField("workitem_id", "iaj_issue_key"),
                        Query.selectField("integration_id", "iaj_integration_id"),
                        Query.selectField("field_value", "assignee_item")
                ))
                .from(Query.fromField(
                        company + "." + TIMELINES_TABLE_NAME, null))
                .where(assigneeQueryCondition, Query.Condition.AND)
                .build();

        List<Query.SelectField> finalQuerySelect = getFieldNames(groupByCriteria.getGroupByFields());
        finalQuerySelect.addAll(calculationCriteria.getSelectFields());

        String workItemsAssigneeJoin = workItemsFetchQuery.toSql() + " as wi inner join ( " + assigneeQuery.toSql() + " ) ia ON "
                + " ia.iaj_issue_key = wi.workitem_id AND ia.iaj_integration_id = wi.integration_id ";

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
            workItemsAssigneeJoin += " INNER JOIN (" + fetchStages.toSql() + ") as stages ON "
                    + "stages.stage_workitem_id = wi.workitem_id AND "
                    + "stages.stage_integration_id = wi.integration_id AND "
                    + "stages.stage = wi.status ";
        }

        Query acrossQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField("(" + workItemsAssigneeJoin + ")", "wi"))
                .where(selectionCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(groupByCriteria.getGroupByFields())
                .orderBy(orderByField)
                .limit(limit)
                .build();

        String query = acrossQuery.toSql();

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", selectionCriteria.getCriteria().getQueryParams());

        String acrossKey = WorkItemQueryCriteria.getGroupByKey(filter, across, false);
        List<DbAggregationResult> aggResults = template.query(query, selectionCriteria.getCriteria().getQueryParams(),
                DbWorkItemConverters.distinctRowMapper(acrossKey, additionalKey, calculation));

        if(stack != null && stack != across) {
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

        return DbListResponse.of(aggResults, aggResults.size());
    }

    private Query.QueryConditions getAssigneeQueryCondition(WorkItemsFilter workItemsFilter, String paramPrefix) {
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();
        ImmutablePair<Long, Long> assigneesDateRange = workItemsFilter.getAssigneesDateRange();
        if (assigneesDateRange != null) {
            if (assigneesDateRange.getLeft() != null) {
                queryConditions.add("(start_date >= to_timestamp(:" + paramPrefix + "assignees_start_time)" +
                        " OR end_date > to_timestamp(:" + paramPrefix + "assignees_start_time))");
                queryParams.put(paramPrefix + "assignees_start_time", assigneesDateRange.getLeft());
            }
            if (assigneesDateRange.getRight() != null) {
                queryConditions.add("(start_date < to_timestamp(:" + paramPrefix + "assignees_end_time)" +
                        " OR end_date <= to_timestamp(:" + paramPrefix + "assignees_end_time))");
                queryParams.put(paramPrefix + "assignees_end_time", assigneesDateRange.getRight());
            }
        }
        queryConditions.add("field_type = 'assignee'");
        queryConditions.add("field_value != 'UNASSIGNED'");
        return Query.conditions(queryConditions, queryParams);
    }

    private List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }
}
