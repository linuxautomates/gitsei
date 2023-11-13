package io.levelops.faceted_search.converters;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeField;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.faceted_search.db.models.workitems.EsExtensibleField;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class EsConverterUtils {
    private static final Map<String, String> ATTRIBUTES_FIELD_TYPE = Map.of("project", "string",
            "code_area", "string", "organization", "string", "acceptance_criteria", "string",
            "teams", "arr");

    public static Map<String, Object> getConvertedExtensibleFields(List<EsExtensibleField> extensibleFields,
                                                                   List<DbWorkItemField> dbWorkItemFields) {
        Map<String, Object> convertedFields = new HashMap<>();
        extensibleFields.forEach(extensibleField -> {
            String type = StringUtils.EMPTY;
            if (dbWorkItemFields != null) { // For customFields
                getCustomFieldType(dbWorkItemFields, null, extensibleField.getName());
            } else { // For Attributes
                type = ATTRIBUTES_FIELD_TYPE.getOrDefault(extensibleField.getName(), "string");
            }
            Object val = getExtensibleValueFromTypeForWorkItem(extensibleField, StringUtils.firstNonEmpty(type, "string"));
            convertedFields.put(extensibleField.getName(), val);
        });
        return convertedFields;
    }


    public static Map<String, Object> getConvertedExtensibleFieldsForJira(List<EsExtensibleField> extensibleFields,
                                                                          List<DbJiraField> dbJiraFields) {
        Map<String, Object> convertedFields = new HashMap<>();
        extensibleFields.forEach(extensibleField -> {
            String type = null;
            if (dbJiraFields != null) {
                type = getCustomFieldType(null, dbJiraFields, extensibleField.getName());
            }
            Object val = getExtensibleValueFromTypeForJiraIssue(extensibleField, type);
            convertedFields.put(extensibleField.getName(), val);
        });
        return convertedFields;
    }

    //-- Get Date Histogram buckets for stacks
    public static List<DateHistogramBucket> getDateHistogramBuckets(JiraIssuesFilter.DISTINCT stack,
                                                                    WorkItemsFilter.DISTINCT workItemStack,
                                                                    Map<String, Aggregate> aggregations) {
        if (stack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_" + workItemStack).isDateHistogram() ?
                            aggregations.get("across_" + workItemStack).dateHistogram().buckets().array() : List.of())
                    : List.of();
        } else {
            return stack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_" + stack).isDateHistogram() ?
                            aggregations.get("across_" + stack).dateHistogram().buckets().array() : List.of()) : List.of();
        }
    }

    //-- Get String Terms buckets for stacks
    public static List<StringTermsBucket> getStringTermsBucketList(JiraIssuesFilter.DISTINCT jirastack,
                                                                   WorkItemsFilter.DISTINCT workItemStack,
                                                                   Map<String, Aggregate> aggregations) {
        if (jirastack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_" + workItemStack).isSterms() ?
                            aggregations.get("across_" + workItemStack).sterms().buckets().array() : List.of())
                    : List.of();
        } else {
            return jirastack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_" + jirastack).isSterms() ?
                            aggregations.get("across_" + jirastack).sterms().buckets().array() : List.of()) : List.of();
        }
    }

    //-- Get Double Terms buckets for stacks
    public static List<DoubleTermsBucket> getDoubleTermsBucketList(JiraIssuesFilter.DISTINCT jirastack,
                                                                   WorkItemsFilter.DISTINCT workItemStack,
                                                                   Map<String, Aggregate> aggregations) {
        if (jirastack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_" + workItemStack).isDterms() ?
                            aggregations.get("across_" + workItemStack).dterms().buckets().array() : List.of())
                    : List.of();
        } else {
            return jirastack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_" + jirastack).isDterms() ?
                            aggregations.get("across_" + jirastack).dterms().buckets().array() : List.of()) : List.of();
        }
    }

    //-- Get Long Terms buckets for stacks
    public static List<LongTermsBucket> getLongTermsBucketList(JiraIssuesFilter.DISTINCT jirastack,
                                                               WorkItemsFilter.DISTINCT workItemStack,
                                                               Map<String, Aggregate> aggregations) {
        if (jirastack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_" + workItemStack).isLterms() ?
                            aggregations.get("across_" + workItemStack).lterms().buckets().array() : List.of())
                    : List.of();
        } else {
            return jirastack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_" + jirastack).isDterms() ?
                            aggregations.get("across_" + jirastack).lterms().buckets().array() : List.of()) : List.of();
        }
    }

    public static List<StringTermsBucket> getStringTermBucketsForAttributes(String attributeStack,
                                                                            JiraIssuesFilter.DISTINCT jirastack,
                                                                            WorkItemsFilter.DISTINCT workItemStack,
                                                                            Map<String, Aggregate> aggregations) {

        if (jirastack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_attribute").isNested() ?
                            aggregations.get("across_attribute").nested().aggregations()
                                    .get("filter_attributes_name").filter().aggregations().get("across_attributes_" + attributeStack)
                                    .sterms().buckets().array() : List.of())
                    : List.of();
        } else {
            return jirastack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_attribute").isNested() ?
                            aggregations.get("across_attribute").nested().aggregations()
                                    .get("filter_attributes_name").filter().aggregations().get("across_attributes_" + attributeStack)
                                    .sterms().buckets().array() : List.of()) : List.of();
        }
    }

    public static List<StringTermsBucket> getStringTermBucketsForCustomFields(JiraIssuesFilter.DISTINCT jirastack,
                                                                              WorkItemsFilter.DISTINCT workItemStack,
                                                                              Map<String, Aggregate> aggregations) {
        if (jirastack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_custom_field").isNested() ?
                            aggregations.get("across_custom_field").nested().aggregations().get("filter_custom_fields_name")
                                    .filter().aggregations().get("across_custom_fields_type").sterms().buckets().array() : List.of())
                    : List.of();
        } else {
            return jirastack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_custom_field").isNested() ?
                            aggregations.get("across_custom_field").nested().aggregations().get("filter_custom_fields_name")
                                    .filter().aggregations().get("across_custom_fields_type").sterms().buckets().array() : List.of()) : List.of();
        }
    }


    //-- Get Multi Term buckets for stacks
    public static List<MultiTermsBucket> getMultiTermsBucketList(JiraIssuesFilter.DISTINCT jiraStack,
                                                                 WorkItemsFilter.DISTINCT workItemsStack,
                                                                 Map<String, Aggregate> aggregations) {
        if (jiraStack == null) { // For WorkItem Stack
            return workItemsStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_" + workItemsStack).isMultiTerms() ?
                            aggregations.get("across_" + workItemsStack).multiTerms().buckets().array() : List.of()) : List.of();
        } else { // For Jira stack
            return jiraStack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_" + jiraStack).isMultiTerms() ?
                            aggregations.get("across_" + jiraStack).multiTerms().buckets().array() : List.of()) : List.of();
        }
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForDateHistogram(DateHistogramBucket dateHistogramBucket, Boolean isQuarter, Boolean valuesOnly) {
        return DbAggregationResult.builder()
                .key(String.valueOf(dateHistogramBucket.key().toInstant().getEpochSecond()))
                .additionalKey((isQuarter ? "Q" : "") + dateHistogramBucket.keyAsString())
                .totalTickets(valuesOnly ? null : dateHistogramBucket.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForDateHistogramForAcrossTrend(DateHistogramBucket dateHistogramBucket, Boolean isQuarter, Boolean valuesOnly) {
        return DbAggregationResult.builder()
                .key(String.valueOf(dateHistogramBucket.key().toInstant().getEpochSecond()))
                .additionalKey((isQuarter ? "Q" : "") + dateHistogramBucket.keyAsString())
                .totalTickets(valuesOnly ? null : dateHistogramBucket.aggregations().get("trend_count").cardinality().value());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForDateHistogramForAcrossTrendForStages(
            DateHistogramBucket dateHistogramBucket, Boolean isQuarter, Boolean valuesOnly) {
        return DbAggregationResult.builder()
                .key(String.valueOf(dateHistogramBucket.key().toInstant().getEpochSecond()))
                .additionalKey((isQuarter ? "Q" : "") + dateHistogramBucket.keyAsString())
                .totalTickets(valuesOnly ? null : dateHistogramBucket.aggregations().get("across_nested").nested().aggregations()
                        .get("trend_count").cardinality().value());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForStrTerms(StringTermsBucket term, Boolean valuesOnly) {
        return DbAggregationResult.builder()
                .key(term.key())
                .totalTickets(valuesOnly ? null : term.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForDoubleTerms(DoubleTermsBucket term, Boolean valuesOnly) {
        return DbAggregationResult.builder()
                .key(String.valueOf(term.key()))
                .totalTickets(valuesOnly ? null : term.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForLongTerms(LongTermsBucket term, Boolean valuesOnly) {
        return DbAggregationResult.builder()
                .key(String.valueOf(term.key()))
                .totalTickets(valuesOnly ? null : term.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForMultiTerm(MultiTermsBucket multiTerm) {
        return DbAggregationResult.builder()
                .key(multiTerm.key().get(0) != null ? multiTerm.key().get(0) : null)
                .additionalKey(multiTerm.key().get(1) != null ? multiTerm.key().get(1) : null)
                .totalTickets(multiTerm.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForMultiTermJira(String key, String additionalKey, Long docCount) {
        return DbAggregationResult.builder()
                .key(key)
                .additionalKey(additionalKey)
                .totalTickets(docCount);
    }

    public static List<String> getVersionsFromEsWorkItemVersions(List<EsWorkItem.EsVersion> versions) {
        return versions.stream().map(EsWorkItem.EsVersion::getName).collect(Collectors.toList());
    }

    public static String isNotEmpty(String key) {
        return StringUtils.isNotEmpty(key) ? key : null;
    }

    public static Object getExtensibleValueFromTypeForWorkItem(EsExtensibleField extensibleField, String type) {
        Object val = null;
        switch (type) {
            case "integer":
                val = extensibleField.getIntValue();
                break;
            case "long":
                val = extensibleField.getLongValue();
                break;
            case "string":
                val = extensibleField.getStrValue();
                break;
            case "dateTime":
                val = extensibleField.getDateValue();
                break;
            case "boolean":
                val = extensibleField.getBoolValue();
                break;
            case "float":
                val = extensibleField.getFloatValue();
                break;
            default:
                val = extensibleField.getArrValue();
                break;
        }
        return val;
    }

    public static Object getExtensibleValueFromTypeForJiraIssue(EsExtensibleField extensibleField, String type) {
        Object val = null;
        switch (type) {
            case "integer":
                val = extensibleField.getIntValue();
                break;
            case "long":
                val = extensibleField.getLongValue();
                break;
            case "dateTime":
                val = extensibleField.getDateValue();
                break;
            case "boolean":
                val = extensibleField.getBoolValue();
                break;
            case "float":
                val = extensibleField.getFloatValue();
                break;
            case "array":
                val = extensibleField.getArrValue();
                break;
            case "number":
                val = extensibleField.getFloatValue();
                if(val !=null) {
                    break;
                }
            case "string":
            case "option-with-child":
            case "option":
            default:
                val = extensibleField.getStrValue();
                break;
        }
        return val;
    }

    public static String getCustomFieldColumn(String customFieldType, String esCustomColumn) {
        switch (customFieldType) {
            case "integer":
                esCustomColumn = "int";
                break;
            case "long":
                esCustomColumn = "long";
                break;
            case "string":
                esCustomColumn = "str";
                break;
            case "dateTime":
                esCustomColumn = "date";
                break;
            case "boolean":
                esCustomColumn = "bool";
                break;
            case "array":
                esCustomColumn = "arr";
                break;
            case "float":
            case "number":
                esCustomColumn = "float";
                break;
            default:
                break;
        }
        return esCustomColumn;
    }

    public static Query getNestedQuery(String path, String fField, String key, String sField, List<String> values) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(getQuery(fField, List.of(key)), getQuery(sField, values)))
                        )
                )
        )._toQuery();
    }

    public static void createExistsQueryForMissingField(String field, boolean shouldBeMissing,
                                                        List<Query> excludesQueryConditions,
                                                        List<Query> includesQueryConditions) {
        if (shouldBeMissing) {
            excludesQueryConditions.add(ExistsQuery.of(q -> q
                    .field(field))._toQuery());
        } else {
            includesQueryConditions.add(ExistsQuery.of(q -> q
                    .field(field))._toQuery());
        }
    }

    public static Query getQueryForMissingField(boolean shouldBeMissing, String field, String value) {
        Query query;
        if (shouldBeMissing) {
            query = TermQuery.of(q -> q
                    .field(field)
                    .value(value))._toQuery();
        } else {
            query = ExistsQuery.of(q -> q
                    .field(field))._toQuery();
        }
        return query;
    }

    public static Query getQuery(String field, List<String> values) {
        return TermsQuery.of(q -> q
                .field(field)
                .terms(TermsQueryField.of(termsField -> termsField
                        .value(values.stream()
                                .map(str -> new FieldValue.Builder()
                                        .stringValue(str)
                                        .build())
                                .collect(Collectors.toList()))))
        )._toQuery();
    }

    public static Query getRangeQuery(String field, JsonData gt, JsonData lt) {
        return RangeQuery.of(q -> q
                        .field(field)
                        .timeZone("UTC")
                        .gt(gt)
                        .lt(lt))
                ._toQuery();
    }

    public static Query getRangeQueryForTime(String field, ImmutablePair<Long, Long> range) {
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        rangeQueryBuilder.field(field);
        rangeQueryBuilder.timeZone("UTC");
        rangeQueryBuilder.format("epoch_second");
        if (range.getLeft() != null) {
            rangeQueryBuilder.gt(JsonData.of(range.getLeft()));
        }
        if (range.getRight() != null) {
            rangeQueryBuilder.lt(JsonData.of(range.getRight()));
        }
        return rangeQueryBuilder.build()._toQuery();
    }

    public static String getCustomFieldType(List<DbWorkItemField> workItemCustomFields,
                                            List<DbJiraField> dbJiraFields, String customKey) {
        Optional<DbWorkItemField> workItemCustomField = Optional.empty();
        Optional<DbJiraField> jiraCustomField = Optional.empty();

        if (CollectionUtils.isNotEmpty(workItemCustomFields)) {
            workItemCustomField = workItemCustomFields.stream()
                    .filter(dbWorkItemField -> dbWorkItemField.getFieldKey().equalsIgnoreCase(customKey))
                    .findFirst();
        }
        if (CollectionUtils.isNotEmpty(dbJiraFields)) {
            jiraCustomField = dbJiraFields.stream()
                    .filter(dbJiraField -> dbJiraField.getFieldKey().equalsIgnoreCase(customKey))
                    .findFirst();
        }

        String customFieldType = StringUtils.EMPTY;
        if (workItemCustomField.isPresent()) {
            customFieldType = workItemCustomField.get().getFieldType();
        } else if (jiraCustomField.isPresent()) {
            customFieldType = jiraCustomField.get().getFieldType();
        }
        return customFieldType;
    }

    public static List<EsWorkItem.EsVersion> getVersions(List<String> versions) {
        List<EsWorkItem.EsVersion> esVersions = new ArrayList<>();
        versions.forEach(version -> esVersions.add(EsWorkItem.EsVersion.builder()
                .name(version)
                .build()));
        return esVersions;
    }

    public static List<EsExtensibleField> getCustomFields(DbWorkItem workItem, List<DbWorkItemField> workItemFields) {
        Map<String, Object> customFields = workItem.getCustomFields();
        if (customFields == null || customFields.isEmpty()) {
            return List.of();
        }
        log.debug("In getCustomFields for workItem: {}", workItem);
        List<EsExtensibleField> esExtensibleFields = new ArrayList<>();
        for (Map.Entry<String, Object> objectEntry : customFields.entrySet()) {
            for (DbWorkItemField workItemField : workItemFields) {
                if (workItem.getIntegrationId().equals(workItemField.getIntegrationId())
                        && objectEntry.getKey().equals(workItemField.getName())) {
                    log.debug("fieldName: {}, fieldType :{} for workItem Id: {}", workItemField.getName(),
                            workItemField.getFieldType(), workItem.getWorkItemId());
                    switch (workItemField.getFieldType()) {
                        case "integer":
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .intValue((Integer) objectEntry.getValue())
                                    .build());
                            break;
                        case "long":
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .longValue((Long) objectEntry.getValue())
                                    .build());
                            break;
                        case "string":
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .strValue((String) objectEntry.getValue())
                                    .build());
                            break;
                        case "dateTime":
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .dateValue((Timestamp) objectEntry.getValue())
                                    .build());
                            break;
                        case "boolean":
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .boolValue((Boolean) objectEntry.getValue())
                                    .build());
                            break;
                        case "float":
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .floatValue((Float) objectEntry.getValue())
                                    .build());
                            break;
                        default:
                            esExtensibleFields.add(EsExtensibleField.builder()
                                    .arrValue(objectEntry.getValue())
                                    .build());
                            break;
                    }
                }
            }
        }
        return esExtensibleFields;
    }

    public static List<EsExtensibleField> getAttributes(Map<String, Object> extensibleFields) {
        if (extensibleFields == null || extensibleFields.isEmpty()) {
            return List.of();
        }
        List<EsExtensibleField> esExtensibleFields = new ArrayList<>();
        for (String name : extensibleFields.keySet()) {
            Object val = extensibleFields.get(name);
            log.debug("In getExtensibleFieldValues for name: {}, the values class is {}", name, val.getClass());
            if (val instanceof String) {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .strValue((String) val)
                        .build());
            } else if (val instanceof Integer) {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .intValue((Integer) val)
                        .build());
            } else if (val instanceof Float) {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .floatValue((Float) val)
                        .build());
            } else if (val instanceof Long) {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .longValue((Long) val)
                        .build());
            } else if (val instanceof Boolean) {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .boolValue((Boolean) val)
                        .build());
            } else if (val instanceof Timestamp) {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .dateValue((Timestamp) val)
                        .build());
            } else {
                esExtensibleFields.add(EsExtensibleField.builder()
                        .name(name)
                        .arrValue(val)
                        .build());
            }
        }
        return esExtensibleFields;
    }

    public static Map<String, List<RuntimeField>> getRuntimeFields(JiraIssuesFilter filter) {
        switch (filter.getCalculation()) {
            case resolution_time:
                return Map.of("solve_time", List.of(RuntimeField.of(r0 -> r0
                        .type(RuntimeFieldType.Long)
                        .script(r1 -> r1
                                .inline(InlineScript.of(s -> s
                                        .source("long sol_t = 0L;" +
                                                "if(doc['resolved_at'].size() == 0) {\n" +
                                                "    sol_t = new Date().getTime() - doc['created_at'].value.toInstant().getEpochSecond();\n" +
                                                " } else {\n" +
                                                "     sol_t = doc['resolved_at'].value.toInstant().getEpochSecond() - " +
                                                "      doc['created_at'].value.toInstant().getEpochSecond();\n" +
                                                " }" +
                                                "emit(sol_t);")))))));
            case response_time:
                return Map.of("resp_time", List.of(RuntimeField.of(r0 -> r0
                        .type(RuntimeFieldType.Long)
                        .script(r1 -> r1
                                .inline(InlineScript.of(s -> s
                                        .source("long res_t = 0L;" +
                                                "if(doc['first_comment_at'].size() == 0) {\n" +
                                                "    res_t = new Date().getTime() - doc['created_at'].value.toInstant().getEpochSecond();\n" +
                                                " } else {\n" +
                                                "     res_t = doc['first_comment_at'].value.toInstant().getEpochSecond() - " +
                                                "      doc['created_at'].value.toInstant().getEpochSecond();\n" +
                                                " }" +
                                                "emit(sol_t);")))))));
            case ticket_count:
            case age:
            default:
                return Map.of();
        }
    }

    @NotNull
    public static List<DbAggregationResult> sanitizeResponse(List<DbAggregationResult> dbAggregationResults) {
        return dbAggregationResults.stream()
                .map(dbAggregationResult -> {
                    if ("null".equalsIgnoreCase(dbAggregationResult.getKey())) {
                        return dbAggregationResult.toBuilder().key(null).build();
                    }
                    return dbAggregationResult;
                }).collect(Collectors.toList());
    }
}
