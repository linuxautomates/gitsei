package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RelativeDevProductivityReport.RelativeDevProductivityReportBuilder.class)
public class RelativeDevProductivityReport {

    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("org_user_id")
    private final UUID orgUserId;
    @JsonProperty("ou_id")
    private final UUID ouID;
    @JsonProperty("dev_productivity_profile_id")
    private final UUID devProductivityProfileId;
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
    @JsonProperty("score")
    private final  Integer score;
    @JsonProperty("report")
    private final DevProductivityResponse report;
    @JsonProperty("created_at")
    private final Instant createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
