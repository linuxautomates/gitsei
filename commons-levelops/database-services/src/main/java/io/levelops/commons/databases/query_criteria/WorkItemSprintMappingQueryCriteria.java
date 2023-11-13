package io.levelops.commons.databases.query_criteria;

import io.levelops.commons.databases.models.filters.WorkItemsSprintMappingFilter;
import io.levelops.commons.models.Query;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkItemSprintMappingQueryCriteria {
    public static Query getSelectionCriteria(WorkItemsSprintMappingFilter workItemsMilestoneFilter, String paramPrefix) {
        if (StringUtils.isEmpty(paramPrefix)) {
            paramPrefix = "";
        }
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();
        if (workItemsMilestoneFilter.getIgnorableWorkitemType() != null) {
            queryConditions.add("ignorable_workitem_type = :" + paramPrefix + "ignorableWorkitemType");
            queryParams.put(paramPrefix + "ignorableWorkitemType", workItemsMilestoneFilter.getIgnorableWorkitemType());
        }

        return Query.builder()
                .where(Query.conditions(queryConditions, queryParams), Query.Condition.AND)
                .build();
    }
}
