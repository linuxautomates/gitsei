package io.levelops.commons.databases.utils;

import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbStackedAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.models.Query;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class WorkItemsReportUtil {

    public static String getStackQuery(Query acrossQuery, WorkItemsFilter filter, WorkItemsFilter.DISTINCT stack) {
        Query stackGroupByCriteria = WorkItemQueryCriteria.getGroupByCriteria(filter, stack, true);

        List<Query.SelectField> stackSelectFields = acrossQuery.getSelectFields();
        stackSelectFields.addAll(stackGroupByCriteria.getSelectFields());

        List<Query.GroupByField> stackGroupByFields = acrossQuery.getGroupByFields();
        stackGroupByFields.addAll(stackGroupByCriteria.getGroupByFields());
        acrossQuery.getCriteria().getConditions().addAll(stackGroupByCriteria.getCriteria().getConditions());

        addGroupBySelect(stackSelectFields, WorkItemQueryCriteria.getGroupByKey(filter, stack, true), stack);

        Query stackQuery =  Query.builder()
                .select(stackSelectFields)
                .from(acrossQuery.getFromFields())
                .where(acrossQuery.getCriteria(), Query.Condition.AND)
                .groupBy(stackGroupByFields)
                .orderBy(acrossQuery.getOrderByFields())
                .build();

        String stackQuerySql = stackQuery.toSql();
        log.info("stack sql = {}", stackQuerySql);
        log.info("params = {}", acrossQuery.getCriteria().getQueryParams());

        return stackQuerySql;
    }

    private static void addGroupBySelect(List<Query.SelectField> stackSelectFields, String groupByKey, WorkItemsFilter.DISTINCT stack) {
        boolean contains = false;
        for(Query.SelectField selectField : stackSelectFields) {
            if(selectField.getField().equalsIgnoreCase(groupByKey) ||
                    (StringUtils.isNotEmpty(selectField.getAlias()) && selectField.getAlias().equalsIgnoreCase(groupByKey))) {
                contains = true;
                break;
            }
        }
        if(!contains)
            stackSelectFields.add(Query.selectField(groupByKey, null));
        if (WorkItemsFilter.isAcrossUsers(stack)) {
            stackSelectFields.add(Query.selectField(stack.toString(), null));
        }
    }

    public static List<DbAggregationResult> mergeResult(List<DbAggregationResult> aggResults,
                                                        List<DbStackedAggregationResult> stackAggResults) {
        Map<String, List<DbAggregationResult>> acrossKeyToStacks = new HashMap<>();
        for (DbStackedAggregationResult stackAggResult : stackAggResults) {
            if (stackAggResult.getStackedAggResult() != null) {
                String key = String.join("_", stackAggResult.getRowKey(), stackAggResult.getRowAdditionalKey());
                if (acrossKeyToStacks.containsKey(key)) {
                    List<DbAggregationResult> dbStackedAggregationResults = new ArrayList<>(acrossKeyToStacks.get(key));
                    dbStackedAggregationResults.add(stackAggResult.getStackedAggResult());
                    acrossKeyToStacks.put(key, dbStackedAggregationResults);
                } else {
                    acrossKeyToStacks.put(key, List.of(stackAggResult.getStackedAggResult()));
                }
            }
        }
        List<DbAggregationResult> results = new ArrayList<>();
        aggResults.forEach(aggResult -> results.add(aggResult.toBuilder()
                .stacks(acrossKeyToStacks.getOrDefault(String.join("_", aggResult.getKey(), aggResult.getAdditionalKey()), null))
                .build()));
        return results;
    }

    private static List<Query.SelectField> getFieldNames(List<Query.GroupByField> groupByFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        groupByFields.forEach(groupByField -> selectFieldWithoutAlias.add(Query.selectField(groupByField.getField(), null)));
        return selectFieldWithoutAlias;
    }
}
