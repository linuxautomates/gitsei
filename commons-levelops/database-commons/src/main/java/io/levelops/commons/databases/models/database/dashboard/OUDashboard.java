package io.levelops.commons.databases.models.database.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OUDashboard.OUDashboardBuilder.class)
public class OUDashboard {
    @JsonProperty("ou_id")
    UUID ouId;
    @JsonProperty("dashboard_order")
    Integer dashboardOrder;
    @JsonProperty("dashboard_id")
    Integer dashboardId;
    @JsonProperty("name")
    String name;
    @JsonProperty(value = "display_name")
    private String displayName;



}
