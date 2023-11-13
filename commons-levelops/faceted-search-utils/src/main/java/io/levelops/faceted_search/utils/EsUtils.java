package io.levelops.faceted_search.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class EsUtils {

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
    public static List<StringTermsBucket> getStringTermsBucketList(JiraIssuesFilter.DISTINCT stack,
                                                                   WorkItemsFilter.DISTINCT workItemStack,
                                                                   Map<String, Aggregate> aggregations) {
        if (stack == null) {
            return workItemStack != WorkItemsFilter.DISTINCT.none ?
                    (aggregations.get("across_" + workItemStack).isSterms() ?
                            aggregations.get("across_" + workItemStack).sterms().buckets().array() : List.of())
                    : List.of();
        } else {
            return stack != JiraIssuesFilter.DISTINCT.none ?
                    (aggregations.get("across_" + stack).isSterms() ?
                            aggregations.get("across_" + stack).sterms().buckets().array() : List.of()) : List.of();
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

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForDateHistogram(DateHistogramBucket dateHistogramBucket) {
        return DbAggregationResult.builder()
                .key(String.valueOf(dateHistogramBucket.key().toInstant().getEpochSecond()))
                .additionalKey(dateHistogramBucket.keyAsString())
                .totalTickets(dateHistogramBucket.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForSterms(StringTermsBucket term) {
        return DbAggregationResult.builder()
                .key(term.key())
                .totalTickets(term.docCount());
    }

    public static DbAggregationResult.DbAggregationResultBuilder getDbAggResultForMultiTerm(MultiTermsBucket multiTerm) {
        return DbAggregationResult.builder()
                .key(multiTerm.key().get(0) != null ? multiTerm.key().get(0) : null)
                .additionalKey(multiTerm.key().get(1) != null ? multiTerm.key().get(1) : null)
                .totalTickets(multiTerm.docCount());
    }



    public static String isNotEmpty(String key) {
        return StringUtils.isNotEmpty(key) ? key : null;
    }


    public static String getCustomFieldColumn(String customFieldType, String esCustomColumn) {
        switch (customFieldType) {
            case "integer":
                esCustomColumn = "int";
                break;
            case "long":
                esCustomColumn = "long";
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
            case "string":
            case "option-with-child":
            case "option":
            default:
                esCustomColumn = "str";
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

    public static Query getRangeQueryForTimeinMills(String field, ImmutablePair<Long, Long> range) {
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        rangeQueryBuilder.field(field);
        rangeQueryBuilder.timeZone("UTC");
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

    public static void getRegex(String begins, String ends, String contains, String key, List<Query> queries) {
        if (begins != null) {
            queries.add(getWildCardQuery(key, begins + "*"));
        }
        if (ends != null) {
            queries.add(getWildCardQuery(key, "*" + ends));
        }
        if (contains != null) {
            queries.add(getWildCardQuery(key, "*" + contains + "*"));
        }
    }

    public static Query getWildCardQuery(String field, String wildcard) {
        return WildcardQuery.of(q -> q
                .field(field)
                .wildcard(wildcard))._toQuery();
    }

    public static Query getNestedWildCardQuery(String path, String field, String wildcard) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(getWildCardQuery(field, wildcard)))
                        )
                ))._toQuery();
    }

    public static Query getNestedQuery(String path, String fieldName, List<String> values) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(getQuery(fieldName, values)))
                        )
                )
        )._toQuery();
    }

    public static Query getNestedRangeTimeQuery(String path, String fieldName, ImmutablePair<Long, Long> range) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(getRangeQueryForTimeinMills(fieldName, range)))
                        )
                )
        )._toQuery();
    }

    public static Query getNestedRangeQueryForNumbers(String path, String fieldName,  JsonData lt, JsonData gt) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(getRangeQueryForNumbers(fieldName, lt, gt)))
                        )
                )
        )._toQuery();
    }

    public static Query getRangeQueryForNumbers(String field, JsonData lt, JsonData gt) {

        RangeQuery.Builder rangeQueryBuilder =  new RangeQuery.Builder().field(field);
        if(lt != null && gt != null){
            rangeQueryBuilder.gte(lt).lt(gt);
        }else if(lt != null) {
            rangeQueryBuilder.lt(lt);
        }else if(gt != null) {
            rangeQueryBuilder.gte(gt);
        }
        return rangeQueryBuilder.build()._toQuery();
    }

    public static String getCalenderInterval(String interval, String date, long dateInMillis) {

        date =  date.substring(0, date.lastIndexOf("-")+3);
        String[] dateArr = date.split("-");
        switch (interval.toLowerCase()) {
            case "year":
                return dateArr[0];
            case "month":
                return dateArr[1]+"-"+dateArr[0];
            case "day":
                return dateArr[2]+"-"+dateArr[1]+"-"+dateArr[0];
            case "quarter":
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(dateInMillis);
                int month = cal.get(Calendar.MONTH);
                int quarter = (month / 3) + 1;
                return "Q"+quarter+"-"+dateArr[0];
            case "day_of_week":
            case "week":
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(dateInMillis);
                int weekNumber = calendar.get(Calendar.WEEK_OF_YEAR);
                return weekNumber+"-"+dateArr[0];
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided " + interval);
        }

    }

    public static ImmutablePair<Long, Long> getTimeRangeInMillis(Long left, Long right){

        left = left == null ? null : TimeUnit.SECONDS.toMillis(left);
        right = right == null ? null : TimeUnit.SECONDS.toMillis(right);

        return ImmutablePair.of(left, right);
    }

    public static Map<String, Aggregation> getTermsAgg(Map<String, Aggregation> aggs, String across) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("across_" + across, Aggregation.of(a -> a
                .terms(t -> t
                        .field(across)
                        .size(Integer.MAX_VALUE)
                )
                .aggregations(aggs)
        ));
        return aggConditions;
    }

    public static Map<String, Aggregation> getTermsAggWithMissing(Map<String, Aggregation> aggs, String across) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("across_" + across, Aggregation.of(a -> a
                .terms(t -> t
                        .field(across)
                        .size(Integer.MAX_VALUE)
                        .missing(0d)
                )
                .aggregations(aggs)
        ));
        return aggConditions;
    }

    public static Map<String, Aggregation> getSumAgg(String field) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("sum_" + field, Aggregation.of(a -> a
                .sum(s -> s
                        .field(field)
                )
        ));
        return aggConditions;
    }

    public static Map<String, Aggregation> getDateHistogramAgg(Map<String, Aggregation> aggs, String field, CalendarInterval interval, String format) {
        if (interval == null) {
            throw new RuntimeException("Both Calendar Interval is null & script is empty!");
        }
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("date_histo_" + field, Aggregation.of(a -> a
                .dateHistogram(d -> d.field(field)
                        .calendarInterval(interval)
                        .format(format))
                .aggregations(aggs)
        ));
        return aggConditions;
    }

    /**
     * Returns either a Date Histogram or a Terms Aggs
     * @param aggs
     * @param field
     * @param interval
     * @param format
     * @param script
     * @return
     */
    public static Map<String, Aggregation> getDateHistogramAgg(Map<String, Aggregation> aggs, String field, CalendarInterval interval, String format, String script) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        if (interval != null) {
            aggConditions.put("date_histo_" + field, Aggregation.of(a -> a
                    .dateHistogram(d -> d.field(field)
                            .calendarInterval(interval)
                            .format(format))
                    .aggregations(aggs)
            ));
        } else if (StringUtils.isNotEmpty(script)) {
            aggConditions.put("date_histo_" + field, Aggregation.of(a -> a
                    .terms(TermsAggregation.of(t -> t
                            .script(s -> s
                                    .inline(i -> i
                                            .source(script)
                                    )
                            ))
                    )
                    .aggregations(aggs)
            ));
        } else {
            throw new RuntimeException("Both Calendar Interval is null & script is empty!");
        }
        return aggConditions;
    }

    public static Map<String, Aggregation> getReverseNestedAgg(Map<String, Aggregation> aggs, String aggName) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("reverse_" + aggName, Aggregation.of(a -> a
                .reverseNested(r -> r.path(null))
                .aggregations(aggs)
        ));
        return aggConditions;
    }



    public static Map<String, Aggregation> getNestedAgg(Map<String, Aggregation> aggs, String path) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("nested_" + path, Aggregation.of(a -> a
                .nested(n -> n
                        .path(path)
                )
                .aggregations(aggs)
        ));
        return aggConditions;
    }

    public static Map<String, Aggregation> getFilterAgg(Map<String, Aggregation> aggs, String field, List<String> values) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("filter_" + field, Aggregation.of(a -> a
                .filter(f -> f
                        .terms(t -> t
                                .field(field)
                                .terms(TermsQueryField.of(termsField -> termsField
                                        .value(values.stream()
                                                .map(str -> new FieldValue.Builder()
                                                        .stringValue(str)
                                                        .build())
                                                .collect(Collectors.toList()))))
                        ))
                .aggregations(aggs)
        ));
        return aggConditions;
    }

    public static Map<String, Aggregation> getFilterExcludeAgg(Map<String, Aggregation> aggs, String field, List<String> values) {
        Map<String, Aggregation> aggConditions = new HashMap<>();
        aggConditions.put("filter_" + field, Aggregation.of(a -> a
                .filter(f -> f
                        .bool(b ->
                                b.mustNot(q ->
                                        q.terms(t -> t
                                                .field(field)
                                                .terms(TermsQueryField.of(termsField -> termsField
                                                        .value(values.stream()
                                                                .map(str -> new FieldValue.Builder()
                                                                        .stringValue(str)
                                                                        .build())
                                                                .collect(Collectors.toList())))))
                                )
                        )
                )
                .aggregations(aggs)
        ));
        return aggConditions;
    }

    public static Map<String, CompositeAggregationSource> getTermCompositeSource(String sourceName, String field) {
        Map<String, CompositeAggregationSource> compositeSources = new HashMap<>();
        String name = (StringUtils.isNotBlank(sourceName)) ?  sourceName : "comp_key_" + sourceName;
        CompositeAggregationSource s = CompositeAggregationSource.of(c -> c.terms(t -> t.field(field)));
        compositeSources.put(name, s);
        return compositeSources;
    }
    public static Map<String, CompositeAggregationSource> getDateHistoCompositeSource(String sourceName, String field, CalendarInterval interval, String format) {
        Map<String, CompositeAggregationSource> compositeSources = new HashMap<>();
        String name = (StringUtils.isNotBlank(sourceName)) ?  sourceName : "date_histo_" + sourceName;
        CompositeAggregationSource s = CompositeAggregationSource.of(c -> c.dateHistogram(d -> d
                .field(field)
                .calendarInterval(interval)
                .format(format)
        ));
        compositeSources.put(name, s);
        return compositeSources;
    }

    public static CompositeAggregation getCompositeAgg(Integer size, List<Map<String, CompositeAggregationSource>> compositeCources, Map<String, String> afterKey) {
        Integer sanitizedSize = ObjectUtils.firstNonNull(size, 1000);
        if (MapUtils.isEmpty(afterKey)) {
            CompositeAggregation c = CompositeAggregation.of(a -> a
                    .size(sanitizedSize)
                    .sources(compositeCources));
            return c;
        } else {
            CompositeAggregation c = CompositeAggregation.of(a -> a
                    .size(sanitizedSize)
                    .sources(compositeCources)
                    .after(afterKey));
            return c;
        }
    }

    public static Map<String, Aggregation> combineCompositeAndAgg(String aggName, Map<String, Aggregation> aggs, CompositeAggregation compositeAgg) {
        Aggregation baAgg = Aggregation.of(a -> a
                .composite(compositeAgg)
                .aggregations(aggs)
        );
        return Map.of(aggName, baAgg);
    }
}