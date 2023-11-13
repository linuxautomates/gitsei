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
@JsonDeserialize(builder = OrgDevProductivityRawStatsReport.OrgDevProductivityRawStatsReportBuilder.class)
public class OrgDevProductivityRawStatsReport {

    @JsonProperty("ou_id")
    private final UUID ouId;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("raw_stats")
    private List<FeatureRawStat> rawStats;


    //region Report Interval Details
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("start_time")
    private final Long startTime; //Date serializes to ms, we need secs so using Long
    @JsonProperty("end_time")
    private final Long endTime;  //Date serializes to ms, we need secs so using Long
    //endregion

    //region Report Completeness

    //region Report Completeness - Org
    @JsonProperty("missing_user_reports_count")
    private final Integer missingUserReportsCount;
    @JsonProperty("stale_user_reports_count")
    private final Integer staleUserReportsCount;
    //endregion

    @JsonProperty("result_time")
    private final Long resultTime;  //Date serializes to ms, we need secs so using Long
    //endregion
}
