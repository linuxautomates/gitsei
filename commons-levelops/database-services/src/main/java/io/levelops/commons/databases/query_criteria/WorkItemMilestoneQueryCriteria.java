package io.levelops.commons.databases.query_criteria;

import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.models.Query;
import io.levelops.commons.utils.NumberUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter.PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS;
import static io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter.PARTIAL_MATCH_ARRAY_COLUMNS;
import static io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter.PARTIAL_MATCH_ATTRIBUTES_COLUMNS;
import static io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter.PARTIAL_MATCH_COLUMNS;
import static io.levelops.commons.databases.utils.IssueMgmtPartialMatchUtil.createPartialMatchFilter;

public class WorkItemMilestoneQueryCriteria {

    public static Query getSelectionCriteria(WorkItemsMilestoneFilter workItemsMilestoneFilter, String paramPrefix) {
        if (StringUtils.isEmpty(paramPrefix)) {
            paramPrefix = "";
        }
        ArrayList<Query.SelectField> milestoneSelectFields = new ArrayList<>();
        milestoneSelectFields.add(Query.selectField("field_value", "milestone_field_value"));
        milestoneSelectFields.add(Query.selectField("parent_field_value", "milestone_parent_field_value"));
        milestoneSelectFields.add(Query.selectField("name", "milestone_name"));
        milestoneSelectFields.add(Query.selectField("concat(parent_field_value, '\\',  name)", "milestone_full_name"));
        milestoneSelectFields.add(Query.selectField("integration_id", "milestone_integration_id"));
        milestoneSelectFields.add(Query.selectField("field_type", "milestone_field_type"));
        milestoneSelectFields.add(Query.selectField("start_date", "milestone_start_date"));
        milestoneSelectFields.add(Query.selectField("end_date", "milestone_end_date"));
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();

        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getProjects())) {
            queryConditions.add("attributes->>'project' IN (:" + paramPrefix + "projects)");
            queryParams.put(paramPrefix + "projects", workItemsMilestoneFilter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeProjects())) {
            queryConditions.add("attributes->>'project' NOT IN (:" + paramPrefix + "exclude_projects)");
            queryParams.put(paramPrefix + "exclude_projects", workItemsMilestoneFilter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getIntegrationIds())) {
            queryConditions.add("integration_id IN (:" + paramPrefix + "integs)");
            queryParams.put(paramPrefix + "integs",
                    workItemsMilestoneFilter.getIntegrationIds()
                            .stream().map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeIntegrationIds())) {
            queryConditions.add("integration_id NOT IN (:" + paramPrefix + "excl_integs)");
            queryParams.put(paramPrefix + "excl_integs",
                    workItemsMilestoneFilter.getExcludeIntegrationIds()
                            .stream().map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getNames())) {
            queryConditions.add("name IN (:" + paramPrefix + "names)");
            queryParams.put(paramPrefix + "names", workItemsMilestoneFilter.getNames());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getFullNames())) {
            queryConditions.add("parent_field_value || '\\' || name IN (:" + paramPrefix + "full_names)");
            queryParams.put(paramPrefix + "full_names", workItemsMilestoneFilter.getFullNames());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeNames())) {
            queryConditions.add("name NOT IN (:" + paramPrefix + "exclude_names)");
            queryParams.put(paramPrefix + "exclude_names", workItemsMilestoneFilter.getExcludeNames());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeFullNames())) {
            queryConditions.add("parent_field_value || '\\' || name NOT IN (:" + paramPrefix + "exclude_full_names)");
            queryParams.put(paramPrefix + "exclude_full_names", workItemsMilestoneFilter.getExcludeFullNames());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getFieldValues())) {
            queryConditions.add("field_value IN (:" + paramPrefix + "field_values)");
            queryParams.put(paramPrefix + "field_values", workItemsMilestoneFilter.getFieldValues());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeFieldValues())) {
            queryConditions.add("field_value NOT IN (:" + paramPrefix + "exclude_field_values)");
            queryParams.put(paramPrefix + "exclude_field_values", workItemsMilestoneFilter.getExcludeFieldValues());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getFieldTypes())) {
            queryConditions.add("field_type IN (:" + paramPrefix + "field_types)");
            queryParams.put(paramPrefix + "field_types", workItemsMilestoneFilter.getFieldTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeFieldTypes())) {
            queryConditions.add("field_type NOT IN (:" + paramPrefix + "exclude_field_types)");
            queryParams.put(paramPrefix + "exclude_field_types", workItemsMilestoneFilter.getExcludeFieldTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getParentFieldValues())) {
            queryConditions.add("parent_field_value IN (:" + paramPrefix + "parent_field_values)");
            queryParams.put(paramPrefix + "parent_field_values", workItemsMilestoneFilter.getParentFieldValues());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeParentFieldValues())) {
            queryConditions.add("parent_field_value NOT IN (:" + paramPrefix + "exclude_parent_field_values)");
            queryParams.put(paramPrefix + "exclude_parent_field_values", workItemsMilestoneFilter.getExcludeParentFieldValues());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getProjectIds())) {
            queryConditions.add("project_id IN (:" + paramPrefix + "project_ids)");
            queryParams.put(paramPrefix + "project_ids", workItemsMilestoneFilter.getProjectIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeProjectIds())) {
            queryConditions.add("project_id NOT IN (:" + paramPrefix + "exclude_project_ids)");
            queryParams.put(paramPrefix + "exclude_project_ids", workItemsMilestoneFilter.getExcludeProjectIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getStates())) {
            queryConditions.add("state IN (:" + paramPrefix + "states)");
            queryParams.put(paramPrefix + "states", workItemsMilestoneFilter.getStates());
        }
        if (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getExcludeStates())) {
            queryConditions.add("state NOT IN (:" + paramPrefix + "exclude_states)");
            queryParams.put(paramPrefix + "exclude_states", workItemsMilestoneFilter.getExcludeStates());
        }
        if (workItemsMilestoneFilter.getCompletedAtRange() != null) {
            ImmutablePair<Long, Long> completedAtRange = workItemsMilestoneFilter.getCompletedAtRange();
            if (completedAtRange.getLeft() != null) {
                queryConditions.add("extract(epoch FROM completed_at) >= :" + paramPrefix + "completed_after");
                queryParams.put(paramPrefix + "completed_after", completedAtRange.getLeft());
            }
            if (completedAtRange.getRight() != null) {
                queryConditions.add("extract(epoch FROM completed_at) < :" + paramPrefix + "completed_before");
                queryParams.put(paramPrefix + "completed_before", completedAtRange.getRight());
            }
        }
        if (workItemsMilestoneFilter.getStartedAtRange() != null) {
            if (workItemsMilestoneFilter.getStartedAtRange().getLeft() != null) {
                queryConditions.add(" start_date >= to_timestamp(:" + paramPrefix + "workitem_start_at_gt)");
                queryParams.put(paramPrefix + "workitem_start_at_gt", workItemsMilestoneFilter.getStartedAtRange().getLeft());
            }
            if (workItemsMilestoneFilter.getStartedAtRange().getRight() != null) {
                queryConditions.add(" start_date < to_timestamp(:" + paramPrefix + "workitem_start_at_lt)");
                queryParams.put(paramPrefix + "workitem_start_at_lt", workItemsMilestoneFilter.getStartedAtRange().getRight());
            }
        }
        if (workItemsMilestoneFilter.getEndedAtRange() != null) {
            if (workItemsMilestoneFilter.getEndedAtRange().getLeft() != null) {
                queryConditions.add(" end_date >= to_timestamp(:" + paramPrefix + "workitem_end_at_gt)");
                queryParams.put(paramPrefix + "workitem_end_at_gt", workItemsMilestoneFilter.getEndedAtRange().getLeft());
            }
            if (workItemsMilestoneFilter.getEndedAtRange().getRight() != null) {
                queryConditions.add(" end_date < to_timestamp(:" + paramPrefix + "workitem_end_at_lt)");
                queryParams.put(paramPrefix + "workitem_end_at_lt", workItemsMilestoneFilter.getEndedAtRange().getRight());
            }
        }
        if (MapUtils.isNotEmpty(workItemsMilestoneFilter.getPartialMatch())) {
            createPartialMatchFilter(workItemsMilestoneFilter.getPartialMatch(), queryConditions, queryParams, "",
                    PARTIAL_MATCH_COLUMNS, PARTIAL_MATCH_ARRAY_COLUMNS, PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS, PARTIAL_MATCH_ATTRIBUTES_COLUMNS,
                    false);
        }
        return Query.builder()
                .select(milestoneSelectFields)
                .where(Query.conditions(queryConditions, queryParams), Query.Condition.AND)
                .build();
    }

    public static void updateQueryForSortFields(Query groupByCriteria, Query.SortByField orderByField, List<Query.SelectField> finalQuerySelect) {
            String field = orderByField.getField();
            finalQuerySelect.add(Query.selectField(field, null));
            groupByCriteria.getGroupByFields().add(Query.groupByField(field));
    }
}