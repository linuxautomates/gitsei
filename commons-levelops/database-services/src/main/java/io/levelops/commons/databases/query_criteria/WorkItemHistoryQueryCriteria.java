package io.levelops.commons.databases.query_criteria;

import io.levelops.commons.databases.models.filters.WorkItemHistoryFilter;
import io.levelops.commons.models.Query;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkItemHistoryQueryCriteria {

    public static Query getSelectionCriteria(WorkItemHistoryFilter workItemHistoryFilter, String paramPrefix) {
        if(StringUtils.isEmpty(paramPrefix)) {
            paramPrefix = "";
        }
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();

        if (CollectionUtils.isNotEmpty(workItemHistoryFilter.getFieldTypes())) {
            queryConditions.add("field_type IN (:" + paramPrefix + "fieldTypes)");
            queryParams.put(paramPrefix + "fieldTypes", workItemHistoryFilter.getFieldTypes());
        }

        return Query.builder()
                .where(Query.conditions(queryConditions, queryParams), Query.Condition.AND)
                .build();
    }
}
