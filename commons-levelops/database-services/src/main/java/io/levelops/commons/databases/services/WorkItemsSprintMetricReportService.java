package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsSprintMappingFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbStackedAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemSprintMappingQueryCriteria;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class WorkItemsSprintMetricReportService {
    private static final String WORKITEMS = "issue_mgmt_workitems";
    private static final String MILESTONE = "issue_mgmt_milestones";
    private static final String SPRINT_MAPPINGS = "issue_mgmt_sprint_mappings";
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

    private final NamedParameterJdbcTemplate template;
    private static final List<Query.SelectField> FINAL_QUERY_SELECT = List.of(
            Query.selectField("array_agg(json_build_object('sprint_mapping', to_jsonb(sprint_mapping_json), 'workitem_type', workitem_type))", "sprint_mappings"),
            Query.selectField("sprint_mapping_integration_id"),
            Query.selectField("sprint_mapping_sprint_id"),
            Query.selectField("sprint_mapping_name"),
            Query.selectField("EXTRACT(EPOCH FROM sprint_mapping_start_date)", "sprint_mapping_start_date"),
            Query.selectField("EXTRACT(EPOCH FROM sprint_mapping_completed_at)", "sprint_mapping_completed_at")
    );

    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public WorkItemsSprintMetricReportService(DataSource dataSource, WorkItemFieldsMetaService workItemFieldsMetaService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DbListResponse<DbAggregationResult> generateReport(String company, WorkItemsFilter workItemsFilter,
                                                              WorkItemsMilestoneFilter milestoneFilter,
                                                              WorkItemsSprintMappingFilter sprintMappingFilter,
                                                              WorkItemsFilter.DISTINCT stack,
                                                              boolean valuesOnly,
                                                              final OUConfiguration ouConfig)
            throws SQLException {
        WorkItemsFilter.DISTINCT across = workItemsFilter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        WorkItemsFilter.CALCULATION calculation = workItemsFilter.getCalculation();
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.sprint_mapping;
        }
        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            additionalKey = Optional.of("interval");
        } else if (WorkItemsFilter.isAcrossUsers(across)) {
            additionalKey = Optional.of(across.toString());
        }
        String rowKey;

        List<DbWorkItemField> workItemCustomFields = null;
        if(MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
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

        Query milestoneSelectionCriteria =
                WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        Query sprintMappingSelectionCriteria =
                WorkItemSprintMappingQueryCriteria.getSelectionCriteria(sprintMappingFilter, null);

        workItemSelectionCriteria.getCriteria().getQueryParams()
                .putAll(milestoneSelectionCriteria.getCriteria().getQueryParams());
        workItemSelectionCriteria.getCriteria().getQueryParams()
                .putAll(sprintMappingSelectionCriteria.getCriteria().getQueryParams());

        if (across != WorkItemsFilter.DISTINCT.none && CollectionUtils.isEmpty(workItemGroupByCriteria.getGroupByFields())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }

        List<Query.SelectField> workSelectFields = new ArrayList<>();
        workSelectFields.add(Query.selectField("*"));
        workSelectFields.addAll(workItemSelectionCriteria.getSelectFields());
        workSelectFields.addAll(workItemGroupByCriteria.getSelectFields());

        Query workItemQuery = Query.builder().select(workSelectFields)
                .from(Query.fromField(company + "." + WORKITEMS))
                .build();

        String intermediateQuery = getIntermediateQuery(company, workItemSelectionCriteria, milestoneSelectionCriteria, sprintMappingSelectionCriteria, workItemQuery, milestoneFilter.getSprintCount());

        Query finalQuery = Query.builder().select(FINAL_QUERY_SELECT)
                .from(Query.fromField("( " + intermediateQuery + " ) a"))
                .groupBy(workItemGroupByCriteria.getGroupByFields())
                .orderBy(orderByField)
                .limit(limit)
                .build();

        String query = finalQuery.toSql();

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", workItemSelectionCriteria.getCriteria().getQueryParams());
        List<DbAggregationResult> aggResults;
        rowKey = WorkItemQueryCriteria.getGroupByKey(workItemsFilter, across, false);
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
            log.info("stackQuerySQL = {}", stackQuerySql);
            List<DbStackedAggregationResult> stackAggResults = template.query(stackQuerySql,
                    finalQuery.getCriteria().getQueryParams(),
                    DbWorkItemConverters.distinctRowMapperForStacks(stackKey, stackAdditionalKey, rowKey, additionalKey, calculation));

            aggResults = WorkItemsReportUtil.mergeResult(aggResults, stackAggResults);
        }
        return DbListResponse.of(aggResults, aggResults.size());
    }

    public DbListResponse<DbAggregationResult> generateCountReport(String company, WorkItemsFilter workItemsFilter,
                                                                   WorkItemsMilestoneFilter milestoneFilter,
                                                                   WorkItemsSprintMappingFilter sprintMappingFilter,
                                                                   WorkItemsFilter.DISTINCT stack,
                                                                   boolean valuesOnly,
                                                                   final OUConfiguration ouConfig) throws SQLException {

        WorkItemsFilter.DISTINCT across = workItemsFilter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        WorkItemsFilter.CALCULATION calculation = workItemsFilter.getCalculation();
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.sprint_mapping_count;
        }
        Optional<String> additionalKey = Optional.empty();
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            additionalKey = Optional.of("interval");
        }
        String rowKey;

        List<DbWorkItemField> workItemCustomFields = null;
        if(MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
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

        Query milestoneSelectionCriteria =
                WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);

        Query sprintMappingSelectionCriteria =
                WorkItemSprintMappingQueryCriteria.getSelectionCriteria(sprintMappingFilter, null);

        if(workItemSelectionCriteria.getCriteria() == null) {
            workItemSelectionCriteria.setCriteria(Query.conditions(new ArrayList<>(), new HashMap<>()));
        }
        workItemSelectionCriteria.getCriteria().getQueryParams()
                .putAll(milestoneSelectionCriteria.getCriteria().getQueryParams());
        workItemSelectionCriteria.getCriteria().getQueryParams()
                .putAll(sprintMappingSelectionCriteria.getCriteria().getQueryParams());

        if (across != WorkItemsFilter.DISTINCT.none && CollectionUtils.isEmpty(workItemGroupByCriteria.getGroupByFields())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }

        List<Query.SelectField> workSelectFields = new ArrayList<>();
        workSelectFields.add(Query.selectField("*"));
        workSelectFields.addAll(workItemSelectionCriteria.getSelectFields());
        workSelectFields.addAll(workItemGroupByCriteria.getSelectFields());

        Query workItemQuery = Query.builder().select(workSelectFields)
                .from(Query.fromField(company + "." + WORKITEMS))
                .build();

        String intermediateQuery = getIntermediateQuery(company, workItemSelectionCriteria, milestoneSelectionCriteria,
                sprintMappingSelectionCriteria, workItemQuery, milestoneFilter.getSprintCount());

        List<Query.SelectField> finalQuerySelect = workItemCalculationCriteria.getSelectFields();

        Query finalQuery = Query.builder().select(finalQuerySelect)
                .from(Query.fromField("( " + intermediateQuery + " ) a"))
                .groupBy(workItemGroupByCriteria.getGroupByFields())
                .build();

        String query = finalQuery.toSql();

        log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", workItemSelectionCriteria.getCriteria().getQueryParams());
        List<DbAggregationResult> aggResults;
        rowKey = workItemGroupByCriteria.getGroupByFields().get(0).getField();
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
            List<DbStackedAggregationResult> stackAggResults = template.query(stackQuerySql,
                    finalQuery.getCriteria().getQueryParams(),
                    DbWorkItemConverters.distinctRowMapperForStacks(stackKey, stackAdditionalKey, rowKey, additionalKey, calculation));

            aggResults = WorkItemsReportUtil.mergeResult(aggResults, stackAggResults);
        }

        return DbListResponse.of(aggResults, aggResults.size());
    }

    @NotNull
    private String getIntermediateQuery(String company, Query workItemSelectionCriteria, Query milestoneSelectionCriteria, Query sprintMappingSelectionCriteria, Query workItemQuery, int sprintCount) {
        Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(MILESTONE_SELECT_FIELDS)
                .from(Query.fromField(company + "." + MILESTONE))
                .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
        if (sprintCount > 0) {
            fetchMilestonesBuilder = fetchMilestonesBuilder
                    .orderBy(Query.sortByField("end_date", "DESC", false))
                    .limit(sprintCount);
        }
        Query milestoneQuery = fetchMilestonesBuilder.build();

        Query sprintMappingQuery = Query.builder().select(SPRINT_MAPPING_SELECT_FIELDS)
                .from(Query.fromField(company + "." + SPRINT_MAPPINGS))
                .build();

        String whereClause = "";
        if (CollectionUtils.isNotEmpty(workItemSelectionCriteria.getCriteria().getConditions())) {
            whereClause = " WHERE " + String.join(" AND ", workItemSelectionCriteria.getCriteria().getConditions());
        }
        String sprintMappingWhereClause = "";
        if (CollectionUtils.isNotEmpty(sprintMappingSelectionCriteria.getCriteria().getConditions())) {
            sprintMappingWhereClause = " WHERE " + String.join(" AND ", sprintMappingSelectionCriteria.getCriteria().getConditions());
        }

        String sprintMappingAndSprintQuery = sprintMappingQuery.toSql() + " as sm INNER JOIN (" + milestoneQuery.toSql() + ") as smpj " +
                " ON sm.integration_id = smpj.integration_id  AND sm.sprint_id = smpj.parent_field_value || '\\' || smpj.name " + sprintMappingWhereClause;

        return workItemQuery.toSql() + " as workitems " + " INNER JOIN (" + sprintMappingAndSprintQuery + ") smj " +
                " ON workitems.integration_id = smj.sprint_mapping_integration_id  AND workitems.workitem_id = smj.sprint_mapping_workitem_id" +
                whereClause;
    }
}
