package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.Query;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.parseFloatRange;
import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfLists;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfMaps;
import static io.levelops.commons.caching.CacheHashUtils.hashDataNumericRange;
import static io.levelops.commons.caching.CacheHashUtils.hashDataTimestamp;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemsFilter.WorkItemsFilterBuilder.class)
public class WorkItemsFilter {
    private static final String WORK_ITEM_PREFIX = "workitem_";
    private static final int WORK_ITEM_PREFIX_LENGTH = WORK_ITEM_PREFIX.length();

    public static final List<DISTINCT> TIMESTAMP_ACROSS_OPTIONS = List.of(DISTINCT.trend, DISTINCT.workitem_created_at,
            DISTINCT.workitem_resolved_at, DISTINCT.workitem_updated_at);
    public static final List<DISTINCT> USERS_ACROSS_OPTIONS = List.of(DISTINCT.assignee, DISTINCT.reporter);

    private static final Set<String> NUMERICAL_SORT_BY_FIELDS = Set.of("story_points", "original_estimate", "desc_size", "hops", "bounces", "num_attachments");

    DISTINCT across;
    CALCULATION calculation;
    Integer acrossLimit;
    String aggInterval;
    String attributeAcross;
    String attributeStack;
    String customAcross;
    String customStack;
    Map<String, SortingOrder> sort;
    Long ingestedAt;

    List<String> integrationIds;
    List<UUID> ids;
    List<String> workItemIds;
    List<String> sprintIds;
    List<String> priorities;
    List<String> epics;
    List<String> assignees;
    List<String> versions;
    List<String> statuses;
    List<String> workItemTypes;
    List<String> labels;
    List<String> fixVersions;
    List<String> statusCategories;
    List<String> parentWorkItemIds;
    List<String> parentWorkItemTypes;
    List<String> projects;
    List<String> reporters;
    List<String> firstAssignees;
    List<String> stages;
    List<EXTRA_CRITERIA> extraCriteria;
    Map<EXTRA_CRITERIA, Object> hygieneCriteriaSpecs;
    Map<String, List<String>> attributes;
    Map<String, Object> customFields;
    Map<String, Boolean> missingFields;
    Map<String, Long> ingestedAtByIntegrationId;
    Boolean includeSolveTime;
    Boolean includeSprintFullNames; // used by /list endpoint only

    List<String> ticketCategories;
    String ticketCategorizationSchemeId;
    @JsonIgnore
    List<TicketCategorizationFilter> ticketCategorizationFilters; // ticket categorization filters (metadata - DO NOT CACHE)
    ImmutablePair<Long, Long> assigneesDateRange;

    Map<String, Map<String, String>> partialMatch;
    ImmutablePair<Long, Long> workItemResolvedRange;
    ImmutablePair<Long, Long> workItemCreatedRange;
    ImmutablePair<Long, Long> workItemUpdatedRange;
    ImmutablePair<Long, Long> snapshotRange;
    ImmutablePair<Float, Float> storyPointsRange; // range filter

    List<String> excludePriorities;
    List<String> excludeWorkItemIds;
    List<String> excludeEpics;
    List<String> excludeVersions;
    List<String> excludeStatuses;
    List<String> excludeWorkItemTypes;
    List<String> excludeLabels;
    List<String> excludeFixVersions;
    List<String> excludeSprintIds;
    List<String> excludeStatusCategories;
    List<String> excludeAssignees;
    List<String> excludeParentWorkItemIds;
    List<String> excludeParentWorkItemTypes;
    List<String> excludeProjects;
    List<String> excludeReporters;
    List<String> excludeFirstAssignees;
    List<String> excludeStages;
    Map<String, List<String>> excludeAttributes;
    Map<String, Object> excludeCustomFields;

    /*
    This will be used only in Faceted Search. It will NOT be used in UI/Server Api.
    Before using in Server Api, add it to generateCacheRawString() & also to fromDefaultListRequest()
     */
    Map<Integer, ImmutablePair<Long, Long>> integrationIdByIssueUpdatedRange;

    public Map<EXTRA_CRITERIA, Object> getHygieneCriteriaSpecs() {
        return MapUtils.emptyIfNull(hygieneCriteriaSpecs);
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    public enum DISTINCT {
        // across and values supported
        priority,
        project,
        status,
        assignee,
        assignees,
        workitem_type,
        label,
        fix_version,
        component,
        version,
        reporter,
        status_category,
        resolution,
        attribute,
        custom_field,
        // across, but no values
        epic,
        parent_workitem_id,
        ticket_category,
        sprint_mapping,
        // timeline, across, but no values
        trend,
        workitem_created_at,
        workitem_updated_at,
        workitem_resolved_at,
        sprint,
        story_points,
        first_assignee,
        stage,
        // no across, no values
        none;

        public static WorkItemsFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(WorkItemsFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        age,
        hops,
        bounces,
        issue_count,
        resolution_time,
        stage_times_report,
        stage_bounce_report,
        story_point_report,
        effort_report,
        response_time,
        sprint_mapping,
        sprint_mapping_count,
        assign_to_resolve,
        assignees;

        public static WorkItemsFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(WorkItemsFilter.CALCULATION.class, st);
        }
    }

    public enum MISSING_BUILTIN_FIELD {
        priority,
        parent_workitem_id,
        story_points,
        resolution,
        status_category,
        workitem_resolved_at,
        workitem_due_at,
        first_attachment_at,
        first_comment_at,
        status,
        assignee,
        first_assignee,
        reporter,
        component,
        label,
        fix_version,
        version,
        epic,
        project;

        public static WorkItemsFilter.MISSING_BUILTIN_FIELD fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(WorkItemsFilter.MISSING_BUILTIN_FIELD.class, st);
        }
    }

    //used to be called hygiene queries
    public enum EXTRA_CRITERIA {
        idle,
        no_due_date,
        no_assignee,
        no_components,
        poor_description,
        missed_response_time,
        missed_resolution_time;

        public static EXTRA_CRITERIA fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(EXTRA_CRITERIA.class, st);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TicketCategorizationFilter.TicketCategorizationFilterBuilder.class)
    public static class TicketCategorizationFilter {
        String id;
        String name;
        Integer index;
        WorkItemsFilter filter;
    }

    public Query getCalculation(CALCULATION calculation) throws SQLException {
        List<Query.SelectField> selectFields;
        switch (calculation) {
            case issue_count:
                selectFields = Collections.singletonList(Query.selectField("COUNT(DISTINCT (workitem_id, integration_id))", "ct"));
                break;
            case stage_times_report:
                selectFields = List.of(
                        Query.selectField("MIN(time_spent)", "mn"),
                        Query.selectField("MAX(time_spent)", "mx"),
                        Query.selectField("COUNT(DISTINCT id)", "ct"),
                        Query.selectField("AVG(time_spent)", "mean_time"),
                        Query.selectField("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY time_spent)", "median")
                );
                break;
            case stage_bounce_report:
                selectFields = List.of(
                        Query.selectField("COUNT(DISTINCT id)", "ct"),
                        Query.selectField("AVG(count)", "mean"),
                        Query.selectField("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY count)", "median")
                );
                break;
            case age:
                selectFields = List.of(
                        Query.selectField("AVG(age)", "mean"),
                        Query.selectField("MAX(age)", "max"),
                        Query.selectField("MIN(age)", "min"),
                        Query.selectField("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY age)", "median"),
                        Query.selectField("COUNT(id)", "count"),
                        Query.selectField("PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY age)", "p90"),
                        Query.selectField("sum(story_point)", "total_story_points")
                );
                break;
            case resolution_time:
                selectFields = List.of(
                        Query.selectField("AVG(solve_time)", "mean"),
                        Query.selectField("MAX(solve_time)", "max"),
                        Query.selectField("MIN(solve_time)", "min"),
                        Query.selectField("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY solve_time)", "median"),
                        Query.selectField("COUNT(id)", "count"),
                        Query.selectField("PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY solve_time)", "p90")
                );
                break;
            case story_point_report:
                selectFields = List.of(
                        Query.selectField("COUNT(id)", "count"),
                        Query.selectField("SUM(story_points)", "story_points_sum"),
                        Query.selectField("SUM(CASE WHEN (story_points = 0 OR story_points IS NULL) THEN 1 ELSE 0 END)", "unestimated_tickets_count")
                );
                break;
            case effort_report:
                selectFields = List.of(
                        Query.selectField("COUNT(id)", "count"),
                        Query.selectField("SUM(COALESCE((attributes->'effort')::REAL, 0))", "effort_sum"),
                        Query.selectField("SUM(CASE WHEN (COALESCE((attributes->'effort')::REAL, 0) = 0) THEN 1 ELSE 0 END)", "unestimated_tickets_count")
                );
                break;
            case response_time:
                selectFields = List.of(
                        Query.selectField("MIN(response_time)", "mn"),
                        Query.selectField("MAX(response_time)", "mx"),
                        Query.selectField("COUNT(DISTINCT id)", "ct"),
                        Query.selectField("PERCENTILE_DISC (0.5)WITHIN GROUP(ORDER BY response_time)", "median")
                );
                break;
            case assign_to_resolve:
                selectFields = List.of(
                        Query.selectField("MIN(assign)", "min"),
                        Query.selectField("MAX(assign)", "max"),
                        Query.selectField("COUNT(*)", "count"),
                        Query.selectField("PERCENTILE_DISC (0.5)WITHIN GROUP(ORDER BY assign)", "median")
                );
                break;
            case sprint_mapping:
                selectFields = List.of(
                        Query.selectField("array_agg(json_build_object('sprint_mapping', to_jsonb(sprint_mapping_json), 'workitem_type', workitem_type))", "sprint_mappings"),
                        Query.selectField("sprint_mapping_integration_id"),
                        Query.selectField("sprint_mapping_sprint_id"),
                        Query.selectField("sprint_mapping_name"),
                        Query.selectField("EXTRACT(EPOCH FROM sprint_mapping_start_date)", "sprint_mapping_start_date"),
                        Query.selectField("EXTRACT(EPOCH FROM sprint_mapping_completed_at)", "sprint_mapping_completed_at")
                );
                break;
            case sprint_mapping_count:
                selectFields = List.of(
                        Query.selectField("count(distinct (sprint_mapping_integration_id, sprint_mapping_sprint_id))", "ct"));
                break;
            case assignees:
                selectFields = List.of(
                        Query.selectField("ARRAY_AGG(DISTINCT assignee_item)", "assignees")
                );
                break;
            case hops:
                selectFields = List.of(
                        Query.selectField("MAX(hops)", "max"),
                        Query.selectField("MIN(hops)", "min"),
                        Query.selectField("COUNT(hops)", "count"),
                        Query.selectField("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY hops)", "median"));
                break;
            case bounces:
                selectFields = List.of(
                        Query.selectField("MAX(bounces)", "max"),
                        Query.selectField("MIN(bounces)", "min"),
                        Query.selectField("COUNT(bounces)", "count"),
                        Query.selectField("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY bounces)", "median"));
                break;
            default:
                throw new SQLException("Unsupported across: " + calculation);
        }

        return Query.builder().select(selectFields).build();
    }

    public Query.SortByField getOrderBy(Map<String, SortingOrder> sort, DISTINCT across,
                                        CALCULATION calculation) throws SQLException {
        return getOrderBy(null, null, sort, across, calculation);
    }
    private static String sanitizeAlias(String column, List<String> characters) {
        String sanitizedColumn = column;
        for (String c : characters) {
            sanitizedColumn = sanitizedColumn.replaceAll(c, "_");
        }
        return sanitizedColumn;
    }

    public Query.SortByField getOrderBy(List<String> metrics, String customAcross, Map<String, SortingOrder> sort, DISTINCT across,
                                        CALCULATION calculation) throws SQLException {
        String sortByField = calculation.toString(); // default
        SortingOrder sortOrder = SortingOrder.DESC; // default
        if (MapUtils.isNotEmpty(sort)) {
            sortByField = sort.keySet().stream().findFirst().isPresent() ?
                    sort.keySet().stream().findFirst().get() : calculation.toString();
            if (WorkItemsFilter.DISTINCT.custom_field.equals(across) && sortByField.equalsIgnoreCase(across.toString()) && CALCULATION.resolution_time.equals(calculation) && null!=customAcross) {
                String customColumn = customAcross;
                String sanitizedColumn = sanitizeAlias(customColumn, List.of("\\.", "-"));
                sortByField=across + "_" + sanitizedColumn;
            }
            sortOrder = sort.values().stream().findFirst().isPresent() ?
                    sort.values().stream().findFirst().get() : SortingOrder.DESC;
        }
        if (calculation == CALCULATION.sprint_mapping) {
            sortByField = "sprint_mapping_completed_at";
            sortOrder = MapUtils.isNotEmpty(sort) ? sort.values().stream().findFirst().isPresent() ?
                    sort.values().stream().findFirst().get() : SortingOrder.ASC : SortingOrder.ASC;
            return Query.sortByField(sortByField, sortOrder.toString(), false);
        }
        if (across == DISTINCT.sprint) {
            sortOrder = MapUtils.isNotEmpty(sort) ? sort.values().stream().findFirst().isPresent() ?
                    sort.values().stream().findFirst().get() : SortingOrder.DESC : SortingOrder.DESC;
            if (MapUtils.isNotEmpty(sort)) {
                if (sort.get("milestone_start_date") != null) {
                    sortByField = "milestone_start_date";
                    return Query.sortByField(sortByField, sortOrder.toString(), false);
                } else if (sort.get("milestone_end_date") != null) {
                    sortByField = "milestone_end_date";
                    return Query.sortByField(sortByField, sortOrder.toString(), false);
                }
            }
        }

        if (Objects.nonNull(across) && Objects.isNull(CALCULATION.fromString(sortByField))) {
            if (Objects.nonNull(DISTINCT.fromString(sortByField)) && BooleanUtils.isTrue(isAcrossTimeField(DISTINCT.fromString(sortByField)))) {
                sortByField = across + "_epoch";
            } else {
                switch (calculation) {
                    case story_point_report:
                        sortByField = "story_points_sum";
                        break;
                    case effort_report:
                        sortByField = "effort_sum";
                        break;
                    default:
                        if (!NUMERICAL_SORT_BY_FIELDS.contains(sortByField)) {
                            sortByField = "lower(" + sortByField + ")";
                        }
                }
            }
        } else if (!across.toString().equals(sortByField)) {
            if (!calculation.toString().equals(sortByField)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort option " + sortByField);
            }
            switch (calculation) {
                case issue_count:
                    sortByField = "ct";
                    break;
                case stage_times_report:
                    sortByField = "mean_time";
                    break;
                case age:
                case response_time:
                case hops:
                case bounces:
                case assign_to_resolve:
                case stage_bounce_report:
                    sortByField = "median";
                    break;
                case resolution_time:
                     sortByField = "median";
                    if (null !=metrics && CollectionUtils.isNotEmpty(metrics)) {
                        String metricKey = metricToKeyMapping(metrics);
                        if (metricKey != null) {
                            sortByField = metricKey;
                        }
                    }
                    break;
                case story_point_report:
                    sortByField = "story_points_sum";
                    break;
                case effort_report:
                    sortByField = "effort_sum";
                default:
            }
        }

        return Query.sortByField(sortByField, sortOrder.toString(), false);
    }

    private static String metricToKeyMapping(List<String> metricValue) {
        Map<String, String> metricToKeyMap = new HashMap<>();
        metricToKeyMap.put("median_resolution_time", "median");
        metricToKeyMap.put("average_resolution_time", "mean");
        metricToKeyMap.put("90th_percentile_resolution_time", "p90");
        metricToKeyMap.put("number_of_tickets_closed", "count");
        String key=metricToKeyMap.get(metricValue.get(0));
        return key;
    }


    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        hashDataUsingToString(dataToHash, "across", across);
        hashDataUsingToString(dataToHash, "acrossLimit", acrossLimit);
        hashDataUsingToString(dataToHash, "calculation", calculation);
        hashDataUsingToString(dataToHash, "attributeAcross", attributeAcross);
        hashDataUsingToString(dataToHash, "customAcross", customAcross);
        hashDataUsingToString(dataToHash, "aggInterval", aggInterval);
        hashData(dataToHash, "integrationIds", integrationIds);
        hashDataUsingToString(dataToHash, "customStack", customStack);
        hashDataUsingToString(dataToHash, "attributeStack", attributeStack);
        hashData(dataToHash, "parentWorkItemIds", parentWorkItemIds);
        hashData(dataToHash, "parentWorkItemTypes", parentWorkItemTypes);
        hashData(dataToHash, "priorities", priorities);
        hashData(dataToHash, "epics", epics);
        hashData(dataToHash, "ingestedAt", ingestedAt);
        hashData(dataToHash, "ids", CollectionUtils.emptyIfNull(ids).stream().map(Objects::toString).collect(Collectors.toList()));
        hashData(dataToHash, "workItemIds", workItemIds);
        hashData(dataToHash, "statuses", statuses);
        hashData(dataToHash, "assignees", assignees);
        hashData(dataToHash, "reporters", reporters);
        hashData(dataToHash, "workItemTypes", workItemTypes);
        hashData(dataToHash, "versions", versions);
        hashData(dataToHash, "fixVersions", fixVersions);
        hashData(dataToHash, "projects", projects);
        hashData(dataToHash, "labels", labels);
        hashData(dataToHash, "sprintIds", sprintIds);
        hashData(dataToHash, "firstAssignees", firstAssignees);
        hashData(dataToHash, "stages", stages);
        hashDataMapOfMaps(dataToHash, "partialMatch", partialMatch);
        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> critStr = extraCriteria.stream().map(Enum::toString).sorted().collect(Collectors.toList());
            dataToHash.append(",extraCriteria=").append(String.join(",", critStr));
        }
        if (MapUtils.isNotEmpty(hygieneCriteriaSpecs)) {
            TreeSet<String> fields = hygieneCriteriaSpecs.keySet().stream().map(Enum::toString).collect(Collectors.toCollection(TreeSet::new));
            dataToHash.append(",hygieneCriteriaSpecs=(");
            for (String field : fields) {
                Object data = hygieneCriteriaSpecs.get(WorkItemsFilter.EXTRA_CRITERIA.fromString(field));
                dataToHash.append(field).append("=").append(NumberUtils.toInt(String.valueOf(data))).append(",");
            }
            dataToHash.append(")");
        }
        hashData(dataToHash, "statusCategories", statusCategories);
        hashData(dataToHash, "assigneesDateRange", assigneesDateRange);
        hashData(dataToHash, "excludeParentWorkItemIds", excludeParentWorkItemIds);
        hashData(dataToHash, "excludeParentWorkItemTypes", excludeParentWorkItemTypes);
        hashData(dataToHash, "excludePriorities", excludePriorities);
        hashData(dataToHash, "excludeWorkItemIds", excludeWorkItemIds);
        hashData(dataToHash, "excludeEpics", excludeEpics);
        hashData(dataToHash, "excludeStatuses", excludeStatuses);
        hashData(dataToHash, "excludeSprintIds", excludeSprintIds);
        hashData(dataToHash, "excludeAssignees", excludeAssignees);
        hashData(dataToHash, "excludeReporters", excludeReporters);
        hashData(dataToHash, "excludeIssueTypes", excludeWorkItemTypes);
        hashData(dataToHash, "excludeFixVersions", excludeFixVersions);
        hashData(dataToHash, "excludeVersions", excludeVersions);
        hashData(dataToHash, "excludeProjects", excludeProjects);
        hashData(dataToHash, "excludeLabels", excludeLabels);
        hashData(dataToHash, "excludeStatusCategories", excludeStatusCategories);
        hashData(dataToHash, "excludeStages", excludeStages);
        hashData(dataToHash, "excludeFirstAssignees", excludeFirstAssignees);
        hashData(dataToHash, "includeSolveTime", includeSolveTime);
        hashData(dataToHash, "includeSprintFullNames", includeSprintFullNames);
        hashDataTimestamp(dataToHash, "workItemCreatedRange", workItemCreatedRange);
        hashDataTimestamp(dataToHash, "workItemUpdatedRange", workItemUpdatedRange);
        hashDataTimestamp(dataToHash, "workItemResolvedRange", workItemResolvedRange);
        hashDataTimestamp(dataToHash, "snapshotRange", snapshotRange);
        hashDataNumericRange(dataToHash, "storyPointsRange", storyPointsRange);
        hashDataMapOfLists(dataToHash, "attributes", attributes);
        hashDataMapOfStrings(dataToHash, "missingFields", missingFields);
        hashDataMapOfLists(dataToHash, "excludeAttributes", excludeAttributes);
        hashDataMapOfStrings(dataToHash, "customFields", customFields);
        hashDataMapOfStrings(dataToHash, "excludeCustomFields", excludeCustomFields);
        hashDataMapOfStrings(dataToHash, "latestIngestedAtByIntegrationId", ingestedAtByIntegrationId);

        if (StringUtils.isNotEmpty(ticketCategorizationSchemeId)) {
            dataToHash.append(",ticketCategorizationSchemeId=").append(ticketCategorizationSchemeId);
        }

        if (CollectionUtils.isNotEmpty(ticketCategories)) {
            ArrayList<String> tempList = new ArrayList<>(ticketCategories);
            Collections.sort(tempList);
            dataToHash.append(",ticketCategories=").append(String.join(",", tempList));
        }

        if (MapUtils.isNotEmpty(sort)) {
            hashDataMapOfStrings(dataToHash, "sort", sort);
        }
        return dataToHash.toString();
    }

    public static void hashDataUsingToString(StringBuilder dataToHash, String fieldName, Object o) {
        if (o != null) {
            hashData(dataToHash, fieldName, o.toString());
        }
    }

    public static void hashDataMapOfStrings(StringBuilder dataToHash, String fieldName, Map<String, ?> map) {
        if (!MapUtils.isNotEmpty(map)) {
            return;
        }
        TreeSet<String> fields = new TreeSet<>(map.keySet());
        dataToHash.append(",").append(fieldName).append("=(");
        for (String field : fields) {
            String data = String.valueOf(map.get(field));
            dataToHash.append(field).append("=").append(data).append(",");
        }
        dataToHash.append(")");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> partialMatchMap(Map<String, Object> filterObj) {
        return filterObj.get("partial_match") != null ? (Map<String, Map<String, String>>) filterObj.get("partial_match") :
                filterObj.get(WORK_ITEM_PREFIX + "partial_match") != null ? (Map<String, Map<String, String>>) filterObj.get(WORK_ITEM_PREFIX + "partial_match") : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Boolean> parseMissingFilters(DefaultListRequest filter) {
        Map<String, Boolean> missingFields = MapUtils.emptyIfNull((Map<String, Boolean>) filter.getFilter().get("missing_fields"));
        log.info("missingFields = {}", missingFields);

        Map<String, Boolean> missingFieldsSanitized = new HashMap<>();
        for (Map.Entry<String, Boolean> e : missingFields.entrySet()) {
            if (e.getKey().startsWith(WORK_ITEM_PREFIX)) {
                String newKey = e.getKey().substring(WORK_ITEM_PREFIX_LENGTH);
                log.info("key = {}, newKey = {}", e.getKey(), newKey);
                missingFieldsSanitized.put(newKey, e.getValue());
            } else {
                missingFieldsSanitized.put(e.getKey(), e.getValue());
            }
        }
        log.info("missingFieldsSanitized = {}", missingFieldsSanitized);
        return missingFieldsSanitized;
    }

    @SuppressWarnings("unchecked")
    public static WorkItemsFilter fromDefaultListRequest(DefaultListRequest filter, WorkItemsFilter.DISTINCT across,
                                                         WorkItemsFilter.CALCULATION calculation) throws BadRequestException {
        ImmutablePair<Long, Long> createdRange = getTimeRange(filter, "workitem_created_at");
        ImmutablePair<Long, Long> updatedRange = getTimeRange(filter, "workitem_updated_at");
        ImmutablePair<Long, Long> resolvedRange = getTimeRange(filter, "workitem_resolved_at");
        ImmutablePair<Long, Long> snapshotRange = getTimeRange(filter, "workitem_snapshot_range");
        ImmutablePair<Float, Float> storyPointRange = parseFloatRange(filter, "workitem_story_points");
        ImmutablePair<Long, Long> assigneesRange = getTimeRange(filter, "workitem_assignees_range");
        final Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        ArrayList<String> workItemIds = new ArrayList<>();
        workItemIds.addAll(getListOrDefault(filter.getFilter(), "workitem_ids"));
        workItemIds.addAll(getListOrDefault(filter.getFilter(), "keys"));
        Map<String, Map<String, String>> partialMatchMap = partialMatchMap(filter.getFilter());
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        WorkItemsFilter.WorkItemsFilterBuilder bldr = WorkItemsFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .aggInterval(filter.getAggInterval())
                .acrossLimit(filter.getAcrossLimit())
                .ingestedAt(Objects.nonNull(filter.getFilter().get("ingested_at"))
                        ? Long.parseLong(String.valueOf(filter.getFilter().get("ingested_at"))) : null)
                .sort(sort)
                .priorities(getListOrDefault(filter.getFilter(), "workitem_priorities"))
                .versions(getListOrDefault(filter.getFilter(), "workitem_versions"))
                .statuses(getListOrDefault(filter.getFilter(), "workitem_statuses"))
                .workItemTypes(getListOrDefault(filter.getFilter(), "workitem_types"))
                .workItemIds(workItemIds)
                .labels(getListOrDefault(filter.getFilter(), "workitem_labels"))
                .fixVersions(getListOrDefault(filter.getFilter(), "workitem_fix_versions"))
                .sprintIds(getListOrDefault(filter.getFilter(), "workitem_sprint_ids"))
                .statusCategories(getListOrDefault(filter.getFilter(), "workitem_status_categories"))
                .epics(getListOrDefault(filter.getFilter(), "workitem_epics"))
                .assignees(getListOrDefault(filter.getFilter(), "workitem_assignees"))
                .parentWorkItemIds(getListOrDefault(filter.getFilter(), "workitem_parent_workitem_ids"))
                .parentWorkItemTypes(getListOrDefault(filter.getFilter(), "workitem_parent_workitem_types"))
                .projects(getListOrDefault(filter.getFilter(), "workitem_projects"))
                .reporters(getListOrDefault(filter.getFilter(), "workitem_reporters"))
                .firstAssignees(getListOrDefault(filter.getFilter(), "workitem_first_assignees"))
                .stages(getListOrDefault(filter.getFilter(), "workitem_stages"))
                .ticketCategories(getListOrDefault(filter.getFilter(), "workitem_ticket_categories"))
                .ticketCategorizationSchemeId(filter.getFilter().getOrDefault("workitem_ticket_categorization_scheme", "").toString())
                .includeSolveTime((Boolean) filter.getFilter().getOrDefault("include_solve_time", false))
                .includeSprintFullNames((Boolean) filter.getFilter().getOrDefault("include_sprint_full_names", false))
                .storyPointsRange(storyPointRange)
                .assigneesDateRange(assigneesRange)
                .workItemCreatedRange(createdRange)
                .workItemUpdatedRange(updatedRange)
                .workItemResolvedRange(resolvedRange)
                .snapshotRange(snapshotRange)
                .partialMatch(partialMatchMap)
                .extraCriteria(MoreObjects.firstNonNull(
                                getListOrDefault(filter.getFilter(), "workitem_hygiene_types"),
                                List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(WorkItemsFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .hygieneCriteriaSpecs(getHygieneCriteriaSpecs(filter.getFilter()))
                .attributes((Map<String, List<String>>) filter.getFilter().get("workitem_attributes"))
                .customFields((Map<String, Object>) filter.getFilter().get("workitem_custom_fields"))
                .missingFields(parseMissingFilters(filter))
                .excludePriorities(getListOrDefault(excludedFields, "workitem_priorities"))
                .excludeWorkItemIds(getListOrDefault(excludedFields, "workitem_ids"))
                .excludeVersions(getListOrDefault(excludedFields, "workitem_versions"))
                .excludeStatuses(getListOrDefault(excludedFields, "workitem_statuses"))
                .excludeWorkItemTypes(getListOrDefault(excludedFields, "workitem_types"))
                .excludeLabels(getListOrDefault(excludedFields, "workitem_labels"))
                .excludeFixVersions(getListOrDefault(excludedFields, "workitem_fix_versions"))
                .excludeSprintIds(getListOrDefault(excludedFields, "workitem_sprint_ids"))
                .excludeStatusCategories(getListOrDefault(excludedFields, "workitem_status_categories"))
                .excludeAssignees(getListOrDefault(excludedFields, "workitem_assignees"))
                .excludeParentWorkItemIds(getListOrDefault(excludedFields, "workitem_parent_workitem_ids"))
                .excludeParentWorkItemTypes(getListOrDefault(excludedFields, "workitem_parent_workitem_types"))
                .excludeProjects(getListOrDefault(excludedFields, "workitem_projects"))
                .excludeReporters(getListOrDefault(excludedFields, "workitem_reporters"))
                .excludeStages(getListOrDefault(excludedFields, "workitem_stages"))
                .excludeFirstAssignees(getListOrDefault(excludedFields, "workitem_first_assignees"))
                .excludeAttributes(MapUtils.emptyIfNull(
                        (Map<String, List<String>>) excludedFields.get("workitem_attributes")))
                .excludeCustomFields(MapUtils.emptyIfNull(
                        (Map<String, Object>) excludedFields.get("workitem_custom_fields")));
        if (across != null) {
            bldr.across(across);
        }
        if (calculation != null) {
            bldr.calculation(calculation);
        }

        WorkItemsFilter workItemsFilter = bldr.build();
        log.info("workItemsFilter = {}", workItemsFilter);
        return workItemsFilter;
    }

    public static boolean isAcrossTimeField(DISTINCT across) {
        return TIMESTAMP_ACROSS_OPTIONS.contains(across);
    }

    public static boolean isAcrossUsers(DISTINCT across) {
        return USERS_ACROSS_OPTIONS.contains(across);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<String> getListOrDefault(Map<String, Object> filter, String key) {
        try {
            var value = (Collection) MapUtils.emptyIfNull(filter).getOrDefault(key, Collections.emptyList());
            if (value.size() > 0 && !(value.iterator().next() instanceof String)) {
                return (List<String>) value.stream().map(i -> i.toString()).collect(Collectors.toList());
            }
            // to handle list or sets
            return (List<String>) value.stream().collect(Collectors.toList());
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

    private static Map<WorkItemsFilter.EXTRA_CRITERIA, Object> getHygieneCriteriaSpecs(Map<String, Object> filter) {
        try {
            Object idleDays = filter.get(WorkItemsFilter.EXTRA_CRITERIA.idle.toString());
            Object poorDescCt = filter.get(WorkItemsFilter.EXTRA_CRITERIA.poor_description.toString());
            Map<WorkItemsFilter.EXTRA_CRITERIA, Object> specs = new HashMap<>();
            if (idleDays != null) {
                specs.put(WorkItemsFilter.EXTRA_CRITERIA.idle, idleDays);
            }
            if (poorDescCt != null) {
                specs.put(WorkItemsFilter.EXTRA_CRITERIA.poor_description, poorDescCt);
            }
            return specs;
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data.");
        }
    }
}
