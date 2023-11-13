package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemsSprintMappingFilter.WorkItemsSprintMappingFilterBuilder.class)
public class WorkItemsSprintMappingFilter {
    Boolean ignorableWorkitemType;

    public static WorkItemsSprintMappingFilter fromDefaultListRequest(DefaultListRequest filter) {
        Boolean ignorableWorkitemType = null;
        if (filter.getFilterValue("sprint_mapping_ignorable_workitem_type", Boolean.class).isPresent())
            ignorableWorkitemType = filter.getFilterValue("sprint_mapping_ignorable_workitem_type", Boolean.class).get();
        return WorkItemsSprintMappingFilter.builder()
                .ignorableWorkitemType(ignorableWorkitemType)
                .build();
    }
}
