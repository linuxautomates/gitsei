package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemsPrioritySLAFilter.WorkItemsPrioritySLAFilterBuilder.class)
public class WorkItemsPrioritySLAFilter {

    List<String> integrationIds;
    List<String> priorities;
    List<String> projects;
    List<String> workitemTypes;

    List<String> excludePriorities;
    List<String> excludeProjects;
    List<String> excludeWorkitemTypes;

    @SuppressWarnings("unchecked")
    public static WorkItemsPrioritySLAFilter fromDefaultListRequest(DefaultListRequest filter) {
        final Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        WorkItemsPrioritySLAFilter.WorkItemsPrioritySLAFilterBuilder bldr = WorkItemsPrioritySLAFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .priorities(getListOrDefault(filter.getFilter(), "priorities"))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .workitemTypes(getListOrDefault(filter.getFilter(), "workitem_types"))
                .excludePriorities(getListOrDefault(excludedFields, "priorities"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeWorkitemTypes(getListOrDefault(excludedFields, "workitem_types"));
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
}