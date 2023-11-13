package io.levelops.commons.databases.query_criteria;

import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.models.Query;
import io.levelops.commons.utils.NumberUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkItemTimelineQueryCriteria {

    public static Query getSelectionCriteria(String tblQualifier,WorkItemsTimelineFilter workItemsTimelineFilter, String paramPrefix) {
        List<Query.SelectField> timelineSelectFields = new ArrayList<>();
        tblQualifier = tblQualifier == null ? "" : tblQualifier;
        timelineSelectFields.add(Query.selectField(tblQualifier+"integration_id", "timeline_integration_id"));
        timelineSelectFields.add(Query.selectField(tblQualifier+"workitem_id", "timeline_workitem_id"));
        timelineSelectFields.add(Query.selectField(tblQualifier+"field_value", "timeline_field_value"));
        timelineSelectFields.add(Query.selectField(tblQualifier+"field_type", "timeline_field_type"));
        if (StringUtils.isEmpty(paramPrefix)) {
            paramPrefix = "";
        }
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();

        if (CollectionUtils.isNotEmpty(workItemsTimelineFilter.getWorkItemIds())) {
            queryConditions.add("workitem_id IN (:" + paramPrefix + "workItemIds)");
            queryParams.put(paramPrefix + "workItemIds", workItemsTimelineFilter.getWorkItemIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsTimelineFilter.getIntegrationIds())) {
            queryConditions.add("integration_id IN (:" + paramPrefix + "integs)");
            queryParams.put(paramPrefix + "integs",
                    workItemsTimelineFilter.getIntegrationIds()
                            .stream().map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(workItemsTimelineFilter.getFieldTypes())) {
            queryConditions.add("field_type IN (:" + paramPrefix + "fieldTypes)");
            queryParams.put(paramPrefix + "fieldTypes", workItemsTimelineFilter.getFieldTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsTimelineFilter.getFieldValues())) {
            queryConditions.add("field_value IN (:" + paramPrefix + "fieldValues)");
            queryParams.put(paramPrefix + "fieldValues", workItemsTimelineFilter.getFieldValues());
        }
        if (CollectionUtils.isNotEmpty(workItemsTimelineFilter.getExcludeFieldValues())) {
            queryConditions.add("field_value NOT IN (:" + paramPrefix + "excludeFieldValues)");
            queryParams.put(paramPrefix + "excludeFieldValues", workItemsTimelineFilter.getExcludeFieldValues());
        }

        return Query.builder()
                .select(timelineSelectFields)
                .where(Query.conditions(queryConditions, queryParams), Query.Condition.AND)
                .build();
    }

    public static Query getSelectionCriteria(WorkItemsTimelineFilter workItemsTimelineFilter, String paramPrefix) {
        return getSelectionCriteria("",workItemsTimelineFilter,paramPrefix);
    }
}
