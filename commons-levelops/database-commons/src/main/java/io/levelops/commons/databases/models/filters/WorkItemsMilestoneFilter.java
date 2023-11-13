package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfMaps;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.commons.caching.CacheHashUtils.hashDataTimestamp;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemsMilestoneFilter.WorkItemsMilestoneFilterBuilder.class)
public class WorkItemsMilestoneFilter {

    private static final String WORKITEM_PREFIX = "workitem_";
    public static final Map<String,String> PARTIAL_MATCH_ARRAY_COLUMNS = Map.of();
    public static final Map<String,String> PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS = Map.of();
    public static final Map<String,String> PARTIAL_MATCH_ATTRIBUTES_COLUMNS = Map.of(WORKITEM_PREFIX+"project","project");
    public static final Map<String,String> PARTIAL_MATCH_COLUMNS = Map.of(WORKITEM_PREFIX+"parent_field_value","parent_field_value",WORKITEM_PREFIX+"sprint_name","sprint_name",
            WORKITEM_PREFIX+"state","state",WORKITEM_PREFIX+"milestone_full_name","milestone_full_name",WORKITEM_PREFIX+"sprint_full_names","sprint_full_names");

    List<String> integrationIds;
    List<String> names;
    List<String> fullNames; // parent + "\\" + name
    List<String> states;
    List<String> fieldTypes;
    List<String> fieldValues;
    List<String> parentFieldValues;
    List<String> projectIds;
    List<String> projects;

    Map<String, SortingOrder> sort;
    int sprintCount;
    ImmutablePair<Long, Long> startedAtRange;
    ImmutablePair<Long, Long> completedAtRange;
    ImmutablePair<Long, Long> endedAtRange;
    Map<String, Map<String, String>> partialMatch;

    List<String> excludeIntegrationIds;
    List<String> excludeNames;
    List<String> excludeFullNames;
    List<String> excludeStates;
    List<String> excludeFieldTypes;
    List<String> excludeFieldValues;
    List<String> excludeParentFieldValues;
    List<String> excludeProjectIds;
    List<String> excludeProjects;

    @SuppressWarnings("unchecked")
    public static WorkItemsMilestoneFilter fromSprintRequest(DefaultListRequest filter, String prefix) throws BadRequestException {
        ImmutablePair<Long, Long> completedAtRage = getTimeRange(filter, prefix + "sprint_completed_at");
        ImmutablePair<Long, Long> startedAtRange = getTimeRange(filter, prefix + "sprint_started_at");
        ImmutablePair<Long, Long> endedAtRange = getTimeRange(filter, prefix + "sprint_ended_at");
        Map<String, Map<String, String>> partialMatchMap = partialMatchMap(filter.getFilter());
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        final Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        WorkItemsMilestoneFilter.WorkItemsMilestoneFilterBuilder bldr = WorkItemsMilestoneFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .fieldValues(getListOrDefault(filter.getFilter(), prefix + "sprint_ids"))
                .fieldTypes(List.of("sprint"))
                .parentFieldValues(getListOrDefault(filter.getFilter(), prefix + "parent_sprints"))
                .names(getListOrDefault(filter.getFilter(), prefix + "sprint_names"))
                .fullNames(getListOrDefault(filter.getFilter(), prefix + "sprint_full_names"))
                .states(getListOrDefault(filter.getFilter(), prefix + "sprint_states"))
                .projectIds(getListOrDefault(filter.getFilter(), "project_id"))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .startedAtRange(startedAtRange)
                .completedAtRange(completedAtRage)
                .endedAtRange(endedAtRange)
                .sort(sort)
                .sprintCount((Integer) filter.getFilter().getOrDefault(prefix + "sprint_count", 0))
                .partialMatch(partialMatchMap)
                .excludeIntegrationIds(getListOrDefault(excludedFields, "integration_ids"))
                .excludeFieldValues(getListOrDefault(excludedFields, prefix + "sprint_ids"))
                .excludeParentFieldValues(getListOrDefault(excludedFields, prefix + "parent_sprints"))
                .excludeNames(getListOrDefault(excludedFields, prefix + "sprint_names"))
                .excludeFullNames(getListOrDefault(excludedFields, prefix + "sprint_full_names"))
                .excludeStates(getListOrDefault(excludedFields, prefix + "sprint_states"))
                .excludeProjectIds(getListOrDefault(excludedFields, "project_id"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"));
        return bldr.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<String> getListOrDefault(Map<String, Object> filter, String key) {
        try {
            var value = (Collection) MapUtils.emptyIfNull(filter).getOrDefault(key, Collections.emptyList());
            if (value == null || value.size() < 1){
                return List.of();
            }
            if (value.size() > 0 && !(value.iterator().next() instanceof String)){
                return (List<String>) value.stream().map(i -> i.toString()).collect(Collectors.toList());
            }
            // to handle list or sets
            return (List<String>) value.stream().collect(Collectors.toList());
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    public boolean isSpecified() {
        // TODO find a better way to do this
        // Note: only check fields that meaningfully impacts the query
        // (for example: fieldType is hard-coded to 'sprints' so not meaningful alone)
        return CollectionUtils.isNotEmpty(names) ||
                CollectionUtils.isNotEmpty(excludeNames) ||
                CollectionUtils.isNotEmpty(fullNames) ||
                CollectionUtils.isNotEmpty(excludeFullNames) ||
                (MapUtils.isNotEmpty(partialMatch) &&
                        (CollectionUtils.containsAny(PARTIAL_MATCH_COLUMNS.keySet(), partialMatch.keySet()) ||
                                CollectionUtils.containsAny(PARTIAL_MATCH_ARRAY_COLUMNS.keySet(), partialMatch.keySet()) ||
                                CollectionUtils.containsAny(PARTIAL_MATCH_ATTRIBUTES_COLUMNS.keySet(), partialMatch.keySet()) ||
                                CollectionUtils.containsAny(PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS.keySet(), partialMatch.keySet()))) ||
//                CollectionUtils.isNotEmpty(excludeFieldTypes) ||
//                CollectionUtils.isNotEmpty(fieldTypes) ||
                CollectionUtils.isNotEmpty(fieldValues) ||
                CollectionUtils.isNotEmpty(excludeFieldValues) ||
                CollectionUtils.isNotEmpty(parentFieldValues) ||
                CollectionUtils.isNotEmpty(excludeParentFieldValues) ||
                CollectionUtils.isNotEmpty(states) ||
                CollectionUtils.isNotEmpty(excludeStates) ||
                sprintCount != 0 ||
                (startedAtRange != null && !ImmutablePair.nullPair().equals(startedAtRange)) ||
                (endedAtRange != null && !ImmutablePair.nullPair().equals(endedAtRange)) ||
                (completedAtRange != null && !ImmutablePair.nullPair().equals(completedAtRange));
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        hashData(dataToHash, "integrationIds", integrationIds);
        hashData(dataToHash, "names", names);
        hashData(dataToHash, "fullNames", fullNames);
        hashData(dataToHash, "fieldValues", fieldValues);
        hashData(dataToHash, "fieldTypes", fieldTypes);
        hashData(dataToHash, "parentFieldValues", parentFieldValues);
        hashData(dataToHash, "states", states);
        hashData(dataToHash, "projectIds", projectIds);
        hashData(dataToHash, "projects", projects);
        hashData(dataToHash, "excludeIntegrationIds", excludeIntegrationIds);
        hashData(dataToHash, "excludeNames", excludeNames);
        hashData(dataToHash, "excludeFullNames", excludeFullNames);
        hashData(dataToHash, "excludeParentFieldValues", excludeParentFieldValues);
        hashData(dataToHash, "excludeFieldValues", excludeFieldValues);
        hashData(dataToHash, "excludeFieldTypes", excludeFieldTypes);
        hashData(dataToHash, "excludeProjectIds", excludeProjectIds);
        hashData(dataToHash, "excludeProjects", excludeProjects);
        hashData(dataToHash, "excludeStates", excludeStates);
        hashData(dataToHash, "sprintCount", sprintCount);
        hashDataTimestamp(dataToHash, "startRange", startedAtRange);
        hashDataTimestamp(dataToHash, "completedAtRange", completedAtRange);
        hashDataTimestamp(dataToHash, "endRange", endedAtRange);
        hashDataMapOfMaps(dataToHash, "partialMatch", partialMatch);
        if (MapUtils.isNotEmpty(sort))
            hashDataMapOfStrings(dataToHash, "sort", sort);
        return dataToHash.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> partialMatchMap(Map<String, Object> filterObj) {
        return MapUtils.emptyIfNull((Map<String, Map<String, String>>) filterObj.get("partial_match"));
    }
}
