package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IndustryDevProductivityFixedIntervalFilter.IndustryDevProductivityFixedIntervalFilterBuilder.class)
public class IndustryDevProductivityFixedIntervalFilter {
    @JsonProperty("interval")
    ReportIntervalType reportInterval;
    @JsonProperty("sort")
    Map<String, SortingOrder> sort;

    public static IndustryDevProductivityFixedIntervalFilter fromListRequest(DefaultListRequest filter) {
        return IndustryDevProductivityFixedIntervalFilter.builder()
                .reportInterval(ReportIntervalType.fromString(filter.getFilterValue("interval", String.class).orElse(ReportIntervalType.LAST_QUARTER.toString())))
                .sort(SortingConverter.fromFilter(filter.getSort()))
                .build();
    }
}
