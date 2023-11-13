package io.levelops.commons.databases.models.filters;

import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.EnumUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class JiraSprintFilter {
    List<String> integrationIds;
    List<String> sprintIds;
    String state;
    List<String> names;
    List<String> sprintFullNames;
    List<String> excludeNames;
    List<String> excludeSprintFullNames;
    List<String> distributionStages;
    List<Integer> completionPercentiles;
    String nameStartsWith;
    String nameEndsWith;
    String nameContains;
    Long startDateAfter;
    Long startDateBefore;
    Long endDateAfter;
    Long endDateBefore;
    Long completedAtAfter;
    Long completedAtBefore;
    Integer sprintCount;

    public enum CALCULATION {
        sprint_ticket_count_report,
        sprint_story_points_report;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static JiraSprintFilter fromDefaultListRequest(DefaultListRequest filter) {
        var ids = (Collection) filter.getFilter().get("integration_ids");
        List<String> integrationIds = null;
        if (CollectionUtils.isNotEmpty(ids) && !(ids.iterator().next() instanceof String)){
            integrationIds = (List<String>) ids.stream().filter(item -> item != null).map(item -> String.valueOf(item)).collect(Collectors.toList());
        }
        return JiraSprintFilter.builder()
                .integrationIds(ListUtils.emptyIfNull(integrationIds))
                .sprintIds(ListUtils.emptyIfNull((List<String>)filter.getFilter().get("sprint_ids")))
                .sprintFullNames(ListUtils.emptyIfNull((List<String>)filter.getFilter().get("sprint_full_names")))
                .names(ListUtils.emptyIfNull((List<String>)filter.getFilter().get("sprint_names")))
                .excludeNames(ListUtils.emptyIfNull((List<String>)filter.getFilter().get("exclude_names")))
                .excludeSprintFullNames(ListUtils.emptyIfNull((List<String>)filter.getFilter().get("exclude_sprint_full_names")))
                .state((String)filter.getFilter().get("state"))
                .nameStartsWith((String)filter.getFilter().get("name_start_with"))
                .nameEndsWith((String)filter.getFilter().get("name_end_with"))
                .nameContains((String)filter.getFilter().get("name_contains"))
                .startDateAfter((Long)filter.getFilter().get("start_date_after"))
                .startDateBefore((Long)filter.getFilter().get("start_date_before"))
                .endDateAfter((Long)filter.getFilter().get("end_date_after"))
                .endDateBefore((Long)filter.getFilter().get("end_date_before"))
                .completedAtAfter((Long)filter.getFilter().get("completed_at_after"))
                .completedAtBefore((Long)filter.getFilter().get("completed_at_before"))
                .completionPercentiles(ListUtils.emptyIfNull((List<Integer>)filter.getFilter().get("percentiles")))
                .distributionStages(ListUtils.emptyIfNull((List<String>)filter.getFilter().get("distribution_stages")))
                .build();
    }
}