package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemHistoryFilter.WorkItemHistoryFilterBuilder.class)
public class WorkItemHistoryFilter {
    List<String> fieldTypes;
    List<String> integrationIds;
}
