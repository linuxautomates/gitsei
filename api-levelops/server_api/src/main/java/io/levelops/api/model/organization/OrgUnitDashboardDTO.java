package io.levelops.api.model.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUnitDashboardDTO.OrgUnitDashboardDTOBuilder.class)
public class OrgUnitDashboardDTO {
    @JsonProperty("ou_id")
    private final UUID ouId;
    @JsonProperty("dashboard_order")
    private final Integer dashboardOrder;
    @JsonProperty("is_default")
    private final Boolean isDefault;
    @JsonProperty("dashboard_id")
    private final Integer dashboardId;

}
