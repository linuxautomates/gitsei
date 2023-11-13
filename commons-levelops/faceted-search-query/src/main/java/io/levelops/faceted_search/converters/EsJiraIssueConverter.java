package io.levelops.faceted_search.converters;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.PercentilesBucketAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TDigestPercentilesAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResult.DbAggregationResultBuilder;
import io.levelops.commons.databases.models.response.JiraIssueSprintMappingAggResult;
import io.levelops.commons.faceted_search.db.models.workitems.EsDevProdWorkItemResponse;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.converters.EsConverterUtils.getConvertedExtensibleFieldsForJira;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDateHistogramBuckets;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDateHistogram;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDateHistogramForAcrossTrend;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDoubleTerms;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForLongTerms;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForMultiTerm;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForMultiTermJira;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForStrTerms;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDoubleTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getLongTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getMultiTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getStringTermBucketsForCustomFields;
import static io.levelops.faceted_search.converters.EsConverterUtils.getStringTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getVersionsFromEsWorkItemVersions;
import static io.levelops.faceted_search.converters.EsConverterUtils.isNotEmpty;

/*
 * This class is meant for converting ES model to DB model.
 */
public class EsJiraIssueConverter {

    public static DbJiraIssue getIssueFromEsWorkItem(EsWorkItem esWorkItem, List<DbJiraField> dbJiraFields, EsDevProdWorkItemResponse devProdWorkItemResponse,
                                                     boolean hasTicketPortion, boolean hasStoryPointsPortion, boolean hasAssigneeTime, String ticketCategory) {
        DbJiraIssue.DbJiraIssueBuilder jiraIssueBuilder = DbJiraIssue.builder()
                .id(String.valueOf(esWorkItem.getId()))
                .key(esWorkItem.getWorkitemId())
                .project(esWorkItem.getProject())
                .summary(esWorkItem.getSummary())
                .status(isNotEmpty(esWorkItem.getStatus()))
                .issueType(isNotEmpty(esWorkItem.getWorkItemType()))
                .priority(isNotEmpty((esWorkItem.getPriority())))
                .priorityOrder(esWorkItem.getPriorityOrder())
                .integrationId(esWorkItem.getIntegrationId().toString())
                .epic(isNotEmpty(esWorkItem.getEpic()))
                .labels(esWorkItem.getLabels())
                .parentKey(esWorkItem.getParentWorkItemId())
                .numAttachments(esWorkItem.getNumAttachments())
                .assignee(esWorkItem.getAssignee().getDisplayName())
                .assigneeId(esWorkItem.getAssignee() != null ? esWorkItem.getAssignee().getId() : null)
                .firstAssignee(esWorkItem.getFirstAssignee() != null ? esWorkItem.getFirstAssignee().getDisplayName() : null)
                .firstAssigneeId(esWorkItem.getFirstAssignee() != null ? esWorkItem.getFirstAssignee().getId() : null)
                .reporter(esWorkItem.getReporter() != null ? esWorkItem.getReporter().getDisplayName() : null)
                .reporterId(esWorkItem.getReporter() != null ? esWorkItem.getReporter().getId() : null)
                .originalEstimate(esWorkItem.getOriginalEstimate() != null ? esWorkItem.getOriginalEstimate().longValue() : 0L)
                .storyPoints(esWorkItem.getStoryPoints() != null ? esWorkItem.getStoryPoints().intValue() : null)
                .bounces(esWorkItem.getBounces())
                .hops(esWorkItem.getHops())
                .components(esWorkItem.getComponents())
                .descSize(esWorkItem.getDescSize())
                .issueCreatedAt(esWorkItem.getCreatedAt().getTime())
                .issueUpdatedAt(esWorkItem.getUpdatedAt().getTime())
                .firstAttachmentAt(esWorkItem.getFirstAttachmentAt() != null ? esWorkItem.getFirstAttachmentAt().getTime() : 0L)
                .ingestedAt(esWorkItem.getIngestedAt().getTime())
                .firstCommentAt(esWorkItem.getFirstCommentAt() != null ? esWorkItem.getFirstCommentAt().getTime() : 0L)
                .issueResolvedAt(esWorkItem.getResolvedAt() != null ? esWorkItem.getResolvedAt().getTime() : 0L)
                .issueDueAt(esWorkItem.getDueAt() != null ? esWorkItem.getDueAt().getTime() : 0L)
                .versions(getVersionsFromEsWorkItemVersions(esWorkItem.getVersions()))
                .fixVersions(getVersionsFromEsWorkItemVersions(esWorkItem.getFixVersions()))
                .isActive(Boolean.TRUE.equals(esWorkItem.getIsActive()))
                .resolution(esWorkItem.getResolution())
                .statusCategory(esWorkItem.getStatusCategory())
                .sprintIds(CollectionUtils.isNotEmpty(esWorkItem.getSprints()) ? esWorkItem.getSprints().stream().map(EsWorkItem.EsSprint::getSprintId)
                        .filter(Objects::nonNull)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList()) : List.of())
                .customFields(getConvertedExtensibleFieldsForJira(esWorkItem.getCustomFields(), dbJiraFields))
                .assigneeList(getAssignees(esWorkItem.getHistAssignees()))
                .statuses(getStatuses(esWorkItem.getHistStatuses()));
        if (devProdWorkItemResponse != null) {
            jiraIssueBuilder = jiraIssueBuilder
                    .ticketPortion(hasTicketPortion ? devProdWorkItemResponse.getTicketPortion() : null)
                    .storyPointsPortion(hasStoryPointsPortion ? devProdWorkItemResponse.getStoryPointsPortion() : null)
                    .assigneeTime(hasAssigneeTime ? devProdWorkItemResponse.getAssigneeTime().longValue() : null);
        }
        //Allow empty ticketCategory, In the future we might need it.
        if (ticketCategory != null) {
            jiraIssueBuilder = jiraIssueBuilder
                    .ticketCategory(ticketCategory);
        }
        return jiraIssueBuilder.build();
    }

    public static List<DbAggregationResult> getStacks(List<StringTermsBucket> termsBuckets,
                                                      List<MultiTermsBucket> multiTermsBuckets,
                                                      List<DateHistogramBucket> dateHistogramBuckets,
                                                      List<StringTermsBucket> strBucketsForCustomFields,
                                                      List<DoubleTermsBucket> doubleBucketsForCustomFields,
                                                      List<LongTermsBucket> longBucketsFoCustomFields,
                                                      JiraIssuesFilter.DISTINCT stack,
                                                      JiraIssuesFilter.CALCULATION calculation) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        switch (stack) {
            case first_assignee:
            case assignee:
            case reporter:
                termsBuckets.forEach(term -> {
                    List<StringTermsBucket> terms2 = term.aggregations().get("across_" + stack.name()).sterms().buckets().array();
                    terms2.forEach(term2 -> {
                        String key = null;
                        if (!term.key().equals("_UNASSIGNED_")) {
                            key = term2.key();
                        }
                        DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForMultiTermJira(key, term.key(), term2.docCount());
                        dbAggResultBuilder = getCalculation(term2.aggregations(), calculation, dbAggResultBuilder, null);
                        dbAggregationResults.add(dbAggResultBuilder.build());
                    });
                });
                break;
            case issue_created:
            case issue_resolved:
            case issue_due:
            case issue_updated:
            case issue_due_relative:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogram(dateHistogramBucket, false, false);
                    dbAggResultBuilder = getCalculation(dateHistogramBucket.aggregations(), calculation, dbAggResultBuilder, null);
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
            case trend:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, false, false);
                    dbAggResultBuilder = getCalculation(dateHistogramBucket.aggregations(), calculation, dbAggResultBuilder, null);
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
            case custom_field:
                if (CollectionUtils.isNotEmpty(strBucketsForCustomFields)) {
                    strBucketsForCustomFields.forEach(stringTermsBucket -> {
                        DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(stringTermsBucket, false);
                        dbAggResultBuilder = getCalculation(stringTermsBucket.aggregations().get("custom_nested_root").reverseNested().aggregations(),
                                calculation, dbAggResultBuilder, null);
                        dbAggregationResults.add(dbAggResultBuilder.build());
                    });
                } else if (CollectionUtils.isNotEmpty(doubleBucketsForCustomFields)) {
                    doubleBucketsForCustomFields.forEach(doubleTermsBucket -> {
                        DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDoubleTerms(doubleTermsBucket, false);
                        dbAggResultBuilder = getCalculation(doubleTermsBucket.aggregations().get("custom_nested_root").reverseNested().aggregations(),
                                calculation, dbAggResultBuilder, null);
                        dbAggregationResults.add(dbAggResultBuilder.build());
                    });
                } else if (CollectionUtils.isNotEmpty(longBucketsFoCustomFields)) {
                    longBucketsFoCustomFields.forEach(longTermsBucket -> {
                        DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForLongTerms(longTermsBucket, false);
                        dbAggResultBuilder = getCalculation(longTermsBucket.aggregations().get("custom_nested_root").reverseNested().aggregations(),
                                calculation, dbAggResultBuilder, null);
                        dbAggregationResults.add(dbAggResultBuilder.build());
                    });
                }
                break;
            default:
                termsBuckets.forEach(term1 -> {
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(term1, false);
                    dbAggResultBuilder = getCalculation(term1.aggregations(), calculation, dbAggResultBuilder, null);
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    public static List<DbAggregationResult> getStacksForCustomFields(List<StringTermsBucket> stringTermsBuckets,
                                                                     List<DoubleTermsBucket> doubleTermsBuckets,
                                                                     List<LongTermsBucket> longTermsBuckets,
                                                                     List<DateHistogramBucket> dateHistograms, JiraIssuesFilter.DISTINCT stack,
                                                                     JiraIssuesFilter.CALCULATION calculation) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        stringTermsBuckets.forEach(term1 -> {
            AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForStrTerms(term1, false));
            buildStackResults(stack, term1.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForStrTerms(term1, false));
        });
        doubleTermsBuckets.forEach(term2 -> {
            AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForDoubleTerms(term2, false));
            buildStackResults(stack, term2.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForDoubleTerms(term2, false));
        });
        longTermsBuckets.forEach(term3 -> {
            AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForLongTerms(term3, false));
            buildStackResults(stack, term3.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForLongTerms(term3, false));
        });
        dateHistograms.forEach(term4 -> {
            AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForDateHistogram(term4, false, false));
            buildStackResults(stack, term4.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForDateHistogram(term4, false, false));

        });
        return dbAggregationResults;
    }

    private static void buildStackResults(JiraIssuesFilter.DISTINCT stack, Map<String, Aggregate> aggregateMap,
                                          AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder,
                                          JiraIssuesFilter.CALCULATION calculation,
                                          List<DbAggregationResult> dbAggregationResults,
                                          DbAggregationResultBuilder dbAggregationResultBuilder) {
        Map<String, Aggregate> aggregations;
        switch (stack) {
            case custom_field:
                aggregations = aggregateMap.get("custom_nested_root").reverseNested().aggregations();
                dbAggResultBuilder.set(getCalculation(aggregations, calculation, dbAggResultBuilder.get(), null));
                dbAggregationResults.add(dbAggResultBuilder.get().build());
                break;
            case assignee:
            case reporter:
                List<StringTermsBucket> terms2 = aggregateMap.get("across_" + stack.name()).sterms().buckets().array();
                terms2.forEach(termsBucket -> {
                    String key1 = dbAggregationResultBuilder.build().getKey();
                    dbAggResultBuilder.set(getDbAggResultForMultiTermJira(termsBucket.key(), key1, termsBucket.docCount()));
                    dbAggResultBuilder.set(getCalculation(termsBucket.aggregations(), calculation, dbAggResultBuilder.get(), null));
                    dbAggregationResults.add(dbAggResultBuilder.get().build());
                });
                break;
            default:
                dbAggResultBuilder.set(dbAggregationResultBuilder);
                dbAggResultBuilder.set(getCalculation(aggregateMap, calculation, dbAggResultBuilder.get(), null));
                dbAggregationResults.add(dbAggResultBuilder.get().build());

        }
    }

    public static List<DbAggregationResult> getAggResultFromSearchResponse(SearchResponse<EsWorkItem> searchResponse, JiraIssuesFilter.DISTINCT across,
                                                                           JiraIssuesFilter.DISTINCT stack, JiraIssuesFilter.CALCULATION calculation,
                                                                           JiraIssuesFilter jiraIssuesFilter, Boolean valuesOnly) {

        List<DbAggregationResult> dbAggregationResults;
        if (calculation == JiraIssuesFilter.CALCULATION.stage_bounce_report
                || calculation == JiraIssuesFilter.CALCULATION.stage_times_report) {
            dbAggregationResults = getAggResultForStages(searchResponse, across, stack, calculation, jiraIssuesFilter);
        } else {
            dbAggregationResults = getAggResult(searchResponse, across, stack, calculation, jiraIssuesFilter, valuesOnly);
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }


    public static List<DbAggregationResult> getAggResult(SearchResponse<EsWorkItem> searchResponse, JiraIssuesFilter.DISTINCT across,
                                                         JiraIssuesFilter.DISTINCT stack, JiraIssuesFilter.CALCULATION calculation,
                                                         JiraIssuesFilter jiraIssuesFilter, Boolean valuesOnly) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        switch (across) {
            case first_assignee:
            case assignee:
            case reporter:
                int acrossLimit = jiraIssuesFilter.getAcrossLimit() != null ? jiraIssuesFilter.getAcrossLimit() : 90;
                List<StringTermsBucket> terms = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();
                terms.forEach(term -> {
                    List<StringTermsBucket> terms2 = term.aggregations().get("across_" + across.name()).sterms().buckets().array();
                    terms2.forEach(term2 -> {
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, term2.aggregations());
                        List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                        List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                        List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                        if (stack.toString().equals("custom_field")) {
                            Aggregate aggregate1 = term2.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name")
                                    .filter().aggregations().get("across_custom_fields_type");
                            if (aggregate1.isSterms()) {
                                strBucketsForCustomFields = aggregate1.sterms().buckets().array();
                            } else if (aggregate1.isDterms()) {
                                doubleBucketsForCustomFields = aggregate1.dterms().buckets().array();
                            } else if (aggregate1.isLterms()) {
                                longBucketsFoCustomFields = aggregate1.lterms().buckets().array();
                            }
                        }
                        List<DbAggregationResult> stacks = getStacks(stringTermsBucketList, List.of(), List.of(),
                                strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                        String key = null;
                        if (!term.key().equals("_UNASSIGNED_")) {
                            key = term2.key();
                        }
                        DbAggregationResultBuilder dbAggResultBuilder;
                        if (valuesOnly) {
                            dbAggResultBuilder = getDbAggResultForMultiTermJira(key, term.key(), null);
                        } else {
                            dbAggResultBuilder = getDbAggResultForMultiTermJira(key, term.key(), term2.docCount());
                            dbAggResultBuilder = getCalculation(term2.aggregations(), calculation, dbAggResultBuilder, null);
                            if (stack != JiraIssuesFilter.DISTINCT.none) {
                                dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                            }
                        }
                        if (valuesOnly) {
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        } else if (dbAggregationResults.size() < acrossLimit) {
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            case issue_created:
            case issue_resolved:
            case issue_due:
            case issue_updated:
            case issue_due_relative:
                List<DateHistogramBucket> dateHistogramBuckets = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack.toString().equals("custom_field")) {
                        Aggregate aggregate1 = dateHistogramBucket.aggregations().get("across_custom_field")
                                .nested().aggregations().get("filter_custom_fields_name")
                                .filter().aggregations().get("across_custom_fields_type");
                        if (aggregate1.isSterms()) {
                            strBucketsForCustomFields = aggregate1.sterms().buckets().array();
                        } else if (aggregate1.isDterms()) {
                            doubleBucketsForCustomFields = aggregate1.dterms().buckets().array();
                        } else if (aggregate1.isLterms()) {
                            longBucketsFoCustomFields = aggregate1.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacks(stringTermsBucketList, multiTermsBucketList,
                            dateHistograms, strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogram(dateHistogramBucket, jiraIssuesFilter.getAggInterval().equalsIgnoreCase("quarter"), valuesOnly);
                    if (!valuesOnly) {
                        dbAggResultBuilder = getCalculation(dateHistogramBucket.aggregations(), calculation, dbAggResultBuilder, null);
                        if (stack != JiraIssuesFilter.DISTINCT.none) {
                            dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                        }
                    }
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
            case trend:
                List<DateHistogramBucket> dateHistogramBucketsForTrend = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBucketsForTrend.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack.toString().equals("custom_field")) {
                        Aggregate aggregate1 = dateHistogramBucket.aggregations().get("across_custom_field")
                                .nested().aggregations().get("filter_custom_fields_name")
                                .filter().aggregations().get("across_custom_fields_type");
                        if (aggregate1.isSterms()) {
                            strBucketsForCustomFields = aggregate1.sterms().buckets().array();
                        } else if (aggregate1.isDterms()) {
                            doubleBucketsForCustomFields = aggregate1.dterms().buckets().array();
                        } else if (aggregate1.isLterms()) {
                            longBucketsFoCustomFields = aggregate1.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacks(stringTermsBucketList, multiTermsBucketList, dateHistograms, strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, jiraIssuesFilter.getAggInterval().equalsIgnoreCase("quarter"), valuesOnly);
                    if (!valuesOnly) {
                        dbAggResultBuilder = getCalculation(dateHistogramBucket.aggregations(), calculation, dbAggResultBuilder, null);
                        if (stack != JiraIssuesFilter.DISTINCT.none) {
                            dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                        }
                    }
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
            case custom_field:
                Aggregate aggregate = searchResponse.aggregations().get("across_" + across)
                        .nested().aggregations().get("filter_custom_fields_name").filter().aggregations()
                        .get("across_custom_fields_type");
                if (aggregate.isSterms()) {
                    processForStrAgg(stack, calculation, valuesOnly, dbAggregationResults, aggregate);
                } else if (aggregate.isDterms()) {
                    processForDoubleAgg(stack, calculation, valuesOnly, dbAggregationResults, aggregate);
                } else if (aggregate.isLterms()) {
                    processForLongAgg(stack, calculation, valuesOnly, dbAggregationResults, aggregate);
                }

                break;
            case sprint_mapping:
                List<CompositeBucket> compositeBuckets = searchResponse.aggregations().get("across_" + across)
                        .composite().buckets().array();
                compositeBuckets.forEach(term -> dbAggregationResults.addAll(getCalculationForSprintsReport(term, searchResponse, jiraIssuesFilter)));
                break;
            case stage:
                List<StringTermsBucket> sTermsBuckets = searchResponse.aggregations().get("across_" + across)
                        .nested().aggregations().get("across_historical_status").sterms().buckets().array();
                sTermsBuckets.forEach(term -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, term.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, term.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, term.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack.toString().equals("custom_field")) {
                        Aggregate aggregate1 = term.aggregations().get("across_custom_field")
                                .nested().aggregations().get("filter_custom_fields_name")
                                .filter().aggregations().get("across_custom_fields_type");
                        if (aggregate1.isSterms()) {
                            strBucketsForCustomFields = aggregate1.sterms().buckets().array();
                        } else if (aggregate1.isDterms()) {
                            doubleBucketsForCustomFields = aggregate1.dterms().buckets().array();
                        } else if (aggregate1.isLterms()) {
                            longBucketsFoCustomFields = aggregate1.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacks(stringTermsBucketList,
                            multiTermsBucketList, dateHistograms, strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(term, valuesOnly);
                    if (!valuesOnly) {
                        dbAggResultBuilder = getCalculation(term.aggregations(), calculation, dbAggResultBuilder, null);
                        if (stack != JiraIssuesFilter.DISTINCT.none) {
                            dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                        }
                    }
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
            case none:
                List<StringTermsBucket> termsBucketsNone = searchResponse.aggregations().get("across_" + across)
                        .sterms().buckets().array();
                termsBucketsNone.forEach(term -> {
                    List<StringTermsBucket> stringTermsBucketList;
                    stringTermsBucketList = getStringTermsBucketList(stack, null, term.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack.toString().equals("custom_field")) {
                        Aggregate aggregate1 = term.aggregations().get("across_custom_field")
                                .nested().aggregations().get("filter_custom_fields_name")
                                .filter().aggregations().get("across_custom_fields_type");
                        if (aggregate1.isSterms()) {
                            strBucketsForCustomFields = aggregate1.sterms().buckets().array();
                        } else if (aggregate1.isDterms()) {
                            doubleBucketsForCustomFields = aggregate1.dterms().buckets().array();
                        } else if (aggregate1.isLterms()) {
                            longBucketsFoCustomFields = aggregate1.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacks(stringTermsBucketList,
                            List.of(), List.of(), strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(term, valuesOnly);
                    if (!valuesOnly) {
                        dbAggResultBuilder = getCalculation(term.aggregations(), calculation, dbAggResultBuilder, null);
                        if (stack != JiraIssuesFilter.DISTINCT.none) {
                            dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                        }
                    }
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
            default:
                List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_" + across)
                        .sterms().buckets().array();
                termsBuckets.forEach(term -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, term.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, term.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, term.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack.toString().equals("custom_field")) {
                        Aggregate aggregate1 = term.aggregations().get("across_custom_field")
                                .nested().aggregations().get("filter_custom_fields_name")
                                .filter().aggregations().get("across_custom_fields_type");
                        if (aggregate1.isSterms()) {
                            strBucketsForCustomFields = aggregate1.sterms().buckets().array();
                        } else if (aggregate1.isDterms()) {
                            doubleBucketsForCustomFields = aggregate1.dterms().buckets().array();
                        } else if (aggregate1.isLterms()) {
                            longBucketsFoCustomFields = aggregate1.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacks(stringTermsBucketList,
                            multiTermsBucketList, dateHistograms, strBucketsForCustomFields, doubleBucketsForCustomFields,
                            longBucketsFoCustomFields, stack, calculation);
                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(term, valuesOnly);
                    if (!valuesOnly) {
                        dbAggResultBuilder = getCalculation(term.aggregations(), calculation, dbAggResultBuilder, null);
                        if (stack != JiraIssuesFilter.DISTINCT.none) {
                            dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                        }
                    }
                    dbAggregationResults.add(dbAggResultBuilder.build());
                });
                break;
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    private static void processForLongAgg(JiraIssuesFilter.DISTINCT stack, JiraIssuesFilter.CALCULATION calculation, Boolean valuesOnly, List<DbAggregationResult> dbAggregationResults, Aggregate aggregate) {
        List<LongTermsBucket> nestedBuckets = aggregate.lterms().buckets().array();
        nestedBuckets.forEach(term -> {
            Map<String, Aggregate> customNestedRoot = term.aggregations().get("custom_nested_root").reverseNested().aggregations();
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, customNestedRoot);
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, customNestedRoot);
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, customNestedRoot);
            List<DateHistogramBucket> dateHistTermsBucketList = getDateHistogramBuckets(stack, null, customNestedRoot);
            List<DbAggregationResult> stacks = getStacksForCustomFields(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList, dateHistTermsBucketList, stack, calculation);
            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForLongTerms(term, valuesOnly);
            if (!valuesOnly) {
                dbAggResultBuilder = getCalculation(customNestedRoot, calculation, dbAggResultBuilder, null);
                if (stack != JiraIssuesFilter.DISTINCT.none) {
                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                }
            }
            dbAggregationResults.add(dbAggResultBuilder.build());
        });
    }

    private static void processForDoubleAgg(JiraIssuesFilter.DISTINCT stack, JiraIssuesFilter.CALCULATION calculation, Boolean valuesOnly, List<DbAggregationResult> dbAggregationResults, Aggregate aggregate) {
        List<DoubleTermsBucket> nestedBuckets = aggregate.dterms().buckets().array();
        nestedBuckets.forEach(term -> {
            Map<String, Aggregate> customNestedRoot = term.aggregations().get("custom_nested_root").reverseNested().aggregations();
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, customNestedRoot);
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, customNestedRoot);
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, customNestedRoot);
            List<DateHistogramBucket> dateHistTermsBucketList = getDateHistogramBuckets(stack, null, customNestedRoot);
            List<DbAggregationResult> stacks = getStacksForCustomFields(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList, dateHistTermsBucketList, stack, calculation);
            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDoubleTerms(term, valuesOnly);
            if (!valuesOnly) {
                dbAggResultBuilder = getCalculation(customNestedRoot, calculation, dbAggResultBuilder, null);
                if (stack != JiraIssuesFilter.DISTINCT.none) {
                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                }
            }
            dbAggregationResults.add(dbAggResultBuilder.build());
        });
    }

    private static void processForStrAgg(JiraIssuesFilter.DISTINCT stack, JiraIssuesFilter.CALCULATION calculation, Boolean valuesOnly, List<DbAggregationResult> dbAggregationResults, Aggregate aggregate) {
        List<StringTermsBucket> nestedBuckets = aggregate.sterms().buckets().array();
        nestedBuckets.forEach(term -> {
            Map<String, Aggregate> customNestedRoot = term.aggregations().get("custom_nested_root").reverseNested().aggregations();
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, customNestedRoot);
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, customNestedRoot);
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, customNestedRoot);
            List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, customNestedRoot);
            if (stack == JiraIssuesFilter.DISTINCT.custom_field) {
                Aggregate aggregate1 = term.aggregations().get("custom_nested_root").reverseNested().aggregations().get("across_custom_field")
                        .nested().aggregations().get("filter_custom_fields_name").filter().aggregations()
                        .get("across_custom_fields_type");
                if (aggregate1.isSterms()) {
                    stringTermsBucketList = aggregate1.sterms().buckets().array();
                } else if (aggregate.isDterms()) {
                    doubleTermsBucketList = aggregate1.dterms().buckets().array();
                } else if (aggregate.isLterms()) {
                    longTermsBucketList = aggregate1.lterms().buckets().array();
                }
            }
            List<DbAggregationResult> stacks = getStacksForCustomFields(stringTermsBucketList,
                    doubleTermsBucketList, longTermsBucketList, dateHistograms, stack, calculation);
            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(term, valuesOnly);
            if (!valuesOnly) {
                dbAggResultBuilder = getCalculation(customNestedRoot, calculation, dbAggResultBuilder, null);
                if (stack != JiraIssuesFilter.DISTINCT.none) {
                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                }
            }
            dbAggregationResults.add(dbAggResultBuilder.build());
        });
    }


    public static List<DbAggregationResult> getAggResultForStages(SearchResponse<EsWorkItem> searchResponse, JiraIssuesFilter.DISTINCT across,
                                                                  JiraIssuesFilter.DISTINCT stack, JiraIssuesFilter.CALCULATION calculation,
                                                                  JiraIssuesFilter jiraIssuesFilter) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        switch (across) {
            case issue_resolved:
            case issue_updated:
            case issue_created:
            case issue_due:
                List<DateHistogramBucket> dateHistogramBuckets = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(stack, null, dateHistogramBucket.aggregations()) : List.of();
                    List<StringTermsBucket> stringTermsBucketBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                            if (jiraIssuesFilter.getStages().contains(stringTermsBucket.key())) {
                                Map<String, Aggregate> nestedAggs = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations();
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, List.of(), multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogram(dateHistogramBucket, false, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        } else {
                            Map<String, Aggregate> nestedAggs = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations();
                            List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, List.of(), multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogram(dateHistogramBucket, false, false);
                            getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                            if (stack != JiraIssuesFilter.DISTINCT.none) {
                                dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                            }
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            case trend:
                List<DateHistogramBucket> dateHistogramBucketsForTrend = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBucketsForTrend.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(stack, null, dateHistogramBucket.aggregations()) : List.of();
                    List<StringTermsBucket> stringTermsBucketBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                            if (jiraIssuesFilter.getStages().contains(stringTermsBucket.key())) {
                                Map<String, Aggregate> nestedAggs = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations();
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, false, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        } else {
                            Map<String, Aggregate> nestedAggs = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations();
                            List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, false, false);
                            getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                            if (stack != JiraIssuesFilter.DISTINCT.none) {
                                dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                            }
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            case assignee:
            case reporter:
                List<MultiTermsBucket> multiTermsBuckets = searchResponse.aggregations().get("across_" + across.name()).multiTerms().buckets().array();
                multiTermsBuckets.forEach(multiTermsBucket -> {
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, multiTermsBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, multiTermsBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, multiTermsBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, multiTermsBucket.aggregations());
                    List<MultiTermsBucket> multiTermsBucketListFromResponse = getMultiTermsBucketList(stack, null, multiTermsBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(stack, null, multiTermsBucket.aggregations()) : List.of();
                    List<StringTermsBucket> sTermBucketsForStages = multiTermsBucket.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    sTermBucketsForStages.forEach(stringTermsBucket -> {
                        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                            if (jiraIssuesFilter.getStages().contains(stringTermsBucket.key())) {
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketListFromResponse, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                                Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForMultiTerm(multiTermsBucket);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        } else {
                            List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketListFromResponse, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                            Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForMultiTerm(multiTermsBucket);
                            getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                            if (stack != JiraIssuesFilter.DISTINCT.none) {
                                dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                            }
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            case custom_field:
                Aggregate aggregate = searchResponse.aggregations().get("across_custom_fields").nested().aggregations().get("filter_custom_fields_name")
                        .filter().aggregations().get("across_custom_fields_type");
                if (aggregate.isSterms()) {
                    List<StringTermsBucket> strTermsBuckets = aggregate.sterms().buckets().array();
                    strTermsBuckets.forEach(stringTermsBucket -> {
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, stringTermsBucket.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, stringTermsBucket.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, stringTermsBucket.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, stringTermsBucket.aggregations());
                        List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, stringTermsBucket.aggregations());
                        List<StringTermsBucket> sTermBucketsForStages = stringTermsBucket.aggregations().get("across_nested").reverseNested()
                                .aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        sTermBucketsForStages.forEach(stringTermsBucket1 -> {
                            if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                                if (jiraIssuesFilter.getStages().contains(stringTermsBucket1.key())) {
                                    List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), jiraIssuesFilter, stack, calculation, stringTermsBucket1.key());
                                    Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations().get("across_nested").reverseNested().aggregations();
                                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(stringTermsBucket, false);
                                    getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket1.key());
                                    if (stack != JiraIssuesFilter.DISTINCT.none) {
                                        dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                    }
                                    dbAggregationResults.add(dbAggResultBuilder.build());
                                }
                            } else {
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), jiraIssuesFilter, stack, calculation, stringTermsBucket1.key());
                                Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations().get("across_nested").reverseNested().aggregations();
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(stringTermsBucket, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket1.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        });
                    });
                } else if (aggregate.isDterms()) {
                    List<DoubleTermsBucket> dTermsBuckets = aggregate.dterms().buckets().array();
                    dTermsBuckets.forEach(doubleTermsBucket -> {
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, doubleTermsBucket.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, doubleTermsBucket.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, doubleTermsBucket.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, doubleTermsBucket.aggregations());
                        List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, doubleTermsBucket.aggregations());
                        List<StringTermsBucket> sTermBucketsForStages = doubleTermsBucket.aggregations().get("across_nested").reverseNested()
                                .aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        sTermBucketsForStages.forEach(stringTermsBucket1 -> {
                            if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                                if (jiraIssuesFilter.getStages().contains(stringTermsBucket1.key())) {
                                    List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), jiraIssuesFilter, stack, calculation, stringTermsBucket1.key());
                                    Map<String, Aggregate> nestedAggs = doubleTermsBucket.aggregations().get("across_nested").reverseNested().aggregations();
                                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDoubleTerms(doubleTermsBucket, false);
                                    getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket1.key());
                                    if (stack != JiraIssuesFilter.DISTINCT.none) {
                                        dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                    }
                                    dbAggregationResults.add(dbAggResultBuilder.build());
                                }
                            } else {
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), jiraIssuesFilter, stack, calculation, stringTermsBucket1.key());
                                Map<String, Aggregate> nestedAggs = doubleTermsBucket.aggregations().get("across_nested").reverseNested().aggregations();
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForDoubleTerms(doubleTermsBucket, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket1.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        });
                    });
                } else if (aggregate.isLterms()) {
                    List<LongTermsBucket> lTermsBuckets = aggregate.lterms().buckets().array();
                    lTermsBuckets.forEach(longTermsBucket -> {
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, longTermsBucket.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, longTermsBucket.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, longTermsBucket.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, longTermsBucket.aggregations());
                        List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, longTermsBucket.aggregations());
                        List<StringTermsBucket> sTermBucketsForStages = longTermsBucket.aggregations().get("across_nested").reverseNested()
                                .aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        sTermBucketsForStages.forEach(stringTermsBucket1 -> {
                            if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                                if (jiraIssuesFilter.getStages().contains(stringTermsBucket1.key())) {
                                    List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), jiraIssuesFilter, stack, calculation, stringTermsBucket1.key());
                                    Map<String, Aggregate> nestedAggs = longTermsBucket.aggregations().get("across_nested").reverseNested().aggregations();
                                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForLongTerms(longTermsBucket, false);
                                    getCalculation(nestedAggs, calculation, dbAggResultBuilder, String.valueOf(stringTermsBucket1.key()));
                                    if (stack != JiraIssuesFilter.DISTINCT.none) {
                                        dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                    }
                                    dbAggregationResults.add(dbAggResultBuilder.build());
                                }
                            } else {
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), jiraIssuesFilter, stack, calculation, stringTermsBucket1.key());
                                Map<String, Aggregate> nestedAggs = longTermsBucket.aggregations().get("across_nested").reverseNested().aggregations();
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForLongTerms(longTermsBucket, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, String.valueOf(stringTermsBucket1.key()));
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        });
                    });
                }
                break;
            case sprint:
                List<StringTermsBucket> sTermsBucketsSprint = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();
                sTermsBucketsSprint.forEach(sTerm -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, sTerm.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, sTerm.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, sTerm.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, sTerm.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(stack, null, sTerm.aggregations()) : List.of();
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, sTerm.aggregations());
                    List<StringTermsBucket> sTermBucketsForStages = sTerm.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    sTermBucketsForStages.forEach(stringTermsBucket -> {
                        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                            if (jiraIssuesFilter.getStages().contains(stringTermsBucket.key())) {
                                Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(sTerm, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        } else {
                            Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                            List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(sTerm, false);
                            getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                            if (stack != JiraIssuesFilter.DISTINCT.none) {
                                dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                            }
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            default:
                List<MultiTermsBucket> multiTermsBucket;
                List<StringTermsBucket> sTermsBuckets;
                List<DoubleTermsBucket> dTermsBuckets;
                sTermsBuckets = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();
                sTermsBuckets.forEach(sTerm -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(stack, null, sTerm.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(stack, null, sTerm.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(stack, null, sTerm.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(stack, null, sTerm.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(stack, null, sTerm.aggregations()) : List.of();
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(stack, null, sTerm.aggregations());
                    List<StringTermsBucket> sTermBucketsForStages = sTerm.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    sTermBucketsForStages.forEach(stringTermsBucket -> {
                        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
                            if (jiraIssuesFilter.getStages().contains(stringTermsBucket.key())) {
                                Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                                List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                                if (across == JiraIssuesFilter.DISTINCT.none) {
                                    if (stack == JiraIssuesFilter.DISTINCT.none) {
                                        dbAggregationResults.add(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()).build());
                                    } else {
                                        dbAggregationResults.add(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()).build());
                                    }
                                } else {
                                    DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(sTerm, false);
                                    getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                                    if (stack != JiraIssuesFilter.DISTINCT.none) {
                                        dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                    }
                                    dbAggregationResults.add(dbAggResultBuilder.build());
                                }
                            }
                        } else {
                            Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                            List<DbAggregationResult> stacks = getStacksForStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForCustomFields, jiraIssuesFilter, stack, calculation, stringTermsBucket.key());
                            if (across == JiraIssuesFilter.DISTINCT.none) {
                                if (stack == JiraIssuesFilter.DISTINCT.none) {
                                    dbAggregationResults.add(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()).build());
                                } else {
                                    dbAggregationResults.add(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()).build());
                                }
                            } else {
                                DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(sTerm, false);
                                getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                                if (stack != JiraIssuesFilter.DISTINCT.none) {
                                    dbAggResultBuilder = dbAggResultBuilder.stacks(stacks);
                                }
                                dbAggregationResults.add(dbAggResultBuilder.build());
                            }
                        }
                    });
                });
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    private static List<DbAggregationResult> getStacksForStages(List<StringTermsBucket> stringTermsBuckets,
                                                                List<DateHistogramBucket> dateHistogramBuckets,
                                                                List<MultiTermsBucket> multiTermsBuckets,
                                                                List<DoubleTermsBucket> doubleTermsBuckets,
                                                                List<StringTermsBucket> strBucketsForCustomFields,
                                                                JiraIssuesFilter jiraIssuesFilter,
                                                                JiraIssuesFilter.DISTINCT stack,
                                                                JiraIssuesFilter.CALCULATION calculation,
                                                                String stage) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        switch (stack) {
            case issue_resolved:
            case issue_updated:
            case issue_created:
            case issue_due:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForDateHistogram(dateHistogramBucket, false, false));
                    List<StringTermsBucket> termsBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    termsBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            dbAggResultBuilder.set(getCalculation(dateHistogramBucket.aggregations().get("across_nested").nested().aggregations(), calculation, dbAggResultBuilder.get(), stringTermsBucket.key()));
                            dbAggregationResults.add(dbAggResultBuilder.get().build());
                        }
                    });
                });
                break;
            case trend:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    AtomicReference<DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, false, false));
                    List<StringTermsBucket> termsBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    termsBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            dbAggResultBuilder.set(getCalculation(dateHistogramBucket.aggregations().get("across_nested").nested().aggregations(), calculation, dbAggResultBuilder.get(), stringTermsBucket.key()));
                            dbAggregationResults.add(dbAggResultBuilder.get().build());
                        }
                    });
                });
                break;
            case assignee:
            case reporter:
                multiTermsBuckets.forEach(multiTerm -> {
                    List<StringTermsBucket> stringTermsBucketBuckets = multiTerm.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            Map<String, Aggregate> nestedAggs = multiTerm.aggregations().get("across_nested").nested().aggregations();
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForMultiTerm(multiTerm);
                            dbAggResultBuilder = getCalculation(nestedAggs, calculation, dbAggResultBuilder, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            case custom_field:
                strBucketsForCustomFields.forEach(sTerm -> {
                    List<StringTermsBucket> stringTermsBucketBuckets = sTerm.aggregations().get("across_nested").reverseNested()
                            .aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(sTerm, false);
                            dbAggResultBuilder = getCalculation(sTerm.aggregations().get("across_nested").reverseNested().aggregations(), calculation, dbAggResultBuilder, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
                break;
            default:
                stringTermsBuckets.forEach(sTerm -> {
                    List<StringTermsBucket> stringTermsBucketBuckets = sTerm.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            DbAggregationResultBuilder dbAggResultBuilder = getDbAggResultForStrTerms(sTerm, false);
                            dbAggResultBuilder = getCalculation(sTerm.aggregations().get("across_nested").nested().aggregations(), calculation, dbAggResultBuilder, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultBuilder.build());
                        }
                    });
                });
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    private static List<DbJiraAssignee> getAssignees(List<EsWorkItem.EsHistoricalAssignee> esHistoricalAssignees) {
        List<DbJiraAssignee> assignees = new ArrayList<>();
        esHistoricalAssignees.forEach(esHistoricalAssignee ->
                assignees.add(DbJiraAssignee.builder()
                        .assignee(esHistoricalAssignee.getAssignee().getDisplayName())
                        .startTime(esHistoricalAssignee.getStartTime().getTime())
                        .endTime(esHistoricalAssignee.getEndTime().getTime())
                        .build()));
        return assignees;
    }

    private static List<DbJiraStatus> getStatuses(List<EsWorkItem.EsHistoricalStatus> esHistoricalStatuses) {
        List<DbJiraStatus> statuses = new ArrayList<>();
        esHistoricalStatuses.forEach(esHistoricalStatus ->
                statuses.add(DbJiraStatus.builder()
                        .status(esHistoricalStatus.getStatus())
                        .statusId(esHistoricalStatus.getStatusId())
                        .startTime(esHistoricalStatus.getStartTime().getTime())
                        .endTime(esHistoricalStatus.getEndTime().getTime())
                        .build()));
        return statuses;
    }

    private static DbAggregationResult.DbAggregationResultBuilder getCalculation(Map<String, Aggregate> aggregation, JiraIssuesFilter.CALCULATION calculation,
                                                                                 DbAggregationResult.DbAggregationResultBuilder dbAggregationResultBuilder,
                                                                                 String stage) {
        switch (calculation) {
            case ticket_count:
                return dbAggregationResultBuilder
                        .totalStoryPoints(Double.valueOf(aggregation.get("total_story_points") != null ? aggregation.get("total_story_points").sum().value() : 0).longValue())
                        .meanStoryPoints(aggregation.get("mean_story_points") != null ? aggregation.get("mean_story_points").avg().value() : 0.0);
            case response_time:
            case hops:
            case bounces:
            case assign_to_resolve:
                return dbAggregationResultBuilder
                        .max(Double.valueOf(aggregation.get(calculation.name()).stats().max()).longValue())
                        .min(Double.valueOf(aggregation.get(calculation.name()).stats().min()).longValue())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"));
            case resolution_time:
                return dbAggregationResultBuilder
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .max(Double.valueOf(aggregation.get(calculation.name()).stats().max()).longValue())
                        .min(Double.valueOf(aggregation.get(calculation.name()).stats().min()).longValue())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                        .p90(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "90.0"));
            case age:
                return dbAggregationResultBuilder
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .max(Double.valueOf(aggregation.get(calculation.name()).stats().max()).longValue())
                        .min(Double.valueOf(aggregation.get(calculation.name()).stats().min()).longValue())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                        .p90(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "90.0"))
                        .totalStoryPoints(Double.valueOf(aggregation.get("total_story_points").sum().value()).longValue());
            case story_points:
                return dbAggregationResultBuilder
                        .totalStoryPoints(Double.valueOf(aggregation.get("total_story_points").sum().value()).longValue())
                        .totalUnestimatedTickets(Double.valueOf(aggregation.get("total_unestimated_tickets").sum().value()).longValue());
            case stage_times_report:
                return dbAggregationResultBuilder
                        .max((long) aggregation.get(calculation.name()).stats().max())
                        .min((long) aggregation.get(calculation.name()).stats().min())
                        .stage(stage != null ? stage : "[SOMETHINGS WRONG]")
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                        .p90(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "90.0"))
                        .p95(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "95.0"));
            case assignees:
                return dbAggregationResultBuilder
                        .assignees(aggregation.get(calculation.name()).sterms().buckets().array()
                                .stream()
                                .map(StringTermsBucket::key)
                                .collect(Collectors.toList()));
            case stage_bounce_report:
                return dbAggregationResultBuilder
                        .stage(stage != null ? stage : "[SOMETHINGS WRONG]")
                        .mean(aggregation.get(calculation.name() + "_mean").simpleValue().value())
                        .median(getPercentileFromPercentileBucket(aggregation.get(calculation.name() + "_percentiles").percentilesBucket(), "50.0"));
            case state_transition_time:
                return dbAggregationResultBuilder
                        .max(Double.valueOf(aggregation.get(calculation.name()).nested().aggregations().get("state_transition").filter().aggregations().get("stats").stats().max()).longValue())
                        .min(Double.valueOf(aggregation.get(calculation.name()).nested().aggregations().get("state_transition").filter().aggregations().get("stats").stats().min()).longValue())
                        .median(Double.valueOf(aggregation.get(calculation.name()).nested().aggregations().get("state_transition").filter().aggregations()
                                .get("percentile").tdigestPercentiles().values().keyed().get("50.0")).longValue());
            default:
                return dbAggregationResultBuilder;
        }
    }

    private static Long getPercentileFromPercentileBucket(PercentilesBucketAggregate percentilesBucket, String s) {
        String value = percentilesBucket.values().keyed().getOrDefault(s, null);
        if (value == null) {
            return 0L;
        }
        double val = Double.parseDouble(value);
        return (long) val;
    }

    private static Long getPercentileFromTDigestPercentile(TDigestPercentilesAggregate percentiles, String s) {
        String value = percentiles.values().keyed().getOrDefault(s, null);
        if (value == null) {
            return null;
        }
        double val = Double.parseDouble(value);
        return (long) val;
    }

    private static List<DbAggregationResult> getCalculationForSprintsReport(CompositeBucket compositeBucket, SearchResponse<EsWorkItem> searchResponse,
                                                                            JiraIssuesFilter jiraIssuesFilter) {
        if ((jiraIssuesFilter.getSprintMappingSprintCompletedAtAfter() != null && jiraIssuesFilter.getSprintMappingSprintCompletedAtBefore() != null) ||
                (jiraIssuesFilter.getSprintMappingSprintStartedAtAfter() != null && jiraIssuesFilter.getSprintMappingSprintStartedAtBefore() != null) ||
                (jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtAfter() != null && jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtBefore() != null)) {
            Long sprintMappingSprintCompletedAtAfter = jiraIssuesFilter.getSprintMappingSprintCompletedAtAfter();
            Long sprintMappingSprintCompletedAtBefore = jiraIssuesFilter.getSprintMappingSprintCompletedAtBefore();
            Long sprintMappingSprintStartedAtAfter = jiraIssuesFilter.getSprintMappingSprintStartedAtAfter();
            Long sprintMappingSprintStartedAtBefore = jiraIssuesFilter.getSprintMappingSprintStartedAtBefore();
            Long sprintMappingSprintPlannedCompletedAtAfter = jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtAfter();
            Long sprintMappingSprintPlannedCompletedAtBefore = jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtBefore();
            List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
            compositeBucket.aggregations().get("nested").nested().aggregations().get("sprint_id")
                    .sterms().buckets().array().stream()
                    .filter(id -> {
                        String sprintName = id.aggregations().get("sprint_name").sterms().buckets().array().stream().findFirst().map(t -> t.key()).get();
                        if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNames())){
                            return jiraIssuesFilter.getSprintMappingSprintNames().contains(sprintName);
                        }
                        if(StringUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNameStartsWith())){
                            return sprintName.startsWith(jiraIssuesFilter.getSprintMappingSprintNameStartsWith());
                        }
                        if(StringUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNameEndsWith())){
                            return sprintName.endsWith(jiraIssuesFilter.getSprintMappingSprintNameEndsWith());
                        }
                        if(StringUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNameContains())){
                            return sprintName.contains(jiraIssuesFilter.getSprintMappingSprintNameContains());
                        }
                        return true;
                    })
                    .forEach(id -> {
                        id.aggregations().get("sprint_name").sterms().buckets().array().forEach(name -> {
                            name.aggregations().get("sprint_completed_at").lterms().buckets().array().forEach(ca -> {
                                ca.aggregations().get("sprint_goal").sterms().buckets().array().forEach(goal -> {
                                    goal.aggregations().get("sprint_start_time").lterms().buckets().array().forEach(st -> {
                                        st.aggregations().get("sprint_end_time").lterms().buckets().array().forEach(ed -> {
                                            List<JiraIssueSprintMappingAggResult> sprintMappingAggResults = getSprintMappingAggs(searchResponse,
                                                    Integer.valueOf(StringUtils.replace(compositeBucket.key().get("integration_id").toString(),
                                                            "\"", "")), id.key(), jiraIssuesFilter);
                                            if (sprintMappingAggResults != null) {
                                                String key = null;
                                                if(sprintMappingSprintStartedAtAfter != null && sprintMappingSprintStartedAtBefore != null){
                                                    if (sprintMappingSprintStartedAtBefore > Long.parseLong(String.valueOf(st.key())) && sprintMappingSprintStartedAtAfter
                                                            <= Long.parseLong(String.valueOf(st.key()))){
                                                        key = String.valueOf(st.key());
                                                    } else return;
                                                }
                                                if(sprintMappingSprintPlannedCompletedAtAfter != null && sprintMappingSprintPlannedCompletedAtBefore != null){
                                                    if (sprintMappingSprintPlannedCompletedAtBefore > Long.parseLong(String.valueOf(ed.key())) && sprintMappingSprintPlannedCompletedAtAfter
                                                            <= Long.parseLong(String.valueOf(ed.key()))){
                                                        key = String.valueOf(ed.key());
                                                    } else return;
                                                }
                                                if(sprintMappingSprintCompletedAtAfter != null && sprintMappingSprintCompletedAtBefore != null){
                                                    if (sprintMappingSprintCompletedAtBefore > Long.parseLong(String.valueOf(ca.key())) && sprintMappingSprintCompletedAtAfter
                                                            <= Long.parseLong(String.valueOf(ca.key()))){
                                                        key = String.valueOf(ca.key());
                                                    } else return;
                                                }
                                                if(key != null){
                                                    dbAggregationResults.add(DbAggregationResult.builder()
                                                            .key(key)
                                                            .integrationId(StringUtils.replace(compositeBucket.key().get("integration_id").toString(),
                                                                    "\"", ""))
                                                            .sprintId(id.key())
                                                            .sprintName(name.key())
                                                            .sprintGoal(goal.key())
                                                            .sprintStartedAt(st.key())
                                                            .sprintCompletedAt(ca.key())
                                                            .sprintMappingAggs(sprintMappingAggResults)
                                                            .build());
                                                }
                                            }
                                        });
                                    });
                                });
                            });
                        });
                    });
            return EsConverterUtils.sanitizeResponse(dbAggregationResults);
        }
        return List.of();
    }

    private static List<JiraIssueSprintMappingAggResult> getSprintMappingAggs(SearchResponse<EsWorkItem> searchResponse,
                                                                              Integer integrationId, String sprintId,
                                                                              JiraIssuesFilter jiraIssuesFilter) {
        List<JiraIssueSprintMappingAggResult> jiraIssueSprintMappingAggResults = new ArrayList<>();

        searchResponse.hits().hits().forEach(i -> {
            assert i.source() != null;
            i.source().getHistoricalSprints().forEach(s -> {
                if (Objects.equals(i.source().getIntegrationId(), integrationId) && StringUtils.equals(s.getSprintId(), sprintId) &&
                        StringUtils.equals(s.getState().toUpperCase(), jiraIssuesFilter.getSprintMappingSprintState().toUpperCase())) {
                    i.source().getSprintMappings().forEach(j -> {
                        if (StringUtils.equals(j.getSprintId(), s.getSprintId())
                                && jiraIssuesFilter.getSprintMappingIgnorableIssueType() == j.getIgnorableIssueType()) {
                            jiraIssueSprintMappingAggResults.add(JiraIssueSprintMappingAggResult.builder()
                                    .issueType(i.source().getWorkItemType())
                                    .sprintMapping(DbJiraIssueSprintMapping.builder()
                                            .id(j.getId())
                                            .integrationId(integrationId.toString())
                                            .issueKey(i.source().getWorkitemId())
                                            .sprintId(j.getSprintId())
                                            .addedAt(j.getAddedAt())
                                            .delivered(j.getDelivered())
                                            .planned(j.getPlanned())
                                            .outsideOfSprint(j.getOutsideOfSprint())
                                            .ignorableIssueType(j.getIgnorableIssueType())
                                            .storyPointsPlanned(j.getStoryPointsPlanned().intValue())
                                            .storyPointsDelivered(j.getStoryPointsDelivered().intValue())
                                            .build())
                                    .build());
                        }
                    });
                }
            });
        });
        return jiraIssueSprintMappingAggResults.size() == 0 ? null : jiraIssueSprintMappingAggResults;
    }
}
