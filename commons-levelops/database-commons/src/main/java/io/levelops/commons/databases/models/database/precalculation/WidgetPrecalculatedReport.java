package io.levelops.commons.databases.models.database.precalculation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WidgetPrecalculatedReport.WidgetPrecalculatedReportBuilder.class)
public class WidgetPrecalculatedReport {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("widget_id")
    private final UUID widgetId;
    @JsonProperty("widget")
    private final Widget widget;
    @JsonProperty("list_request")
    private final DefaultListRequest listRequest;

    @JsonProperty("ou_ref_id")
    private final Integer ouRefId;
    @JsonProperty("ou_id")
    private final UUID ouID;

    @JsonProperty("report_sub_type")
    private final String reportSubType;
    @JsonProperty("report")
    private final Object report;
    @JsonProperty("calculated_at")
    private final Instant calculatedAt;


    @JsonProperty("interval")
    private final String interval;
    @JsonProperty("start_time")
    private final Instant startTime;
    @JsonProperty("end_time")
    private final Instant endTime;

    @JsonProperty("created_at")
    private final Instant createdAt;
    @JsonProperty("updated_at")
    private final Instant updatedAt;
}
