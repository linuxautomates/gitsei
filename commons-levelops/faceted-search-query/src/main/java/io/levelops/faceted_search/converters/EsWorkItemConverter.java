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
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonpDeserializer;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.IssueMgmtSprintMappingAggResult;
import io.levelops.commons.faceted_search.db.models.workitems.EsDevProdWorkItemResponse;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.converters.EsConverterUtils.getConvertedExtensibleFields;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDateHistogramBuckets;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDateHistogram;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDateHistogramForAcrossTrend;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDateHistogramForAcrossTrendForStages;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForDoubleTerms;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForLongTerms;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForMultiTerm;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForMultiTermJira;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDbAggResultForStrTerms;
import static io.levelops.faceted_search.converters.EsConverterUtils.getDoubleTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getLongTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getMultiTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getStringTermBucketsForAttributes;
import static io.levelops.faceted_search.converters.EsConverterUtils.getStringTermBucketsForCustomFields;
import static io.levelops.faceted_search.converters.EsConverterUtils.getStringTermsBucketList;
import static io.levelops.faceted_search.converters.EsConverterUtils.getVersionsFromEsWorkItemVersions;
import static io.levelops.faceted_search.converters.EsConverterUtils.isNotEmpty;

/*
 * This class is meant for converting ES model to DB model.
 */
public class EsWorkItemConverter {

    public static DbWorkItem getWorkItemsFromEsWorkItem(EsWorkItem esWorkItem, List<DbWorkItemField> dbWorkItemFields, EsDevProdWorkItemResponse devProdWorkItemResponse,
                                                        boolean hasTicketPortion, boolean hasStoryPointsPortion, boolean hasAssigneeTime) {
        DbWorkItem.DbWorkItemBuilder dbWorkItemBuilder = DbWorkItem.builder()
                .id(esWorkItem.getId())
                .workItemId(esWorkItem.getWorkitemId())
                .project(esWorkItem.getProject())
                .summary(isNotEmpty(esWorkItem.getSummary()))
                .status(isNotEmpty(esWorkItem.getStatus()))
                .workItemType(isNotEmpty(esWorkItem.getWorkItemType()))
                .priority(isNotEmpty((esWorkItem.getPriority())))
                .integrationId(esWorkItem.getIntegrationId().toString())
                .epic(isNotEmpty(esWorkItem.getEpic()))
                .labels(esWorkItem.getLabels())
                .parentWorkItemId(esWorkItem.getParentWorkItemId())
                .numAttachments(esWorkItem.getNumAttachments())
                .assignee(esWorkItem.getAssignee() != null ? esWorkItem.getAssignee().getDisplayName() : "_UNASSIGNED_")
                .assigneeId(esWorkItem.getAssignee() != null ? esWorkItem.getAssignee().getId() : null)
                .reporter(esWorkItem.getReporter() != null ? esWorkItem.getReporter().getDisplayName() : null)
                .reporterId(esWorkItem.getReporter() != null ? esWorkItem.getReporter().getId() : null)
                .originalEstimate((float) (esWorkItem.getOriginalEstimate() != null ? esWorkItem.getOriginalEstimate().longValue() : 0L))
                .storyPoint(esWorkItem.getStoryPoints() != null ? esWorkItem.getStoryPoints() : 0.0f)
                .bounces(esWorkItem.getBounces())
                .hops(esWorkItem.getHops())
                .components(esWorkItem.getComponents())
                .descSize(esWorkItem.getDescSize())
                .workItemCreatedAt(getWorkItemTimeStamp(esWorkItem.getCreatedAt()))
                .workItemUpdatedAt(getWorkItemTimeStamp(esWorkItem.getUpdatedAt()))
                .firstAttachmentAt(esWorkItem.getFirstAttachmentAt() != null ? getWorkItemTimeStamp(esWorkItem.getFirstAttachmentAt()) : null)
                .ingestedAt(esWorkItem.getIngestedAt().getTime())
                .firstCommentAt(esWorkItem.getFirstCommentAt() != null ? getWorkItemTimeStamp(esWorkItem.getFirstCommentAt()) : null)
                .workItemResolvedAt(esWorkItem.getResolvedAt() != null ? getWorkItemTimeStamp(esWorkItem.getResolvedAt()) : null)
                .workItemDueAt(esWorkItem.getDueAt() != null ? getWorkItemTimeStamp(esWorkItem.getDueAt()) : null)
                .versions(getVersionsFromEsWorkItemVersions(esWorkItem.getVersions()))
                .fixVersions(getVersionsFromEsWorkItemVersions(esWorkItem.getFixVersions()))
                .isActive(Boolean.TRUE.equals(esWorkItem.getIsActive()))
                .resolution(isNotEmpty(esWorkItem.getResolution()))
                .statusCategory(esWorkItem.getStatusCategory())
                .customFields(getConvertedExtensibleFields(esWorkItem.getCustomFields(), dbWorkItemFields))
                .attributes(getConvertedExtensibleFields(esWorkItem.getAttributes(), null))
                .sprintIds(esWorkItem.getHistoricalSprints().stream().map(EsWorkItem.EsHistoricalSprint::getSprintId)
                        .map(UUID::fromString).collect(Collectors.toList()));
        if (devProdWorkItemResponse != null) {
            dbWorkItemBuilder = dbWorkItemBuilder
                    .ticketPortion(hasTicketPortion ? devProdWorkItemResponse.getTicketPortion() : null)
                    .storyPointsPortion(hasStoryPointsPortion ? devProdWorkItemResponse.getStoryPointsPortion() : null)
                    .assigneeTime(hasAssigneeTime ? devProdWorkItemResponse.getAssigneeTime().longValue() : null);
        }
        return dbWorkItemBuilder.build();
    }

    private static Timestamp getWorkItemTimeStamp(Timestamp time) {
        return new Timestamp(TimeUnit.SECONDS.toMillis(time.getTime()));
    }

    public static List<DbAggregationResult> getStacksForWorkItems(List<StringTermsBucket> termsBuckets,
                                                                  List<DoubleTermsBucket> doubleTermsBucketList,
                                                                  List<LongTermsBucket> longTermsBucketList,
                                                                  List<MultiTermsBucket> multiTermsBuckets,
                                                                  List<DateHistogramBucket> dateHistogramBuckets,
                                                                  List<StringTermsBucket> strBucketsForCustomFields,
                                                                  List<DoubleTermsBucket> doubleBucketsForCustomFields,
                                                                  List<LongTermsBucket> longBucketsFoCustomFields,
                                                                  WorkItemsFilter.DISTINCT stack,
                                                                  WorkItemsFilter.CALCULATION calculation) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultForStack = new AtomicReference<>();
        switch (stack) {
            case first_assignee:
                multiTermsBuckets.forEach(multiTerm -> {
                    AtomicBoolean isAssigneeEmptyInMultiTerm = new AtomicBoolean(false);
                    dbAggResultForStack.set(getDbAggResultForMultiTerm(multiTerm));
                    populateCalculationInAgg(dbAggResultForStack, calculation, multiTerm.aggregations(), isAssigneeEmptyInMultiTerm, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInMultiTerm.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResultForStack.get().build());
                });
                break;
            case workitem_resolved_at:
            case workitem_updated_at:
            case workitem_created_at:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    AtomicBoolean isAssigneeEmpty = new AtomicBoolean(false);
                    dbAggResultForStack.set(getDbAggResultForDateHistogram(dateHistogramBucket, false, false));
                    populateCalculationInAgg(dbAggResultForStack, calculation, dateHistogramBucket.aggregations(), isAssigneeEmpty, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmpty.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResultForStack.get().build());
                });
                break;
            case trend:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    AtomicBoolean isAssigneeEmpty = new AtomicBoolean(false);
                    dbAggResultForStack.set(getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, false, false));
                    populateCalculationInAgg(dbAggResultForStack, calculation, dateHistogramBucket.aggregations(), isAssigneeEmpty, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmpty.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResultForStack.get().build());
                });
                break;
            case assignee:
            case reporter:
                termsBuckets.forEach(term -> {
                    List<StringTermsBucket> terms2 = term.aggregations().get("across_" + stack.name()).sterms().buckets().array();
                    terms2.forEach(term2 -> {
                        AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                        dbAggResultForStack.set(getDbAggResultForStrTerms(term2, false).additionalKey(term.key()));
                        populateCalculationInAgg(dbAggResultForStack, calculation, term2.aggregations(), isAssigneeEmptyInDefault, null);
                        if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                            return;
                        }
                        dbAggregationResults.add(dbAggResultForStack.get().build());
                    });
                });
                break;
            case attribute:
            case custom_field:
                if (CollectionUtils.isNotEmpty(strBucketsForCustomFields)) {
                    strBucketsForCustomFields.forEach(stringTermsBucket -> {
                        dbAggResultForStack.set(getDbAggResultForStrTerms(stringTermsBucket, false));
                        populateCalculationInAgg(dbAggResultForStack, calculation, stringTermsBucket.aggregations().get("custom_nested_root")
                                        .reverseNested().aggregations(),
                                new AtomicBoolean(false), null);
                        dbAggregationResults.add(dbAggResultForStack.get().build());
                    });
                } else if (CollectionUtils.isNotEmpty(doubleBucketsForCustomFields)) {
                    doubleBucketsForCustomFields.forEach(doubleTermsBucket -> {
                        dbAggResultForStack.set(getDbAggResultForDoubleTerms(doubleTermsBucket, false));
                        populateCalculationInAgg(dbAggResultForStack, calculation, doubleTermsBucket.aggregations().get("custom_nested_root")
                                        .reverseNested().aggregations(),
                                new AtomicBoolean(false), null);
                        dbAggregationResults.add(dbAggResultForStack.get().build());
                    });
                } else if (CollectionUtils.isNotEmpty(longBucketsFoCustomFields)) {
                    longBucketsFoCustomFields.forEach(longTermsBucket -> {
                        dbAggResultForStack.set(getDbAggResultForLongTerms(longTermsBucket, false));
                        populateCalculationInAgg(dbAggResultForStack, calculation, longTermsBucket.aggregations().get("custom_nested_root")
                                        .reverseNested().aggregations(),
                                new AtomicBoolean(false), null);
                        dbAggregationResults.add(dbAggResultForStack.get().build());
                    });
                }
                break;
            case story_points:
                doubleTermsBucketList.forEach(term1 -> {
                    AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                    dbAggResultForStack.set(getDbAggResultForDoubleTerms(term1, false));
                    populateCalculationInAgg(dbAggResultForStack, calculation, term1.aggregations(), isAssigneeEmptyInDefault, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResultForStack.get().build());
                });
                break;
            default:
                termsBuckets.forEach(term1 -> {
                    AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                    dbAggResultForStack.set(getDbAggResultForStrTerms(term1, false));
                    populateCalculationInAgg(dbAggResultForStack, calculation, term1.aggregations(), isAssigneeEmptyInDefault, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResultForStack.get().build());
                });
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    public static List<DbAggregationResult> getAggForWorkItemFromSearchResponse(SearchResponse<EsWorkItem> searchResponse, WorkItemsFilter workItemsFilter, WorkItemsMilestoneFilter milestoneFilter, WorkItemsFilter.DISTINCT across, WorkItemsFilter.DISTINCT stack, WorkItemsFilter.CALCULATION calculation, Boolean valuesOnly) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult = new AtomicReference<>();
        if (calculation == WorkItemsFilter.CALCULATION.sprint_mapping) {
            addAggsForSprintMapping(searchResponse, milestoneFilter, dbAggregationResults, dbAggResult);
        } else if (calculation == WorkItemsFilter.CALCULATION.stage_bounce_report || calculation == WorkItemsFilter.CALCULATION.stage_times_report) {
            addAggsForStages(searchResponse, workItemsFilter, across, stack, calculation, dbAggregationResults, dbAggResult);
        } else {
            addAggs(searchResponse, workItemsFilter, across, stack, calculation, dbAggregationResults, dbAggResult, valuesOnly);
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    private static void addAggsForSprintMapping(SearchResponse<EsWorkItem> searchResponse, WorkItemsMilestoneFilter milestoneFilter, List<DbAggregationResult> dbAggregationResults, AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult) {
        List<CompositeBucket> compositeBuckets = searchResponse.aggregations().get(WorkItemsFilter.CALCULATION.sprint_mapping.name()).composite().buckets().array();
        List<Hit<EsWorkItem>> hits = searchResponse.hits().hits();
        compositeBuckets.forEach(compositeBucket -> {
            String integrationId = getCompositeBucketKeyValue(compositeBucket, "integration_id");
            String sprintMappingId = getCompositeBucketKeyValue(compositeBucket, "sprint_mappings.id");
            List<StringTermsBucket> nestedAgg1 = compositeBucket.aggregations().get("sprint_mapping_nested").nested().aggregations().get("sprint_mapping_name").sterms().buckets().array();

            nestedAgg1.forEach(milestoneNameBucket -> {
                String milestoneName = milestoneNameBucket.key();
                List<LongTermsBucket> nestedAgg2 = milestoneNameBucket.aggregations().get("sprint_mapping_start_time").lterms().buckets().array();

                nestedAgg2.forEach(milestoneStartTimeBucket -> {
                    String milestoneStartTime = String.valueOf(milestoneStartTimeBucket.key());
                    List<LongTermsBucket> nestedAgg3 = milestoneStartTimeBucket.aggregations().get("sprint_mapping_completed_at").lterms().buckets().array();

                    nestedAgg3.forEach(milestoneCompletedAtBucket -> {
                        String milestoneCompletedAt = String.valueOf(milestoneCompletedAtBucket.key());
                        prepareAggResultsForSprintMetricReport(dbAggregationResults, dbAggResult, hits, integrationId, sprintMappingId, milestoneName, milestoneStartTime, milestoneCompletedAt, milestoneFilter);
                    });
                });
            });
        });
    }

    private static void prepareAggResultsForSprintMetricReport(List<DbAggregationResult> dbAggregationResults,
                                                               AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult,
                                                               List<Hit<EsWorkItem>> hits, String integrationId, String sprintMappingId,
                                                               String milestoneName, String milestoneStartTime, String milestoneCompletedAt, WorkItemsMilestoneFilter milestoneFilter) {
        List<EsWorkItem> issueMgmtHits = getEsWorkItemForBucket(hits, integrationId, sprintMappingId,
                milestoneName, milestoneStartTime, milestoneCompletedAt, milestoneFilter.getStates().get(0));
        if (issueMgmtHits.size() == 0) {
            return;
        }
        List<Pair<EsWorkItem, EsWorkItem.EsSprintMapping>> workItemTypesAndSprintMappings = new ArrayList<>();
        issueMgmtHits.forEach(esWorkItem -> {
            esWorkItem.getSprintMappings().forEach(esSprintMapping -> {
                if (esSprintMapping.getId().equals(sprintMappingId)) {
                    workItemTypesAndSprintMappings.add(Pair.of(esWorkItem, esSprintMapping));
                }
            });
        });
        if (milestoneFilter.getCompletedAtRange() != null) {
            Long milestoneCompletedAtAfter = milestoneFilter.getCompletedAtRange().getLeft();
            Long milestoneCompletedAtBefore = milestoneFilter.getCompletedAtRange().getRight();
            if (milestoneCompletedAtAfter <= Long.parseLong(milestoneCompletedAt) && milestoneCompletedAtBefore > Long.parseLong(milestoneCompletedAt)) {
                dbAggResult.set(DbAggregationResult.builder()
                        .integrationId(integrationId)
                        .sprintId(sprintMappingId)
                        .sprintName(milestoneName)
                        .sprintStartedAt(Long.valueOf(milestoneStartTime))
                        .sprintCompletedAt(Long.valueOf(milestoneCompletedAt))
                        .issueMgmtSprintMappingAggResults(workItemTypesAndSprintMappings.stream()
                                .map(pair -> IssueMgmtSprintMappingAggResult.builder()
                                        .sprintMapping(DbIssueMgmtSprintMapping.builder()
                                                .integrationId(String.valueOf(pair.getFirst().getIntegrationId()))
                                                .workitemId(pair.getFirst().getWorkitemId())
                                                .sprintId(pair.getSecond().getId())
                                                .addedAt(pair.getSecond().getAddedAt())
                                                .planned(pair.getSecond().getPlanned())
                                                .delivered(pair.getSecond().getDelivered())
                                                .outsideOfSprint(pair.getSecond().getOutsideOfSprint())
                                                .ignorableWorkitemType(pair.getSecond().getIgnorableIssueType())
                                                .storyPointsPlanned(pair.getSecond().getStoryPointsPlanned())
                                                .storyPointsDelivered(pair.getSecond().getStoryPointsDelivered())
                                                .build())
                                        .workitemType(pair.getFirst().getWorkItemType())
                                        .build())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                        )
                );
                dbAggregationResults.add(dbAggResult.get().build());
            }
        }
    }

    private static List<EsWorkItem> getEsWorkItemForBucket(List<Hit<EsWorkItem>> hits, String integrationId,
                                                           String sprintMappingId, String milestoneName,
                                                           String milestoneStartTime, String milestoneCompletedAt, String milestoneState) {
        String[] split = sprintMappingId.split("\\\\");
        if (!split[split.length - 1].equals(milestoneName)) {
            return List.of();
        }
        List<EsWorkItem> esWorkItemList = new ArrayList<>();
        hits.forEach(esWorkItemHit -> {
            EsWorkItem esWorkItem = esWorkItemHit.source();
            if (esWorkItem != null) {
                List<String> sprintMappingsIds = esWorkItem.getSprintMappings().stream().map(EsWorkItem.EsSprintMapping::getId).collect(Collectors.toList());
                List<String> milestoneNames = new ArrayList<>();
                List<String> milestoneStates = new ArrayList<>();
                List<Long> milestoneStartTimes = new ArrayList<>();
                List<Long> milestoneCompletedAts = new ArrayList<>();
                boolean isStateMatching = true;
                for (EsWorkItem.EsMilestone milestone : esWorkItem.getMilestones()) {
                    if (milestone.getFullName().equals(sprintMappingId) && !milestone.getState().equals(milestoneState)) {
                        isStateMatching = false;
                        break;
                    }
                    if (milestone.getName() != null) {
                        milestoneNames.add(milestone.getName());
                    }
                    if (milestone.getState() != null) {
                        milestoneStates.add(milestone.getState());
                    }
                    if (milestone.getStartTime() != null) {
                        milestoneStartTimes.add(milestone.getStartTime().getTime());
                    }
                    if (milestone.getCompletedAt() != null) {
                        milestoneCompletedAts.add(milestone.getCompletedAt().getTime());
                    }
                }
                if (!isStateMatching) { // exclude those EsWorkItem whos state is not milestoneState
                    return;
                }
                if (esWorkItem.getIntegrationId().toString().equals(integrationId)
                        && sprintMappingsIds.contains(sprintMappingId)
                        && milestoneNames.contains(milestoneName)
                        && (milestoneState != null && milestoneStates.contains(milestoneState))
                        && (milestoneStartTime != null && milestoneStartTimes.contains(Long.valueOf(milestoneStartTime)))
                        && (milestoneCompletedAt != null && milestoneCompletedAts.contains(Long.valueOf(milestoneCompletedAt)))) {
                    esWorkItemList.add(esWorkItem);
                }
            }
        });
        return esWorkItemList;
    }

    private static String getCompositeBucketKeyValue(CompositeBucket compositeBucket, String name) {
        return compositeBucket.key().get(name).deserialize(JsonpDeserializer.stringDeserializer());
    }

    private static void addAggsForStages(SearchResponse<EsWorkItem> searchResponse, WorkItemsFilter workItemsFilter,
                                         WorkItemsFilter.DISTINCT across, WorkItemsFilter.DISTINCT stack,
                                         WorkItemsFilter.CALCULATION calculation,
                                         List<DbAggregationResult> dbAggregationResults,
                                         AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult) {
        switch (across) {
            case workitem_resolved_at:
            case workitem_updated_at:
            case workitem_created_at:
                String attributesColName = workItemsFilter.getAttributeStack();
                List<DateHistogramBucket> dateHistogramBuckets =
                        searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    List<StringTermsBucket> stringTermsBucketBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForAttr = StringUtils.isNotEmpty(attributesColName) ?
                            getStringTermBucketsForAttributes(attributesColName, null, stack, dateHistogramBucket.aggregations()) : List.of();
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(null, stack, dateHistogramBucket.aggregations()) : List.of();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        Map<String, Aggregate> nestedAggs = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations();
                        List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, null, multiTermsBucketList, doubleTermsBucketList, strBucketsForAttr, strBucketsForCustomFields, workItemsFilter, stack, calculation, stringTermsBucket.key());
                        if (stack != WorkItemsFilter.DISTINCT.none) {
                            dbAggResult.set(getDbAggResultForDateHistogram(dateHistogramBucket, false, false).stacks(stacks));
                        } else {
                            dbAggResult.set(getDbAggResultForDateHistogram(dateHistogramBucket, false, false));
                        }
                        populateCalculationInAgg(dbAggResult, calculation, nestedAggs, null, stringTermsBucket.key());
                        dbAggregationResults.add(dbAggResult.get().build());
                    });
                });
                break;
            case trend:
                attributesColName = workItemsFilter.getAttributeStack();
                List<DateHistogramBucket> dateHistogramBucketsForTrend = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBucketsForTrend.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForAttr = StringUtils.isNotEmpty(attributesColName) ?
                            getStringTermBucketsForAttributes(attributesColName, null, stack, dateHistogramBucket.aggregations()) : List.of();
                    List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(null, stack, dateHistogramBucket.aggregations()) : List.of();
                    List<StringTermsBucket> stringTermsBucketBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForAttr, strBucketsForCustomFields, workItemsFilter, stack, calculation, stringTermsBucket.key());
                        if (stack != WorkItemsFilter.DISTINCT.none) {
                            dbAggResult.set(getDbAggResultForDateHistogramForAcrossTrendForStages(dateHistogramBucket, false, false).stacks(stacks));
                        } else {
                            dbAggResult.set(getDbAggResultForDateHistogramForAcrossTrendForStages(dateHistogramBucket, false, false));
                        }
                        populateCalculationInAgg(dbAggResult, calculation, dateHistogramBucket.aggregations().get("across_nested").nested().aggregations(), null, stringTermsBucket.key());
                        dbAggregationResults.add(dbAggResult.get().build());
                    });
                });
                break;
            default:
                List<MultiTermsBucket> multiTermsBuckets;
                List<StringTermsBucket> sTermsBuckets;
                List<DoubleTermsBucket> dTermsBuckets;
                attributesColName = workItemsFilter.getAttributeStack();
                if (across == WorkItemsFilter.DISTINCT.custom_field) {
                    sTermsBuckets = searchResponse.aggregations().get("across_custom_fields").nested().aggregations().get("filter_custom_fields_name")
                            .filter().aggregations().get("across_custom_fields_type").sterms().buckets().array();
                    getDbAggResult(workItemsFilter, across, stack, calculation, dbAggregationResults, dbAggResult, sTermsBuckets);
                } else if (across == WorkItemsFilter.DISTINCT.attribute) {
                    sTermsBuckets = searchResponse.aggregations().get("across_" + across.name()).nested()
                            .aggregations().get("filter_attributes_name").filter().aggregations().get("across_attributes_" + attributesColName).sterms().buckets().array();
                    getDbAggResult(workItemsFilter, across, stack, calculation, dbAggregationResults, dbAggResult, sTermsBuckets);
                } else if (across == WorkItemsFilter.DISTINCT.assignee || across == WorkItemsFilter.DISTINCT.reporter) {
                    List<MultiTermsBucket> multiTermsBucketList = searchResponse.aggregations().get("across_" + across.name()).multiTerms().buckets().array();
                    multiTermsBucketList.forEach(multiTermsBucket -> {
                        String key = multiTermsBucket.key().get(0);
                        String additionalKey = multiTermsBucket.key().get(1);
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, multiTermsBucket.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, multiTermsBucket.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, multiTermsBucket.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, multiTermsBucket.aggregations());
                        List<StringTermsBucket> strBucketsForAttr = StringUtils.isNotEmpty(attributesColName) ?
                                getStringTermBucketsForAttributes(attributesColName, null, stack, multiTermsBucket.aggregations()) : List.of();
                        List<MultiTermsBucket> multiTermsBucketListFromResponse = getMultiTermsBucketList(null, stack, multiTermsBucket.aggregations());
                        List<StringTermsBucket> sTermBucketsForStages = multiTermsBucket.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(null, stack, multiTermsBucket.aggregations()) : List.of();
                        sTermBucketsForStages.forEach(stringTermsBucket -> {
                            List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, dateHistograms, multiTermsBucketListFromResponse,
                                    doubleTermsBucketList, strBucketsForAttr, strBucketsForCustomFields, workItemsFilter, stack, calculation, stringTermsBucket.key());
                            Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                            if (stack == WorkItemsFilter.DISTINCT.none) {
                                dbAggResult.set(DbAggregationResult.builder().key(StringUtils.isNotEmpty(key) ? key : "_UNASSIGNED_").additionalKey(StringUtils.isNotEmpty(additionalKey) ? additionalKey : "_UNASSIGNED_").totalTickets(multiTermsBucket.docCount()));
                            } else {
                                dbAggResult.set(getDbAggResultForMultiTerm(multiTermsBucket).stacks(stacks));
                            }
                            populateCalculationInAgg(dbAggResult, calculation, nestedAggs, null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResult.get().build());
                        });
                    });

                } else if (across == WorkItemsFilter.DISTINCT.story_points) {
                    dTermsBuckets = searchResponse.aggregations().get("across_" + across).dterms().buckets().array();
                    dTermsBuckets.forEach(dTerm -> {
                        Map<String, Aggregate> nestedAggs = dTerm.aggregations().get("across_nested").nested().aggregations();
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, dTerm.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, dTerm.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, dTerm.aggregations());
                        List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, dTerm.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, dTerm.aggregations());
                        List<StringTermsBucket> strBucketsForAttr = StringUtils.isNotEmpty(attributesColName) ?
                                getStringTermBucketsForAttributes(attributesColName, null, stack, dTerm.aggregations()) : List.of();
                        List<StringTermsBucket> sTermBucketsForStages = dTerm.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(null, stack, dTerm.aggregations()) : List.of();
                        sTermBucketsForStages.forEach(stringTermsBucket -> {
                            List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForAttr, strBucketsForCustomFields, workItemsFilter, stack, calculation, stringTermsBucket.key());
                            if (stack == WorkItemsFilter.DISTINCT.none) {
                                dbAggResult.set(DbAggregationResult.builder().key(String.valueOf(dTerm.key())).totalTickets(dTerm.docCount()));
                            } else {
                                dbAggResult.set(getDbAggResultForDoubleTerms(dTerm, false).stacks(stacks));
                            }
                            populateCalculationInAgg(dbAggResult, calculation, nestedAggs, null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResult.get().build());
                        });
                    });

                } else if (across == WorkItemsFilter.DISTINCT.sprint) {
                    sTermsBuckets = searchResponse.aggregations().get("across_" + across).nested().aggregations()
                            .get("across_nested_sprint").sterms().buckets().array();
                    sTermsBuckets.forEach(sTerm -> {
                        List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, sTerm.aggregations());
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, sTerm.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, sTerm.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, sTerm.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, sTerm.aggregations());
                        List<StringTermsBucket> strBucketsForAttr = StringUtils.isNotEmpty(attributesColName) ?
                                getStringTermBucketsForAttributes(attributesColName, null, stack, sTerm.aggregations()) : List.of();
                        List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(null, stack, sTerm.aggregations()) : List.of();
                        List<StringTermsBucket> sTermBucketsForStages = sTerm.aggregations().get("across_nested").nested().aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        sTermBucketsForStages.forEach(stringTermsBucket -> {
                            Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                            List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, strBucketsForAttr, strBucketsForCustomFields, workItemsFilter, stack, calculation, stringTermsBucket.key());
                            if (stack == WorkItemsFilter.DISTINCT.none) {
                                dbAggResult.set(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()));
                            } else {
                                dbAggResult.set(getDbAggResultForStrTerms(sTerm, false).stacks(stacks));
                            }
                            populateCalculationInAgg(dbAggResult, calculation, nestedAggs, null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResult.get().build());
                        });
                    });
                } else {
                    sTermsBuckets = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();
                    sTermsBuckets.forEach(sTerm -> {
                        List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, sTerm.aggregations());
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, sTerm.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, sTerm.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, sTerm.aggregations());
                        List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, sTerm.aggregations());
                        List<StringTermsBucket> strBucketsForAttr = StringUtils.isNotEmpty(attributesColName) ?
                                getStringTermBucketsForAttributes(attributesColName, null, stack, sTerm.aggregations()) : List.of();
                        List<StringTermsBucket> strBucketsForCustomFields = stack.toString().equals("custom_field") ? getStringTermBucketsForCustomFields(null, stack, sTerm.aggregations()) : List.of();
                        List<StringTermsBucket> sTermBucketsForStages = sTerm.aggregations().get("across_nested").nested()
                                .aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
                        sTermBucketsForStages.forEach(stringTermsBucket -> {
                            Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                            List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, dateHistograms,
                                    multiTermsBucketList, doubleTermsBucketList, strBucketsForAttr, strBucketsForCustomFields, workItemsFilter, stack, calculation, stringTermsBucket.key());
                            if (across == WorkItemsFilter.DISTINCT.none) {
                                if (stack == WorkItemsFilter.DISTINCT.none) {
                                    dbAggResult.set(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()));
                                } else {
                                    dbAggResult.set(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()));
                                }
                            } else {
                                if (stack == WorkItemsFilter.DISTINCT.none) {
                                    dbAggResult.set(DbAggregationResult.builder().key(sTerm.key() != null ? sTerm.key() : null).totalTickets(stringTermsBucket.docCount()));
                                } else {
                                    dbAggResult.set(getDbAggResultForStrTerms(sTerm, false).stacks(stacks));
                                }
                            }
                            populateCalculationInAgg(dbAggResult, calculation, nestedAggs, null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResult.get().build());
                        });
                    });
                }
        }
    }

    private static void getDbAggResult(WorkItemsFilter workItemsFilter, WorkItemsFilter.DISTINCT across,
                                       WorkItemsFilter.DISTINCT stack, WorkItemsFilter.CALCULATION calculation,
                                       List<DbAggregationResult> dbAggregationResults,
                                       AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult,
                                       List<StringTermsBucket> sTermsBuckets) {
        sTermsBuckets.forEach(stringTermsBucket -> {
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, stringTermsBucket.aggregations());
            List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, stringTermsBucket.aggregations());
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, stringTermsBucket.aggregations());
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, stringTermsBucket.aggregations());
            List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, stringTermsBucket.aggregations());
            List<StringTermsBucket> sTermBucketsForStages = stringTermsBucket.aggregations().get("across_nested").reverseNested()
                    .aggregations().get(across.name() + "_nested_across").sterms().buckets().array();
            sTermBucketsForStages.forEach(stringTermsBucket1 -> {
                List<DbAggregationResult> stacks = getStacksForWorkItemsStages(stringTermsBucketList, dateHistograms, multiTermsBucketList, doubleTermsBucketList, List.of(), List.of(), workItemsFilter, stack, calculation, stringTermsBucket1.key());
                Map<String, Aggregate> nestedAggs = stringTermsBucket.aggregations();
                if (across == WorkItemsFilter.DISTINCT.none) {
                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(DbAggregationResult.builder().key(stringTermsBucket.key() != null ? stringTermsBucket.key() : null).totalTickets(stringTermsBucket.docCount()));
                    } else {
                        dbAggResult.set(DbAggregationResult.builder().key(stringTermsBucket.key() != null ? stringTermsBucket.key() : null).totalTickets(stringTermsBucket.docCount()));
                    }
                } else {
                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(DbAggregationResult.builder().key(stringTermsBucket.key() != null ? stringTermsBucket.key() : null).totalTickets(stringTermsBucket.docCount()));
                    } else {
                        dbAggResult.set(getDbAggResultForStrTerms(stringTermsBucket, false).stacks(stacks));
                    }
                }
                populateCalculationInAgg(dbAggResult, calculation, nestedAggs.get("across_nested").reverseNested().aggregations(), null,
                        stringTermsBucket1.key());
                dbAggregationResults.add(dbAggResult.get().build());
            });
        });
    }


    public static List<DbAggregationResult> getStacksForCustomFields(List<StringTermsBucket> stringTermsBuckets,
                                                                     List<DoubleTermsBucket> doubleTermsBuckets,
                                                                     List<LongTermsBucket> longTermsBuckets,
                                                                     List<DateHistogramBucket> dateHistograms, WorkItemsFilter.DISTINCT stack,
                                                                     WorkItemsFilter.CALCULATION calculation) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        stringTermsBuckets.forEach(term1 -> {
            AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForStrTerms(term1, false));
            buildStackResults(stack, term1.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForStrTerms(term1, false));
        });
        doubleTermsBuckets.forEach(term2 -> {
            AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForDoubleTerms(term2, false));
            buildStackResults(stack, term2.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForDoubleTerms(term2, false));
        });
        longTermsBuckets.forEach(term3 -> {
            AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForLongTerms(term3, false));
            buildStackResults(stack, term3.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForLongTerms(term3, false));
        });
        dateHistograms.forEach(term4 -> {
            AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultBuilder = new AtomicReference<>(getDbAggResultForDateHistogram(term4, false, false));
            buildStackResults(stack, term4.aggregations(), dbAggResultBuilder, calculation, dbAggregationResults, getDbAggResultForDateHistogram(term4, false, false));

        });
        return dbAggregationResults;
    }

    private static void buildStackResults(WorkItemsFilter.DISTINCT stack, Map<String, Aggregate> aggregateMap,
                                          AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultBuilder,
                                          WorkItemsFilter.CALCULATION calculation,
                                          List<DbAggregationResult> dbAggregationResults,
                                          DbAggregationResult.DbAggregationResultBuilder dbAggregationResultBuilder) {
        Map<String, Aggregate> aggregations;
        switch (stack) {
            case attribute:
            case custom_field:
                aggregations = aggregateMap.get("custom_nested_root").reverseNested().aggregations();
                populateCalculationInAgg(dbAggResultBuilder, calculation, aggregations, new AtomicBoolean(false), null);
                dbAggregationResults.add(dbAggResultBuilder.get().build());
                break;
            case assignee:
            case reporter:
                List<StringTermsBucket> terms2 = aggregateMap.get("across_" + stack.name()).sterms().buckets().array();
                terms2.forEach(termsBucket -> {
                    String key1 = dbAggregationResultBuilder.build().getKey();
                    dbAggResultBuilder.set(getDbAggResultForMultiTermJira(termsBucket.key(), key1, termsBucket.docCount()));
                    populateCalculationInAgg(dbAggResultBuilder, calculation, termsBucket.aggregations(), new AtomicBoolean(false), null);
                    dbAggregationResults.add(dbAggResultBuilder.get().build());
                });
                break;
            default:
                dbAggResultBuilder.set(dbAggregationResultBuilder);
                populateCalculationInAgg(dbAggResultBuilder, calculation, aggregateMap, new AtomicBoolean(false), null);
                dbAggregationResults.add(dbAggResultBuilder.get().build());
        }
    }

    private static List<DbAggregationResult> getStacksForWorkItemsStages(List<StringTermsBucket> stringTermsBuckets,
                                                                         List<DateHistogramBucket> dateHistogramBuckets,
                                                                         List<MultiTermsBucket> multiTermsBuckets,
                                                                         List<DoubleTermsBucket> doubleTermsBuckets,
                                                                         List<StringTermsBucket> strBucketsForAttr,
                                                                         List<StringTermsBucket> strBucketsForCustomFields, WorkItemsFilter workItemsFilter,
                                                                         WorkItemsFilter.DISTINCT stack,
                                                                         WorkItemsFilter.CALCULATION calculation,
                                                                         String stage) {
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultForStack = new AtomicReference<>();
        switch (stack) {
            case workitem_resolved_at:
            case workitem_updated_at:
            case workitem_created_at:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    dbAggResultForStack.set(getDbAggResultForDateHistogram(dateHistogramBucket, false, false));
                    List<StringTermsBucket> termsBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    termsBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            populateCalculationInAgg(dbAggResultForStack, calculation, dateHistogramBucket.aggregations().get("across_nested").nested().aggregations(), null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultForStack.get().build());
                        }
                    });
                });
                break;
            case trend:
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    List<StringTermsBucket> termsBuckets = dateHistogramBucket.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    termsBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            dbAggResultForStack.set(getDbAggResultForDateHistogramForAcrossTrendForStages(dateHistogramBucket, false, false));
                            populateCalculationInAgg(dbAggResultForStack, calculation, dateHistogramBucket.aggregations().get("across_nested").nested().aggregations(), null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultForStack.get().build());
                        }
                    });
                });
                break;
            case assignee:
            case reporter:
                multiTermsBuckets.forEach(multiTerm -> {
                    String key = multiTerm.key().get(0);
                    String additionalKey = multiTerm.key().get(1);
                    List<StringTermsBucket> stringTermsBucketBuckets = multiTerm.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            Map<String, Aggregate> nestedAggs = multiTerm.aggregations().get("across_nested").nested().aggregations();
                            dbAggResultForStack.set(DbAggregationResult.builder()
                                    .key(StringUtils.isNotEmpty(key) ? key : "_UNASSIGNED_")
                                    .additionalKey(StringUtils.isNotEmpty(additionalKey) ? additionalKey : "_UNASSIGNED_")
                                    .totalTickets(multiTerm.docCount()));
                            populateCalculationInAgg(dbAggResultForStack, calculation, nestedAggs, null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultForStack.get().build());
                        }
                    });
                });
                break;
            case story_points:
                doubleTermsBuckets.forEach(doubleTermsBucket -> {
                    List<StringTermsBucket> stringTermsBucketBuckets = doubleTermsBucket.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            dbAggResultForStack.set(getDbAggResultForDoubleTerms(doubleTermsBucket, false));
                            populateCalculationInAgg(dbAggResultForStack, calculation, doubleTermsBucket.aggregations().get("across_nested").nested().aggregations(), null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultForStack.get().build());
                        }
                    });
                });
                break;
            case custom_field:
                getDbAgResultsForAttrAndCustomFields(strBucketsForCustomFields, stack, calculation, stage, dbAggregationResults, dbAggResultForStack);
                break;
            case attribute:
                getDbAgResultsForAttrAndCustomFields(strBucketsForAttr, stack, calculation, stage, dbAggregationResults, dbAggResultForStack);
                break;
            default:
                stringTermsBuckets.forEach(sTerm -> {
                    List<StringTermsBucket> stringTermsBucketBuckets = sTerm.aggregations().get("across_nested").nested().aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
                    stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                        if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                            dbAggResultForStack.set(getDbAggResultForStrTerms(sTerm, false));
                            populateCalculationInAgg(dbAggResultForStack, calculation, sTerm.aggregations().get("across_nested").nested().aggregations(), null, stringTermsBucket.key());
                            dbAggregationResults.add(dbAggResultForStack.get().build());
                        }
                    });
                });
        }
        return EsConverterUtils.sanitizeResponse(dbAggregationResults);
    }

    private static void getDbAgResultsForAttrAndCustomFields(List<StringTermsBucket> strBucketsForCustomFields, WorkItemsFilter.DISTINCT stack, WorkItemsFilter.CALCULATION calculation, String stage, List<DbAggregationResult> dbAggregationResults, AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResultForStack) {
        strBucketsForCustomFields.forEach(sTerm -> {
            List<StringTermsBucket> stringTermsBucketBuckets = sTerm.aggregations().get("across_nested").reverseNested()
                    .aggregations().get(stack.name() + "_nested_across").sterms().buckets().array();
            stringTermsBucketBuckets.forEach(stringTermsBucket -> {
                if (stage.equalsIgnoreCase(stringTermsBucket.key())) {
                    dbAggResultForStack.set(getDbAggResultForStrTerms(sTerm, false));
                    populateCalculationInAgg(dbAggResultForStack, calculation, sTerm.aggregations().get("across_nested").reverseNested().aggregations(), null, stringTermsBucket.key());
                    dbAggregationResults.add(dbAggResultForStack.get().build());
                }
            });
        });
        return;
    }

    private static void addAggs(SearchResponse<EsWorkItem> searchResponse, WorkItemsFilter workItemsFilter,
                                WorkItemsFilter.DISTINCT across, WorkItemsFilter.DISTINCT stack,
                                WorkItemsFilter.CALCULATION calculation,
                                List<DbAggregationResult> dbAggregationResults,
                                AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult,
                                Boolean valuesOnly) {
        switch (across) {
            case sprint_mapping:
                List<MultiTermsBucket> multiTermsBuckets = searchResponse.aggregations().get("across_" + across).multiTerms().buckets().array();
                multiTermsBuckets.forEach(multiTerm -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, multiTerm.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, multiTerm.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, multiTerm.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, multiTerm.aggregations());
                    List<DateHistogramBucket> dateHistogramBuckets = getDateHistogramBuckets(null, stack, multiTerm.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                        Aggregate aggregate = null;
                        if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                            aggregate = multiTerm.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name").filter()
                                    .aggregations().get("across_custom_fields_type");
                        }
                        if (stack == WorkItemsFilter.DISTINCT.attribute) {
                            String attributesCol = workItemsFilter.getAttributeStack();
                            aggregate = multiTerm.aggregations().get("across_" + stack.name())
                                    .nested().aggregations().get("filter_attributes_name").filter()
                                    .aggregations().get("across_attributes_" + attributesCol);
                        }
                        if (aggregate.isSterms()) {
                            strBucketsForCustomFields = aggregate.sterms().buckets().array();
                        } else if (aggregate.isDterms()) {
                            doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                        } else if (aggregate.isLterms()) {
                            longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList,
                            longTermsBucketList, multiTermsBucketList,
                            dateHistogramBuckets, strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(getDbAggResultForMultiTerm(multiTerm));
                    } else {
                        dbAggResult.set(getDbAggResultForMultiTerm(multiTerm).stacks(stacks));
                    }
                    AtomicBoolean isAssigneeEmptyMultiTerm = new AtomicBoolean(false);
                    populateCalculationInAgg(dbAggResult, calculation, multiTerm.aggregations(), isAssigneeEmptyMultiTerm, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyMultiTerm.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResult.get()
                            .build());
                });
                break;
            case workitem_resolved_at:
            case workitem_updated_at:
            case workitem_created_at:
                List<DateHistogramBucket> dateHistogramBuckets =
                        searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBuckets.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                        Aggregate aggregate = null;
                        if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                            aggregate = dateHistogramBucket.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name").filter()
                                    .aggregations().get("across_custom_fields_type");
                        }
                        if (stack == WorkItemsFilter.DISTINCT.attribute) {
                            String attributesCol = workItemsFilter.getAttributeStack();
                            aggregate = dateHistogramBucket.aggregations().get("across_" + stack.name())
                                    .nested().aggregations().get("filter_attributes_name").filter()
                                    .aggregations().get("across_attributes_" + attributesCol);
                        }
                        if (aggregate.isSterms()) {
                            strBucketsForCustomFields = aggregate.sterms().buckets().array();
                        } else if (aggregate.isDterms()) {
                            doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                        } else if (aggregate.isLterms()) {
                            longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList, multiTermsBucketList,
                            dateHistograms, strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);

                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(getDbAggResultForDateHistogram(dateHistogramBucket, workItemsFilter.getAggInterval().equalsIgnoreCase("quarter"), false));
                    } else {
                        dbAggResult.set(getDbAggResultForDateHistogram(dateHistogramBucket, workItemsFilter.getAggInterval().equalsIgnoreCase("quarter"), false)
                                .stacks(stacks));
                    }

                    AtomicBoolean isAssigneeEmpty = new AtomicBoolean(false);
                    populateCalculationInAgg(dbAggResult, calculation, dateHistogramBucket.aggregations(), isAssigneeEmpty, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmpty.get()) {
                        return;
                    }

                    dbAggregationResults.add(dbAggResult.get()
                            .build());
                });
                break;
            case trend:
                List<DateHistogramBucket> dateHistogramBucketsForTrend = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateHistogramBucketsForTrend.forEach(dateHistogramBucket -> {
                    List<MultiTermsBucket> multiTermsBucketList = getMultiTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, dateHistogramBucket.aggregations());
                    List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, dateHistogramBucket.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                        Aggregate aggregate = null;
                        if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                            aggregate = dateHistogramBucket.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name").filter()
                                    .aggregations().get("across_custom_fields_type");
                        }
                        if (stack == WorkItemsFilter.DISTINCT.attribute) {
                            String attributesCol = workItemsFilter.getAttributeStack();
                            aggregate = dateHistogramBucket.aggregations().get("across_" + stack.name())
                                    .nested().aggregations().get("filter_attributes_name").filter()
                                    .aggregations().get("across_attributes_" + attributesCol);
                        }
                        if (aggregate.isSterms()) {
                            strBucketsForCustomFields = aggregate.sterms().buckets().array();
                        } else if (aggregate.isDterms()) {
                            doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                        } else if (aggregate.isLterms()) {
                            longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList, multiTermsBucketList,
                            dateHistograms, strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);

                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, workItemsFilter.getAggInterval().equalsIgnoreCase("quarter"), false));
                    } else {
                        dbAggResult.set(getDbAggResultForDateHistogramForAcrossTrend(dateHistogramBucket, workItemsFilter.getAggInterval().equalsIgnoreCase("quarter"), false)
                                .stacks(stacks));
                    }

                    AtomicBoolean isAssigneeEmpty = new AtomicBoolean(false);
                    populateCalculationInAgg(dbAggResult, calculation, dateHistogramBucket.aggregations(), isAssigneeEmpty, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmpty.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResult.get()
                            .build());
                });
                break;
            case assignee:
            case reporter:
                int acrossLimit = workItemsFilter.getAcrossLimit() != null ? workItemsFilter.getAcrossLimit() : 90;
                List<StringTermsBucket> terms = searchResponse.aggregations().get("across_" + across)
                        .sterms().buckets().array();
                terms.forEach(term -> {
                    List<StringTermsBucket> terms2 = term.aggregations().get("across_" + across.name()).sterms().buckets().array();
                    terms2.forEach(term2 -> {
                        List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, term2.aggregations());
                        List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, term2.aggregations());
                        List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, term2.aggregations());
                        List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                        List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                        List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                        if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                            Aggregate aggregate = null;
                            if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                                aggregate = term2.aggregations().get("across_custom_field")
                                        .nested().aggregations().get("filter_custom_fields_name").filter()
                                        .aggregations().get("across_custom_fields_type");
                            }
                            if (stack == WorkItemsFilter.DISTINCT.attribute) {
                                String attributesCol = workItemsFilter.getAttributeStack();
                                aggregate = term2.aggregations().get("across_" + stack.name())
                                        .nested().aggregations().get("filter_attributes_name").filter()
                                        .aggregations().get("across_attributes_" + attributesCol);
                            }
                            if (aggregate.isSterms()) {
                                strBucketsForCustomFields = aggregate.sterms().buckets().array();
                            } else if (aggregate.isDterms()) {
                                doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                            } else if (aggregate.isLterms()) {
                                longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                            }
                        }
                        List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList,
                                List.of(), List.of(), strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                        if (calculation == WorkItemsFilter.CALCULATION.assign_to_resolve && term2.key().equals("_UNASSIGNED_")) {
                            return;
                        }
                        String key = null;
                        if (!term.key().equals("_UNASSIGNED_")) {
                            key = term2.key();
                        }
                        AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                        DbAggregationResult.DbAggregationResultBuilder aggregationResultBuilder = DbAggregationResult.builder()
                                .key(key)
                                .totalTickets(term2.docCount())
                                .additionalKey(term.key());
                        if (stack != WorkItemsFilter.DISTINCT.none) {
                            aggregationResultBuilder = aggregationResultBuilder.stacks(stacks);
                        }
                        dbAggResult.set(aggregationResultBuilder);
                        populateCalculationInAgg(dbAggResult, calculation, term2.aggregations(), isAssigneeEmptyInDefault, null);
                        if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                            return;
                        }
                        if (valuesOnly) {
                            dbAggregationResults.add(dbAggResult.get().build());
                        } else if (dbAggregationResults.size() < acrossLimit) {
                            dbAggregationResults.add(dbAggResult.get().build());
                        }
                    });
                });
                break;
            case story_points:
                List<DoubleTermsBucket> doubleTermsBuckets = searchResponse.aggregations().get("across_" + across)
                        .dterms().buckets().array();
                doubleTermsBuckets.forEach(term -> {
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, term.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, term.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, term.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                        Aggregate aggregate = null;
                        if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                            aggregate = term.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name").filter()
                                    .aggregations().get("across_custom_fields_type");
                        }
                        if (stack == WorkItemsFilter.DISTINCT.attribute) {
                            String attributesCol = workItemsFilter.getAttributeStack();
                            aggregate = term.aggregations().get("across_" + stack.name())
                                    .nested().aggregations().get("filter_attributes_name").filter()
                                    .aggregations().get("across_attributes_" + attributesCol);
                        }
                        if (aggregate.isSterms()) {
                            strBucketsForCustomFields = aggregate.sterms().buckets().array();
                        } else if (aggregate.isDterms()) {
                            doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                        } else if (aggregate.isLterms()) {
                            longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList,
                            longTermsBucketList, List.of(), List.of(), strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(getDbAggResultForDoubleTerms(term, false));
                    } else {
                        dbAggResult.set(getDbAggResultForDoubleTerms(term, false).stacks(stacks));
                    }
                    populateCalculationInAgg(dbAggResult, calculation, term.aggregations(), isAssigneeEmptyInDefault, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResult.get().build());
                });
                break;
            case stage:
                List<StringTermsBucket> sTermsBuckets = searchResponse.aggregations().get("across_" + across)
                        .nested().aggregations().get("across_historical_status").sterms().buckets().array();
                sTermsBuckets.forEach(term -> {
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, term.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, term.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, term.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                        Aggregate aggregate = null;
                        if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                            aggregate = term.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name").filter()
                                    .aggregations().get("across_custom_fields_type");
                        }
                        if (stack == WorkItemsFilter.DISTINCT.attribute) {
                            String attributesCol = workItemsFilter.getAttributeStack();
                            aggregate = term.aggregations().get("across_" + stack.name())
                                    .nested().aggregations().get("filter_attributes_name").filter()
                                    .aggregations().get("across_attributes_" + attributesCol);
                        }
                        if (aggregate.isSterms()) {
                            strBucketsForCustomFields = aggregate.sterms().buckets().array();
                        } else if (aggregate.isDterms()) {
                            doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                        } else if (aggregate.isLterms()) {
                            longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList,
                            List.of(), List.of(), strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    if (calculation == WorkItemsFilter.CALCULATION.assign_to_resolve) {
                        return;
                    }
                    AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                    if (stack == WorkItemsFilter.DISTINCT.none) {
                        dbAggResult.set(getDbAggResultForStrTerms(term, false));
                    } else {
                        dbAggResult.set(getDbAggResultForStrTerms(term, false).stacks(stacks));
                    }
                    populateCalculationInAgg(dbAggResult, calculation, term.aggregations(), isAssigneeEmptyInDefault, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResult.get().build());
                });
                break;
            case custom_field:
                Aggregate aggregateForCustomField = searchResponse.aggregations().get("across_custom_field")
                        .nested().aggregations().get("filter_custom_fields_name").filter()
                        .aggregations().get("across_custom_fields_type");
                if (aggregateForCustomField.isSterms()) {
                    processForSrtTermsAgg(stack, calculation, dbAggregationResults, dbAggResult, aggregateForCustomField, workItemsFilter);
                } else if (aggregateForCustomField.isLterms()) {
                    processForLTermsAgg(stack, calculation, dbAggregationResults, dbAggResult, aggregateForCustomField, workItemsFilter);
                } else if (aggregateForCustomField.isDterms()) {
                    processForDoubleTermsAgg(stack, calculation, dbAggregationResults, dbAggResult, aggregateForCustomField, workItemsFilter);
                }
                break;
            case attribute:
                String attributesColName = workItemsFilter.getAttributeAcross();
                Aggregate aggregateForAttribute = searchResponse.aggregations().get("across_" + across.name())
                        .nested().aggregations().get("filter_attributes_name").filter()
                        .aggregations().get("across_attributes_" + attributesColName);
                if (aggregateForAttribute.isSterms()) {
                    processForSrtTermsAgg(stack, calculation, dbAggregationResults, dbAggResult, aggregateForAttribute, workItemsFilter);
                } else if (aggregateForAttribute.isLterms()) {
                    processForLTermsAgg(stack, calculation, dbAggregationResults, dbAggResult, aggregateForAttribute, workItemsFilter);
                } else if (aggregateForAttribute.isDterms()) {
                    processForDoubleTermsAgg(stack, calculation, dbAggregationResults, dbAggResult, aggregateForAttribute, workItemsFilter);
                }
                break;
            default:
                List<StringTermsBucket> termsBuckets;
                if (across == WorkItemsFilter.DISTINCT.sprint) {
                    termsBuckets = searchResponse.aggregations().get("across_" + across)
                            .nested().aggregations().get("across_sprint_nested").sterms().buckets().array();
                } else {
                    termsBuckets = searchResponse.aggregations().get("across_" + across)
                            .sterms().buckets().array();
                }
                termsBuckets.forEach(term -> {
                    List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, term.aggregations());
                    List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, term.aggregations());
                    List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, term.aggregations());
                    List<StringTermsBucket> strBucketsForCustomFields = new ArrayList<>();
                    List<DoubleTermsBucket> doubleBucketsForCustomFields = new ArrayList<>();
                    List<LongTermsBucket> longBucketsFoCustomFields = new ArrayList<>();
                    if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                        Aggregate aggregate = null;
                        if (stack == WorkItemsFilter.DISTINCT.custom_field) {
                            aggregate = term.aggregations().get("across_custom_field")
                                    .nested().aggregations().get("filter_custom_fields_name").filter()
                                    .aggregations().get("across_custom_fields_type");
                        }
                        if (stack == WorkItemsFilter.DISTINCT.attribute) {
                            String attributesCol = workItemsFilter.getAttributeStack();
                            aggregate = term.aggregations().get("across_" + stack.name())
                                    .nested().aggregations().get("filter_attributes_name").filter()
                                    .aggregations().get("across_attributes_" + attributesCol);
                        }
                        if (aggregate.isSterms()) {
                            strBucketsForCustomFields = aggregate.sterms().buckets().array();
                        } else if (aggregate.isDterms()) {
                            doubleBucketsForCustomFields = aggregate.dterms().buckets().array();
                        } else if (aggregate.isLterms()) {
                            longBucketsFoCustomFields = aggregate.lterms().buckets().array();
                        }
                    }
                    List<DbAggregationResult> stacks = getStacksForWorkItems(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList,
                            List.of(), List.of(), strBucketsForCustomFields, doubleBucketsForCustomFields, longBucketsFoCustomFields, stack, calculation);
                    if (calculation == WorkItemsFilter.CALCULATION.assign_to_resolve && term.key().equals("_UNASSIGNED_")) {
                        return;
                    }
                    AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
                    if (across == WorkItemsFilter.DISTINCT.none) {
                        if (stack == WorkItemsFilter.DISTINCT.none) {
                            dbAggResult.set(DbAggregationResult.builder()
                                    .totalTickets(term.docCount()));
                        } else {
                            dbAggResult.set(DbAggregationResult.builder()
                                    .totalTickets(term.docCount())
                                    .stacks(stacks));
                        }
                    } else {
                        if (stack == WorkItemsFilter.DISTINCT.none) {
                            dbAggResult.set(getDbAggResultForStrTerms(term, false));
                        } else {
                            dbAggResult.set(getDbAggResultForStrTerms(term, false).stacks(stacks));
                        }
                    }
                    populateCalculationInAgg(dbAggResult, calculation, term.aggregations(), isAssigneeEmptyInDefault, null);
                    if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                        return;
                    }
                    dbAggregationResults.add(dbAggResult.get().build());
                });
        }
    }

    private static void processForDoubleTermsAgg(WorkItemsFilter.DISTINCT stack, WorkItemsFilter.CALCULATION calculation, List<DbAggregationResult> dbAggregationResults, AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult, Aggregate aggregate, WorkItemsFilter workItemsFilter) {
        List<DoubleTermsBucket> dTermsBuckets = aggregate.dterms().buckets().array();
        dTermsBuckets.forEach(term -> {
            Map<String, Aggregate> customNestedRoot = term.aggregations().get("custom_nested_root").reverseNested().aggregations();
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, customNestedRoot);
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, customNestedRoot);
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, customNestedRoot);
            List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, customNestedRoot);
            if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
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
            List<DbAggregationResult> stacks = getStacksForCustomFields(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList,
                    dateHistograms, stack, calculation);
            if (calculation == WorkItemsFilter.CALCULATION.assign_to_resolve) {
                return;
            }
            AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
            if (stack == WorkItemsFilter.DISTINCT.none) {
                dbAggResult.set(getDbAggResultForDoubleTerms(term, false));
            } else {
                dbAggResult.set(getDbAggResultForDoubleTerms(term, false).stacks(stacks));
            }
            populateCalculationInAgg(dbAggResult, calculation, customNestedRoot, isAssigneeEmptyInDefault, null);
            if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                return;
            }
            dbAggregationResults.add(dbAggResult.get().build());
        });
    }

    private static void processForLTermsAgg(WorkItemsFilter.DISTINCT stack, WorkItemsFilter.CALCULATION calculation, List<DbAggregationResult> dbAggregationResults, AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult, Aggregate aggregate, WorkItemsFilter workItemsFilter) {
        List<LongTermsBucket> lTermsBuckets = aggregate.lterms().buckets().array();
        lTermsBuckets.forEach(term -> {
            Map<String, Aggregate> customNestedRoot = term.aggregations().get("custom_nested_root").reverseNested().aggregations();
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, customNestedRoot);
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, customNestedRoot);
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, customNestedRoot);
            List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, customNestedRoot);
            if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
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
            List<DbAggregationResult> stacks = getStacksForCustomFields(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList,
                    dateHistograms, stack, calculation);
            if (calculation == WorkItemsFilter.CALCULATION.assign_to_resolve) {
                return;
            }
            AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
            if (stack == WorkItemsFilter.DISTINCT.none) {
                dbAggResult.set(getDbAggResultForLongTerms(term, false));
            } else {
                dbAggResult.set(getDbAggResultForLongTerms(term, false).stacks(stacks));
            }
            populateCalculationInAgg(dbAggResult, calculation, customNestedRoot, isAssigneeEmptyInDefault, null);
            if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                return;
            }
            dbAggregationResults.add(dbAggResult.get().build());
        });
    }

    private static void processForSrtTermsAgg(WorkItemsFilter.DISTINCT stack, WorkItemsFilter.CALCULATION calculation,
                                              List<DbAggregationResult> dbAggregationResults,
                                              AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult,
                                              Aggregate aggregate, WorkItemsFilter workItemsFilter) {
        List<StringTermsBucket> sTermsBuckets = aggregate.sterms().buckets().array();
        sTermsBuckets.forEach(term -> {
            Map<String, Aggregate> customNestedRoot = term.aggregations().get("custom_nested_root").reverseNested().aggregations();
            List<StringTermsBucket> stringTermsBucketList = getStringTermsBucketList(null, stack, customNestedRoot);
            List<DoubleTermsBucket> doubleTermsBucketList = getDoubleTermsBucketList(null, stack, customNestedRoot);
            List<LongTermsBucket> longTermsBucketList = getLongTermsBucketList(null, stack, customNestedRoot);
            List<DateHistogramBucket> dateHistograms = getDateHistogramBuckets(null, stack, customNestedRoot);
            if (stack == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.attribute) {
                String suffixForQuery = stack == WorkItemsFilter.DISTINCT.custom_field ? "custom_fields" : "attributes";
                String postFixForQuery = stack == WorkItemsFilter.DISTINCT.custom_field ? suffixForQuery + "_type" : suffixForQuery + "_" + workItemsFilter.getAttributeStack();
                Aggregate aggregate1 = term.aggregations().get("custom_nested_root").reverseNested().aggregations().get("across_" + stack.name())
                        .nested().aggregations().get("filter_" + suffixForQuery + "_name").filter().aggregations()
                        .get("across_" + postFixForQuery);
                if (aggregate1.isSterms()) {
                    stringTermsBucketList = aggregate1.sterms().buckets().array();
                } else if (aggregate.isDterms()) {
                    doubleTermsBucketList = aggregate1.dterms().buckets().array();
                } else if (aggregate.isLterms()) {
                    longTermsBucketList = aggregate1.lterms().buckets().array();
                }
            }
            List<DbAggregationResult> stacks = getStacksForCustomFields(stringTermsBucketList, doubleTermsBucketList, longTermsBucketList,
                    dateHistograms, stack, calculation);
            if (calculation == WorkItemsFilter.CALCULATION.assign_to_resolve && term.key().equals("_UNASSIGNED_")) {
                return;
            }
            AtomicBoolean isAssigneeEmptyInDefault = new AtomicBoolean(false);
            if (stack == WorkItemsFilter.DISTINCT.none) {
                dbAggResult.set(getDbAggResultForStrTerms(term, false));
            } else {
                dbAggResult.set(getDbAggResultForStrTerms(term, false).stacks(stacks));
            }
            populateCalculationInAgg(dbAggResult, calculation, customNestedRoot, isAssigneeEmptyInDefault, null);
            if (calculation == WorkItemsFilter.CALCULATION.assignees && isAssigneeEmptyInDefault.get()) {
                return;
            }
            dbAggregationResults.add(dbAggResult.get().build());
        });
    }

    private static void populateCalculationInAgg(AtomicReference<DbAggregationResult.DbAggregationResultBuilder> dbAggResult,
                                                 WorkItemsFilter.CALCULATION calculation,
                                                 Map<String, Aggregate> aggregation,
                                                 AtomicBoolean isAssigneeEmpty, String stage) {
        switch (calculation) {
            case issue_count:
                break;
            case resolution_time:
                dbAggResult.set(dbAggResult.get()
                        .max((long) aggregation.get(calculation.name()).stats().max())
                        .min((long) aggregation.get(calculation.name()).stats().min())
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                        .p90(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "90.0"))
                );
                break;
            case assign_to_resolve:
            case hops:
            case bounces:
                dbAggResult.set(dbAggResult.get()
                        .max((long) aggregation.get(calculation.name()).stats().max())
                        .min((long) aggregation.get(calculation.name()).stats().min())
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                );
                break;
            case age:
                dbAggResult.set(dbAggResult.get()
                        .max((long) aggregation.get(calculation.name()).stats().max())
                        .min((long) aggregation.get(calculation.name()).stats().min())
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                        .p90(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "90.0"))
                        .totalStoryPoints((long) aggregation.get("total_story_points").sum().value())
                );
                break;
            case story_point_report:
                dbAggResult.set(dbAggResult.get()
                        .totalStoryPoints((long) aggregation.get("total_story_points").sum().value())
                        .totalUnestimatedTickets((long) aggregation.get("total_unestimated_tickets").sum().value())
                );
                break;
            case effort_report:
                dbAggResult.set(dbAggResult.get()
                        .totalEffort((long) aggregation.get("total_effort").sum().value())
                        .totalUnestimatedTickets((long) aggregation.get("total_unestimated_tickets").sum().value())
                );
                break;
            case sprint_mapping:
                break;
            case sprint_mapping_count:
                break;
            case assignees:
                List<String> assignees = aggregation.get(calculation.name()).sterms().buckets().array()
                        .stream()
                        .map(StringTermsBucket::key)
                        .collect(Collectors.toList());
                if (assignees.size() == 0 && isAssigneeEmpty != null) {
                    isAssigneeEmpty.set(true);
                }
                dbAggResult.set(dbAggResult.get()
                        .assignees(assignees)
                        .total((long) CollectionUtils.size(assignees))
                );
                break;
            case stage_times_report:
                dbAggResult.set(dbAggResult.get()
                        .max((long) aggregation.get(calculation.name()).stats().max())
                        .min((long) aggregation.get(calculation.name()).stats().min())
                        .stage(stage != null ? stage : "[SOMETHINGS WRONG]")
                        .mean(aggregation.get(calculation.name()).stats().avg())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                );
                break;
            case stage_bounce_report:
                dbAggResult.set(dbAggResult.get()
                        .stage(stage != null ? stage : "[SOMETHINGS WRONG]")
                        .mean(aggregation.get(calculation.name() + "_mean").simpleValue().value())
                        .median(getPercentileFromPercentileBucket(aggregation.get(calculation.name() + "_percentiles").percentilesBucket(), "50.0"))
                );
                break;
            default:
                dbAggResult.set(dbAggResult.get()
                        .max((long) aggregation.get(calculation.name()).stats().max())
                        .min((long) aggregation.get(calculation.name()).stats().min())
                        .median(getPercentileFromTDigestPercentile(aggregation.get(calculation.name() + "_percentiles").tdigestPercentiles(), "50.0"))
                );
        }
    }

    private static Long getPercentileFromPercentileBucket(PercentilesBucketAggregate percentilesBucket, String s) {
        String value = percentilesBucket.values().keyed().getOrDefault(s, null);
        if (value == null) {
            return null;
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
}
