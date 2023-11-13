package io.levelops.commons.databases.services.jira.conditions;

import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JiraSprintConditionsBuilder {

    public static void generateSprintsConditions(List<String> conditions, String paramPrefix, Map<String, Object> params, JiraSprintFilter filter) {
        // -- integration ids
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            conditions.add("integration_id IN (:" + paramPrefix + "integration_ids)");
            params.put(paramPrefix + "integration_ids", filter.getIntegrationIds().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }
        // -- sprint ids
        if (CollectionUtils.isNotEmpty(filter.getSprintIds())) {
            conditions.add("sprint_id IN (:" + paramPrefix + "sprint_ids)");
            params.put(paramPrefix + "sprint_ids", filter.getSprintIds().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }
        // -- state
        if (StringUtils.isNotEmpty(filter.getState())) {
            conditions.add("UPPER(state) = :" + paramPrefix + "state");
            params.put(paramPrefix + "state", StringUtils.upperCase(filter.getState()));
        }
        // -- start date
        if (filter.getStartDateBefore() != null) {
            conditions.add("start_date < :" + paramPrefix + "start_date_before");
            params.put(paramPrefix + "start_date_before", filter.getStartDateBefore());
        }
        if (filter.getStartDateAfter() != null) {
            conditions.add("start_date >= :" + paramPrefix + "start_date_after");
            params.put(paramPrefix + "start_date_after", filter.getStartDateAfter());
        }
        // -- end date
        if (filter.getEndDateBefore() != null) {
            conditions.add("end_date < :" + paramPrefix + "end_date_before");
            params.put(paramPrefix + "end_date_before", filter.getEndDateBefore());
        }
        if (filter.getEndDateAfter() != null) {
            conditions.add("end_date >= :" + paramPrefix + "end_date_after");
            params.put(paramPrefix + "end_date_after", filter.getEndDateAfter());
        }
        // -- completed at
        if (filter.getCompletedAtBefore() != null) {
            conditions.add("completed_at < :" + paramPrefix + "completed_at_before");
            params.put(paramPrefix + "completed_at_before", filter.getCompletedAtBefore());
        }
        if (filter.getCompletedAtAfter() != null) {
            conditions.add("completed_at >= :" + paramPrefix + "completed_at_after");
            params.put(paramPrefix + "completed_at_after", filter.getCompletedAtAfter());
        }
        // -- name
        if (CollectionUtils.isNotEmpty(filter.getNames())) {
            conditions.add("UPPER(name) IN (:" + paramPrefix + "name)");
            params.put(paramPrefix + "name", filter.getNames().stream().map(StringUtils::upperCase).collect(Collectors.toList()));
        } else if (StringUtils.isNotBlank(filter.getNameContains())) {
            conditions.add("name ILIKE :" + paramPrefix + "name");
            params.put(paramPrefix + "name", "%" + filter.getNameContains() + "%");
        } else if (StringUtils.isNotBlank(filter.getNameStartsWith())) {
            conditions.add("name ILIKE :" + paramPrefix + "name");
            params.put(paramPrefix + "name", filter.getNameStartsWith() + "%");
        } else if (StringUtils.isNotBlank(filter.getNameEndsWith())) {
            conditions.add("name ILIKE :" + paramPrefix + "name");
            params.put(paramPrefix + "name", "%" + filter.getNameEndsWith());
        } else if (CollectionUtils.isNotEmpty(filter.getExcludeNames())) {
            conditions.add("UPPER(name) NOT IN (:" + paramPrefix + "names)");
            params.put(paramPrefix + "names", filter.getExcludeNames().stream().map(StringUtils::upperCase).collect(Collectors.toList()));
        }
    }

}
