package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUsersDevProductivityRawStatsReport.OrgUsersDevProductivityRawStatsReportBuilder.class)
public class OrgUsersDevProductivityRawStatsReport {

    @JsonProperty("ou_id")
    private final UUID ouId;

    @JsonProperty("ou_ref_id")
    private final Integer ouRefId;

    @JsonProperty("interval")
    private final ReportIntervalType interval;

    @JsonProperty("records")
    private List<UserDevProductivityRawStatsReport> records;
}
