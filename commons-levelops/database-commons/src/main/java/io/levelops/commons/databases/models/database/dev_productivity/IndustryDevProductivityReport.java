package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IndustryDevProductivityReport.IndustryDevProductivityReportBuilder.class)
public class IndustryDevProductivityReport {

    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("interval")
    private final ReportIntervalType interval;
    @JsonProperty("score")
    private final  Integer score;
    @JsonProperty("report")
    private final DevProductivityResponse report;
    @JsonProperty("created_at")
    private final Instant createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;

}
