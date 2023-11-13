package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgAndUsersDevProductivityReportMappings.OrgAndUsersDevProductivityReportMappingsBuilder.class)
public class OrgAndUsersDevProductivityReportMappings {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("dev_productivity_profile_id")
    private final UUID devProductivityProfileId;

    @JsonProperty("dev_productivity_parent_profile_id")
    private final UUID devProductivityParentProfileId;

    @JsonProperty("interval")
    private final ReportIntervalType interval;

    @JsonProperty("ou_id")
    private final UUID ouID;

    @JsonProperty("org_user_ids")
    private final List<UUID> orgUserIds;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
