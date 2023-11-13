package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfMaps;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.commons.caching.CacheHashUtils.hashDataUsingToString;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraIssuesFilter.JiraIssuesFilterBuilder.class)
public class JiraIssuesFilter {
    @JsonProperty("across")
    DISTINCT across;
    @JsonProperty("filter_across_values")
    Boolean filterAcrossValues;
    @JsonProperty("across_limit")
    Integer acrossLimit;
    @JsonProperty("agg_interval")
    String aggInterval;
    @JsonProperty("custom_across")
    String customAcross;
    @JsonProperty("custom_stacks")
    List<String> customStacks;
    @JsonProperty("calculate_single_state")
    Boolean calculateSingleState;

    @JsonProperty("ingested_at")
    Long ingestedAt; // contains the ingestedAt of the first integration (for backward compatibility but still required even when ingestedAtByIntegrationId is provided)
    @JsonProperty("ingested_at_by_integration_id")
    Map<String, Long> ingestedAtByIntegrationId;
    @JsonProperty("from_state")
    String fromState;
    @JsonProperty("to_state")
    String toState;
    @JsonProperty("calculation")
    CALCULATION calculation;
    @JsonProperty("extra_criteria")
    List<EXTRA_CRITERIA> extraCriteria;

    @JsonProperty("ids")
    List<UUID> ids;
    @JsonProperty("keys")
    List<String> keys;
    @JsonProperty("priorities")
    List<String> priorities;
    @JsonProperty("statuses")
    List<String> statuses;
    @JsonProperty("assignees")
    List<String> assignees;
    @JsonProperty("assignee_display_names")
    List<String> assigneeDisplayNames;
    @JsonProperty("historical_assignees")
    List<String> historicalAssignees;
    @JsonProperty("un_assigned")
    Boolean unAssigned;
    @JsonProperty("reporters")
    List<String> reporters;
    @JsonProperty("reporter_display_names")
    List<String> reporterDisplayNames;
    @JsonProperty("issue_types")
    List<String> issueTypes;
    @JsonProperty("fix_versions")
    List<String> fixVersions;
    @JsonProperty("versions")
    List<String> versions;
    @JsonProperty("integration_ids")
    List<String> integrationIds;
    @JsonProperty("projects")
    List<String> projects;
    @JsonProperty("components")
    List<String> components;
    @JsonProperty("labels")
    List<String> labels;
    @JsonProperty("stages")
    List<String> stages;
    @JsonProperty("velocity_stages")
    List<String> velocityStages;
    @JsonProperty("epics")
    List<String> epics;
    @JsonProperty("parent_keys")
    List<String> parentKeys;
    @JsonProperty("parent_issue_types")
    List<String> parentIssueTypes;
    @JsonProperty("parent_labels")
    List<String> parentLabels;
    @JsonProperty("sprint_ids")
    List<String> sprintIds;
    @JsonProperty("sprint_names")
    List<String> sprintNames;
    @JsonProperty("sprint_full_names")
    List<String> sprintFullNames;
    @JsonProperty("sprint_states")
    List<String> sprintStates;
    @JsonProperty("links")
    List<String> links;
    @JsonProperty("sprint_count")
    Integer sprintCount;
    @JsonProperty("resolutions")
    List<String> resolutions;
    @JsonProperty("status_categories")
    List<String> statusCategories;
    @JsonProperty("time_assignees")
    List<String> timeAssignees;//this is only used in assignee time report
    @JsonProperty("time_statuses")
    List<String> timeStatuses;//this is only used in status time report
    @JsonProperty("exclude_time_assignees")
    List<String> excludeTimeAssignees;//this is only used in assignee time report
    @JsonProperty("issue_created_range")
    ImmutablePair<Long, Long> issueCreatedRange;
    @JsonProperty("issue_updated_range")
    ImmutablePair<Long, Long> issueUpdatedRange;
    @JsonProperty("issue_resolution_range")
    ImmutablePair<Long, Long> issueResolutionRange;
    @JsonProperty("issue_released_range")
    ImmutablePair<Long, Long> issueReleasedRange;
    @JsonProperty("issue_due_range")
    ImmutablePair<Long, Long> issueDueRange;
    @JsonProperty("snapshot_range")
    ImmutablePair<Long, Long> snapshotRange;
    @JsonProperty("age")
    ImmutablePair<Long, Long> age;
    @JsonProperty("custom_fields")
    Map<String, Object> customFields;
    @JsonProperty("story_points")
    Map<String, String> storyPoints;
    @JsonProperty("parent_story_points")
    Map<String, String> parentStoryPoints;
    @JsonProperty("missing_fields")
    Map<String, Boolean> missingFields;
    @JsonProperty("summary")
    String summary;
    @JsonProperty("filter_by_last_sprint")
    Boolean filterByLastSprint;
    @JsonProperty("include_solve_time")
    Boolean includeSolveTime;
    @JsonProperty("is_active")
    Boolean isActive;
    @JsonProperty("ignore_o_u")
    Boolean ignoreOU;
    @JsonProperty("field_size")
    Map<String, Map<String, String>> fieldSize;
    @JsonProperty("partial_match")
    Map<String, Map<String, String>> partialMatch;
    @JsonProperty("hygiene_criteria_specs")
    Map<EXTRA_CRITERIA, Object> hygieneCriteriaSpecs;
    @JsonProperty("sort")
    Map<String, SortingOrder> sort;
    @JsonProperty("ticket_categories")
    List<String> ticketCategories;
    @JsonProperty("ticket_categorization_scheme_id")
    String ticketCategorizationSchemeId;
    @JsonProperty("ticket_categorization_filters")
    @JsonIgnore
    List<TicketCategorizationFilter> ticketCategorizationFilters; // ticket categorization filters (metadata - DO NOT CACHE)
    @JsonProperty("assignees_date_range")
    ImmutablePair<Long, Long> assigneesDateRange;
    // region sprint mappings
    @JsonProperty("sprint_mapping_sprint_ids")
    List<String> sprintMappingSprintIds;
    @JsonProperty("sprint_mapping_ignorable_issue_type")
    Boolean sprintMappingIgnorableIssueType;
    @JsonProperty("sprint_mapping_sprint_completed_at_after")
    Long sprintMappingSprintCompletedAtAfter;
    @JsonProperty("sprint_mapping_sprint_completed_at_before")
    Long sprintMappingSprintCompletedAtBefore;
    @JsonProperty("sprint_mapping_sprint_started_at_after")
    Long sprintMappingSprintStartedAtAfter;
    @JsonProperty("sprint_mapping_sprint_started_at_before")
    Long sprintMappingSprintStartedAtBefore;
    @JsonProperty("sprint_mapping_sprint_planned_completed_at_after")
    Long sprintMappingSprintPlannedCompletedAtAfter;
    @JsonProperty("sprint_mapping_sprint_planned_completed_at_before")
    Long sprintMappingSprintPlannedCompletedAtBefore;
    @JsonProperty("sprint_mapping_sprint_names")
    List<String> sprintMappingSprintNames;
    @JsonProperty("sprint_mapping_sprint_name_starts_with")
    String sprintMappingSprintNameStartsWith;
    @JsonProperty("sprint_mapping_sprint_name_ends_with")
    String sprintMappingSprintNameEndsWith;
    @JsonProperty("sprint_mapping_sprint_name_contains")
    String sprintMappingSprintNameContains;
    @JsonProperty("sprint_mapping_exclude_sprint_names")
    List<String> sprintMappingExcludeSprintNames;
    @JsonProperty("sprint_mapping_sprint_state")
    String sprintMappingSprintState;
    //endregion

    @JsonProperty("exclude_keys")
    List<String> excludeKeys;
    @JsonProperty("exclude_priorities")
    List<String> excludePriorities;
    @JsonProperty("exclude_statuses")
    List<String> excludeStatuses;
    @JsonProperty("exclude_assignees")
    List<String> excludeAssignees;
    @JsonProperty("exclude_reporters")
    List<String> excludeReporters;
    @JsonProperty("exclude_issue_types")
    List<String> excludeIssueTypes;
    @JsonProperty("exclude_fix_versions")
    List<String> excludeFixVersions;
    @JsonProperty("exclude_versions")
    List<String> excludeVersions;
    @JsonProperty("exclude_integration_ids")
    List<String> excludeIntegrationIds;
    @JsonProperty("exclude_projects")
    List<String> excludeProjects;
    @JsonProperty("exclude_components")
    List<String> excludeComponents;
    @JsonProperty("exclude_labels")
    List<String> excludeLabels;
    @JsonProperty("exclude_epics")
    List<String> excludeEpics;
    @JsonProperty("exclude_parent_keys")
    List<String> excludeParentKeys;
    @JsonProperty("exclude_parent_issue_types")
    List<String> excludeParentIssueTypes;
    @JsonProperty("exclude_parent_labels")
    List<String> excludeParentLabels;
    @JsonProperty("exclude_stages")
    List<String> excludeStages;
    @JsonProperty("exclude_velocity_stages")
    List<String> excludeVelocityStages;
    @JsonProperty("exclude_resolutions")
    List<String> excludeResolutions;
    @JsonProperty("exclude_status_categories")
    List<String> excludeStatusCategories;
    @JsonProperty("exclude_sprint_ids")
    List<String> excludeSprintIds;
    @JsonProperty("exclude_sprint_names")
    List<String> excludeSprintNames;
    @JsonProperty("exclude_sprint_full_names")
    List<String> excludeSprintFullNames;
    @JsonProperty("exclude_sprint_states")
    List<String> excludeSprintStates;
    @JsonProperty("first_assignees")
    List<String> firstAssignees;
    @JsonProperty("first_assignee_display_names")
    List<String> firstAssigneeDisplayNames;
    @JsonProperty("exclude_links")
    List<String> excludeLinks;

    List<String> linkedIssueKeys;
    @JsonProperty("exclude_custom_fields")
    Map<String, Object> excludeCustomFields;

    /*
    This will be used only in Faceted Search. It will NOT be used in UI/Server Api.
    Before using in Server Api, add it to generateCacheRawString() & also to fromDefaultListRequest()
     */
    @JsonProperty("integration_id_by_issue_updated_range")
    Map<Integer, ImmutablePair<Long, Long>> integrationIdByIssueUpdatedRange;

    @JsonProperty("or_filter")
    JiraOrFilter orFilter;

    // Note: most aggs do not support pagination
    @JsonProperty("page")
    Integer page;
    @JsonProperty("page_size")
    Integer pageSize;

    public Map<EXTRA_CRITERIA, Object> getHygieneCriteriaSpecs() {
        return MapUtils.emptyIfNull(hygieneCriteriaSpecs);
    }

    public enum DISTINCT {
        priority,
        project,
        status,
        assignee,
        first_assignee,
        issue_type,
        component,
        custom_field,
        label,
        fix_version,
        version,
        reporter,
        epic,
        velocity_stage,
        parent,
        status_category,
        //these are across time
        issue_created,
        issue_updated,
        issue_due,
        issue_due_relative,
        issue_resolved,
        resolution,
        trend,
        sprint,
        ticket_category,
        stage,
        sprint_mapping,
        none;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        hops, // min, max, median and ticket count
        bounces, // min, max, median and ticket count
        response_time, // min, max, median (COALESCE(first_comment_at,extract(epoch from now())) - issue_created_at) and ticket count
        resolution_time, // min, max, median (COALESCE(issue_resolved_at,extract(epoch from now())) - issue_created_at) and ticket count
        assign_to_resolve,
        state_transition_time,
        stage_bounce_report,
        stage_times_report,
        velocity_stage_times_report,
        ticket_count, // just a count
        age, // min, max , median , 90th percentile and ticket count
        story_points,
        assignees,
        sprint_mapping,
        sprint_mapping_count,
        priority;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    //used to be called hygiene queries
    public enum EXTRA_CRITERIA {
        idle,
        no_due_date,
        no_assignee,
        no_components,
        poor_description,
        inactive_assignees,
        //the hygiene types below this line are special as they are not 'hygiene' queries
        missed_response_time,
        missed_resolution_time;

        public static EXTRA_CRITERIA fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(EXTRA_CRITERIA.class, st);
        }
    }

    public enum MISSING_BUILTIN_FIELD {
        priority, status, assignee, reporter, component, label, fix_version, version, epic, project, first_assignee;

        public static MISSING_BUILTIN_FIELD fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(MISSING_BUILTIN_FIELD.class, st);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TicketCategorizationFilter.TicketCategorizationFilterBuilder.class)
    public static class TicketCategorizationFilter {
        @JsonProperty("id")
        String id;
        @JsonProperty("name")
        String name;
        @JsonProperty("index")
        Integer index;
        @JsonProperty("filter")
        JiraIssuesFilter filter;
//        @JsonProperty("issue_inheritance_mode")
//        IssueInheritanceMode issueInheritanceMode;
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        hashDataUsingToString(dataToHash, "across", across);
        hashDataUsingToString(dataToHash, "acrossLimit", acrossLimit);
        hashDataUsingToString(dataToHash, "aggInterval", aggInterval);
        hashDataUsingToString(dataToHash, "customAcross", customAcross);
        hashData(dataToHash, "customStacks", customStacks);
        hashData(dataToHash, "fromState", fromState);
        hashData(dataToHash, "summary", summary);
        hashData(dataToHash, "toState", toState);
        hashDataUsingToString(dataToHash, "calculation", calculation);
        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> critStr = extraCriteria.stream().map(Enum::toString).sorted().collect(Collectors.toList());
            dataToHash.append(",extraCriteria=").append(String.join(",", critStr));
        }
        hashData(dataToHash, "ids", CollectionUtils.emptyIfNull(ids).stream().map(Objects::toString).collect(Collectors.toList()));
        hashData(dataToHash, "keys", keys);
        hashData(dataToHash, "parentKeys", parentKeys);
        hashData(dataToHash, "parentIssueTypes", parentIssueTypes);
        hashData(dataToHash, "parentLabels", parentLabels);
        hashData(dataToHash, "priorities", priorities);
        hashData(dataToHash, "stages", stages);
        hashData(dataToHash, "velocityStages", velocityStages);
        hashData(dataToHash, "statuses", statuses);
        hashData(dataToHash, "assignees", assignees);
        hashData(dataToHash, "reporters", reporters);
        hashData(dataToHash, "issueTypes", issueTypes);
        hashData(dataToHash, "versions", versions);
        hashData(dataToHash, "fixVersions", fixVersions);
        hashData(dataToHash, "integrationIds", integrationIds);
        hashData(dataToHash, "projects", projects);
        hashData(dataToHash, "components", components);
        hashData(dataToHash, "labels", labels);
        hashData(dataToHash, "epics", epics);
        hashData(dataToHash, "sprintCount", sprintCount);
        hashData(dataToHash, "sprintIds", sprintIds);
        hashData(dataToHash, "sprintNames", sprintNames);
        hashData(dataToHash, "sprintFullNames", sprintFullNames);
        hashData(dataToHash, "sprintStates", sprintStates);
        hashData(dataToHash, "resolutions", resolutions);
        hashData(dataToHash, "statusCategories", statusCategories);
        hashData(dataToHash, "timeAssignees", timeAssignees);
        hashData(dataToHash, "timeStatuses", timeStatuses);
        hashData(dataToHash, "links", links);
        hashData(dataToHash, "excludeTimeAssignees", excludeTimeAssignees);
        hashData(dataToHash, "excludeKeys", excludeKeys);
        hashData(dataToHash, "excludeParentKeys", excludeParentKeys);
        hashData(dataToHash, "excludeParentIssueTypes", excludeParentIssueTypes);
        hashData(dataToHash, "excludeParentLabels", excludeParentLabels);
        hashData(dataToHash, "excludePriorities", excludePriorities);
        hashData(dataToHash, "excludeStatuses", excludeStatuses);
        hashData(dataToHash, "excludeSprintIds", excludeSprintIds);
        hashData(dataToHash, "excludeSprintNames", excludeSprintNames);
        hashData(dataToHash, "excludeSprintFullNames", excludeSprintFullNames);
        hashData(dataToHash, "excludeSprintStates", excludeSprintStates);
        hashData(dataToHash, "excludeAssignees", excludeAssignees);
        hashData(dataToHash, "excludeReporters", excludeReporters);
        hashData(dataToHash, "excludeIssueTypes", excludeIssueTypes);
        hashData(dataToHash, "excludeFixVersions", excludeFixVersions);
        hashData(dataToHash, "excludeVersions", excludeVersions);
        hashData(dataToHash, "excludeIntegrationIds", excludeIntegrationIds);
        hashData(dataToHash, "excludeProjects", excludeProjects);
        hashData(dataToHash, "excludeComponents", excludeComponents);
        hashData(dataToHash, "excludeLabels", excludeLabels);
        hashData(dataToHash, "excludeEpics", excludeEpics);
        hashData(dataToHash, "excludeStages", excludeStages);
        hashData(dataToHash, "excludeVelocityStages", excludeVelocityStages);
        hashData(dataToHash, "excludeResolutions", excludeResolutions);
        hashData(dataToHash, "excludeStatusCategories", excludeStatusCategories);
        hashData(dataToHash, "excludeLinks", excludeLinks);
        hashData(dataToHash, "firstAssignees", firstAssignees);
        hashData(dataToHash, "ticketCategorizationSchemeId", ticketCategorizationSchemeId);
        hashData(dataToHash, "ticketCategories", ticketCategories);
        hashData(dataToHash, "issueCreatedRange", issueCreatedRange);
        hashData(dataToHash, "issueUpdatedRange", issueUpdatedRange);
        hashData(dataToHash, "issueDueRange", issueDueRange);
        hashData(dataToHash, "issueResolutionRange", issueResolutionRange);
        hashData(dataToHash, "snapshotRange", snapshotRange);
        hashData(dataToHash, "assigneesDateRange", assigneesDateRange);
        hashData(dataToHash, "age", age);
        hashData(dataToHash, "page", page);
        hashData(dataToHash, "page_size", pageSize);
        hashData(dataToHash, "filterByLastSprint", filterByLastSprint);
        hashData(dataToHash, "includeSolveTime", includeSolveTime);
        hashData(dataToHash, "filterAcrossValues", filterAcrossValues);

        hashDataMapOfStrings(dataToHash, "customfields", customFields);
        hashDataMapOfStrings(dataToHash, "excludeCustomFields", excludeCustomFields);
        hashDataMapOfStrings(dataToHash, "storyPoints", storyPoints);
        hashDataMapOfStrings(dataToHash, "parentStoryPoints", parentStoryPoints);
        hashDataMapOfStrings(dataToHash, "missingFields", missingFields);
        if (MapUtils.isNotEmpty(hygieneCriteriaSpecs)) {
            TreeSet<String> fields = hygieneCriteriaSpecs.keySet().stream().map(Enum::toString).collect(Collectors.toCollection(TreeSet::new));
            dataToHash.append(",hygieneCriteriaSpecs=(");
            for (String field : fields) {
                Object data = hygieneCriteriaSpecs.get(EXTRA_CRITERIA.fromString(field));
                dataToHash.append(field).append("=").append(NumberUtils.toInt(String.valueOf(data))).append(",");
            }
            dataToHash.append(")");
        }

        hashDataMapOfMaps(dataToHash, "fieldSize", fieldSize);
        hashDataMapOfMaps(dataToHash, "partialMatch", partialMatch);

        // region sprint mappings
        hashData(dataToHash, "sprintMappingSprintIds", sprintMappingSprintIds);
        hashData(dataToHash, "sprintMappingIgnorableIssueType", sprintMappingIgnorableIssueType);
        hashData(dataToHash, "sprintMappingSprintCompletedAtAfter", sprintMappingSprintCompletedAtAfter);
        hashData(dataToHash, "sprintMappingSprintCompletedAtBefore", sprintMappingSprintCompletedAtBefore);
        hashData(dataToHash, "sprintMappingSprintNames", sprintMappingSprintNames);
        hashData(dataToHash, "sprintMappingSprintNameStartsWith", sprintMappingSprintNameStartsWith);
        hashData(dataToHash, "sprintMappingSprintNameEndsWith", sprintMappingSprintNameEndsWith);
        hashData(dataToHash, "sprintMappingSprintNameContains", sprintMappingSprintNameContains);
        hashData(dataToHash, "sprintMappingExcludeSprintNames", sprintMappingExcludeSprintNames);
        hashData(dataToHash, "sprintMappingSprintState", sprintMappingSprintState);
        // end region

        hashDataUsingToString(dataToHash, "ingested_at", ingestedAt);
        hashDataMapOfStrings(dataToHash, "latestIngestedAtByIntegrationId", ingestedAtByIntegrationId);
        if (orFilter != null) {
            dataToHash.append(orFilter.generateCacheRawString());
        }
        if (filterByLastSprint != null) {
            dataToHash.append(",filterByLastSprint=").append(filterByLastSprint);
        }
        if (MapUtils.isNotEmpty(sort))
            hashDataMapOfStrings(dataToHash, "sort", sort);
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }
}
