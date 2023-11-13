package io.levelops.commons.dashboard_widget.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DashboardMetadata.DashboardMetadataBuilder.class)
public class DashboardMetadata {
    @JsonProperty("dashboard_time_range")
    private final Boolean dashboardTimeRange;
    @JsonProperty("show_org_unit_selection")
    private final Boolean showOrgUnitSelection;
    @JsonProperty("dashboard_time_range_filter")
    private final ReportIntervalType dashboardTimeRangeFilter;
}