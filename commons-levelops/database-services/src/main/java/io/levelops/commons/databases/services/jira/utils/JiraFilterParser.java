package io.levelops.commons.databases.services.jira.utils;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter.TicketCategorizationFilter;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraFilterParser {

    private final JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
    private final IntegrationService integService;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private final LoadingCache<Pair<String, String>, Optional<Long>> ingestedAtCache;

    @Autowired
    public JiraFilterParser(JiraFieldConditionsBuilder jiraFieldConditionsBuilder,
                            IntegrationService integService,
                            IntegrationTrackingService integrationTrackingService,
                            TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService) {
        this.jiraFieldConditionsBuilder = jiraFieldConditionsBuilder;
        this.integService = integService;
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
        ingestedAtCache = initIngestedAtCache(integrationTrackingService);
    }

    private static LoadingCache<Pair<String, String>, Optional<Long>> initIngestedAtCache(final IntegrationTrackingService integrationTrackingService) {
        return CacheBuilder.from("maximumSize=1000,expireAfterWrite=15m").build(CacheLoader.from(pair -> {
            String company = pair.getFirst();
            String integrationId = pair.getSecond();
            return integrationTrackingService.get(company, integrationId)
                    .map(IntegrationTracker::getLatestIngestedAt);
        }));
    }

    public Optional<Long> getIngestedAtFromCache(String company, String integrationId) {
        try {
            return ingestedAtCache.get(Pair.of(company, integrationId));
        } catch (ExecutionException e) {
            log.warn("Failed to load ingestedAt for company=" + company + ", integrationId=" + integrationId, e);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public static ImmutablePair<Long, Long> parseOrDateRange(@Nonnull Map<String, Object> orFilter, String field) {
        Map<String, String> dateRange = (Map<String, String>) Optional.ofNullable(orFilter)
                .map(f -> f.get(field))
                .map(Map.class::cast).orElse(Map.of());
        Long start = dateRange.get("$gte") != null ? Long.parseLong(dateRange.get("$gte")) - 1 : null;
        Long end = dateRange.get("$lte") != null ? Long.parseLong(dateRange.get("$lte")) + 1 : null;
        start = dateRange.get("$gt") != null ? Long.valueOf(dateRange.get("$gt")) : start;
        end = dateRange.get("$lt") != null ? Long.valueOf(dateRange.get("$lt")) : end;
        return ImmutablePair.of(start, end);
    }

    @SuppressWarnings("unchecked")
    public JiraOrFilter createOrFilter(Map<String, Object> orFilter) {
        // there shouldnt be prefixes in OR filter as the prefix is on the jira_or map itself
        ImmutablePair<Long, Long> createdRange = parseOrDateRange(orFilter, "issue_created_at");
        ImmutablePair<Long, Long> updatedRange = parseOrDateRange(orFilter, "issue_updated_at");
        ImmutablePair<Long, Long> resolutionRange = parseOrDateRange(orFilter, "issue_resolved_at");
        ImmutablePair<Long, Long> dueRange = parseOrDateRange(orFilter, "issue_due_at");
        ImmutablePair<Long, Long> ageRange = parseOrDateRange(orFilter, "age");
        Map<String, Map<String, String>> fieldSizeMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) orFilter.get("field_size"));
        return JiraOrFilter.builder()
                .issueCreatedRange(createdRange)
                .issueUpdatedRange(updatedRange)
                .issueDueRange(dueRange)
                .age(ageRange)
                .issueResolutionRange(resolutionRange)
                .assignees(getListOrDefault(orFilter, "assignees"))
                .components(getListOrDefault(orFilter, "components"))
                .extraCriteria(MoreObjects.firstNonNull(
                                getListOrDefault(orFilter, "hygiene_types"),
                                List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(JiraIssuesFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .keys(getListOrDefault(orFilter, "keys"))
                .priorities(getListOrDefault(orFilter, "priorities"))
                .statuses(getListOrDefault(orFilter, "statuses"))
                .assignees(getListOrDefault(orFilter, "assignees"))
                .reporters(getListOrDefault(orFilter, "reporters"))
                .fixVersions(getListOrDefault(orFilter, "fix_versions"))
                .versions(getListOrDefault(orFilter, "versions"))
                .issueTypes(getListOrDefault(orFilter, "issue_types"))
                .parentIssueTypes(getListOrDefault(orFilter, "parent_issue_types"))
                .epics(getListOrDefault(orFilter, "epics"))
                .parentKeys(getListOrDefault(orFilter, "parent_keys"))
                .projects(getListOrDefault(orFilter, "projects"))
                .components(getListOrDefault(orFilter, "components"))
                .labels(getListOrDefault(orFilter, "labels"))
                .fixVersions(getListOrDefault(orFilter, "fix_versions"))
                .versions(getListOrDefault(orFilter, "versions"))
                .links(getListOrDefault(orFilter, "links"))
                .storyPoints((Map<String, String>) orFilter.get("story_points"))
                .parentStoryPoints((Map<String, String>) orFilter.get("parent_story_points"))
                .customFields((Map<String, Object>) orFilter.get("custom_fields"))
                .missingFields(MapUtils.emptyIfNull(
                        (Map<String, Boolean>) orFilter.get("missing_fields")))
                .summary((String) orFilter.getOrDefault("summary", null))
                .fieldSize(fieldSizeMap)
                .partialMatch(jiraPartialMatchMap(orFilter, false))
                .sprintCount((Integer) orFilter.getOrDefault("sprint_count", 0))
                .sprintIds(getListOrDefault(orFilter, "sprint_ids"))
                .sprintNames(getListOrDefault(orFilter, "sprint_names"))
                .sprintFullNames(getListOrDefault(orFilter, "sprint_full_names"))
                .sprintStates(getListOrDefault(orFilter, "sprint_states"))
                .resolutions(getListOrDefault(orFilter, "resolutions"))
                .statusCategories(getListOrDefault(orFilter, "status_categories"))
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    public static class LatestIngestedAt {
        @Nonnull
        Long latestIngestedAt;
        @Nullable
        Map<String, Long> latestIngestedAtByIntegrationId;
    }

    public LatestIngestedAt getIngestedAt(String company, List<IntegrationType> type, DefaultListRequest filter) throws SQLException {
        //if filter has ingested_at use it (do not make call to get integration)
        if (filter.getFilter().get("ingested_at") != null) {
            return LatestIngestedAt.builder()
                    .latestIngestedAt(DateUtils.truncate(new Date(TimeUnit.SECONDS.toMillis(Long.valueOf(String.valueOf(filter.getFilter().get("ingested_at"))))), Calendar.DATE))
                    .build();
        }

        //Default IngestedAt is start of today's day
        long defaultIngestedAt = DateUtils.truncate(new Date(), Calendar.DATE);

        // filter out integrations that don't match the application type we want
        List<Integer> integrationIdsFilter = getListOrDefault(filter.getFilter(), "integration_ids").stream()
                .map(id -> Integer.valueOf(id))
                .collect(Collectors.toList());
        List<String> integrationIds;
        try {
            integrationIds = PaginationUtils.streamThrowingRuntime(0, 1, page ->
                            integService.listByFilter(company, null, type.stream().map(IntegrationType::toString).collect(Collectors.toList()), null, integrationIdsFilter, List.of(), page, 25).getRecords())
                    .map(Integration::getId)
                    .collect(Collectors.toList());
        } catch (RuntimeStreamException e) {
            throw new SQLException("Failed to filter integration ids", e);
        }

        // for each integration, find out the latest ingested at
        Map<String, Long> latestIngestedAtByIntegrationId = integrationIds.stream()
                .collect(Collectors.toMap(integrationId -> integrationId,
                        integrationId -> getIngestedAtFromCache(company, integrationId).orElse(defaultIngestedAt),
                        (a, b) -> b));

        // If no integration is found, return the default ingested at.
        // Otherwise, for backward compatibility, single out the first integration's latest ingested at.
        Long latestIngestedAt = IterableUtils.getFirst(integrationIds)
                .map(latestIngestedAtByIntegrationId::get)
                .orElse(defaultIngestedAt);
        return LatestIngestedAt.builder()
                .latestIngestedAt(latestIngestedAt)
                .latestIngestedAtByIntegrationId(latestIngestedAtByIntegrationId)
                .build();
    }

    public JiraIssuesFilter createFilter(@Nonnull String company,
                                         @Nonnull DefaultListRequest filter,
                                         @Nullable JiraIssuesFilter.CALCULATION calc,
                                         @Nullable JiraIssuesFilter.DISTINCT across,
                                         @Nullable String customAcross,
                                         @Nullable String aggInterval,
                                         boolean withPrefix) throws SQLException, BadRequestException {
        return createFilter(company, filter, calc, across, customAcross, aggInterval, withPrefix, true);
    }

    public JiraIssuesFilter createFilter(@Nonnull String company,
                                         @Nonnull DefaultListRequest filter,
                                         @Nullable JiraIssuesFilter.CALCULATION calc,
                                         @Nullable JiraIssuesFilter.DISTINCT across,
                                         @Nullable String customAcross,
                                         @Nullable String aggInterval,
                                         boolean withPrefix,
                                         boolean getLatestIngestedAtDate) throws SQLException, BadRequestException {
        String prefix = withPrefix ? "jira_" : "";
        return createFilter(company, filter, calc, across, customAcross, aggInterval, prefix, true);
    }

    public JiraIssuesFilter createFilter(@Nonnull String company,
                                         @Nonnull DefaultListRequest filter,
                                         @Nullable JiraIssuesFilter.CALCULATION calc,
                                         @Nullable JiraIssuesFilter.DISTINCT across,
                                         @Nullable String customAcross,
                                         @Nullable String aggInterval,
                                         String prefix,
                                         boolean getLatestIngestedAtDate) throws SQLException, BadRequestException {
        return createFilter(company, filter, calc, across, customAcross, aggInterval, prefix, getLatestIngestedAtDate, true);
    }

    @SuppressWarnings("unchecked")
    public JiraIssuesFilter createFilter(@Nonnull String company,
                                         @Nonnull DefaultListRequest filter,
                                         @Nullable JiraIssuesFilter.CALCULATION calc,
                                         @Nullable JiraIssuesFilter.DISTINCT across,
                                         @Nullable String customAcross,
                                         @Nullable String aggInterval,
                                         String prefix,
                                         boolean getLatestIngestedAtDate,
                                         boolean needVelocityStagesFilter) throws SQLException, BadRequestException {
        List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
        ImmutablePair<Long, Long> createdRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "issue_created_at");
        ImmutablePair<Long, Long> updatedRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "issue_updated_at");
        ImmutablePair<Long, Long> resolutionRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "issue_resolved_at");
        ImmutablePair<Long, Long> releaseRange = DefaultListRequestUtils.getTimeRange(filter, prefix,  "released_in");

        ImmutablePair<Long, Long> dueRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "issue_due_at");
        ImmutablePair<Long, Long> ageRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "age");
        ImmutablePair<Long, Long> snapshotRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "snapshot_range");
        ImmutablePair<Long, Long> assigneesRange = DefaultListRequestUtils.getTimeRange(filter, prefix, "assignees_range");

        final Map<String, String> stateTransition = MapUtils.emptyIfNull(
                (Map<String, String>) filter.getFilter().get(prefix + "state_transition"));
        Map<String, Map<String, String>> fieldSizeMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get(prefix + "field_size"));
        Map<String, Map<String, String>> partialMatchMap = jiraPartialMatchMap(filter.getFilter(), StringUtils.isNotEmpty(prefix));

        validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
        validatePartialMatchFilter(company, integrationIds, partialMatchMap);

        String fromState = null;
        String toState = null;
        if (MapUtils.isNotEmpty(stateTransition)) {
            fromState = stateTransition.get("from_state");
            toState = stateTransition.get("to_state");
            if (StringUtils.isEmpty(fromState) || StringUtils.isEmpty(toState)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "missing or empty value of 'from_state' or 'to_state' in 'state_transition'");
            }
        }

        String ticketCategorizationSchemeId = filter.getFilterValue("ticket_categorization_scheme", String.class).orElse(null);
        List<TicketCategorizationFilter> ticketCategorizationFilters = generateTicketCategorizationFilters(company, ticketCategorizationSchemeId);

        LatestIngestedAt latestIngestedAt = getIngestedAt(company, List.of(IntegrationType.JIRA), filter);

        JiraIssuesFilter.JiraIssuesFilterBuilder jiraIssuesFilterBuilder = getJiraIssuesFilterBuilderWithExcludedFilter(filter, prefix)
                .orFilter(createOrFilter(filter.<String, Object>getFilterValueAsMap(prefix + "or").orElse(Map.of())))
                .ingestedAt(latestIngestedAt.getLatestIngestedAt())
                .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                .hygieneCriteriaSpecs(getHygieneCriteriaSpecs(filter.getFilter()))
                .issueCreatedRange(createdRange)
                .issueDueRange(dueRange)
                .issueUpdatedRange(updatedRange)
                .issueResolutionRange(resolutionRange)
                .issueReleasedRange(releaseRange)
                .age(ageRange)
                .snapshotRange(snapshotRange)
                .calculation(calc)
                .across(across)
                .filterAcrossValues(filter.getFilterAcrossValues())
                .acrossLimit(filter.getAcrossLimit())
                .aggInterval(aggInterval)
                .customAcross(customAcross)
                .customStacks(getListOrDefault(filter.getFilter(), prefix + "custom_stacks"))
                .extraCriteria(MoreObjects.firstNonNull(
                                getListOrDefault(filter.getFilter(), prefix + "hygiene_types"),
                                List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(JiraIssuesFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .keys(getListOrDefault(filter.getFilter(), prefix + "keys"))
                .priorities(getListOrDefault(filter.getFilter(), prefix + "priorities"))
                .statuses(getListOrDefault(filter.getFilter(), prefix + "statuses"))
                .assignees(getListOrDefault(filter.getFilter(), prefix + "assignees"))
                .assigneeDisplayNames(getListOrDefault(filter.getFilter(), prefix + "assignee_display_names"))
                .historicalAssignees(getListOrDefault(filter.getFilter(), prefix + "historical_assignees"))
                .timeAssignees(getListOrDefault(filter.getFilter(), prefix + "time_assignees"))
                .reporters(getListOrDefault(filter.getFilter(), prefix + "reporters"))
                .fixVersions(getListOrDefault(filter.getFilter(), prefix + "fix_versions"))
                .versions(getListOrDefault(filter.getFilter(), prefix + "versions"))
                .stages(getListOrDefault(filter.getFilter(), prefix + "stages"))
                .velocityStages(getListOrDefault(filter.getFilter(), prefix + "velocity_stages"))
                .issueTypes(getListOrDefault(filter.getFilter(), prefix + "issue_types"))
                .integrationIds(integrationIds)
                .epics(getListOrDefault(filter.getFilter(), prefix + "epics"))
                .parentKeys(getListOrDefault(filter.getFilter(), prefix + "parent_keys"))
                .parentIssueTypes(getListOrDefault(filter.getFilter(), prefix + "parent_issue_types"))
                .parentLabels(getListOrDefault(filter.getFilter(), prefix + "parent_labels"))
                .projects(getListOrDefault(filter.getFilter(), prefix + "projects"))
                .components(getListOrDefault(filter.getFilter(), prefix + "components"))
                .labels(getListOrDefault(filter.getFilter(), prefix + "labels"))
                .firstAssignees(getListOrDefault(filter.getFilter(), prefix + "first_assignees"))
                .fixVersions(getListOrDefault(filter.getFilter(), prefix + "fix_versions"))
                .versions(getListOrDefault(filter.getFilter(), prefix + "versions"))
                .storyPoints((Map<String, String>) filter.getFilter().get(prefix + "story_points"))
                .parentStoryPoints((Map<String, String>) filter.getFilter().get(prefix + "parent_story_points"))
                .fromState(fromState)
                .toState(toState)
                .customFields((Map<String, Object>) filter.getFilter().get(prefix + "custom_fields"))
                .missingFields(MapUtils.emptyIfNull(
                        (Map<String, Boolean>) filter.getFilter().get(prefix + "missing_fields")))
                .summary((String) filter.getFilter().getOrDefault(prefix + "summary", null))
                .fieldSize(fieldSizeMap)
                .partialMatch(partialMatchMap)
                .sprintCount((Integer) filter.getFilter().getOrDefault(prefix + "sprint_count", 0))
                .sprintIds(getListOrDefault(filter.getFilter(), prefix + "sprint_ids"))
                .sprintNames(getListOrDefault(filter.getFilter(), prefix + "sprint_names"))
                .sprintFullNames(getListOrDefault(filter.getFilter(), prefix + "sprint_full_names"))
                .sprintStates(getListOrDefault(filter.getFilter(), prefix + "sprint_states"))
                .resolutions(getListOrDefault(filter.getFilter(), prefix + "resolutions"))
                .statusCategories(getListOrDefault(filter.getFilter(), prefix + "status_categories"))
                .ticketCategories(filter.<String>getFilterValueAsList(prefix + "ticket_categories").orElse(null))
                .ticketCategorizationFilters(ticketCategorizationFilters)
                .ticketCategorizationSchemeId(ticketCategorizationSchemeId)
                .assigneesDateRange(assigneesRange)
                .excludeVelocityStages(getListOrDefault(filter.getFilter(), "excludeVelocityStages"))
                .calculateSingleState(MapUtils.getBoolean(filter.getFilter(), "calculateSingleState", false))
                .links(getListOrDefault(filter.getFilter(), prefix + "links"))
                .filterByLastSprint((Boolean) filter.getFilter().get(prefix + "last_sprint"))
                .includeSolveTime((Boolean) filter.getFilter().getOrDefault(prefix + "include_solve_time", false))
                .sprintMappingSprintIds(getListOrDefault(filter.getFilter(), prefix + "sprint_mapping_sprint_ids"))
                .sprintMappingIgnorableIssueType((Boolean) filter.getFilter().get(prefix + "sprint_mapping_ignorable_issue_type"))
                .sprintMappingSprintCompletedAtAfter((Long) filter.getFilter().get(prefix + "sprint_mapping_sprint_completed_at_after"))
                .sprintMappingSprintCompletedAtBefore((Long) filter.getFilter().get(prefix + "sprint_mapping_sprint_completed_at_before"))
                .sprintMappingSprintStartedAtAfter((Long) filter.getFilter().get(prefix + "sprint_mapping_sprint_started_at_after"))
                .sprintMappingSprintStartedAtBefore((Long) filter.getFilter().get(prefix + "sprint_mapping_sprint_started_at_before"))
                .sprintMappingSprintPlannedCompletedAtAfter((Long) filter.getFilter().get(prefix + "sprint_mapping_sprint_planned_completed_at_after"))
                .sprintMappingSprintPlannedCompletedAtBefore((Long) filter.getFilter().get(prefix + "sprint_mapping_sprint_planned_completed_at_before"))
                .sprintMappingSprintNames(getListOrDefault(filter.getFilter(), prefix + "sprint_mapping_sprint_names"))
                .sprintMappingSprintNameStartsWith((String) filter.getFilter().get(prefix + "sprint_mapping_sprint_name_starts_with"))
                .sprintMappingSprintNameEndsWith((String) filter.getFilter().get(prefix + "sprint_mapping_sprint_name_ends_with"))
                .sprintMappingSprintNameContains((String) filter.getFilter().get(prefix + "sprint_mapping_sprint_name_contains"))
                .sprintMappingExcludeSprintNames(getListOrDefault(filter.getFilter(), prefix + "sprint_mapping_exclude_sprint_names"))
                .sprintMappingSprintState((String) filter.getFilter().get(prefix + "sprint_mapping_sprint_state"))
                .sort(SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())))
                .page(filter.getPage())
                .pageSize(filter.getPageSize());
        if (!needVelocityStagesFilter) {
            jiraIssuesFilterBuilder = jiraIssuesFilterBuilder.velocityStages(Collections.emptyList())
                    .excludeVelocityStages(Collections.emptyList());
        }
        return jiraIssuesFilterBuilder.build();
    }

    public JiraIssuesFilter createFilterFromConfig(String company, DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, JiraIssuesFilter.CALCULATION calc, JiraIssuesFilter.DISTINCT across, String customAcross,
                                                   String aggInterval, boolean withPrefix) throws SQLException, BadRequestException {
        JiraIssuesFilter jiraIssuesFilter = createFilter(company, filter, calc, across, customAcross, aggInterval, withPrefix);
        JiraIssuesFilter.JiraIssuesFilterBuilder filterBuilder = jiraIssuesFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "jira_keys":
                    filterBuilder.keys(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getKeys()));
                    break;
                case "jira_priorities":
                    filterBuilder.priorities(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getPriorities()));
                    break;
                case "jira_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getStatuses()));
                    break;
                case "jira_assignees":
                    filterBuilder.assignees(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getAssignees()));
                    break;
                case "jira_reporters":
                    filterBuilder.reporters(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getReporters()));
                    break;
                case "jira_issue_types":
                    filterBuilder.issueTypes(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getIssueTypes()));
                    break;
                case "jira_epics":
                    filterBuilder.epics(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getEpics()));
                    break;
                case "jira_parent_keys":
                    filterBuilder.parentKeys(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getParentKeys()));
                    break;
                case "jira_projects":
                    filterBuilder.projects(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getProjects()));
                    break;
                case "jira_components":
                    filterBuilder.components(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getComponents()));
                    break;
                case "jira_labels":
                    filterBuilder.labels(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getLabels()));
                    break;
                case "assignee_display_names":
                    filterBuilder.labels(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getAssigneeDisplayNames()));
                    break;
                case "historical_assignees":
                    filterBuilder.labels(getIntersection(getRowValue(row, column.getValue()), jiraIssuesFilter.getHistoricalAssignees()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    @Nullable
    public List<TicketCategorizationFilter> generateTicketCategorizationFilters(String company, @Nullable String ticketCategorizationSchemeId) throws BadRequestException {
        if (StringUtils.isEmpty(ticketCategorizationSchemeId)) {
            return null;
        }
        TicketCategorizationScheme ticketCategorizationScheme = ticketCategorizationSchemeDatabaseService.get(company, UUID.fromString(ticketCategorizationSchemeId).toString())
                .orElseThrow(() -> new BadRequestException("No ticket categorization scheme with this id=" + ticketCategorizationSchemeId));
        if (ticketCategorizationScheme.getConfig() == null || MapUtils.isEmpty(ticketCategorizationScheme.getConfig().getCategories())) {
            return null;
        }
        return generateTicketCategorizationFilters(company, ticketCategorizationScheme.getConfig().getCategories().values().stream().collect(Collectors.toList()));
    }

    public List<TicketCategorizationFilter> generateTicketCategorizationFilters(String company, @Nullable List<TicketCategorizationScheme.TicketCategorization> categories) throws BadRequestException {
        return categories.stream()
                .filter(category -> StringUtils.isNotEmpty(category.getName()))
                .map(category -> {
                    try {
                        // convert category filter into an instance of JiraIssueFilter
                        Map<String, Object> categoryFilter = new HashMap<>(MapUtils.emptyIfNull(category.getFilter()));
                        categoryFilter.remove("ticket_categorization_scheme"); // make sure we don't have an infinite loop
                        categoryFilter.remove("ticket_categories");
                        DefaultListRequest categoryFilterRequest = DefaultListRequest.builder()
                                .filter(categoryFilter)
                                .build();
                        return TicketCategorizationFilter.builder()
                                .id(category.getId())
                                .index(category.getIndex())
                                .name(category.getName())
                                .filter(createFilter(company, categoryFilterRequest, null, null, null, null, false))
                                .build();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        log.warn("Failed to parse ticket category filter from category=" + category.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<String> getListOrDefault(Map<String, Object> filter, String key) {
        try {
            var values = (Collection) filter.getOrDefault(key, List.of());
            if (CollectionUtils.isEmpty(values)) {
                return List.of();
            }
            // handle collections with non-string values
            if (CollectionUtils.isNotEmpty(values) && !(values.iterator().next() instanceof String)) {
                Function<Object, String> toStringFunction = "integration_ids".equalsIgnoreCase(key) ? (item) -> String.valueOf(item) : (item) -> item.toString();
                return (List<String>) values.stream().filter(item -> item != null).map(toStringFunction).collect(Collectors.toList());
            }
            // to handle list or sets
            return (List<String>) values.stream().collect(Collectors.toList());
        } catch (ClassCastException e) {
            log.error("Unable to get List<String> out the key '{}' in the filters: {}", key, filter, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

    public void validateFieldSizeFilter(String company, List<String> integrationIds, Map<String, Map<String, String>> fieldSizes) {
        if (MapUtils.isEmpty(fieldSizes)) {
            return;
        }
        ArrayList<String> fieldSizeKeys = new ArrayList<>(fieldSizes.keySet());
        try {
            String unknownField = jiraFieldConditionsBuilder.checkFieldsPresent(company, integrationIds, fieldSizeKeys);
            if (unknownField != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, unknownField + " is not valid field for size based filter");
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while fetching list of jira fields");
        }
    }

    private void validatePartialMatchFilter(String company, List<String> integrationIds,
                                            Map<String, Map<String, String>> partialMatchMap) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        try {
            Set<String> jiraFieldKeys = jiraFieldConditionsBuilder.getDbJiraFields(company, integrationIds, partialMatchKeys).stream()
                    .map(DbJiraField::getFieldKey).collect(Collectors.toSet());
            List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                    .filter(key -> !JiraIssueService.PARTIAL_MATCH_COLUMNS.contains(key)
                            && !JiraIssueService.PARTIAL_MATCH_ARRAY_COLUMNS.contains(key)
                            && !jiraFieldKeys.contains(key))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
                log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                        + " are not valid fields for partial match based filter");
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while fetching list of jira fields");
        }
    }

    @SuppressWarnings("unchecked")
    private JiraIssuesFilter.JiraIssuesFilterBuilder getJiraIssuesFilterBuilderWithExcludedFilter(
            DefaultListRequest filter, String prefix) {
        final Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        return JiraIssuesFilter.builder()
                .excludeKeys(getListOrDefault(excludedFields, prefix + "keys"))
                .excludeVersions(getListOrDefault(excludedFields, prefix + "versions"))
                .excludeStatuses(getListOrDefault(excludedFields, prefix + "statuses"))
                .excludeAssignees(getListOrDefault(excludedFields, prefix + "assignees"))
                .excludeTimeAssignees(getListOrDefault(excludedFields, prefix + "time_assignees"))
                .excludeReporters(getListOrDefault(excludedFields, prefix + "reporters"))
                .excludePriorities(getListOrDefault(excludedFields, prefix + "priorities"))
                .excludeIssueTypes(getListOrDefault(excludedFields, prefix + "issue_types"))
                .excludeFixVersions(getListOrDefault(excludedFields, prefix + "fix_versions"))
                .excludeVersions(getListOrDefault(excludedFields, prefix + "versions"))
                .excludeIntegrationIds(getListOrDefault(excludedFields, "integration_ids"))
                .excludeProjects(getListOrDefault(excludedFields, prefix + "projects"))
                .excludeComponents(getListOrDefault(excludedFields, prefix + "components"))
                .excludeLabels(getListOrDefault(excludedFields, prefix + "labels"))
                .excludeEpics(getListOrDefault(excludedFields, prefix + "epics"))
                .excludeParentKeys(getListOrDefault(excludedFields, prefix + "parent_keys"))
                .excludeParentIssueTypes(getListOrDefault(excludedFields, prefix + "parent_issue_types"))
                .excludeParentLabels(getListOrDefault(excludedFields, prefix + "parent_labels"))
                .excludeStages(getListOrDefault(excludedFields, prefix + "stages"))
                .excludeVelocityStages(getListOrDefault(excludedFields, prefix + "velocity_stages"))
                .excludeResolutions(getListOrDefault(excludedFields, prefix + "resolutions"))
                .excludeStatusCategories(getListOrDefault(excludedFields, prefix + "status_categories"))
                .excludeSprintIds(getListOrDefault(excludedFields, prefix + "sprint_ids"))
                .excludeSprintNames(getListOrDefault(excludedFields, prefix + "sprint_names"))
                .excludeSprintFullNames(getListOrDefault(excludedFields, prefix + "sprint_full_names"))
                .excludeSprintStates(getListOrDefault(excludedFields, prefix + "sprint_states"))
                .excludeLinks(getListOrDefault(excludedFields, prefix + "links"))
                .excludeCustomFields(MapUtils.emptyIfNull(
                        (Map<String, Object>) excludedFields.get(prefix + "custom_fields")));
    }

    private Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> getHygieneCriteriaSpecs(Map<String, Object> filter) {
        try {
            Object idleDays = filter.get(JiraIssuesFilter.EXTRA_CRITERIA.idle.toString());
            Object poorDescCt = filter.get(JiraIssuesFilter.EXTRA_CRITERIA.poor_description.toString());
            Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> specs = new HashMap<>();
            if (idleDays != null) {
                specs.put(JiraIssuesFilter.EXTRA_CRITERIA.idle, idleDays);
            }
            if (poorDescCt != null) {
                specs.put(JiraIssuesFilter.EXTRA_CRITERIA.poor_description, poorDescCt);
            }
            return specs;
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data.");
        }
    }

    private List<String> getIntersection(List<String> rowValues, List<String> filterValues) {
        if (rowValues.size() == 0) {
            return filterValues;
        } else if (filterValues.size() == 0) {
            return rowValues;
        } else {
            return new ArrayList<>(CollectionUtils.intersection(rowValues, filterValues));
        }
    }

    private List<String> getRowValue(ConfigTable.Row row, ConfigTable.Column column) {
        String rowValue = row.getValues().get(column.getId());
        if (column.getMultiValue()) {
            String sanitizedRowValue = rowValue.replaceAll("^\\[|]$", "").replaceAll("\"", "");
            return Arrays.asList(sanitizedRowValue.split(","));
        }
        return Collections.singletonList(rowValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> jiraPartialMatchMap(Map<String, Object> filterObj, boolean withPrefix) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filterObj.get("partial_match"));
        if (withPrefix) {
            return partialMatchMap.entrySet().stream()
                    .filter(partialField -> partialField.getKey().startsWith("jira_"))
                    .collect(Collectors.toMap(
                            stringMapEntry -> stringMapEntry.getKey().replaceFirst("^jira_", ""),
                            Map.Entry::getValue));
        }
        return partialMatchMap;
    }
}
