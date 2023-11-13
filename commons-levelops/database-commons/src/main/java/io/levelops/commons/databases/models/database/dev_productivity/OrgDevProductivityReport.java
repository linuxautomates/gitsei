package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgDevProductivityReport.OrgDevProductivityReportBuilder.class)
public class OrgDevProductivityReport {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("ou_id")
    private final UUID ouID;
    @JsonProperty("ou_ref_id")
    private final Integer ouRefId;

    @JsonProperty("requested_ou_id")
    private final UUID requestedOUId;
    @JsonProperty("dev_productivity_profile_id")
    private final UUID devProductivityProfileId;
    @JsonProperty("dev_productivity_parent_profile_id")
    private final UUID devProductivityParentProfileId;
    @JsonProperty("dev_productivity_profile_timestamp")
    private final Instant devProductivityProfileTimestamp;
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("start_time")
    private final Instant startTime;
    @JsonProperty("end_time")
    private final Instant endTime;
    @JsonProperty("week_of_year")
    private final Integer weekOfYear;
    @JsonProperty("year")
    private final Integer year;
    @JsonProperty("latest")
    private final Boolean latest;
    @JsonProperty("score")
    private final  Integer score;
    @JsonProperty("report")
    private final DevProductivityResponse report;

    @JsonProperty("missing_user_reports_count")
    private final Integer missingUserReportsCount;

    @JsonProperty("stale_user_reports_count")
    private final Integer staleUserReportsCount;

    @JsonProperty("created_at")
    private final Instant createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;

}
