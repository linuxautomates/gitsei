package io.levelops.faceted_search.querybuilders.workitems;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.BucketSortAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoostingQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ScriptQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter.CALCULATION;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter.DISTINCT;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter.MISSING_BUILTIN_FIELD;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.faceted_search.db.utils.EsWorkItemCalculation;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.faceted_search.querybuilders.ESRequest;
import io.levelops.faceted_search.utils.EsUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.PARTIAL_MATCH_ARRAY_COLUMNS;
import static io.levelops.commons.databases.services.JiraIssueService.PARTIAL_MATCH_COLUMNS;
import static io.levelops.faceted_search.utils.ESAggResultUtils.getSortOrder;

@Log4j2
public class EsJiraQueryBuilder {
    private static final Set<String> CUSTOM_FIELD_TYPES = Set.of("integer", "long", "boolean", "number", "float", "array", "string", "option-with-child", "option");
    private static final Set<String> CUSTOM_FIELD_NON_LIST_OF_STRINGS_TYPES = Set.of("date", "dateTime", "datetime");

    private static final List<JiraIssuesFilter.DISTINCT> TIMESTAMP_SORTABLE_COLUMNS = List.of(JiraIssuesFilter.DISTINCT.trend,
            JiraIssuesFilter.DISTINCT.issue_created, JiraIssuesFilter.DISTINCT.issue_updated, JiraIssuesFilter.DISTINCT.issue_due,
            JiraIssuesFilter.DISTINCT.issue_due_relative, JiraIssuesFilter.DISTINCT.issue_resolved, JiraIssuesFilter.DISTINCT.issue_updated);

    private static final Boolean DO_NOT_SKIP_INTEGRATION_TYPE_FILTER = false;

    public static ESRequest buildESRequest(JiraIssuesFilter jiraIssuesFilter, List<String> developmentStages,
                                           List<String> historicalAssignees, ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories, List<DbJiraField> dbJiraFields,
                                           Boolean needMissingStoryPointsFilter,
                                           DISTINCT stack, Boolean valuesOnly,
                                           Integer page, Integer pageSize, Boolean isList){
        return buildESRequest(jiraIssuesFilter, developmentStages,
                historicalAssignees, histAssigneesAndHistStatusCategories, dbJiraFields,
                needMissingStoryPointsFilter,
                stack, valuesOnly,
                page, pageSize, isList, DO_NOT_SKIP_INTEGRATION_TYPE_FILTER);
    }

    public static ESRequest buildESRequest(JiraIssuesFilter jiraIssuesFilter, List<String> developmentStages,
                                           List<String> historicalAssignees, ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories, List<DbJiraField> dbJiraFields,
                                           Boolean needMissingStoryPointsFilter,
                                           DISTINCT stack, Boolean valuesOnly,
                                           Integer page, Integer pageSize, Boolean isList, Boolean skipIntegrationTypeFilter) {
        JiraOrFilter orFilter = jiraIssuesFilter.getOrFilter() != null ? jiraIssuesFilter.getOrFilter() : JiraOrFilter.builder().build();
        List<Query> includesQueryConditions = new ArrayList<>();
        List<Query> excludesQueryConditions = new ArrayList<>();
        List<Query> includesOrQueryConditions = new ArrayList<>();

        if(!skipIntegrationTypeFilter) {
            includesQueryConditions.add(EsUtils.getQuery("w_integ_type", List.of("jira")));
        }
        createJiraFilterIncludesCondition(jiraIssuesFilter, developmentStages, historicalAssignees, histAssigneesAndHistStatusCategories, needMissingStoryPointsFilter, includesQueryConditions);
        createJiraFilterExcludesCondition(jiraIssuesFilter, excludesQueryConditions);
        createJiraFilterRangeCondition(jiraIssuesFilter, includesQueryConditions);

        //historicalAssignees is added in OrFilter. I believe this is not needed. For histAssigneesAndHistStatusCategories I am not adding support in OR Filter for now.
        createJiraOrFilterIncludesCondition(orFilter, developmentStages, historicalAssignees, needMissingStoryPointsFilter, includesOrQueryConditions);
        createJiraOrFilterRangeCondition(orFilter, includesOrQueryConditions);
        if (jiraIssuesFilter.getAge() != null) {
            includesQueryConditions.add(getRangeQuery(jiraIssuesFilter.getAge(), "w_age"));
        }
        if (MapUtils.isNotEmpty(jiraIssuesFilter.getPartialMatch())) {
            getPartialMatch(jiraIssuesFilter.getPartialMatch(), includesQueryConditions, dbJiraFields);
        }
        if (MapUtils.isNotEmpty(jiraIssuesFilter.getCustomFields())) {
            getCustomFieldQuery(jiraIssuesFilter.getCustomFields(), dbJiraFields, includesQueryConditions);
        }
        if (MapUtils.isNotEmpty(jiraIssuesFilter.getExcludeCustomFields())) {
            getCustomFieldQuery(jiraIssuesFilter.getExcludeCustomFields(), dbJiraFields, excludesQueryConditions);
        }
        if (MapUtils.isNotEmpty(jiraIssuesFilter.getMissingFields())) {
            Map<String, Boolean> missingCustomFields = new HashMap<>();
            Map<MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields = new EnumMap<>(
                    MISSING_BUILTIN_FIELD.class);
            jiraIssuesFilter.getMissingFields().forEach((field, shouldBeMissing) -> {
                if (DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(field).matches()) {
                    missingCustomFields.put(field, shouldBeMissing);
                } else {
                    Optional.ofNullable(MISSING_BUILTIN_FIELD.fromString(field))
                            .ifPresent(builtinField -> missingBuiltinFields.put(builtinField, shouldBeMissing));
                }
            });
            getMissingFieldsQuery(missingBuiltinFields, missingCustomFields, includesQueryConditions, excludesQueryConditions);
        }
        if (BooleanUtils.isNotFalse(jiraIssuesFilter.getIsActive())) {
            includesQueryConditions.add(TermQuery.of(q -> q
                    .field("w_is_active")
                    .value(BooleanUtils.isNotFalse(jiraIssuesFilter.getIsActive()))
            )._toQuery());
        }
        if (jiraIssuesFilter.getUnAssigned() != null && jiraIssuesFilter.getUnAssigned()) {
            excludesQueryConditions.add(ExistsQuery.of(q -> q
                            .field("w_assignee"))
                    ._toQuery());
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExtraCriteria())) {
            getExtraCriteriaQuery(jiraIssuesFilter, includesQueryConditions, excludesQueryConditions);
        }
        CALCULATION calculation = jiraIssuesFilter.getCalculation();
        if (calculation == null) {
            calculation = CALCULATION.ticket_count;
        }
        Map<String, Aggregation> aggReportsCondition = new HashMap<>();
        DISTINCT across = jiraIssuesFilter.getAcross();
        if (jiraIssuesFilter.getCalculation() == JiraIssuesFilter.CALCULATION.sprint_mapping) {
            across = DISTINCT.sprint_mapping;
            stack = null;
        } else {
            aggReportsCondition = new HashMap<>(EsWorkItemCalculation.getCalculation(EsWorkItemCalculation.CALCULATION.fromString(calculation.name()),
                    jiraIssuesFilter, page, pageSize));
        }

        boolean isStageReport = (jiraIssuesFilter.getCalculation() == JiraIssuesFilter.CALCULATION.stage_bounce_report
                || jiraIssuesFilter.getCalculation() == JiraIssuesFilter.CALCULATION.stage_times_report);

        Map<String, Aggregation> stackAggs = new HashMap<>();
        if (stack != null && !isList && stack != DISTINCT.none) {
            final String customStack = (stack == JiraIssuesFilter.DISTINCT.custom_field) ? IterableUtils.getFirst(jiraIssuesFilter.getCustomStacks())
                    .orElseThrow(() -> new RuntimeException("custom_stacks field must be present with custom_field as stack")) : null;
            stackAggs = getAcrossConditions(stack, jiraIssuesFilter.toBuilder().customAcross(customStack).sort(Map.of(stack.toString(), SortingOrder.ASC)).build(),
                    dbJiraFields, aggReportsCondition, null, null, false, isStageReport, true, null);
        }
        Map<String, Aggregation> aggReportsAndStacks = new HashMap<>();
        if (calculation != CALCULATION.sprint_mapping) {
            aggReportsAndStacks = new HashMap<>(EsWorkItemCalculation.getCalculation(EsWorkItemCalculation.CALCULATION.fromString(calculation.name()), null, page, pageSize));
        }
        aggReportsAndStacks.putAll(stackAggs);
        Map<String, Aggregation> aggConditions = Map.of();
        if (across != null && !isList) {
            aggConditions = getAcrossConditions(across, jiraIssuesFilter,
                    dbJiraFields, aggReportsAndStacks, page, pageSize, valuesOnly, isStageReport, false, stack);
        }

        List<Query> must = new ArrayList<>();
        must.addAll(includesQueryConditions);
        if (CollectionUtils.isNotEmpty(includesOrQueryConditions)) {
            must.add(Query.of(m -> m.bool(bool -> bool.should(includesOrQueryConditions))));
        }

        ESRequest.ESRequestBuilder bldr = ESRequest.builder();
        if (CollectionUtils.isNotEmpty(must)) {
            bldr.must(must);
        }
        if (CollectionUtils.isNotEmpty(excludesQueryConditions)) {
            bldr.mustNot(excludesQueryConditions);
        }

        return bldr
                .aggs(aggConditions)
                .build();
    }

    public static Builder buildSearchRequest(JiraIssuesFilter jiraIssuesFilter, List<String> developmentStages,
                                             List<String> historicalAssignees, ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories, List<DbJiraField> dbJiraFields,
                                             Boolean needMissingStoryPointsFilter,
                                             DISTINCT stack, String indexNameOrAlias, Boolean valuesOnly,
                                             Integer page, Integer pageSize, Boolean isList) {
        ESRequest esRequest = buildESRequest(jiraIssuesFilter, developmentStages, historicalAssignees, histAssigneesAndHistStatusCategories,
                dbJiraFields, needMissingStoryPointsFilter, stack, valuesOnly,
                page, pageSize, isList);

        Builder builder = new Builder();
        return builder
                .index(indexNameOrAlias)
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> {
                            b.must(esRequest.getMust());
                            if (CollectionUtils.isNotEmpty(esRequest.getMustNot())) {
                                b.mustNot(esRequest.getMustNot());
                            }
                            return b;
                        }
                        ))))
                .aggregations(esRequest.getAggs());
    }

    private static void createJiraFilterIncludesCondition(JiraIssuesFilter jiraIssuesFilter,
                                                          List<String> developmentStages,
                                                          List<String> historicalAssignees, ImmutablePair<List<String>, List<String>> histAssigneesAndHistStatusCategories,
                                                          Boolean needMissingStoryPointsFilter,
                                                          List<Query> includesQueryConditions) {
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getPriorities())) {
            includesQueryConditions.add(EsUtils.getQuery("w_priority", jiraIssuesFilter.getPriorities()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getProjects())) {
            includesQueryConditions.add(EsUtils.getQuery("w_project", jiraIssuesFilter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getKeys())) {
            includesQueryConditions.add(EsUtils.getQuery("w_workitem_id", jiraIssuesFilter.getKeys()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStatuses())) {
            includesQueryConditions.add(EsUtils.getQuery("w_status", jiraIssuesFilter.getStatuses()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getAssignees())) {
            includesQueryConditions.add(EsUtils.getQuery("w_assignee.id", jiraIssuesFilter.getAssignees()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getAssigneeDisplayNames())) {
            includesQueryConditions.add(EsUtils.getQuery("w_assignee.display_name", jiraIssuesFilter.getAssigneeDisplayNames()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getReporters())) {
            includesQueryConditions.add(EsUtils.getQuery("w_reporter.id", jiraIssuesFilter.getReporters()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getReporterDisplayNames())) {
            includesQueryConditions.add(EsUtils.getQuery("w_reporter.display_name", jiraIssuesFilter.getReporterDisplayNames()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getIssueTypes())) {
            includesQueryConditions.add(EsUtils.getQuery("w_workitem_type", jiraIssuesFilter.getIssueTypes()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getParentKeys())) {
            includesQueryConditions.add(EsUtils.getQuery("w_parent_key", jiraIssuesFilter.getParentKeys()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStatusCategories())) {
            includesQueryConditions.add(EsUtils.getQuery("w_status_category", jiraIssuesFilter.getStatusCategories()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getIntegrationIds())) {
            includesQueryConditions.add(EsUtils.getQuery("w_integration_id", jiraIssuesFilter.getIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getEpics())) {
            includesQueryConditions.add(EsUtils.getQuery("w_epic", jiraIssuesFilter.getEpics()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getResolutions())) {
            includesQueryConditions.add(EsUtils.getQuery("w_resolution", jiraIssuesFilter.getResolutions()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getFirstAssignees())) {
            includesQueryConditions.add(EsUtils.getQuery("w_first_assignee.id", jiraIssuesFilter.getFirstAssignees()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getFirstAssigneeDisplayNames())) {
            includesQueryConditions.add(EsUtils.getQuery("w_first_assignee.display_name", jiraIssuesFilter.getFirstAssigneeDisplayNames()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getComponents())) {
            includesQueryConditions.add(EsUtils.getQuery("w_components", jiraIssuesFilter.getComponents()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getLabels())) {
            includesQueryConditions.add(EsUtils.getQuery("w_labels", jiraIssuesFilter.getLabels()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getHistoricalAssignees())) {
            includesQueryConditions.add(EsUtils.getQuery("w_hist_assignees.assignee.display_name", jiraIssuesFilter.getHistoricalAssignees()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getSprintIds())) {
            includesQueryConditions.add(EsUtils.getQuery("w_sprints.id", jiraIssuesFilter.getSprintIds()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getSprintNames())) {
            includesQueryConditions.add(EsUtils.getQuery("w_sprints.name", jiraIssuesFilter.getSprintNames()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getSprintStates())) {
            includesQueryConditions.add(EsUtils.getQuery("w_sprints.state", jiraIssuesFilter.getSprintStates()));
        }
        if (jiraIssuesFilter.getSprintMappingSprintState() != null) {
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_sprints")
                    .query(q -> q
                            .term(t -> t
                                    .field("w_hist_sprints.state")
                                    .value(jiraIssuesFilter.getSprintMappingSprintState().toUpperCase()))))._toQuery());
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNames())) {
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_sprints")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermsQuery.of(q -> q
                                            .field("w_hist_sprints.name")
                                            .terms(TermsQueryField.of(termsField -> termsField
                                                    .value(jiraIssuesFilter.getSprintMappingSprintNames().stream()
                                                            .map(str -> new FieldValue.Builder()
                                                                    .stringValue(str)
                                                                    .build())
                                                            .collect(Collectors.toList()))))
                                    )._toQuery())))))._toQuery());
        }
        if(StringUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNameStartsWith())){
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_sprints")
                    .query(q -> q
                            .wildcard(t -> t
                                    .field("w_hist_sprints.name")
                                    .wildcard(jiraIssuesFilter.getSprintMappingSprintNameStartsWith()+"*"))))._toQuery());
        }
        if(StringUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNameEndsWith())){
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_sprints")
                    .query(q -> q
                            .wildcard(t -> t
                                    .field("w_hist_sprints.name")
                                    .wildcard("*"+jiraIssuesFilter.getSprintMappingSprintNameEndsWith()))))._toQuery());
        }
        if(StringUtils.isNotEmpty(jiraIssuesFilter.getSprintMappingSprintNameContains())){
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_sprints")
                    .query(q -> q
                            .wildcard(t -> t
                                    .field("w_hist_sprints.name")
                                    .wildcard("*"+jiraIssuesFilter.getSprintMappingSprintNameContains()+"*"))))._toQuery());
        }
        if (jiraIssuesFilter.getSprintMappingIgnorableIssueType() != null) {
            includesQueryConditions.add(TermQuery.of(t -> t
                    .field("w_sprint_mappings.ignorable_workitem_type")
                    .value(jiraIssuesFilter.getSprintMappingIgnorableIssueType()))._toQuery());
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getVersions())) {
            includesQueryConditions.add(EsUtils.getQuery("w_versions.name", jiraIssuesFilter.getVersions()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getFixVersions())) {
            includesQueryConditions.add(EsUtils.getQuery("w_fix_versions.name", jiraIssuesFilter.getFixVersions()));
        }

        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getLinks())) {

            List<Query> queryList = new ArrayList<>();
            queryList.add(TermsQuery.of(q -> q
                    .field("w_links.relation")
                    .terms(TermsQueryField.of(termsField -> termsField
                            .value(jiraIssuesFilter.getLinks().stream()
                                    .map(str -> new FieldValue.Builder()
                                            .stringValue(str)
                                            .build())
                                    .collect(Collectors.toList())))))._toQuery());

            if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getLinkedIssueKeys())) {
                queryList.add(TermsQuery.of(q -> q
                        .field("w_links.to_workitem_id")
                        .terms(TermsQueryField.of(termsField -> termsField
                                .value(jiraIssuesFilter.getLinkedIssueKeys().stream()
                                        .map(str -> new FieldValue.Builder()
                                                .stringValue(str)
                                                .build())
                                        .collect(Collectors.toList()))))
                )._toQuery());
            }

            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_links")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(queryList))))._toQuery());
        }

        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getStages())) {
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_statuses")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermsQuery.of(q -> q
                                            .field("w_hist_statuses.status")
                                            .terms(TermsQueryField.of(termsField -> termsField
                                                    .value(jiraIssuesFilter.getStages().stream()
                                                            .map(str -> new FieldValue.Builder()
                                                                    .stringValue(str)
                                                                    .build())
                                                            .collect(Collectors.toList()))))
                                    )._toQuery())))))._toQuery());
        }
        if (jiraIssuesFilter.getCalculation() == CALCULATION.state_transition_time) {
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_state_transitions")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermQuery.of(q2 -> q2
                                                    .field("w_hist_state_transitions.from_status")
                                                    .value(jiraIssuesFilter.getFromState()))._toQuery(),
                                            TermQuery.of(q3 -> q3
                                                    .field("w_hist_state_transitions.to_status")
                                                    .value(jiraIssuesFilter.getToState()))._toQuery())))))._toQuery());
        }
        if (jiraIssuesFilter.getCalculation() == CALCULATION.assign_to_resolve) {
            includesQueryConditions.add(ExistsQuery.of(e -> e
                    .field("w_first_assigned_at"))._toQuery());
        }
        if (needMissingStoryPointsFilter) {
            includesQueryConditions.add(ExistsQuery.of(e -> e
                    .field("w_story_points"))._toQuery());
        }
        if (CollectionUtils.isNotEmpty(developmentStages)) {
            includesQueryConditions.add(NestedQuery.of(n -> n
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
            includesQueryConditions.add(NestedQuery.of(n -> n
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
                if(CollectionUtils.isNotEmpty(histAssignees)) {
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

                if(CollectionUtils.isNotEmpty(histStatusCategories)) {
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

                includesQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_assignee_statuses")
                        .query(q0 -> q0
                                .bool(q1 -> q1
                                        .must(termsQueries))))._toQuery());

            }
        }

        if (jiraIssuesFilter.getAcross() == JiraIssuesFilter.DISTINCT.epic) {
            includesQueryConditions.add(ExistsQuery.of(q -> q.field("w_epic"))._toQuery());
        }
    }

    private static void createJiraOrFilterIncludesCondition(JiraOrFilter jiraOrFilter,
                                                            List<String> developmentStages,
                                                            List<String> historicalAssignees,
                                                            Boolean needMissingStoryPointsFilter,
                                                            List<Query> includesQueryConditions) {
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getPriorities())) {
            includesQueryConditions.add(EsUtils.getQuery("w_priority", jiraOrFilter.getPriorities()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getProjects())) {
            includesQueryConditions.add(EsUtils.getQuery("w_project", jiraOrFilter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getKeys())) {
            includesQueryConditions.add(EsUtils.getQuery("w_workitem_id", jiraOrFilter.getKeys()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getStatuses())) {
            includesQueryConditions.add(EsUtils.getQuery("w_status", jiraOrFilter.getStatuses()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getAssignees())) {
            includesQueryConditions.add(EsUtils.getQuery("w_assignee.id", jiraOrFilter.getAssignees()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getReporters())) {
            includesQueryConditions.add(EsUtils.getQuery("w_reporter.id", jiraOrFilter.getReporters()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getIssueTypes())) {
            includesQueryConditions.add(EsUtils.getQuery("w_workitem_type", jiraOrFilter.getIssueTypes()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getParentKeys())) {
            includesQueryConditions.add(EsUtils.getQuery("w_parent_key", jiraOrFilter.getParentKeys()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getStatusCategories())) {
            includesQueryConditions.add(EsUtils.getQuery("w_status_category", jiraOrFilter.getStatusCategories()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getEpics())) {
            includesQueryConditions.add(EsUtils.getQuery("w_epic", jiraOrFilter.getEpics()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getResolutions())) {
            includesQueryConditions.add(EsUtils.getQuery("w_resolution", jiraOrFilter.getResolutions()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getComponents())) {
            includesQueryConditions.add(EsUtils.getQuery("w_components", jiraOrFilter.getComponents()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getLabels())) {
            includesQueryConditions.add(EsUtils.getQuery("w_labels", jiraOrFilter.getLabels()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getSprintIds())) {
            includesQueryConditions.add(EsUtils.getQuery("w_sprints.id", jiraOrFilter.getSprintIds()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getSprintNames())) {
            includesQueryConditions.add(EsUtils.getQuery("w_sprints.name", jiraOrFilter.getSprintNames()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getSprintStates())) {
            includesQueryConditions.add(EsUtils.getQuery("w_sprints.state", jiraOrFilter.getSprintStates()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getVersions())) {
            includesQueryConditions.add(EsUtils.getQuery("w_versions.name", jiraOrFilter.getVersions()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getFixVersions())) {
            includesQueryConditions.add(EsUtils.getQuery("w_fix_versions.name", jiraOrFilter.getFixVersions()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getLinks())) { //? needs research
            includesQueryConditions.add(EsUtils.getQuery("w_links.relation", jiraOrFilter.getLinks()));
        }
        if (CollectionUtils.isNotEmpty(jiraOrFilter.getStages())) {
            includesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_statuses")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermsQuery.of(q -> q
                                            .field("w_hist_statuses.status")
                                            .terms(TermsQueryField.of(termsField -> termsField
                                                    .value(jiraOrFilter.getStages().stream()
                                                            .map(str -> new FieldValue.Builder()
                                                                    .stringValue(str)
                                                                    .build())
                                                            .collect(Collectors.toList()))))
                                    )._toQuery())))))._toQuery());
        }
        if (needMissingStoryPointsFilter) {
            includesQueryConditions.add(ExistsQuery.of(e -> e
                    .field("w_story_points"))._toQuery());
        }
        if (CollectionUtils.isNotEmpty(developmentStages)) {
            includesQueryConditions.add(NestedQuery.of(n -> n
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
            includesQueryConditions.add(NestedQuery.of(n -> n
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
    }

    private static void createJiraFilterExcludesCondition(JiraIssuesFilter jiraIssuesFilter,
                                                          List<Query> excludesQueryConditions) {
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludePriorities())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_priority", jiraIssuesFilter.getExcludePriorities()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeKeys())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_workitem_id", jiraIssuesFilter.getExcludeKeys()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeProjects())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_project", jiraIssuesFilter.getExcludeProjects()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeAssignees())) {
            List<String> excludeAssignees = new ArrayList<>(jiraIssuesFilter.getExcludeAssignees());
            excludeAssignees.add("null");
            excludesQueryConditions.add(EsUtils.getQuery("w_assignee.id", excludeAssignees));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeEpics())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_epic", jiraIssuesFilter.getExcludeEpics()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeIssueTypes())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_workitem_type", jiraIssuesFilter.getExcludeIssueTypes()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeIntegrationIds())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_integration_id", jiraIssuesFilter.getExcludeIntegrationIds()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeParentKeys())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_parent_key", jiraIssuesFilter.getExcludeParentKeys()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeResolutions())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_resolution", jiraIssuesFilter.getExcludeResolutions()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeStatuses())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_status", jiraIssuesFilter.getExcludeStatuses()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeStatusCategories())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_status_category", jiraIssuesFilter.getExcludeStatusCategories()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeReporters())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_reporter.id", jiraIssuesFilter.getExcludeReporters()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeComponents())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_components", jiraIssuesFilter.getExcludeComponents()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeLabels())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_labels", jiraIssuesFilter.getExcludeLabels()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeSprintIds())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_sprints.id", jiraIssuesFilter.getExcludeSprintIds()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeSprintNames())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_sprints.name", jiraIssuesFilter.getExcludeSprintNames()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeSprintStates())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_sprints.state", jiraIssuesFilter.getExcludeSprintStates()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeVersions())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_versions.name", jiraIssuesFilter.getExcludeVersions()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeFixVersions())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_fix_versions.name", jiraIssuesFilter.getExcludeFixVersions()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeLinks())) {
            excludesQueryConditions.add(EsUtils.getQuery("w_links.relation", jiraIssuesFilter.getExcludeLinks()));
        }
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeStages())) {
            excludesQueryConditions.add(NestedQuery.of(n -> n
                    .path("w_hist_statuses")
                    .query(q0 -> q0
                            .bool(q1 -> q1
                                    .must(List.of(TermsQuery.of(q -> q
                                            .field("w_hist_statuses.status")
                                            .terms(TermsQueryField.of(termsField -> termsField
                                                    .value(jiraIssuesFilter.getExcludeStages().stream()
                                                            .map(str -> new FieldValue.Builder()
                                                                    .stringValue(str)
                                                                    .build())
                                                            .collect(Collectors.toList()))))
                                    )._toQuery())))))._toQuery());
        }
    }

    private static void createJiraOrFilterRangeCondition(JiraOrFilter jiraOrFilter,
                                                         List<Query> includesQueryConditions) {
        if (jiraOrFilter.getIssueCreatedRange() != null && jiraOrFilter.getIssueCreatedRange().getLeft() != null &&
                jiraOrFilter.getIssueCreatedRange().getRight() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_created_at", jiraOrFilter.getIssueCreatedRange()));
        }
        if (jiraOrFilter.getIssueUpdatedRange() != null && jiraOrFilter.getIssueUpdatedRange().getLeft() != null &&
                jiraOrFilter.getIssueUpdatedRange().getRight() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_updated_at", jiraOrFilter.getIssueUpdatedRange()));
        }
        if (jiraOrFilter.getIssueDueRange() != null && jiraOrFilter.getIssueDueRange().getLeft() != null &&
                jiraOrFilter.getIssueDueRange().getRight() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_due_at", jiraOrFilter.getIssueDueRange()));
        }
        if (jiraOrFilter.getIssueResolutionRange() != null && jiraOrFilter.getIssueResolutionRange().getRight() != null
                && jiraOrFilter.getIssueResolutionRange().getLeft() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_resolved_at", jiraOrFilter.getIssueResolutionRange()));
        }

        if (jiraOrFilter.getStoryPoints() != null) {
            JsonData gt = null;
            if (jiraOrFilter.getStoryPoints().get("$gt") != null) {
                gt = JsonData.of(jiraOrFilter.getStoryPoints().get("$gt"));
            }
            JsonData lt = null;
            if (jiraOrFilter.getStoryPoints().get("$lt") != null) {
                lt = JsonData.of(jiraOrFilter.getStoryPoints().get("$lt"));
            }
            includesQueryConditions.add(getRangeQuery("w_story_points", gt, lt));
        }
    }

    private static void createJiraFilterRangeCondition(JiraIssuesFilter jiraIssuesFilter,
                                                       List<Query> includesQueryConditions) {
        if (jiraIssuesFilter.getIssueCreatedRange() != null && jiraIssuesFilter.getIssueCreatedRange().getLeft() != null &&
                jiraIssuesFilter.getIssueCreatedRange().getRight() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_created_at", jiraIssuesFilter.getIssueCreatedRange()));
        }
        if (jiraIssuesFilter.getIssueUpdatedRange() != null && jiraIssuesFilter.getIssueUpdatedRange().getLeft() != null &&
                jiraIssuesFilter.getIssueUpdatedRange().getRight() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_updated_at", jiraIssuesFilter.getIssueUpdatedRange()));
        }
        if (jiraIssuesFilter.getIssueDueRange() != null && jiraIssuesFilter.getIssueDueRange().getLeft() != null &&
                jiraIssuesFilter.getIssueDueRange().getRight() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_due_at", jiraIssuesFilter.getIssueDueRange()));
        }
        if (jiraIssuesFilter.getIssueResolutionRange() != null && jiraIssuesFilter.getIssueResolutionRange().getRight() != null
                && jiraIssuesFilter.getIssueResolutionRange().getLeft() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_resolved_at", jiraIssuesFilter.getIssueResolutionRange()));
        }
        if (jiraIssuesFilter.getSnapshotRange() != null && jiraIssuesFilter.getSnapshotRange().getRight() != null
                && jiraIssuesFilter.getSnapshotRange().getLeft() != null) {
            includesQueryConditions.add(EsUtils.getRangeQueryForTime("w_ingested_at", jiraIssuesFilter.getSnapshotRange()));
        }
        if (jiraIssuesFilter.getCalculation() == CALCULATION.age) {
            if (jiraIssuesFilter.getSnapshotRange() != null && StringUtils.isNotBlank(jiraIssuesFilter.getAggInterval())) {
                // get age intervals
                var now = Instant.now();
                var rangeFrom = jiraIssuesFilter.getSnapshotRange().getLeft() != null ? jiraIssuesFilter.getSnapshotRange().getLeft() : now.minus(Duration.ofDays(90)).getEpochSecond();
                var rangeTo = jiraIssuesFilter.getSnapshotRange().getRight() != null ? jiraIssuesFilter.getSnapshotRange().getRight() : now.getEpochSecond();
                var intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, jiraIssuesFilter.getAggInterval());
                intervals.add(1664217000L);
                if (CollectionUtils.isNotEmpty(intervals)) {
                    includesQueryConditions.add(EsUtils.getQuery("w_ingested_at", intervals.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList())));
                }
            }
        }
        if (jiraIssuesFilter.getStoryPoints() != null) {
            JsonData gt = null;
            if (jiraIssuesFilter.getStoryPoints().get("$gt") != null) {
                gt = JsonData.of(jiraIssuesFilter.getStoryPoints().get("$gt"));
            }
            JsonData lt = null;
            if (jiraIssuesFilter.getStoryPoints().get("$lt") != null) {
                lt = JsonData.of(jiraIssuesFilter.getStoryPoints().get("$lt"));
            }
            includesQueryConditions.add(getRangeQuery("w_story_points", gt, lt));
        }
        if (jiraIssuesFilter.getCalculation() == CALCULATION.sprint_mapping) {
            if(jiraIssuesFilter.getSprintMappingSprintCompletedAtAfter() != null && jiraIssuesFilter.getSprintMappingSprintCompletedAtBefore() != null){
                includesQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_sprints")
                        .query(q -> q
                                .range(r -> r
                                        .field("w_hist_sprints.completed_at")
                                        .gte(JsonData.of(jiraIssuesFilter.getSprintMappingSprintCompletedAtAfter()))
                                        .lt(JsonData.of(jiraIssuesFilter.getSprintMappingSprintCompletedAtBefore())))))._toQuery());
            }
            if(jiraIssuesFilter.getSprintMappingSprintStartedAtAfter() != null && jiraIssuesFilter.getSprintMappingSprintStartedAtBefore() != null){
                includesQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_sprints")
                        .query(q -> q
                                .range(r -> r
                                        .field("w_hist_sprints.start_time")
                                        .gte(JsonData.of(jiraIssuesFilter.getSprintMappingSprintStartedAtAfter()))
                                        .lt(JsonData.of(jiraIssuesFilter.getSprintMappingSprintStartedAtBefore())))))._toQuery());
            }
            if(jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtAfter() != null && jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtBefore() != null){
                includesQueryConditions.add(NestedQuery.of(n -> n
                        .path("w_hist_sprints")
                        .query(q -> q
                                .range(r -> r
                                        .field("w_hist_sprints.end_time")
                                        .gte(JsonData.of(jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtAfter()))
                                        .lt(JsonData.of(jiraIssuesFilter.getSprintMappingSprintPlannedCompletedAtBefore())))))._toQuery());
            }
        }
    }

    private static Query getRangeQuery(String field, JsonData gte, JsonData lte) {
        return RangeQuery.of(q -> q
                .field(field)
                .gt(gte)
                .lt(lte))._toQuery();
    }

    private static void getPartialMatch(Map<String, Map<String, String>> partialMatch, List<Query> queries,
                                        List<DbJiraField> dbJiraFields) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();
            log.info("getPartialMatch: dbJiraFields: " + dbJiraFields);
            log.info("getPartialMatch: Building Partial match on: " + partialMatch);
            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (PARTIAL_MATCH_ARRAY_COLUMNS.contains(key) || PARTIAL_MATCH_COLUMNS.contains(key)) {
                switch (key) {
                    case "key":
                        key = "w_workitem_id";
                        break;
                    case "assignee":
                        key = "w_assignee.display_name";
                        break;
                    case "reporter":
                        key = "w_reporter.display_name";
                        break;
                    case "first_assignee":
                        key = "w_first_assignee.display_name";
                        break;
                    case "sprint_name":
                        key = "w_hist_sprints.name";
                        break;
                    case "versions":
                        key = "w_versions.name";
                        break;
                    case "fix_versions":
                        key = "w_fix_versions.name";
                        break;
                    default:
                        key = "w_" + key;
                        break;
                }
                getRegex(begins, ends, contains, key, queries);
            } else if (DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(key).matches()) {
                Optional<DbJiraField> customField = Optional.empty();
                if (CollectionUtils.isNotEmpty(dbJiraFields)) {
                    String finalKey1 = key;
                    customField = dbJiraFields.stream()
                            .filter(dbJiraField -> dbJiraField.getFieldKey().equalsIgnoreCase(finalKey1)).findFirst();
                }
                String customFieldType = StringUtils.EMPTY;
                if (customField.isPresent()) {
                    customFieldType = customField.get().getFieldType();
                }

                log.info("getPartialMatch: custom field type is: " + customFieldType);
                switch (customFieldType) {
                    case "integer":
                        getNestedRegex(begins, ends, contains, key, "w_custom_fields.int", queries);
                        break;
                    case "long":
                        getNestedRegex(begins, ends, contains, key, "w_custom_fields.long", queries);
                        break;
                    case "boolean":
                        getNestedRegex(begins, ends, contains, key, "w_custom_fields.bool", queries);
                        break;
                    case "number":
                    case "float":
                        getNestedRegex(begins, ends, contains, key, "w_custom_fields.float", queries);
                        break;
                    case "array":
                        getNestedRegex(begins, ends, contains, key, "w_custom_fields.arr", queries);
                        break;
                    case "option-with-child":
                    case "option":
                    case "string":
                    default:
                        getNestedRegex(begins, ends, contains, key, "w_custom_fields.str", queries);
                        break;
                }
            }
        }
    }

    private static void getMissingFieldsQuery(
            Map<JiraIssuesFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields,
            Map<String, Boolean> missingCustomFields,
            List<Query> includesQueryConditions,
            List<Query> excludesQueryConditions) {

        if (MapUtils.isNotEmpty(missingBuiltinFields)) {
            missingBuiltinFields.entrySet().stream()
                    .map(missingBuiltinField -> {
                        final boolean shouldBeMissing = Boolean.TRUE.equals(missingBuiltinField.getValue());
                        switch (missingBuiltinField.getKey()) {
                            case priority:
                                includesQueryConditions.add(getQueryForMissingField(shouldBeMissing, "w_priority", "_UNPRIORITIZED_"));
                                break;
                            case status:
                                includesQueryConditions.add(getQueryForMissingField(shouldBeMissing, "w_status", "_UNKNOWN_"));
                                break;
                            case assignee:
                                includesQueryConditions.add(getQueryForMissingField(shouldBeMissing, "w_assignee.display_name", "_UNASSIGNED_"));
                                break;
                            case reporter:
                                includesQueryConditions.add(getQueryForMissingField(shouldBeMissing, "w_reporter", "_UNKNOWN_"));
                                break;
                            case component:
                                getExistsQueryForMissingField("w_components", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            case label:
                                getExistsQueryForMissingField("w_labels", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            case fix_version:
                                getExistsQueryForMissingField("w_fix_versions", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            case version:
                                getExistsQueryForMissingField("w_versions", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            case epic:
                                getExistsQueryForMissingField("w_epic", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            case project:
                                getExistsQueryForMissingField("w_project", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            case first_assignee:
                                getExistsQueryForMissingField("w_first_assignee", shouldBeMissing, excludesQueryConditions, includesQueryConditions);
                                break;
                            default:
                                return null;
                        }
                        return null;
                    })
                    .collect(Collectors.toList());
        }
        if (MapUtils.isNotEmpty(missingCustomFields)) {
            missingCustomFields.forEach((field, shouldBeMissing) -> {
                if (shouldBeMissing) {
                    excludesQueryConditions.add(EsUtils.getNestedQuery("w_custom_fields", "w_custom_fields.name", List.of(field)));
                } else {
                    includesQueryConditions.add(EsUtils.getNestedQuery("w_custom_fields", "w_custom_fields.name", List.of(field)));
                }
            });
        }
    }

    private static void getExistsQueryForMissingField(String field, boolean shouldBeMissing, List<Query> excludesQueryConditions, List<Query> includesQueryConditions) {
        if (shouldBeMissing) {
            excludesQueryConditions.add(ExistsQuery.of(q -> q
                    .field(field))._toQuery());
        } else {
            includesQueryConditions.add(ExistsQuery.of(q -> q
                    .field(field))._toQuery());
        }
    }

    private static Query getQueryForMissingField(boolean shouldBeMissing, String field, String value) {
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

    private static void getCustomFieldQuery(Map<String, Object> customFields,
                                            List<DbJiraField> dbJiraFields,
                                            List<Query> queryConditions) {
        for (var customFieldEntry : customFields.entrySet()) {
            String key = customFieldEntry.getKey();
            Optional<DbJiraField> customField = Optional.empty();
            if (CollectionUtils.isNotEmpty(dbJiraFields)) {
                customField = dbJiraFields.stream()
                        .filter(dbJiraField -> dbJiraField.getFieldKey().equalsIgnoreCase(key)).findFirst();
            }
            String customFieldType = StringUtils.EMPTY;
            if (customField.isPresent()) {
                customFieldType = customField.get().getFieldType();
            }
            List<String> values = new ArrayList<>();
            if (CUSTOM_FIELD_TYPES.contains(customFieldType)) {
                //if custom_field type supports values List<String> convert values to List<String>
                values = (List) customFields.get(key);
            } else if (!CUSTOM_FIELD_NON_LIST_OF_STRINGS_TYPES.contains(customFieldType)) {
                //custom field type date/datetime/dateTime value is NOT List<String>, for these do NOT try to convert
                //for everything else try to convert to List<String>
                try {
                    values = (List) customFields.get(key);
                } catch (Exception e) {
                    //If we fail to convert to List<String>
                    log.warn("getCustomFieldQuery Error converting values! customFieldType = {}", customFieldType, e);
                    values = null;
                }
            }

            log.info("getCustomFieldQuery: custom field type is: {}, values = {}", customFieldType, values);
            if (CollectionUtils.isNotEmpty(values)) {
                switch (customFieldType) {
                    case "integer":
                        queryConditions.add(getNestedQuery("w_custom_fields", "w_custom_fields.name", key, "w_custom_fields.int", values));
                        break;
                    case "long":
                        queryConditions.add(getNestedQuery("w_custom_fields", "w_custom_fields.name", key, "w_custom_fields.long", values));
                        break;
                    case "date":
                    case "datetime":
                    case "dateTime":
                        Map<String, String> timeRange = (Map) customFields.get(key);
                        final Long rangeStart = timeRange.get("$gt") != null ? Long.valueOf(timeRange.get("$gt")) : null;
                        final Long rangeEnd = timeRange.get("$lt") != null ? Long.valueOf(timeRange.get("$lt")) : null;

                        JsonData gte = null;
                        JsonData lte = null;

                        if (rangeStart != null) {
                            gte = JsonData.of(rangeStart);
                        }
                        if (rangeEnd != null) {
                            lte = JsonData.of(rangeEnd);
                        }
                        JsonData finalGte = gte;
                        JsonData finalLte = lte;
                        queryConditions.add(NestedQuery.of(nq -> nq
                                .path("w_custom_fields")
                                .query(q -> q
                                        .bool(b -> b
                                                .must(List.of(EsUtils.getQuery("w_custom_fields.name", List.of(key)),
                                                        getRangeQuery("w_custom_fields.date", finalGte, finalLte)))
                                        )
                                )
                        )._toQuery());
                        break;
                    case "boolean":
                        queryConditions.add(getNestedQuery("w_custom_fields", "w_custom_fields.name", key, "w_custom_fields.bool", values));
                        break;
                    case "number":
                    case "float":
                        queryConditions.add(getNestedQuery("w_custom_fields", "w_custom_fields.name", key, "w_custom_fields.float", values));
                        break;
                    case "array":
                        queryConditions.add(getNestedQuery("w_custom_fields", "w_custom_fields.name", key, "w_custom_fields.arr", values));
                        break;
                    default:
                        queryConditions.add(getNestedQuery("w_custom_fields", "w_custom_fields.name", key, "w_custom_fields.str", values));
                        break;
                }
            }
        }
    }

    private static Query getNestedQuery(String path, String fField, String key, String sField, List<String> values) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(EsUtils.getQuery(fField, List.of(key)), EsUtils.getQuery(sField, values)))
                        )
                )
        )._toQuery();
    }

    private static Query getNestedWildCardQuery(String path, String fField, String key, String sField, String wildcard) {
        return NestedQuery.of(nq -> nq
                .path(path)
                .query(q -> q
                        .bool(b -> b
                                .must(List.of(EsUtils.getQuery(fField, List.of(key)), getWildCardQuery(sField, wildcard)))
                        )
                )
        )._toQuery();
    }

    private static Query getWildCardQuery(String field, String wildcard) {
        return WildcardQuery.of(q -> q
                .field(field)
                .wildcard(wildcard))._toQuery();
    }

    private static void getExtraCriteriaQuery(JiraIssuesFilter jiraIssuesFilter, List<Query> includesQueryConditions,
                                              List<Query> excludesQueryConditions) {
        for (JiraIssuesFilter.EXTRA_CRITERIA hygieneType : jiraIssuesFilter.getExtraCriteria()) {
            switch (hygieneType) {
                case idle:
                    long currentTime = Instant.now().getEpochSecond();
                    long idletime = (currentTime - NumberUtils.toInt(
                            String.valueOf(jiraIssuesFilter.getHygieneCriteriaSpecs().get(JiraIssuesFilter.EXTRA_CRITERIA.idle)),
                            30) * 86400L);
                    JsonData lt = JsonData.of(idletime);
                    includesQueryConditions.add(RangeQuery.of(q -> q
                            .timeZone("UTC")
                            .format("epoch_second")
                            .field("w_updated_at")
                            .lt(lt))._toQuery());
                    break;
                case no_assignee:
                    includesQueryConditions.add(EsUtils.getQuery("w_assignee.display_name", List.of(DbJiraIssue.UNASSIGNED)));
                    break;
                case no_due_date:
                    excludesQueryConditions.add(ExistsQuery.of(q -> q
                            .field("w_due_at"))._toQuery());
                    break;
                case poor_description:
                    includesQueryConditions.add(RangeQuery.of(q -> q
                            .field("w_desc_size")
                            .lt(JsonData.of(NumberUtils.toInt(String.valueOf(
                                    jiraIssuesFilter.getHygieneCriteriaSpecs().get(JiraIssuesFilter.EXTRA_CRITERIA.poor_description)), 10))))._toQuery());
                    break;
                case no_components:
                    excludesQueryConditions.add(ExistsQuery.of(q -> q
                            .field("w_components"))._toQuery());
                    break;
                case inactive_assignees:
                    includesQueryConditions.add(TermQuery.of(q -> q
                            .field("w_assignee.active")
                            .value(false)
                    )._toQuery());
                    break;
                case missed_response_time:
                    includesQueryConditions.add(ScriptQuery.of(sq -> sq
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
                    if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getExcludeStages())) {
                        includesQueryConditions.add(ScriptQuery.of(sq -> sq
                                .script(s -> s
                                        .inline(i -> i
                                                .source("long resolvedAt = 0;" +
                                                        "if (doc['w_resolved_at'].size() == 0) {" +
                                                        "  resolvedAt = new Date().getTime();" +
                                                        "} else {" +
                                                        "  resolvedAt = doc['w_resolved_at'].value.toInstant().toEpochMilli();" +
                                                        "}" +
                                                        "long endTime = 0;" +
                                                        "if (doc['w_hist_statuses.end_time'].size() == 0) {" +
                                                        "  endTime = new Date().getTime();" +
                                                        "} else {" +
                                                        "  endTime = doc['w_hist_statuses.end_time'].value.toInstant().toEpochMilli();" +
                                                        "}" +
                                                        "long startTime = 0;" +
                                                        "if (doc['w_hist_statuses.start_time'].size() == 0) {" +
                                                        "  startTime = new Date().getTime();" +
                                                        "} else {" +
                                                        "  startTime = doc['w_hist_statuses.start_time'].value.toInstant().toEpochMilli();" +
                                                        "}" +
                                                        "long excludeTime = endTime - startTime;" +
                                                        "long solveTime = resolvedAt - excludeTime;" +
                                                        "if (solveTime < 0) {" +
                                                        "  solveTime = 0;" +
                                                        "}" +
                                                        "return solveTime > doc['w_priorities_sla.solve_time'].value;")
                                        )
                                )
                        )._toQuery());
                    } else {
                        includesQueryConditions.add(ScriptQuery.of(sq -> sq
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
                    }
                    break;
            }
        }
    }

    private static List<String> getPartialMatchValues(JiraIssuesFilter jiraIssuesFilter, String partialMatchKey) {
        if(MapUtils.isEmpty(jiraIssuesFilter.getPartialMatch())) {
            return Collections.emptyList();
        }

        Map<String, String> partialFilter = jiraIssuesFilter.getPartialMatch().getOrDefault(partialMatchKey, Map.of());
        List<String> partialFilterValues = new ArrayList<>();
        String begins = partialFilter.get("$begins");
        String ends = partialFilter.get("$ends");
        String contains = partialFilter.get("$contains");

        if (begins != null) {
            partialFilterValues.add(begins + ".*");
        }

        if (ends != null) {
            partialFilterValues.add(".*" + ends);
        }

        if (contains != null) {
            partialFilterValues.add(".*" + contains + ".*");
        }
        return partialFilterValues;
    }

    private static List<String> parseTermAggsIncludeValues(DISTINCT across, JiraIssuesFilter jiraIssuesFilter) {
        List<String> termIncludeValues = new ArrayList<>();
        switch (across) {
            case label:
                if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getLabels())) {
                    termIncludeValues.addAll(jiraIssuesFilter.getLabels());
                }
                termIncludeValues.addAll(getPartialMatchValues(jiraIssuesFilter, "labels"));
                break;
            case component:
                if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getComponents())) {
                    termIncludeValues.addAll(jiraIssuesFilter.getComponents());
                }
                termIncludeValues.addAll(getPartialMatchValues(jiraIssuesFilter, "components"));
                break;
            default:
                termIncludeValues = Collections.emptyList();
        }
        return termIncludeValues;
    }

    private static Map<String, Aggregation> getAcrossConditions(DISTINCT across,
                                                                JiraIssuesFilter jiraIssuesFilter,
                                                                List<DbJiraField> dbJiraFields,
                                                                Map<String, Aggregation> nestedAggregation,
                                                                Integer page, Integer pageSize,
                                                                Boolean valuesOnly,
                                                                Boolean isStageReport,
                                                                Boolean isStackAgg,
                                                                DISTINCT stack) {
        if (across == null) {
            return Map.of();
        }
        Integer acrossLimit = jiraIssuesFilter.getAcrossLimit() != null ? jiraIssuesFilter.getAcrossLimit() : 90;
        Map<String, SortingOrder> sortBy = jiraIssuesFilter.getSort();
        SortOrder esSortOrder = getSortOrder(jiraIssuesFilter);
        CALCULATION calculation = jiraIssuesFilter.getCalculation();

        String keyName = StringUtils.EMPTY;
        if (across == DISTINCT.custom_field || stack == DISTINCT.custom_field) {
            addBucketPagination(nestedAggregation, StringUtils.EMPTY, esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
        } else {
            if (MapUtils.isNotEmpty(sortBy) && sortBy.keySet().stream().findFirst().isPresent()) {
                String sortField = sortBy.keySet().stream().findFirst().get();
                if (!across.toString().equals(sortField)) {
                    if (!calculation.toString().equals(sortField) &&
                            !(List.of(JiraIssuesFilter.CALCULATION.sprint_mapping, JiraIssuesFilter.CALCULATION.sprint_mapping_count).contains(calculation))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + sortField);
                    }
                    addSortingFields(nestedAggregation, calculation, esSortOrder, valuesOnly, isStackAgg, page, acrossLimit, false);
                } else if (across.toString().equals(sortField)) {
                    addBucketPagination(nestedAggregation, StringUtils.EMPTY, esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
                    keyName = "_key";
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + sortField);
                }
            } else {
                if (TIMESTAMP_SORTABLE_COLUMNS.contains(across)) {
                    addBucketPagination(nestedAggregation, StringUtils.EMPTY, esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
                } else {
                    addSortingFields(nestedAggregation, calculation, esSortOrder, valuesOnly, isStackAgg, page, acrossLimit, true);
                }
            }
        }
        Map<String, Aggregation> aggConditions = new HashMap<>();
        String finalKeyName = keyName;
        if (isStageReport) {
            return getStagesAggs(across, jiraIssuesFilter, dbJiraFields,
                    nestedAggregation, isStackAgg, stack,
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
                                        if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
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

                                    if(BooleanUtils.isTrue(jiraIssuesFilter.getFilterAcrossValues())) {
                                        //ToDo: VA - Currently there is a known issue where for Term Agss filtering (include) works for list of Strings or one regex string
                                        List<String> termAggsIncludeValues = parseTermAggsIncludeValues(across, jiraIssuesFilter);
                                        if(CollectionUtils.isNotEmpty(termAggsIncludeValues)) {
                                            builder.include(i -> i.terms(termAggsIncludeValues));
                                        }
                                    }

                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
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

                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case custom_field:
                Optional<DbJiraField> customField = Optional.empty();
                if (CollectionUtils.isNotEmpty(dbJiraFields)) {
                    customField = dbJiraFields.stream()
                            .filter(dbWorkItemField -> dbWorkItemField.getFieldKey().equalsIgnoreCase(jiraIssuesFilter.getCustomAcross()))
                            .findFirst();
                }
                String customFieldType = StringUtils.EMPTY;
                if (customField.isPresent()) {
                    customFieldType = customField.get().getFieldType();
                }
                String esCustomColumn = "";
                esCustomColumn = EsUtils.getCustomFieldColumn(customFieldType, esCustomColumn);
                String finalEsCustomColumn = esCustomColumn;
                Aggregation updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                if (jiraIssuesFilter.getCustomAcross() != null && finalEsCustomColumn != null) {
                    aggConditions.put("across_custom_field", Aggregation.of(a -> a
                            .nested(n -> n
                                    .path("w_custom_fields")
                            )
                            .aggregations(Map.of("filter_custom_fields_name", Aggregation.of(a1 -> a1
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("w_custom_fields.name")
                                                    .value(jiraIssuesFilter.getCustomAcross())
                                            )
                                    )
                                    .aggregations(Map.of("across_custom_fields_type", Aggregation.of(a2 -> a2
                                            .terms(t -> {
                                                        TermsAggregation.Builder builder = t
                                                                .field("w_custom_fields." + finalEsCustomColumn)
                                                                .minDocCount(1)
                                                                .size(Integer.MAX_VALUE);
                                                        if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
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
                                    )
                            )))
                    ))));
                }
                break;
            case sprint_mapping:
                Validate.isTrue(jiraIssuesFilter.getCalculation() == JiraIssuesFilter.CALCULATION.sprint_mapping, "Only sprint_mapping calculation is supported across sprint_mapping");
                //PROP-3431 : pageSize is not relevant here. Hard-coding it to a big number
                final int pageSizeHC = 1000;
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .composite(c -> c
                                .size(pageSizeHC + (page * pageSizeHC))
                                .sources(List.of(Map.of("integration_id", CompositeAggregationSource.of(a1 -> a1
                                        .terms(t -> t
                                                .field("w_integration_id")))))))
                        .aggregations(Map.of("nested", Aggregation.of(a2 -> a2
                                .nested(n -> n
                                        .path("w_hist_sprints"))
                                .aggregations("sprint_id", Aggregation.of(a3 -> a3
                                        .terms(t1 -> t1
                                                .field("w_hist_sprints.id")
                                                .size(pageSizeHC))
                                        .aggregations("sprint_name", Aggregation.of(a4 -> a4
                                                .terms(t2 -> t2
                                                        .field("w_hist_sprints.name")
                                                        .size(pageSizeHC))
                                                .aggregations("sprint_completed_at", Aggregation.of(a5 -> a5
                                                        .terms(t3 -> {
                                                                    TermsAggregation.Builder builder = t3
                                                                            .field("w_hist_sprints.completed_at")
                                                                            .size(pageSizeHC);
                                                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                                                    else
                                                                        return builder;
                                                                }
                                                        )
                                                        .aggregations("sprint_goal", Aggregation.of(a6 -> a6
                                                                .terms(t4 -> t4
                                                                        .field("w_hist_sprints.goal")
                                                                        .missing(" ")
                                                                        .size(pageSizeHC))
                                                                .aggregations("sprint_start_time", Aggregation.of(a7 -> a7
                                                                        .terms(t5 -> t5
                                                                                .field("w_hist_sprints.start_time")
                                                                                .size(pageSizeHC))
                                                                                .aggregations("sprint_end_time", Aggregation.of(a8 -> a8
                                                                                        .terms(t6 -> t6
                                                                                        .field("w_hist_sprints.end_time")
                                                                                        .size(pageSizeHC))))))))))))
                                )))))
                        .aggregations(nestedAggregation)
                ));
                break;
            case first_assignee:
            case reporter:
            case assignee:
                Map<String, Aggregation> bucket;
                if (page != null) {
                    bucket = Map.of("across_" + across.name(), Aggregation.of(a1 -> a1
                            .terms(t -> {
                                        TermsAggregation.Builder builder = t
                                                .field("w_" + across.name() + ".id")
                                                .missing("_UNASSIGNED_");
                                        if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                            return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                        else
                                            return builder;
                                    }
                            ).aggregations(nestedAggregation)
                    ), "bucket_pagination", BucketSortAggregation.of(b -> b
                            .from(valuesOnly ? 0 : page * jiraIssuesFilter.getAcrossLimit())
                            .size(valuesOnly ? Integer.MAX_VALUE : jiraIssuesFilter.getAcrossLimit()))._toAggregation());
                } else {
                    bucket = Map.of("across_" + across.name(), Aggregation.of(a1 -> a1
                            .terms(t -> {
                                        TermsAggregation.Builder builder = t
                                                .field("w_" + across.name() + ".id")
                                                .missing("_UNASSIGNED_")
                                                .size(Integer.MAX_VALUE);
                                        if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                            return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                        else
                                            return builder;
                                    }
                            ).aggregations(nestedAggregation)
                    ), "bucket_pagination", BucketSortAggregation.of(a -> a
                            .from(0)
                            .size(isStackAgg || valuesOnly ? Integer.MAX_VALUE : jiraIssuesFilter.getAcrossLimit()))._toAggregation());
                }
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name() + ".display_name")
                                            .missing("_UNASSIGNED_")
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    return builder;
                                }
                        )
                        .aggregations(bucket)
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
                                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                                    else
                                                        return builder;
                                                }
                                        )
                                        .aggregations(nestedAggregation)
                                )))

                ));
                break;
            case parent:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_parent_workitem_id")
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case issue_type:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_workitem_type")
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;
                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case sprint:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_sprints.name")
                                            .minDocCount(1)
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(Map.of(StringUtils.isEmpty(finalKeyName) ? "_count" : finalKeyName, esSortOrder));
                                    else
                                        return builder;

                                }
                        )
                        .aggregations(nestedAggregation)
                ));
                break;
            case issue_created:
            case issue_updated:
            case issue_due:
            case issue_due_relative:
            case issue_resolved:
            case trend:
                getTimeBasedAcross(across, jiraIssuesFilter, aggConditions, nestedAggregation, esSortOrder);
                break;
            case resolution:
            case status_category:
            case project:
            case status:
            case priority:
            case epic:
            default:
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .terms(t -> {
                                    TermsAggregation.Builder builder = t
                                            .field("w_" + across.name())
                                            .size(Integer.MAX_VALUE);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
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

    private static Map<String, Aggregation> getStagesAggs(DISTINCT across,
                                                          JiraIssuesFilter jiraIssuesFilter,
                                                          List<DbJiraField> dbJiraFields,
                                                          Map<String, Aggregation> nestedAggregation,
                                                          Boolean isStack,
                                                          DISTINCT stack,
                                                          Map<String, SortingOrder> sortBy,
                                                          SortOrder esSortOrder,
                                                          CALCULATION calculation,
                                                          Map<String, Aggregation> aggConditions,
                                                          String finalKeyName) {
        switch (across) {
            case none:
                Aggregation updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                Aggregation stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : JiraIssuesFilter.DISTINCT.none.name(), isStack);
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
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : JiraIssuesFilter.DISTINCT.none.name(), isStack);
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
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : JiraIssuesFilter.DISTINCT.none.name(), isStack);
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
                String customColumn = (isStack ? jiraIssuesFilter.getCustomStacks().get(0) : jiraIssuesFilter.getCustomAcross());
                String customFieldType = EsUtils.getCustomFieldType(null, dbJiraFields, customColumn);
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
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : JiraIssuesFilter.DISTINCT.none.name(), isStack);
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
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
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
                                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
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
            case issue_created:
            case issue_updated:
            case issue_resolved:
            case issue_due:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : JiraIssuesFilter.DISTINCT.none.name(), isStack);
                String interval = jiraIssuesFilter.getAggInterval() != null ?
                        jiraIssuesFilter.getAggInterval() : CalendarInterval.Day.name();
                String field = "";
                switch (across.name()) {
                    case "issue_created":
                        field = "w_created_at";
                        break;
                    case "issue_updated":
                        field = "w_updated_at";
                        break;
                    case "issue_resolved":
                        field = "w_resolved_at";
                        break;
                    case "issue_due":
                        field = "w_due_at";
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
                                            .aggregations("bucket_pagination", updatedBucketAggs);
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
                                                if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
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
                                return aggregations.aggregations("across_" + stack.name(), stackAggs);
                            return aggregations;
                        }));
                        break;
                    default:
                        break;
                }
                break;
            case sprint:
            case ticket_category:
            case resolution:
            case issue_type:
            case status_category:
            case parent:
            case project:
            case status:
            case priority:
            case epic:
            default:
                updatedBucketAggs = getUpdatedBucketAggs(nestedAggregation);
                stackAggs = getUpdatedNestedAggs(nestedAggregation, stack != null ? stack.name() : JiraIssuesFilter.DISTINCT.none.name(), isStack);
                String fieldName = "w_" + across.name();
                if (across == DISTINCT.issue_type) {
                    fieldName = "w_workitem_type";
                } else if (across == DISTINCT.sprint) {
                    fieldName = "w_sprints.name";
                }
                String finalFieldName = fieldName;
                aggConditions.put("across_" + across.name(), Aggregation.of(a -> {
                            Aggregation.Builder.ContainerBuilder aggregations = a
                                    .terms(t -> t.field(finalFieldName))
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
        if ((StringUtils.isNotEmpty(stack) || !stack.equalsIgnoreCase(JiraIssuesFilter.DISTINCT.none.name())) && !isStack) {
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
        Aggregation bucketPagination = null;
        if (nestedAggregation.containsKey("bucket_pagination")) {
            bucketPagination = nestedAggregation.get("bucket_pagination");
            nestedAggregation.remove("bucket_pagination");
        }
        return bucketPagination;
    }

    private static void getTimeBasedAcross(DISTINCT across, JiraIssuesFilter jiraIssuesFilter, Map<String, Aggregation> conditions,
                                           Map<String, Aggregation> innerAggConditions, SortOrder esSortOrder) {
        CALCULATION calculation = jiraIssuesFilter.getCalculation() != null ? jiraIssuesFilter.getCalculation() : CALCULATION.ticket_count;
        Map<String, SortingOrder> sortBy = jiraIssuesFilter.getSort() != null ? jiraIssuesFilter.getSort() : Map.of();
        String interval = jiraIssuesFilter.getAggInterval() != null ? jiraIssuesFilter.getAggInterval() : AGG_INTERVAL.day.name();
        CalendarInterval calendarInterval = null;
        String format = "";
        switch (interval.toLowerCase()) {
            case "year":
                calendarInterval = CalendarInterval.Year;
                format = "yyyy";
                break;
            case "quarter":
                calendarInterval = CalendarInterval.Quarter;
                format = "q-yyyy";
                break;
            case "month":
                calendarInterval = CalendarInterval.Month;
                format = "MM-yyyy";
                break;
            case "week":
                calendarInterval = CalendarInterval.Week;
                format = "ww-yyyy";
                break;
            case "day":
                calendarInterval = CalendarInterval.Day;
                format = "dd-MM-yyyy";
                break;
        }
        final CalendarInterval finalCalendarInterval = calendarInterval;
        String finalFormat = format;

        switch (across) {
            case issue_created:
                conditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> {
                                    DateHistogramAggregation.Builder builder = dh
                                            .field("w_created_at")
                                            .timeZone("UTC")
                                            .calendarInterval(finalCalendarInterval)
                                            .format(finalFormat)
                                            .minDocCount(1);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(x -> x.key(esSortOrder));
                                    else
                                        return builder.order(x -> x.key(SortOrder.Desc));
                                }

                        )
                        .aggregations(innerAggConditions)
                ));
                break;
            case issue_updated:
                conditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> {
                                    DateHistogramAggregation.Builder builder = dh
                                            .field("w_updated_at")
                                            .timeZone("UTC")
                                            .calendarInterval(finalCalendarInterval)
                                            .format(finalFormat)
                                            .minDocCount(1);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(x -> x.key(esSortOrder));
                                    else
                                        return builder.order(x -> x.key(SortOrder.Desc));
                                }
                        )
                        .aggregations(innerAggConditions)
                ));
                break;
            case issue_due:
            case issue_due_relative:
                conditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> {
                                    DateHistogramAggregation.Builder builder = dh
                                            .field("w_due_at")
                                            .timeZone("UTC")
                                            .calendarInterval(finalCalendarInterval)
                                            .format(finalFormat)
                                            .minDocCount(1);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(x -> x.key(esSortOrder));
                                    else
                                        return builder.order(x -> x.key(SortOrder.Desc));
                                }
                        )
                        .aggregations(innerAggConditions)
                ));
                break;
            case issue_resolved:
                conditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> {
                                    DateHistogramAggregation.Builder builder = dh
                                            .field("w_resolved_at")
                                            .timeZone("UTC")
                                            .calendarInterval(finalCalendarInterval)
                                            .format(finalFormat)
                                            .minDocCount(1);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(x -> x.key(esSortOrder));
                                    else
                                        return builder.order(x -> x.key(SortOrder.Desc));
                                }
                        )
                        .aggregations(innerAggConditions)
                ));
                break;
            case trend:
                innerAggConditions.put("trend_count", Aggregation.of(a -> a
                        .cardinality(CardinalityAggregation.of(c -> c
                                .field("w_workitem_integ_id").precisionThreshold(40000)))));
                conditions.put("across_" + across.name(), Aggregation.of(a -> a
                        .dateHistogram(dh -> {
                                    DateHistogramAggregation.Builder builder = dh
                                            .field("w_ingested_at")
                                            .calendarInterval(finalCalendarInterval)
                                            .format(finalFormat)
                                            .minDocCount(1);
                                    if (MapUtils.isNotEmpty(sortBy) || calculation == CALCULATION.ticket_count)
                                        return builder.order(x -> x.key(esSortOrder));
                                    else
                                        return builder.order(x -> x.key(SortOrder.Desc));
                                }
                        )
                        .aggregations(innerAggConditions)
                ));
                break;
        }
    }

    private static Query getRangeQuery(ImmutablePair<Long, Long> range, String field) {
        RangeQuery.Builder rangeBuilder = new RangeQuery.Builder();
        if (range.getLeft() != null) {
            rangeBuilder.gt(JsonData.of(range.getLeft()));
        }
        if (range.getRight() != null) {
            rangeBuilder.lt(JsonData.of(range.getRight()));
        }
        return rangeBuilder
                .field(field)
                .build()._toQuery();
    }

    private static void getRegex(String begins, String ends, String contains, String key, List<Query> queries) {
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

    private static void getNestedRegex(String begins, String ends, String contains, String key, String sField, List<Query> queries) {
        if (begins != null) {
            queries.add(getNestedWildCardQuery("w_custom_fields", "w_custom_fields.name", key, sField, begins + "*"));
        }
        if (ends != null) {
            queries.add(getNestedWildCardQuery("w_custom_fields", "w_custom_fields.name", key, sField, "*" + ends));
        }
        if (contains != null) {
            queries.add(getNestedWildCardQuery("w_custom_fields", "w_custom_fields.name", key, sField, "*" + contains + "*"));
        }
    }

    private static void addSortingFields(Map<String, Aggregation> innerAggs, CALCULATION calculation, SortOrder esSortOrder,
                                         Boolean valuesOnly, Boolean isStackAgg, Integer page, Integer acrossLimit, Boolean isDefaultSort) {
        if(calculation == null) {
            calculation = CALCULATION.ticket_count;
        }
        if (calculation != null) {
            switch (calculation) {
                case age:
                case response_time:
                case bounces:
                case hops:
                case resolution_time:
                case assign_to_resolve:
                case state_transition_time:
                    String key = isDefaultSort ? calculation.name() + ".max" : calculation.name() + "_percentiles.50";
                    addBucketPagination(innerAggs, key, esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
                    break;
                case priority:
                    //PROP-3431 - skip bucket pagination for sprint_metrics_report as it's handled outside
                //case sprint_mapping:
                case sprint_mapping_count:
                case ticket_count:
                    addBucketPagination(innerAggs, StringUtils.EMPTY, esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
                    break;
                case story_points:
                    addBucketPagination(innerAggs, StringUtils.EMPTY, esSortOrder, valuesOnly, true, isStackAgg, page, acrossLimit);
                    break;
                case stage_times_report:
                case velocity_stage_times_report:
                    addBucketPagination(innerAggs, StringUtils.EMPTY, esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
                    break;
                case stage_bounce_report:
                case assignees:
                    addBucketPagination(innerAggs, calculation.name() + ".count", esSortOrder, valuesOnly, false, isStackAgg, page, acrossLimit);
                    break;
            }
        }
    }

    private static void addBucketPagination(Map<String, Aggregation> innerAggs, String calculationNameWithSortField,
                                            SortOrder sortOrder, Boolean valuesOnly, Boolean needMultiSort, Boolean isStackAgg, Integer page, Integer acrossLimit) {
        if (page != null) {
            innerAggs.put("bucket_pagination", BucketSortAggregation.of(a -> {
                BucketSortAggregation.Builder builder = a
                        .from(valuesOnly ? 0 : page * acrossLimit)
                        .size(valuesOnly ? Integer.MAX_VALUE : acrossLimit);
                if (needMultiSort)
                    return builder.sort(List.of(SortOptions.of(x -> x.field(z -> z.order(sortOrder).field("total_story_points"))),
                            SortOptions.of(x -> x.field(z -> z.order(sortOrder).field("story_points")))));
                if (StringUtils.isNotEmpty(calculationNameWithSortField))
                    return builder.sort(sorting -> sorting.field(z -> z.field(calculationNameWithSortField).order(sortOrder)));
                return builder;
            })._toAggregation());
        } else {
            innerAggs.put("bucket_pagination", BucketSortAggregation.of(a -> {
                BucketSortAggregation.Builder builder = a
                        .from(0)
                        .size(isStackAgg || valuesOnly ? Integer.MAX_VALUE : acrossLimit);
                if (needMultiSort)
                    return builder.sort(List.of(SortOptions.of(x -> x.field(z -> z.order(sortOrder).field("total_story_points"))),
                            SortOptions.of(x -> x.field(z -> z.order(sortOrder).field("story_points")))));
                if (StringUtils.isNotEmpty(calculationNameWithSortField))
                    return builder.sort(sorting -> sorting.field(z -> z.field(calculationNameWithSortField).order(sortOrder)));
                return builder;
            })._toAggregation());
        }
    }

}
