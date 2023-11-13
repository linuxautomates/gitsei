package io.levelops.faceted_search.querybuilders.workitems;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ScriptQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.IssueMgmtCustomFieldUtils;
import io.levelops.commons.faceted_search.db.utils.EsWorkItemCalculation;
import io.levelops.faceted_search.utils.EsUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.utils.ESAggResultUtils.getSortOrder;

@Log4j2
public class EsWorkItemQueryBuilder {
    private static final Set<String> CUSTOM_FIELD_TYPES = Set.of("integer", "long", "string", "boolean", "float");
    private static final Map<String, String> ATTRIBUTES_FIELD_TYPE = Map.of("project", "str",
            "code_area", "str", "organization", "str", "acceptance_criteria", "str",
            "teams", "arr");

    public static SearchRequest.Builder buildSearchRequest(WorkItemsFilter workItemFilter,
                                                           WorkItemsMilestoneFilter milestoneFilter,
                                                           List<String> developmentStages,
                                                           List<String> historicalAssignees,
                                                           ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories,
                                                           List<DbWorkItemField> workItemCustomFields,
                                                           Boolean needMissingStoryPointsFilter,
                                                           WorkItemsFilter.DISTINCT stack,
                                                           WorkItemsFilter.CALCULATION calculation, String indexNameOrAlias,
                                                           Boolean valuesOnly, Integer page, Integer pageSize, Boolean isList) {
        List<Query> includeQueryConditions = new ArrayList<>();
        List<Query> excludeQueryConditions = new ArrayList<>();
        includeQueryConditions.add(EsUtils.getQuery("w_integ_type", List.of("issue_mgmt")));
        createWorkItemFilterIncludeCondition(workItemFilter, developmentStages, historicalAssignees, histAssigneesAndHistStatusCategories, needMissingStoryPointsFilter, includeQueryConditions);
        if (CollectionUtils.isNotEmpty(workItemFilter.getExtraCriteria())) {
            createHygieneCondition(workItemFilter, includeQueryConditions, excludeQueryConditions);
        }
        if (MapUtils.isNotEmpty(workItemFilter.getAttributes())) {
            createAttributesConditions(workItemFilter.getAttributes(), includeQueryConditions);
        }
        if (MapUtils.isNotEmpty(workItemFilter.getCustomFields())) {
            createCustomFieldConditions(workItemFilter.getCustomFields(), workItemCustomFields, includeQueryConditions);
        }
        if (MapUtils.isNotEmpty(workItemFilter.getMissingFields())) {
            Map<String, Boolean> missingCustomFields = new HashMap<>();
            Map<String, Boolean> missingAttributeFields = new HashMap<>();
            Map<WorkItemsFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields = new EnumMap<>(
                    WorkItemsFilter.MISSING_BUILTIN_FIELD.class);
            workItemFilter.getMissingFields().forEach((field, shouldBeMissing) -> {
                if (Optional.ofNullable(WorkItemsFilter.MISSING_BUILTIN_FIELD.fromString(field)).isPresent()) {
                    Optional.ofNullable(WorkItemsFilter.MISSING_BUILTIN_FIELD.fromString(field))
                            .map(builtInField -> missingBuiltinFields.put(builtInField, shouldBeMissing));
                } else if (IssueMgmtCustomFieldUtils.isCustomField(field)) {
                    missingCustomFields.put(field, shouldBeMissing);
                } else {
                    missingAttributeFields.put(field, shouldBeMissing);
                }
            });
            createMissingFieldsClause(missingBuiltinFields,
                    missingCustomFields, missingAttributeFields, includeQueryConditions, excludeQueryConditions);
        }
        if (MapUtils.isNotEmpty(workItemFilter.getPartialMatch())) {
            createPartialMatchFilter(includeQueryConditions, workItemFilter.getPartialMatch(),
                    WorkItemQueryCriteria.PARTIAL_MATCH_COLUMNS, WorkItemQueryCriteria.PARTIAL_MATCH_ARRAY_COLUMNS,
                    WorkItemQueryCriteria.PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS,
                    WorkItemQueryCriteria.PARTIAL_MATCH_ATTRIBUTES_COLUMNS, Boolean.FALSE);
        }

        createWorkItemFilterRangeCondition(workItemFilter, includeQueryConditions);
        createWorkItemFilterExcludeCondition(workItemFilter, excludeQueryConditions);

        if (MapUtils.isNotEmpty(workItemFilter.getExcludeAttributes())) {
            createAttributesConditions(workItemFilter.getExcludeAttributes(), excludeQueryConditions);
        }
        if (MapUtils.isNotEmpty(workItemFilter.getExcludeCustomFields())) {
            createCustomFieldConditions(workItemFilter.getExcludeCustomFields(), workItemCustomFields,
                    excludeQueryConditions);
        }
        createMilestoneFilterIncludeCondition(milestoneFilter, includeQueryConditions);
        createMilestoneFilterRangeCondition(milestoneFilter, includeQueryConditions);
        if (MapUtils.isNotEmpty(milestoneFilter.getPartialMatch())) {
            createPartialMatchFilter(includeQueryConditions, milestoneFilter.getPartialMatch(),
                    WorkItemsMilestoneFilter.PARTIAL_MATCH_COLUMNS,
                    WorkItemsMilestoneFilter.PARTIAL_MATCH_ARRAY_COLUMNS, WorkItemsMilestoneFilter.PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS,
                    Map.of(), Boolean.TRUE);
        }
        createMilestoneFilterExcludeCondition(milestoneFilter, excludeQueryConditions);

        if (workItemFilter.getCalculation() == WorkItemsFilter.CALCULATION.age) {
            if (workItemFilter.getSnapshotRange() != null && StringUtils.isNotBlank(workItemFilter.getAggInterval())) {
                // get age intervals
                var now = Instant.now();
                var rangeFrom = workItemFilter.getSnapshotRange().getLeft() != null ? workItemFilter.getSnapshotRange().getLeft() : now.minus(Duration.ofDays(90)).getEpochSecond();
                var rangeTo = workItemFilter.getSnapshotRange().getRight() != null ? workItemFilter.getSnapshotRange().getRight() : now.getEpochSecond();
                var intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, workItemFilter.getAggInterval());
                if (CollectionUtils.isNotEmpty(intervals)) {
                    includeQueryConditions.add(EsUtils.getQuery("w_ingested_at", intervals.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList())));
                }
            }
        }

        Map<String, Aggregation> aggReportsCondition = new HashMap<>();
        if (calculation != null && calculation != WorkItemsFilter.CALCULATION.sprint_mapping) {
            aggReportsCondition = new HashMap<>(EsWorkItemCalculation.getCalculation(EsWorkItemCalculation.CALCULATION.fromString(calculation.name()), null, page, pageSize));
        }

        boolean doesStageRequired = (calculation == WorkItemsFilter.CALCULATION.stage_bounce_report
                || calculation == WorkItemsFilter.CALCULATION.stage_times_report);

        Map<String, Aggregation> stackAggs = new HashMap<>();
        if (stack != null && calculation != WorkItemsFilter.CALCULATION.sprint_mapping && !isList) {
            stackAggs = getAcrossConditions(stack == WorkItemsFilter.DISTINCT.none ? null : stack,
                    workItemFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build(),
                    workItemCustomFields, aggReportsCondition, null, null, false, true, doesStageRequired, null); // keep page & pageSize null for stack
        }

        Map<String, Aggregation> aggReportsAndStacks = new HashMap<>();
        if (calculation != null && calculation != WorkItemsFilter.CALCULATION.sprint_mapping) {
            aggReportsAndStacks = new HashMap<>(EsWorkItemCalculation.getCalculation(EsWorkItemCalculation.CALCULATION.fromString(calculation.name()), null, page, pageSize));
        }

        aggReportsAndStacks.putAll(stackAggs);

        WorkItemsFilter.DISTINCT across = workItemFilter.getAcross();
        Map<String, Aggregation> aggConditions = Map.of();
        if (calculation == WorkItemsFilter.CALCULATION.sprint_mapping) {
            aggConditions = new HashMap<>(EsWorkItemCalculation.getCalculation(EsWorkItemCalculation.CALCULATION.fromString(calculation.name()), null, page, pageSize));
        } else if (across != null && !isList) {
            aggConditions = getAcrossConditions(across, workItemFilter,
                    workItemCustomFields, aggReportsAndStacks, page, pageSize, valuesOnly, false, doesStageRequired, stack);
        }

        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        searchRequest.index(indexNameOrAlias)
                .query(q -> q
                        .bool(b -> b
                                .must(includeQueryConditions)
                                .mustNot(excludeQueryConditions)
                        )
                )
                .aggregations(aggConditions);
        return searchRequest;
    }

    public static Query getNestedRangeQuery(String path, String fieldName, ImmutablePair<Long, Long> values) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(getRangeQueryForTime(fieldName, values))
                        )
                )
        )._toQuery();
    }

    public static Query getRangeQueryForTime(String field, ImmutablePair<Long, Long> range) {
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        rangeQueryBuilder.field(field);
        rangeQueryBuilder.timeZone("UTC");
        if (range.getLeft() != null) {
            rangeQueryBuilder.gte(JsonData.of(range.getLeft()));
        }
        if (range.getRight() != null) {
            rangeQueryBuilder.lt(JsonData.of(range.getRight()));
        }
        return rangeQueryBuilder.build()._toQuery();
    }

    private static void createMilestoneFilterRangeCondition(WorkItemsMilestoneFilter milestoneFilter,
                                                            List<Query> includeQueryConditions) {
        if (milestoneFilter.getStartedAtRange() != null
                && milestoneFilter.getStartedAtRange().getLeft() != null
                && milestoneFilter.getStartedAtRange().getRight() != null) {
            includeQueryConditions.add(getNestedRangeQuery("w_milestones", "w_milestones.start_time",
                    milestoneFilter.getStartedAtRange()));
        }
        if (milestoneFilter.getCompletedAtRange() != null
                && milestoneFilter.getCompletedAtRange().getLeft() != null
                && milestoneFilter.getCompletedAtRange().getRight() != null) {
            includeQueryConditions.add(getNestedRangeQuery("w_milestones", "w_milestones.completed_at",
                    milestoneFilter.getCompletedAtRange()));
        }
        if (milestoneFilter.getEndedAtRange() != null
                && milestoneFilter.getEndedAtRange().getLeft() != null
                && milestoneFilter.getEndedAtRange().getRight() != null) {
            includeQueryConditions.add(getNestedRangeQuery("w_milestones", "w_milestones.end_time",
                    milestoneFilter.getEndedAtRange()));
        }
    }

    private static void createWorkItemFilterRangeCondition(WorkItemsFilter workItemFilter,
                                                           List<Query> includeQueryConditions) {
        if (workItemFilter.getWorkItemResolvedRange() != null
                && workItemFilter.getWorkItemResolvedRange().getLeft() != null
                && workItemFilter.getWorkItemResolvedRange().getRight() != null) {
            includeQueryConditions.add(EsUtils.getRangeQueryForTime("w_resolved_at",
                    workItemFilter.getWorkItemResolvedRange()));
        }
        if (workItemFilter.getWorkItemCreatedRange() != null
                && workItemFilter.getWorkItemCreatedRange().getLeft() != null
                && workItemFilter.getWorkItemCreatedRange().getRight() != null) {
            includeQueryConditions.add(EsUtils.getRangeQueryForTime("w_created_at",
                    workItemFilter.getWorkItemCreatedRange()));
        }
        if (workItemFilter.getWorkItemUpdatedRange() != null
                && workItemFilter.getWorkItemUpdatedRange().getLeft() != null
                && workItemFilter.getWorkItemUpdatedRange().getRight() != null) {
            includeQueryConditions.add(EsUtils.getRangeQueryForTime("w_updated_at",
                    workItemFilter.getWorkItemUpdatedRange()));
        }
        if (workItemFilter.getSnapshotRange() != null
                && workItemFilter.getSnapshotRange().getLeft() != null
                && workItemFilter.getSnapshotRange().getRight() != null) {
            includeQueryConditions.add(EsUtils.getRangeQueryForTime("w_ingested_at",
                    workItemFilter.getSnapshotRange()));
        }
        if (workItemFilter.getStoryPointsRange() != null
                && workItemFilter.getStoryPointsRange().getLeft() != null
                && workItemFilter.getStoryPointsRange().getRight() != null) {
            JsonData gt = null;
            if (workItemFilter.getStoryPointsRange().getLeft() != null) {
                gt = JsonData.of(workItemFilter.getStoryPointsRange().getLeft());
            }
            JsonData lt = null;
            if (workItemFilter.getStoryPointsRange().getRight() != null) {
                lt = JsonData.of(workItemFilter.getStoryPointsRange().getRight());
            }
            includeQueryConditions.add(EsUtils.getRangeQuery("w_story_points", gt, lt));
        }
    }

    private static void createMilestoneFilterIncludeCondition(WorkItemsMilestoneFilter milestoneFilter,
                                                              List<Query> includeQueryConditions) {
        if (CollectionUtils.isNotEmpty(milestoneFilter.getIntegrationIds())) {
            includeQueryConditions.add(EsUtils.getQuery("w_integration_id", milestoneFilter.getIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getNames())) {
            includeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.name", milestoneFilter.getNames()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getFullNames())) {
            includeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.full_name", milestoneFilter.getFullNames()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getStates())) {
            includeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.state", milestoneFilter.getStates()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getParentFieldValues())) {
            includeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.parent_name",
                    milestoneFilter.getParentFieldValues()));
        }
    }

    public static Query getNestedQuery(String path, String fieldName, List<String> values) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(EsUtils.getQuery(fieldName, values)))
                        )
                )
        )._toQuery();
    }

    private static void createMilestoneFilterExcludeCondition(WorkItemsMilestoneFilter milestoneFilter,
                                                              List<Query> excludeQueryConditions) {
        if (CollectionUtils.isNotEmpty(milestoneFilter.getExcludeIntegrationIds())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_integration_id", milestoneFilter.getExcludeIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getExcludeNames())) {
            excludeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.name", milestoneFilter.getExcludeNames()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getExcludeFullNames())) {
            excludeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.full_name", milestoneFilter.getExcludeFullNames()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getExcludeStates())) {
            excludeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.state", milestoneFilter.getExcludeStates()));
        }
        if (CollectionUtils.isNotEmpty(milestoneFilter.getExcludeParentFieldValues())) {
            excludeQueryConditions.add(getNestedQuery("w_milestones", "w_milestones.parent_name",
                    milestoneFilter.getExcludeParentFieldValues()));
        }
    }

    private static void createWorkItemFilterExcludeCondition(WorkItemsFilter workItemFilter,
                                                             List<Query> excludeQueryConditions) {
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludePriorities())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_priority", workItemFilter.getExcludePriorities()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeWorkItemIds())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_workitem_id", workItemFilter.getExcludeWorkItemIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeEpics())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_epic", workItemFilter.getExcludeEpics()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeVersions())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_versions.name", workItemFilter.getExcludeVersions()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeStatuses())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_status", workItemFilter.getExcludeStatuses()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeWorkItemTypes())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_workitem_type", workItemFilter.getExcludeWorkItemTypes()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeLabels())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_labels", workItemFilter.getExcludeLabels()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeFixVersions())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_fix_versions.name", workItemFilter.getExcludeFixVersions()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeSprintIds())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_hist_sprints.id", workItemFilter.getExcludeSprintIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeStatusCategories())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_status_category", workItemFilter.getExcludeStatusCategories()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeAssignees())) {
            List<String> excludeAssignees = new ArrayList<>(workItemFilter.getExcludeAssignees());
            excludeAssignees.add("null");
            excludeQueryConditions.add(EsUtils.getQuery("w_assignee.id", excludeAssignees));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeParentWorkItemIds())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_parent_workitem_id", workItemFilter.getExcludeParentWorkItemIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeProjects())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_project", workItemFilter.getExcludeProjects()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeReporters())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_reporter.id", workItemFilter.getExcludeReporters()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeFirstAssignees())) {
            excludeQueryConditions.add(EsUtils.getQuery("w_first_assignee.id",
                    workItemFilter.getExcludeFirstAssignees()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getExcludeStages())) {
            if (workItemFilter.getCalculation() != null && workItemFilter.getCalculation().equals(WorkItemsFilter.CALCULATION.stage_bounce_report))
                excludeQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_statuses")
                        .query(q0 -> q0
                                .bool(q1 -> q1
                                        .must(List.of(TermsQuery.of(q -> q
                                                .field("w_hist_statuses.status")
                                                .terms(TermsQueryField.of(termsField -> termsField
                                                        .value(workItemFilter.getExcludeStages().stream()
                                                                .map(str -> new FieldValue.Builder()
                                                                        .stringValue(str)
                                                                        .build())
                                                                .collect(Collectors.toList()))))
                                        )._toQuery())))))._toQuery());
        }
    }

    private static void createWorkItemFilterIncludeCondition(WorkItemsFilter workItemFilter,
                                                             List<String> developmentStages,
                                                             List<String> historicalAssignees,
                                                             ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories,
                                                             Boolean needMissingStoryPointsFilter,
                                                             List<Query> includeQueryConditions) {
        if (CollectionUtils.isNotEmpty(workItemFilter.getIntegrationIds())) {
            includeQueryConditions.add(EsUtils.getQuery("w_integration_id", workItemFilter.getIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getWorkItemIds())) {
            includeQueryConditions.add(EsUtils.getQuery("w_workitem_id", workItemFilter.getWorkItemIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getSprintIds())) {
            includeQueryConditions.add(EsUtils.getQuery("w_hist_sprints.id", workItemFilter.getSprintIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getPriorities())) {
            includeQueryConditions.add(EsUtils.getQuery("w_priority", workItemFilter.getPriorities()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getEpics())) {
            includeQueryConditions.add(EsUtils.getQuery("w_epic", workItemFilter.getEpics()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getAssignees())) {
            includeQueryConditions.add(EsUtils.getQuery("w_assignee.id", workItemFilter.getAssignees()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getVersions())) {
            includeQueryConditions.add(EsUtils.getQuery("w_versions.name", workItemFilter.getVersions()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getStatuses())) {
            includeQueryConditions.add(EsUtils.getQuery("w_status", workItemFilter.getStatuses()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getWorkItemTypes())) {
            includeQueryConditions.add(EsUtils.getQuery("w_workitem_type", workItemFilter.getWorkItemTypes()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getLabels())) {
            includeQueryConditions.add(EsUtils.getQuery("w_labels", workItemFilter.getLabels()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getFixVersions())) {
            includeQueryConditions.add(EsUtils.getQuery("w_fix_versions.name", workItemFilter.getFixVersions()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getStatusCategories())) {
            includeQueryConditions.add(EsUtils.getQuery("w_status_category", workItemFilter.getStatusCategories()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getParentWorkItemIds())) {
            includeQueryConditions.add(EsUtils.getQuery("w_parent_workitem_id", workItemFilter.getParentWorkItemIds()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getProjects())) {
            includeQueryConditions.add(EsUtils.getQuery("w_project", workItemFilter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getReporters())) {
            includeQueryConditions.add(EsUtils.getQuery("w_reporter.id", workItemFilter.getReporters()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getFirstAssignees())) {
            includeQueryConditions.add(EsUtils.getQuery("w_first_assignee.id", workItemFilter.getFirstAssignees()));
        }
        if (CollectionUtils.isNotEmpty(workItemFilter.getStages())) {
            if (workItemFilter.getCalculation() != null && workItemFilter.getCalculation().equals(WorkItemsFilter.CALCULATION.stage_bounce_report))
                includeQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_statuses")
                        .query(q0 -> q0
                                .bool(q1 -> q1
                                        .must(List.of(TermsQuery.of(q -> q
                                                .field("w_hist_statuses.status")
                                                .terms(TermsQueryField.of(termsField -> termsField
                                                        .value(workItemFilter.getStages().stream()
                                                                .map(str -> new FieldValue.Builder()
                                                                        .stringValue(str)
                                                                        .build())
                                                                .collect(Collectors.toList()))))
                                        )._toQuery())))))._toQuery());
        }
        if (needMissingStoryPointsFilter) {
            includeQueryConditions.add(ExistsQuery.of(e -> e
                    .field("w_story_points"))._toQuery());
        }
        if (CollectionUtils.isNotEmpty(developmentStages)) {
            includeQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_statuses")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermsQuery.of(q -> q
                                            .field("w_hist_statuses.status")
                                            .terms(TermsQueryField.of(termsField -> termsField
                                                    .value(developmentStages.stream()
                                                            .map(str -> new FieldValue.Builder()
                                                                    .stringValue(str)
                                                                    .build())
                                                            .collect(Collectors.toList()))))
                                    )._toQuery())))))._toQuery());
        }
        if (CollectionUtils.isNotEmpty(historicalAssignees)) {
            includeQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_assignee_statuses")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermsQuery.of(q -> q
                                            .field("w_hist_assignee_statuses.historical_assignee_id")
                                            .terms(TermsQueryField.of(termsField -> termsField
                                                    .value(historicalAssignees.stream()
                                                            .map(str -> new FieldValue.Builder()
                                                                    .stringValue(str)
                                                                    .build())
                                                            .collect(Collectors.toList()))))
                                    )._toQuery())))))._toQuery());
        }
        if(histAssigneesAndHistStatusCategories != null) {
            List<String> histAssignees = histAssigneesAndHistStatusCategories.getLeft();
            List<String> histStatusCategories = histAssigneesAndHistStatusCategories.getRight();
            if (CollectionUtils.isNotEmpty(histAssignees) || CollectionUtils.isNotEmpty(histStatusCategories)) {
                List<Query> termsQueries = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(histAssignees)) {
                    termsQueries.add(TermsQuery.of(q -> q
                            .field("w_hist_assignee_statuses.historical_assignee_id")
                            .terms(TermsQueryField.of(termsField -> termsField
                                    .value(histAssignees.stream()
                                            .map(str -> new FieldValue.Builder()
                                                    .stringValue(str)
                                                    .build())
                                            .collect(Collectors.toList()))))
                    )._toQuery());
                }

                if (CollectionUtils.isNotEmpty(histStatusCategories)) {
                    termsQueries.add(TermsQuery.of(q -> q
                            .field("w_hist_assignee_statuses.issue_status_category")
                            .terms(TermsQueryField.of(termsField -> termsField
                                    .value(histStatusCategories.stream()
                                            .map(str -> new FieldValue.Builder()
                                                    .stringValue(str)
                                                    .build())
                                            .collect(Collectors.toList()))))
                    )._toQuery());
                }

                includeQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_assignee_statuses")
                        .query(q0 -> q0
                                .bool(q1 -> q1
                                        .must(termsQueries))))._toQuery());

            }
        }
    }

    private static Map<String, Aggregation> getAcrossConditions(WorkItemsFilter.DISTINCT across,
                                                                WorkItemsFilter workItemsFilter,
                                                                List<DbWorkItemField> workItemCustomFields,
                                                                Map<String, Aggregation> nestedAggregation,
                                                                Integer page, Integer pageSize,
                                                                Boolean valuesOnly, Boolean isStack, Boolean doesStageRequired, WorkItemsFilter.DISTINCT stack) {
        if (across == null) {
            return Map.of();
        }
        Integer acrossLimit = workItemsFilter.getAcrossLimit() != null ? workItemsFilter.getAcrossLimit() : 90;
        Map<String, SortingOrder> sortBy = workItemsFilter.getSort();
        SortOrder esSortOrder = getSortOrder(workItemsFilter);
        String keyName = StringUtils.EMPTY;
        WorkItemsFilter.CALCULATION calculation = workItemsFilter.getCalculation();
        if (across == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.custom_field ||
                across == WorkItemsFilter.DISTINCT.attribute || stack == WorkItemsFilter.DISTINCT.attribute) {
            addBucketPagination(nestedAggregation, StringUtils.EMPTY, esSortOrder, valuesOnly, isStack, page, acrossLimit);
        } else {
            if (MapUtils.isNotEmpty(sortBy) && sortBy.keySet().stream().findFirst().isPresent()) {
                String sortField = sortBy.keySet().stream().findFirst().get();
                if (!across.toString().equals(sortField)) {
                    if (!calculation.toString().equals(sortField) &&
                            !(List.of(WorkItemsFilter.CALCULATION.sprint_mapping, WorkItemsFilter.CALCULATION.sprint_mapping_count).contains(calculation))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + sortField);
                    }
                    addSortingFields(nestedAggregation, calculation, esSortOrder, valuesOnly, isStack, page, acrossLimit);
                } else if (across.toString().equals(sortField)) {
                    addBucketPagination(nestedAggregation, StringUtils.EMPTY, esSortOrder, valuesOnly, isStack, page, acrossLimit);
                    keyName = "_key";
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + sortField);
                }
            } else {
                addSortingFields(nestedAggregation, calculation, esSortOrder, valuesOnly, isStack, page, acrossLimit);
            }
        }
        Map<String, Aggregation> aggConditions = new HashMap<>();
        String finalKeyName = keyName;
        if (doesStageRequired) {
            return getStagesAggs(across, workItemsFilter, workItemCustomFields,
                    nestedAggregation, isStack, stack,
                    sortBy, esSortOrder, calculation, aggConditions, finalKeyName);
        }
        switch (across) {
            case none:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_integ_type")
                                            .minDocCount(1)
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case label:
            case component:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name() + "s")
                                            .minDocCount(1)
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case version:
            case fix_version:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name() + "s.name")
                                            .minDocCount(1)
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case custom_field:
                String customColumn = (isStack ? workItemsFilter.getCustomStack() : workItemsFilter.getCustomAcross());
                String customFieldType = EsUtils.getCustomFieldType(workItemCustomFields, null, customColumn);
                String esCustomColumn = "";
                Aggregation updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                esCustomColumn = EsUtils.getCustomFieldColumn(customFieldType, esCustomColumn);
                String finalEsCustomColumn = esCustomColumn;
                if (customColumn != null && finalEsCustomColumn != null) {
                    aggConditions.put("across_custom_field", Aggregation.of(a -> a
                            .nested(n -> n
                                    .path("w_custom_fields")
                            )
                            .aggregations(Map.of("filter_custom_fields_name", Aggregation.of(a1 -> a1
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("w_custom_fields.name")
                                                    .value(customColumn)
                                            )
                                    )
                                    .aggregations(Map.of("across_custom_fields_type", Aggregation.of(a2 -> a2
                                                            .terms(t -> t
                                                                    .field("w_custom_fields." + finalEsCustomColumn)
                                                                    .minDocCount(1)
                                                                    .size(Integer.MAX_VALUE)
                                                            )
                                                            .aggregations(Map.of("custom_nested_root", Aggregation.of(a3 ->
                                                                    a3.reverseNested(fn -> fn.path(null))
                                                                            .aggregations(nestedAggregation)
                                                            )))
                                                     .aggregations(Map.of("bucket_aggs" , updatedBucketAggs))
                                            ))
                                    )))
                            )));
                }
                break;
            case attribute:
                String attributeColumn;
                if (isStack) {
                    if (workItemsFilter.getAttributeStack() == null) {
                        throw new IllegalArgumentException("In workItem filter attributeStack is required!");
                    }
                    attributeColumn = workItemsFilter.getAttributeStack();
                } else {
                    if (workItemsFilter.getAttributeAcross() == null) {
                        throw new IllegalArgumentException("In workItem filter attributeAcross is required!");
                    }
                    attributeColumn = workItemsFilter.getAttributeAcross();
                }
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                String finalAttributeColumn = attributeColumn;
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .nested(n -> n
                                .path("w_attributes")
                        )
                        .aggregations(Map.of("filter_attributes_name", Aggregation.of(a1 -> a1
                                .filter(f -> f
                                        .term(t -> t
                                                .field("w_attributes.name")
                                                .value(finalAttributeColumn)
                                        )
                                )
                                .aggregations(Map.of("across_attributes_" + finalAttributeColumn,
                                        Aggregation.of(a2 -> a2
                                                        .terms(t -> {
                                                                    TermsAggregation.Builder builder = t
                                                                            .field("w_attributes." + ATTRIBUTES_FIELD_TYPE.get(finalAttributeColumn))
                                                                            .minDocCount(1)
                                                                            .size(Integer.MAX_VALUE);
                                                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                                                    else
                                                                        return builder;
                                                                }
                                                        )
                                                        .aggregations(Map.of("custom_nested_root", Aggregation.of(a3 ->
                                                                a3.reverseNested(fn -> fn.path(null))
                                                                        .aggregations(nestedAggregation)
                                                        )))
                                                 .aggregations(Map.of("bucket_aggs" , updatedBucketAggs))
                                        )))
                        )))
                ));
                break;
            case sprint_mapping:
                List<MultiTermLookup> sprintMappingFields = new ArrayList<>();
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_integration_id")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_sprint_mappings.id")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_milestones.name")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_milestones.start_time")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_milestones.completed_at")
                ));
                if (doesStageRequired) {
                    sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                            .field("w_hist_statuses.status"))
                    );
                }
                aggConditions.put("across_sprint_mapping", Aggregation.of(a -> a
                        .multiTerms(mt -> mt
                                .terms(sprintMappingFields)
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case sprint:
                aggConditions.put("across_sprint", Aggregation.of(a -> a
                        .nested(na -> na
                                .path("w_milestones")
                        )
                        .aggregations("across_sprint_nested", Aggregation.of(a1 -> a1
                                .terms(t -> {
                                            TermsAggregation.Builder builder = t
                                                    .field("w_milestones.full_name")
                                                    .minDocCount(1)
                                                    .size(Integer.MAX_VALUE);
                                            if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                                return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                            else
                                                return builder;
                                        }
                                )
                                .aggregations(nestedAggregation)
                        ))
                ));
                break;
            case reporter:
            case assignee:
                Map<String, Aggregation> bucket;
                if (page != null) {
                    bucket = Map.of("across_" + across.name(), Aggregation.of(a1 -> a1
                            .terms(t -> {
                                        TermsAggregation.Builder builder = t
                                                .field("w_" + across.name() + ".id")
                                                .missing("_UNASSIGNED_");
                                        if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                            return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                        else
                                            return builder;
                                    }
                            ).aggregations(nestedAggregation)
                    ), "bucket_pagination", BucketSortAggregation.of(b -> b
                            .from(valuesOnly ? 0 : page * acrossLimit)
                            .size(valuesOnly ? Integer.MAX_VALUE : acrossLimit))._toAggregation());
                } else {
                    bucket = Map.of("across_" + across.name(), Aggregation.of(a1 -> a1
                            .terms(t -> {
                                        TermsAggregation.Builder builder = t
                                                .field("w_" + across.name() + ".id")
                                                .missing("_UNASSIGNED_")
                                                .size(Integer.MAX_VALUE);
                                        if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                            return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                        else
                                            return builder;
                                    }
                            ).aggregations(nestedAggregation)
                    ), "bucket_pagination", BucketSortAggregation.of(a -> a
                            .from(0)
                            .size(isStack || valuesOnly ? Integer.MAX_VALUE : acrossLimit))._toAggregation());
                }
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name() + ".display_name")
                                            .missing("_UNASSIGNED_")
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    return builder;
                                }
                        )
                        .aggregations(bucket)
                ));
                break;
            case first_assignee:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name() + ".display_name")
                                            .minDocCount(1)
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case stage:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .nested(n -> n
                                .path("w_hist_statuses")
                        )
                        .aggregations(Map.of("across_historical_status",
                                Aggregation.of(a2 -> a2
                                        .terms(t -> {
                                                    TermsAggregation.Builder builder = t
                                                            .field("w_hist_statuses.status")
                                                            .minDocCount(1)
                                                            .size(Integer.MAX_VALUE);
                                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                                    else
                                                        return builder;
                                                }
                                        )
                                        .aggregations(nestedAggregation)
                                )))

                ));
                break;
            case trend:
            case workitem_created_at:
            case workitem_updated_at:
            case workitem_resolved_at:
                String interval = workItemsFilter.getAggInterval() != null ?
                        workItemsFilter.getAggInterval() : CalendarInterval.Day.name();
                String field = "";
                switch (across.name()) {
                    case "workitem_created_at":
                        field = "w_created_at";
                        break;
                    case "workitem_updated_at":
                        field = "w_updated_at";
                        break;
                    case "workitem_resolved_at":
                        field = "w_resolved_at";
                        break;
                    case "trend":
                        field = "w_ingested_at";
                        nestedAggregation.put("trend_count", Aggregation.of(a -> a
                                .cardinality(CardinalityAggregation.of(c -> c
                                        .field("w_workitem_integ_id")
                                        .precisionThreshold(40000)))));
                        break;
                }
                String finalField = field;
                switch (interval.toLowerCase()) {
                    case "year":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field(finalField)
                                        .calendarInterval(CalendarInterval.Year)
                                        .timeZone("UTC")
                                        .format("yyyy")
                                        .minDocCount(1)
                                        .order(x -> x.key(esSortOrder))
                                )
                                .aggregations(nestedAggregation)));
                        break;
                    case "quarter":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field(finalField)
                                        .calendarInterval(CalendarInterval.Quarter)
                                        .timeZone("UTC")
                                        .format("q-yyyy")
                                        .minDocCount(1)
                                        .order(x -> x.key(esSortOrder))
                                )
                                .aggregations(nestedAggregation)));
                        break;
                    case "biweekly":
                        // Need to implement
                        break;
                    case "week":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field(finalField)
                                        .calendarInterval(CalendarInterval.Week)
                                        .timeZone("UTC")
                                        .format("ww-yyyy")
                                        .minDocCount(1)
                                        .order(x -> x.key(esSortOrder))
                                )
                                .aggregations(nestedAggregation)
                        ));
                        break;
                    case "month":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field(finalField)
                                        .calendarInterval(CalendarInterval.Month)
                                        .timeZone("UTC")
                                        .format("MM-yyyy")
                                        .minDocCount(1)
                                        .order(x -> x.key(esSortOrder))
                                )
                                .aggregations(nestedAggregation)
                        ));
                        break;
                    case "day":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field(finalField)
                                        .calendarInterval(CalendarInterval.Day)
                                        .timeZone("UTC")
                                        .format("dd-MM-yyyy")
                                        .minDocCount(1)
                                        .order(x -> x.key(esSortOrder))
                                )
                                .aggregations(nestedAggregation)
                        ));
                        break;
                    case "day_of_week":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                                .terms(t -> {
                                            TermsAggregation.Builder builder = t
                                                    .script(s -> s
                                                            .inline(i -> i
                                                                    .source("doc['w_created_at'].value.dayOfWeekEnum" +
                                                                            ".getDisplayName(TextStyle.SHORT, Locale.ROOT)")

                                                            )
                                                    )
                                                    .minDocCount(1)
                                                    .size(Integer.MAX_VALUE);
                                            if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                                return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                            else
                                                return builder;
                                        }
                                )
                                .aggregations(nestedAggregation)
                        ));
                        break;
                    default:
                        break;
                }
                break;
            case ticket_category:
            case resolution:
            case workitem_type:
            case status_category:
            case parent_workitem_id:
            case project:
            case status:
            case priority:
            case epic:
            case story_points:
            default:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name())
                                            .minDocCount(1)
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
        }
        return aggConditions;
    }

    private static Map<String, Aggregation> getStagesAggs(WorkItemsFilter.DISTINCT across,
                                                          WorkItemsFilter workItemsFilter,
                                                          List<DbWorkItemField> workItemCustomFields,
                                                          Map<String, Aggregation> nestedAggregation, Boolean isStack,
                                                          WorkItemsFilter.DISTINCT stack,
                                                          Map<String, SortingOrder> sortBy,
                                                          SortOrder esSortOrder, WorkItemsFilter.CALCULATION calculation,
                                                          Map<String, Aggregation> aggConditions, String finalKeyName) {
        switch (across) {
            case none:
                Aggregation updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                Aggregation stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .terms(t -> t.field("w_integ_type"))
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null && stack != null) {
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
                            }
                            return aggregations;
                        }
                ));
                break;
            case label:
            case component:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .terms(t -> t.field("w_" + across.name() + "s"))
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null && stack != null) {
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
                            }
                            return aggregations;
                        }

                ));
                break;
            case version:
            case fix_version:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .terms(t -> t.field("w_" + across.name() + "s.name"))
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null && stack != null) {
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
                            }
                            return aggregations;
                        }

                ));
                break;
            case custom_field:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                String customColumn = (isStack ? workItemsFilter.getCustomStack() : workItemsFilter.getCustomAcross());
                String customFieldType = EsUtils.getCustomFieldType(workItemCustomFields, null, customColumn);
                String esCustomColumn = "";
                esCustomColumn = EsUtils.getCustomFieldColumn(customFieldType, esCustomColumn);
                String finalEsCustomColumn = esCustomColumn;
                if (customColumn != null && finalEsCustomColumn != null) {
                    aggConditions.put("across_custom_fields", Aggregation.of(a -> {
                                Aggregation.Builder.ContainerBuilder aggregations = a
                                        .nested(n -> n
                                                .path("w_custom_fields")
                                        )
                                        .aggregations(Map.of("filter_custom_fields_name", Aggregation.of(a1 -> a1
                                                .filter(f -> f
                                                        .term(t -> t
                                                                .field("w_custom_fields.name")
                                                                .value(customColumn)
                                                        )
                                                )
                                                .aggregations(Map.of("across_custom_fields_type", Aggregation.of(a2 -> a2
                                                        .terms(t -> t.field("w_custom_fields." + finalEsCustomColumn))
                                                        .aggregations("across_nested", Aggregation.of(
                                                                z -> z.reverseNested(n -> n
                                                                                .path("w_hist_statuses"))
                                                                        .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                                agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                                        .aggregations(nestedAggregation)))
                                                        .aggregations("bucket_pagination", updatedBucketAggs)
                                                )))
                                        )));
                                if (stackAggs != null && stack != null) {
                                    return aggregations.aggregations("across_" + stack.name(), stackAggs);
                                }
                                return aggregations;
                            }
                    ));
                }
                break;
            case attribute:
                String attributeColumn;
                if (isStack) {
                    if (workItemsFilter.getAttributeStack() == null) {
                        throw new IllegalArgumentException("In workItem filter attributeStack is required!");
                    }
                    attributeColumn = workItemsFilter.getAttributeStack();
                } else {
                    if (workItemsFilter.getAttributeAcross() == null) {
                        throw new IllegalArgumentException("In workItem filter attributeAcross is required!");
                    }
                    attributeColumn = workItemsFilter.getAttributeAcross();
                }
                String finalAttributeColumn = attributeColumn;
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                    Aggregation.Builder.ContainerBuilder aggregations1 = a
                            .nested(n -> n
                                    .path("w_attributes")
                            )
                            .aggregations(Map.of("filter_attributes_name", Aggregation.of(a1 -> a1
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("w_attributes.name")
                                                    .value(finalAttributeColumn)
                                            )
                                    )
                                    .aggregations(Map.of("across_attributes_" + finalAttributeColumn,
                                            Aggregation.of(a2 -> a2
                                                    .terms(t -> t.field("w_attributes." + ATTRIBUTES_FIELD_TYPE.get(finalAttributeColumn)))
                                                    .aggregations("across_nested", Aggregation.of(
                                                            z -> z.reverseNested(n -> n
                                                                            .path("w_hist_statuses"))
                                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                            agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations("bucket_pagination", updatedBucketAggs)
                                            )))
                            )));
                    if (stackAggs != null && stack != null) {
                        return aggregations1.aggregations("across_" + stack.name(), stackAggs);
                    }
                    return aggregations1;
                }));
                break;
            case sprint_mapping:
                List<MultiTermLookup> sprintMappingFields = new ArrayList<>();
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_integration_id")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_sprint_mappings.id")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_milestones.name")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_milestones.start_time")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_milestones.completed_at")
                ));
                sprintMappingFields.add(MultiTermLookup.of(mt -> mt
                        .field("w_hist_statuses.status"))
                );

                aggConditions.put("across_sprint_mapping", Aggregation.of(a -> a
                        .multiTerms(mt -> mt
                                .terms(sprintMappingFields)
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case sprint:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_sprint", Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .nested(nest -> nest.path("w_milestones"))
                                    .aggregations("across_nested_sprint",
                                            Aggregation.of(s -> s.terms(term -> term.field("w_milestones.full_name").size(Integer.MAX_VALUE))
                                                    .aggregations("across_nested", Aggregation.of(
                                                            z -> z.nested(n -> n
                                                                            .path("w_hist_statuses"))
                                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                            agg.terms(term -> term.field("w_hist_statuses.status"))
                                                                                    .aggregations(nestedAggregation)
                                                                    ))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations("bucket_pagination", updatedBucketAggs)));
                            if (stackAggs != null && stack != null) {
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
                            }
                            return aggregations;
                        }
                ));
                break;
            case reporter:
            case assignee:
                List<MultiTermLookup> reporterOrAssignee = new ArrayList<>();
                reporterOrAssignee.add(MultiTermLookup.of(mt -> mt
                        .field("w_" + across.name() + ".id")
                ));
                reporterOrAssignee.add(MultiTermLookup.of(mt -> mt
                        .field("w_" + across.name() + ".display_name")
                ));
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .multiTerms(mt -> mt
                                            .terms(reporterOrAssignee)
                                    )
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null) {
                                return aggregations
                                        .aggregations("across_" + stack.name(), stackAggs);
                            }
                            return aggregations;
                        }
                ));
                break;
            case stage:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .nested(n -> n
                                .path("w_hist_statuses")
                        )
                        .aggregations(Map.of("across_historical_status",
                                Aggregation.of(a2 -> a2
                                        .terms(t -> {
                                                    TermsAggregation.Builder builder = t
                                                            .field("w_hist_statuses.status")
                                                            .minDocCount(1)
                                                            .size(Integer.MAX_VALUE);
                                                    if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                                    else
                                                        return builder;
                                                }
                                        )
                                        .aggregations(nestedAggregation)
                                )))

                ));
                break;
            case trend:
            case workitem_created_at:
            case workitem_updated_at:
            case workitem_resolved_at:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                String interval = workItemsFilter.getAggInterval() != null ?
                        workItemsFilter.getAggInterval() : CalendarInterval.Day.name();
                String field = "";
                switch (across.name()) {
                    case "workitem_created_at":
                        field = "w_created_at";
                        break;
                    case "workitem_updated_at":
                        field = "w_updated_at";
                        break;
                    case "workitem_resolved_at":
                        field = "w_resolved_at";
                        break;
                    case "trend":
                        field = "w_ingested_at";
                        nestedAggregation.put("trend_count", Aggregation.of(a -> a
                                .cardinality(CardinalityAggregation.of(c -> c
                                        .field("w_workitem_integ_id")
                                        .precisionThreshold(40000)))));
                        break;
                }
                String finalField = field;
                switch (interval.toLowerCase()) {
                    case "year":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .dateHistogram(dh -> dh
                                            .field(finalField)
                                            .calendarInterval(CalendarInterval.Year)
                                            .timeZone("UTC")
                                            .format("yyyy")
                                            .minDocCount(1)
                                            .order(x -> x.key(esSortOrder))
                                    )
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null && stack != null)
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
                            return aggregations;
                        }));

                        break;
                    case "quarter":

                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                                    Aggregation.Builder.ContainerBuilder aggregations = a
                                            .dateHistogram(dh -> dh
                                                    .field(finalField)
                                                    .calendarInterval(CalendarInterval.Quarter)
                                                    .timeZone("UTC")
                                                    .format("q-yyyy")
                                                    .minDocCount(1)
                                                    .order(x -> x.key(esSortOrder))
                                            )
                                            .aggregations("across_nested", Aggregation.of(
                                                    z -> z.nested(n -> n
                                                                    .path("w_hist_statuses"))
                                                            .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                    agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                            .aggregations(nestedAggregation)))
                                            .aggregations("bucket_pagination", updatedBucketAggs);
                                    if (stackAggs != null && stack != null)
                                        return aggregations.aggregations("across_" + stack.name(), stackAggs);
                                    return aggregations;
                                }
                        ));

                        break;
                    case "biweekly":
                        // Need to implement
                        break;
                    case "week":

                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                                    Aggregation.Builder.ContainerBuilder aggregations = a
                                            .dateHistogram(dh -> dh
                                                    .field(finalField)
                                                    .calendarInterval(CalendarInterval.Week)
                                                    .timeZone("UTC")
                                                    .format("ww-yyyy")
                                                    .minDocCount(1)
                                                    .order(x -> x.key(esSortOrder))
                                            )
                                            .aggregations("across_nested", Aggregation.of(
                                                    z -> z.nested(n -> n
                                                                    .path("w_hist_statuses"))
                                                            .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                    agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                            .aggregations(nestedAggregation)))
                                            .aggregations("bucket_pagination", updatedBucketAggs);
                                    if (stackAggs != null && stack != null)
                                        return aggregations.aggregations("across_" + stack.name(), stackAggs);
                                    return aggregations;
                                }
                        ));

                        break;
                    case "month":

                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                                    Aggregation.Builder.ContainerBuilder aggregations = a
                                            .dateHistogram(dh -> dh
                                                    .field(finalField)
                                                    .calendarInterval(CalendarInterval.Month)
                                                    .timeZone("UTC")
                                                    .format("MM-yyyy")
                                                    .minDocCount(1)
                                                    .order(x -> x.key(esSortOrder))
                                            )
                                            .aggregations("across_nested", Aggregation.of(
                                                    z -> z.nested(n -> n
                                                                    .path("w_hist_statuses"))
                                                            .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                    agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                            .aggregations(nestedAggregation)))
                                            .aggregations("bucket_pagination_1", updatedBucketAggs);
                                    if (stackAggs != null && stack != null)
                                        return aggregations.aggregations("across_" + stack.name(), stackAggs);
                                    return aggregations;
                                }
                        ));

                        break;
                    case "day":

                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                                    Aggregation.Builder.ContainerBuilder aggregations = a
                                            .dateHistogram(dh -> dh
                                                    .field(finalField)
                                                    .calendarInterval(CalendarInterval.Day)
                                                    .timeZone("UTC")
                                                    .format("dd-MM-yyyy")
                                                    .minDocCount(1)
                                                    .order(x -> x.key(esSortOrder))
                                            )
                                            .aggregations("across_nested", Aggregation.of(
                                                    z -> z.nested(n -> n
                                                                    .path("w_hist_statuses"))
                                                            .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                                    agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                            .aggregations(nestedAggregation)))
                                            .aggregations("bucket_pagination", updatedBucketAggs);
                                    if (stackAggs != null && stack != null)
                                        return aggregations.aggregations("across_" + stack.name(), stackAggs);
                                    return aggregations;
                                }
                        ));

                        break;
                    case "day_of_week":
                        aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .terms(t -> {
                                                TermsAggregation.Builder builder = t
                                                        .script(s -> s
                                                                .inline(i -> i
                                                                        .source("doc['w_created_at'].value.dayOfWeekEnum" +
                                                                                ".getDisplayName(TextStyle.SHORT, Locale.ROOT)")

                                                                )
                                                        )
                                                        .minDocCount(1)
                                                        .size(Integer.MAX_VALUE);
                                                if (MapUtils.isNotEmpty(sortBy) || calculation == WorkItemsFilter.CALCULATION.issue_count)
                                                    return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                                else
                                                    return builder;
                                            }
                                    )
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null && stack != null)
                                return aggregations.aggregations("across_" + stack.name(), stackAggs)
                                        .aggregations(nestedAggregation);
                            return aggregations;
                        }));
                        break;
                    default:
                        break;
                }
                break;
            case assignees:
                break;
            case ticket_category:
            case resolution:
            case workitem_type:
            case status_category:
            case parent_workitem_id:
            case project:
            case status:
            case priority:
            case epic:
            case story_points:
            default:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : WorkItemsFilter.DISTINCT.none.name(), isStack);
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .terms(t -> t.field("w_" + across.name()))
                                    .aggregations("across_nested", Aggregation.of(
                                            z -> z.nested(n -> n
                                                            .path("w_hist_statuses"))
                                                    .aggregations(across.name() + "_nested_across", Aggregation.of(agg ->
                                                            agg.terms(term -> term.field("w_hist_statuses.status"))
                                                                    .aggregations(nestedAggregation)))
                                                    .aggregations(nestedAggregation)))
                                    .aggregations("bucket_pagination", updatedBucketAggs);
                            if (stackAggs != null && stack != null) {
                                return aggregations.aggregations("across_" + stack.name(), stackAggs)
                                        .aggregations(nestedAggregation);
                            }
                            return aggregations;
                        }

                ));
                break;
        }
        return aggConditions;
    }

    private static Aggregation getUpdatedNestedAggs(Map<String, Aggregation> nestedAggregation, String stack, Boolean isStack) {
        Aggregation aggregation = null;
        if ((StringUtils.isNotEmpty(stack) || !stack.equalsIgnoreCase(WorkItemsFilter.DISTINCT.none.name())) && !isStack) {
            String key = "across_" + (stack.equalsIgnoreCase("custom_field") ? stack + "s" : stack);
            if (nestedAggregation.containsKey(key)) {
                aggregation = nestedAggregation.get(key);
                nestedAggregation.remove(key);
            }
        }
        return aggregation;
    }

    @Nullable
    private static Aggregation getUpdatedBucketAggs(Map<String, Aggregation> nestedAggregation) {
        Aggregation bucket_pagination = null;
        if (nestedAggregation.containsKey("bucket_pagination")) {
            bucket_pagination = nestedAggregation.get("bucket_pagination");
            nestedAggregation.remove("bucket_pagination");
        }
        return bucket_pagination;
    }

    private static void createAttributesConditions(Map<String, List<String>> attributes,
                                                   List<Query> queryCondition) {
        for (var es : attributes.entrySet()) {
            String key = es.getKey();
            List<String> values = ListUtils.emptyIfNull(attributes.get(key));
            if (CollectionUtils.isNotEmpty(values)) {
                queryCondition.add(EsUtils.getNestedQuery("w_attributes",
                        "w_attributes.name", key, "w_attributes." + ATTRIBUTES_FIELD_TYPE.get(key), values));
            }
        }
    }

    private static void createCustomFieldConditions(Map<String, Object> customFields,
                                                    List<DbWorkItemField> workItemCustomFields,
                                                    List<Query> queryConditions) {
        for (var customFieldEntry : customFields.entrySet()) {
            String key = customFieldEntry.getKey();
            String customFieldType = EsUtils.getCustomFieldType(workItemCustomFields, null, key);

            List<String> values = new ArrayList<>();
            if (CUSTOM_FIELD_TYPES.contains(customFieldType)) {
                values = (List) customFields.get(key);
            }
            if (CollectionUtils.isNotEmpty(values)) {
                switch (customFieldType.toLowerCase()) {
                    case "integer":
                        queryConditions.add(EsUtils.getNestedQuery("w_custom_fields",
                                "w_custom_fields.name", key, "w_custom_fields.int", values));
                        break;
                    case "long":
                        queryConditions.add(EsUtils.getNestedQuery("w_custom_fields",
                                "w_custom_fields.name", key, "w_custom_fields.long", values));
                        break;
                    case "string":
                        queryConditions.add(EsUtils.getNestedQuery("w_custom_fields",
                                "w_custom_fields.name", key, "w_custom_fields.str", values));
                        break;
                    case "datetime":
                        Map<String, String> timeRange = (Map) customFields.get(key);
                        final Long rangeStart = timeRange.get("$gt") != null ? Long.valueOf(timeRange.get("$gt")) : null;
                        final Long rangeEnd = timeRange.get("$lt") != null ? Long.valueOf(timeRange.get("$lt")) : null;
                        JsonData gt = null;
                        JsonData lt = null;

                        if (rangeStart != null) {
                            gt = JsonData.of(rangeStart);
                        }
                        if (rangeEnd != null) {
                            lt = JsonData.of(rangeEnd);
                        }
                        JsonData finalGt = gt;
                        JsonData finalLt = lt;
                        queryConditions.add(NestedQuery.of(nq -> nq
                                .path("w_custom_fields")
                                .query(q -> q
                                        .bool(b -> b
                                                .must(List.of(EsUtils.getQuery("w_custom_fields.name", List.of(key)),
                                                        EsUtils.getRangeQuery("w_custom_fields.date", finalGt, finalLt)))
                                        )
                                )
                        )._toQuery());
                        break;
                    case "boolean":
                        queryConditions.add(EsUtils.getNestedQuery("w_custom_fields",
                                "w_custom_fields.name", key, "w_custom_fields.bool", values));
                        break;
                    case "float":
                        queryConditions.add(EsUtils.getNestedQuery("w_custom_fields",
                                "w_custom_fields.name", key, "w_custom_fields.float", values));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static void createMissingFieldsClause(
            Map<WorkItemsFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields,
            Map<String, Boolean> missingCustomFields, Map<String, Boolean> missingAttributeFields,
            List<Query> includeQueryConditions, List<Query> excludeQueryConditions) {
        if (MapUtils.isNotEmpty(missingBuiltinFields)) {
            missingBuiltinFields.forEach((key, value) -> {
                final boolean shouldBeMissing = Boolean.TRUE.equals(value);
                switch (key) {
                    case priority:
                        includeQueryConditions.add(EsUtils.getQueryForMissingField(shouldBeMissing,
                                "w_priority", "_UNPRIORITIZED_"));
                        break;
                    case status:
                        includeQueryConditions.add(EsUtils.getQueryForMissingField(shouldBeMissing,
                                "w_status", "_UNKNOWN_"));
                        break;
                    case resolution:
                        includeQueryConditions.add(EsUtils.getQueryForMissingField(shouldBeMissing,
                                "w_resolution", "_UNKNOWN_"));
                        break;
                    case assignee:
                        includeQueryConditions.add(EsUtils.getQueryForMissingField(shouldBeMissing,
                                "w_assignee", "_UNASSIGNED_"));
                        break;
                    case reporter:
                        includeQueryConditions.add(EsUtils.getQueryForMissingField(shouldBeMissing,
                                "w_reporter", "_UNKNOWN_"));
                        break;
                    case component:
                        EsUtils.createExistsQueryForMissingField("w_components", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case label:
                        EsUtils.createExistsQueryForMissingField("w_labels", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case fix_version:
                        EsUtils.createExistsQueryForMissingField("w_fix_versions", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case version:
                        EsUtils.createExistsQueryForMissingField("w_versions", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case epic:
                        EsUtils.createExistsQueryForMissingField("w_epic", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case project:
                        EsUtils.createExistsQueryForMissingField("w_project", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case first_assignee:
                        EsUtils.createExistsQueryForMissingField("w_first_assignee", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case parent_workitem_id:
                        EsUtils.createExistsQueryForMissingField("w_parent_workitem_id", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case status_category:
                        EsUtils.createExistsQueryForMissingField("w_status_category", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case workitem_resolved_at:
                        EsUtils.createExistsQueryForMissingField("w_resolved_at", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case workitem_due_at:
                        EsUtils.createExistsQueryForMissingField("w_due_at", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case first_attachment_at:
                        EsUtils.createExistsQueryForMissingField("w_first_attachment_at", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case first_comment_at:
                        EsUtils.createExistsQueryForMissingField("w_first_comment_at", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                    case story_points:
                        EsUtils.createExistsQueryForMissingField("w_story_points", shouldBeMissing,
                                excludeQueryConditions, includeQueryConditions);
                        break;
                }
            });
        }
        createMissingFieldConditions(missingCustomFields, includeQueryConditions,
                excludeQueryConditions);
        createMissingFieldForAttributesConditions(missingAttributeFields, includeQueryConditions,
                excludeQueryConditions);
    }

    private static void createMissingFieldForAttributesConditions(Map<String, Boolean> missingAttributeFields,
                                                                  List<Query> includeQueryConditions,
                                                                  List<Query> excludeQueryConditions) {
        if (MapUtils.isNotEmpty(missingAttributeFields)) {
            missingAttributeFields.forEach((field, shouldBeMissing) -> {
                if (shouldBeMissing) {
                    excludeQueryConditions.add(ExistsQuery.of(e -> e
                            .field("w_attributes." + ATTRIBUTES_FIELD_TYPE.get(field))
                    )._toQuery());
                } else {
                    includeQueryConditions.add(ExistsQuery.of(e -> e
                            .field("w_attributes." + ATTRIBUTES_FIELD_TYPE.get(field))
                    )._toQuery());
                }
            });
        }
    }

    private static void createMissingFieldConditions(Map<String, Boolean> missingFields,
                                                     List<Query> includeQueryConditions,
                                                     List<Query> excludeQueryConditions) {
        if (MapUtils.isNotEmpty(missingFields)) {
            List<String> emptyFields = new ArrayList<>();
            List<String> nonEmptyFields = new ArrayList<>();
            missingFields.forEach((field, shouldBeMissing) -> {
                if (shouldBeMissing) {
                    emptyFields.add(field);
                } else {
                    nonEmptyFields.add(field);
                }
            });
            if (CollectionUtils.isNotEmpty(emptyFields)) {
                excludeQueryConditions.add(EsUtils.getQuery("w_custom_fields.name", emptyFields));
            }
            if (CollectionUtils.isNotEmpty(nonEmptyFields)) {
                includeQueryConditions.add(EsUtils.getQuery("w_custom_fields.name", nonEmptyFields));
            }
        }
    }

    private static void createPartialMatchFilter(List<Query> queryCondition,
                                                 Map<String, Map<String, String>> partialMatchMap,
                                                 Map<String, String> partialMatchColumns,
                                                 Map<String, String> partialMatchArrayColumns,
                                                 Map<String, String> partialMatchArrayAttributeColumns,
                                                 Map<String, String> partialMatchAttributeColumns,
                                                 Boolean isMilestone) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (StringUtils.firstNonEmpty(begins, ends, contains) != null) {
                if (IssueMgmtCustomFieldUtils.isCustomField(key)) {
                    key = "w_custom_fields.name";
                    createPartialMatchCondition(queryCondition, key, begins, ends, contains, isMilestone);
                } else if (partialMatchArrayColumns.containsKey(key)) {
                    key = partialMatchArrayColumns.get(key);
                    createPartialMatchCondition(queryCondition, key, begins, ends, contains, isMilestone);
                } else if (partialMatchColumns.containsKey(key)) {
                    key = partialMatchColumns.get(key);
                    createPartialMatchCondition(queryCondition, key, begins, ends, contains, isMilestone);
                } else if (partialMatchAttributeColumns.containsKey(key)) {
                    key = (isMilestone ? "" : "w_") + "attributes." + ATTRIBUTES_FIELD_TYPE.get(partialMatchAttributeColumns.get(key));
                    createPartialMatchConditionForAttributes(queryCondition, key, begins, ends, contains, isMilestone);
                } else if (partialMatchArrayAttributeColumns.containsKey(key)) {
                    key = (isMilestone ? "" : "w_") + "attributes." + ATTRIBUTES_FIELD_TYPE.get(partialMatchAttributeColumns.get(key));
                    createPartialMatchConditionForAttributes(queryCondition, key, begins, ends, contains, isMilestone);
                }
            }
        }
    }

    private static void createPartialMatchConditionForAttributes(List<Query> queryCondition,
                                                                 String keyName, String begins,
                                                                 String ends, String contains, Boolean isMilestone) {
        String field = keyName;
        if (isMilestone) {
            field = "w_milestones." + field;
        }
        String finalKey = field;
        queryCondition.add(EsUtils.getQuery("w_attributes.name", List.of(keyName)));
        if (begins != null) {
            if (isMilestone) {
                queryCondition.add(getNestedWildcardQuery("w_milestones", finalKey, begins + "*"));
            } else {
                queryCondition.add(WildcardQuery.of(q -> q
                        .field(finalKey)
                        .value(begins + "*"))._toQuery());
            }
        }
        if (ends != null) {
            if (isMilestone) {
                queryCondition.add(getNestedWildcardQuery("w_milestones", finalKey, "*" + ends));
            } else {
                queryCondition.add(WildcardQuery.of(q -> q
                        .field(finalKey)
                        .value("*" + ends))._toQuery());
            }
        }
        if (contains != null) {
            if (isMilestone) {
                queryCondition.add(getNestedWildcardQuery("w_milestones", finalKey, "*" + contains + "*"));
            } else {
                queryCondition.add(WildcardQuery.of(q -> q
                        .field(finalKey)
                        .value("*" + contains + "*"))._toQuery());
            }
        }
    }

    public static Query getNestedWildcardQuery(String path, String fieldName, String value) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(WildcardQuery.of(wq -> wq
                                        .field(fieldName)
                                        .value(value))._toQuery()))
                        )
                )
        )._toQuery();
    }

    private static void createPartialMatchCondition(List<Query> queryCondition,
                                                    String keyName, String begins,
                                                    String ends, String contains, Boolean isMilestone) {
        switch (keyName) {
            case "assignee":
                keyName = "assignee.display_name";
                break;
            case "reporter":
                keyName = "reporter.display_name";
                break;
            case "parent_field_value":
                keyName = "parent_name";
                break;
            case "sprint_name":
                keyName = "name";
                break;
            case "milestone_full_name":
                keyName = "full_name";
                break;
        }
        if (isMilestone) {
            keyName = "w_milestones." + keyName;
        } else {
            keyName = "w_" + keyName;
        }
        String key = keyName;
        if (begins != null) {
            if (isMilestone) {
                queryCondition.add(getNestedWildcardQuery("w_milestones", key, begins + "*"));
            } else {
                queryCondition.add(WildcardQuery.of(q -> q
                        .field(key)
                        .value(begins + "*"))._toQuery());
            }
        }
        if (ends != null) {
            if (isMilestone) {
                queryCondition.add(getNestedWildcardQuery("w_milestones", key, "*" + ends));
            } else {
                queryCondition.add(WildcardQuery.of(q -> q
                        .field(key)
                        .value("*" + ends))._toQuery());
            }
        }
        if (contains != null) {
            if (isMilestone) {
                queryCondition.add(getNestedWildcardQuery("w_milestones", key, "*" + contains + "*"));
            } else {
                queryCondition.add(WildcardQuery.of(q -> q
                        .field(key)
                        .value("*" + contains + "*"))._toQuery());
            }
        }
    }

    private static void createHygieneCondition(WorkItemsFilter filter, List<Query> includeQueryCondition,
                                               List<Query> excludeQueryCondition) {
        long currentTime = Instant.now().getEpochSecond();
        Map<WorkItemsFilter.EXTRA_CRITERIA, Object> hygieneSpecs = filter.getHygieneCriteriaSpecs();
        for (WorkItemsFilter.EXTRA_CRITERIA hygieneType : filter.getExtraCriteria()) {
            switch (hygieneType) {
                case idle:
                    long idletime = (currentTime - NumberUtils.toInt(
                            String.valueOf(hygieneSpecs.get(WorkItemsFilter.EXTRA_CRITERIA.idle)),
                            30) * 86400L);
                    JsonData lt = JsonData.of(idletime);
                    includeQueryCondition.add(RangeQuery.of(q -> q
                            .timeZone("UTC")
                            .format("epoch_second")
                            .field("w_updated_at")
                            .lt(lt))._toQuery());
                    break;
                case no_assignee:
                    includeQueryCondition.add(EsUtils.getQuery("w_assignee.display_name", List.of(DbWorkItem.UNASSIGNED)));
                    break;
                case no_due_date:
                    excludeQueryCondition.add(ExistsQuery.of(t -> t
                            .field("w_due_at")
                    )._toQuery());
                    break;
                case poor_description:
                    includeQueryCondition.add(RangeQuery.of(q -> q
                            .field("w_desc_size")
                            .lt(JsonData.of(NumberUtils.toInt(String.valueOf(
                                    hygieneSpecs.get(WorkItemsFilter.EXTRA_CRITERIA.poor_description)), 10))))._toQuery());
                    break;
                case no_components:
                    excludeQueryCondition.add(ExistsQuery.of(e -> e
                            .field("w_components")
                    )._toQuery());
                    break;
                case missed_response_time:
                    includeQueryCondition.add(ScriptQuery.of(sq -> sq
                            .script(s -> s
                                    .inline(i -> i
                                            .source("long response_sla = 0L;\n" +
                                                    "if (doc['w_priorities_sla.response_time'].size() == 0) {\n" +
                                                    "      response_sla = 86400L;\n" +
                                                    "} else {\n" +
                                                    "       response_sla = doc['w_priorities_sla.response_time'].value;\n" +
                                                    " }\n" +
                                                    "return doc['w_response_time'].value > response_sla;")
                                    )
                            )
                    )._toQuery());
                    break;
                case missed_resolution_time:
                    includeQueryCondition.add(ScriptQuery.of(sq -> sq
                            .script(s -> s
                                    .inline(i -> i
                                            .source("long solve_sla = 0L;\n" +
                                                    "if (doc['w_priorities_sla.solve_time'].size() == 0) {\n" +
                                                    "      solve_sla = 86400L;\n" +
                                                    "} else {\n" +
                                                    "       solve_sla = doc['w_priorities_sla.solve_time'].value;\n" +
                                                    " }\n" +
                                                    "return doc['w_solve_time'].value > solve_sla;")
                                    )
                            )
                    )._toQuery());
                    break;
            }
        }
    }

    private static void addSortingFields(Map<String, Aggregation> innerAggs, WorkItemsFilter.CALCULATION calculation, SortOrder esSortOrder,
                                         Boolean valuesOnly, Boolean isStack, Integer page, Integer acrossLimit) {
        if (calculation == null) {
            calculation = WorkItemsFilter.CALCULATION.issue_count;
        }
        switch (calculation) {
            case age:
            case response_time:
            case resolution_time:
            case assign_to_resolve:
            case hops:
            case bounces:
                addBucketPagination(innerAggs, calculation.name() + "_percentiles.50", esSortOrder, valuesOnly, isStack, page, acrossLimit);
                break;
            case sprint_mapping:
            case sprint_mapping_count:
            case issue_count:
            case stage_times_report:
                addBucketPagination(innerAggs, StringUtils.EMPTY, esSortOrder, valuesOnly, isStack, page, acrossLimit);
                break;
            case story_point_report:
                addBucketPagination(innerAggs, "total_story_points.sum", esSortOrder, valuesOnly, isStack, page, acrossLimit);
                break;
            case effort_report:
                addBucketPagination(innerAggs, "total_effort.sum", esSortOrder, valuesOnly, isStack, page, acrossLimit);
                break;
            case stage_bounce_report:
            case assignees:
                addBucketPagination(innerAggs, calculation.name() + ".count", esSortOrder, valuesOnly, isStack, page, acrossLimit);
                break;
        }
    }

    private static void addBucketPagination(Map<String, Aggregation> nestedAggregation, String calculationNameWithSortField,
                                            SortOrder sortOrder, Boolean valuesOnly, Boolean isStack, Integer page, Integer acrossLimit) {
        if (page != null) {
            nestedAggregation.put("bucket_pagination", BucketSortAggregation.of(a -> {
                BucketSortAggregation.Builder builder = a
                        .from(valuesOnly ? 0 : page * acrossLimit)
                        .size(valuesOnly ? Integer.MAX_VALUE : acrossLimit);
                if (StringUtils.isNotEmpty(calculationNameWithSortField))
                    builder.sort(sorting -> sorting.field(z -> z.field(calculationNameWithSortField).order(sortOrder)));
                return builder;
            })._toAggregation());
        } else {
            nestedAggregation.put("bucket_pagination", BucketSortAggregation.of(a -> {
                BucketSortAggregation.Builder builder = a
                        .from(0)
                        .size(isStack || valuesOnly ? Integer.MAX_VALUE : acrossLimit);
                if (StringUtils.isNotEmpty(calculationNameWithSortField))
                    builder.sort(sorting -> sorting.field(z -> z.field(calculationNameWithSortField).order(sortOrder)));
                return builder;
            })._toAggregation());
        }
    }

}
