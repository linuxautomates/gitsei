package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUnitDashboardMapping.OrgUnitDashboardMappingBuilder.class)
public class OrgUnitDashboardMapping {
    UUID id;
    UUID orgUnitId;
    Integer dashboardId;
    Integer dashboardOrder;
}
