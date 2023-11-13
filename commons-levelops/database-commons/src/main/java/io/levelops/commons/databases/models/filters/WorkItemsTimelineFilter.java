package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemsTimelineFilter.WorkItemsTimelineFilterBuilder.class)
public class WorkItemsTimelineFilter {
    List<String> fieldTypes;
    List<String> integrationIds;
    List<String> workItemIds;
    List<String> fieldValues;
    List<String> excludeFieldValues;
}
