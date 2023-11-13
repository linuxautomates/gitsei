package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = UserDevProductivityRawStatsReport.UserDevProductivityRawStatsReportBuilder.class)
public class UserDevProductivityRawStatsReport {
    @JsonProperty("org_user_id")
    private final UUID orgUserId;
    @JsonProperty("full_name")
    private final String fullName;
    @JsonProperty("ou_attributes")
    private final Map<String,Object> ouAttributes;
    @JsonProperty("raw_stats")
    private final List<FeatureRawStat> rawStats;

    //region Report Interval Details
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("start_time")
    private final Long startTime; //Date serializes to ms, we need secs so using Long
    @JsonProperty("end_time")
    private final Long endTime;  //Date serializes to ms, we need secs so using Long
    //endregion

    //region Report Completeness

    //region Report Completeness - User
    @JsonProperty("incomplete")
    private final Boolean incomplete;
    @JsonProperty("missing_features")
    private final List<String> missingFeatures;
    //endregion

    @JsonProperty("result_time")
    private final Long resultTime;  //Date serializes to ms, we need secs so using Long
    //endregion
}
