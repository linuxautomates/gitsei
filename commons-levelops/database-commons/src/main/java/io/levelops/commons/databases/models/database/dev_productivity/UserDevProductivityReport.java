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
@JsonDeserialize(builder = UserDevProductivityReport.UserDevProductivityReportBuilder.class)
public class UserDevProductivityReport {
    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("org_user_id")
    private final UUID orgUserId;
    @JsonProperty("org_user_ref_id")
    private final Integer orgUserRefId;
    @JsonProperty("requested_org_user_id")
    private final UUID requestedOrgUserId;
    @JsonProperty("dev_productivity_profile_id")
    private final UUID devProductivityProfileId;
    @JsonProperty("dev_productivity_profile_timestamp")
    private final Instant devProductivityProfileTimestamp;
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("week_of_year")
    private final Integer weekOfYear;
    @JsonProperty("year")
    private final Integer year;
    @JsonProperty("start_time")
    private final Instant startTime;
    @JsonProperty("end_time")
    private final Instant endTime;
    @JsonProperty("latest")
    private final Boolean latest;
    @JsonProperty("score")
    private final  Integer score;
    @JsonProperty("report")
    private final DevProductivityResponse report;

    @JsonProperty("incomplete")
    private final Boolean incomplete;

    @JsonProperty("missing_features")
    private final List<String> missingFeatures;

    @JsonProperty("created_at")
    private final Instant createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
