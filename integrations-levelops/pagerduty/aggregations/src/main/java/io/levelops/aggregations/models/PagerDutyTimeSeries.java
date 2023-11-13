package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.Builder.Default;
import lombok.experimental.Accessors;

import java.util.Map;

@Value
@AllArgsConstructor
@EqualsAndHashCode
@Accessors(fluent = true, chain = true)
@Builder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyTimeSeries.PagerDutyTimeSeriesBuilder.class)
public class PagerDutyTimeSeries {
    @JsonProperty("from")
    private Long from;
    @JsonProperty("to")
    private Long to;
    @JsonProperty("by_incident_resolved")
    private SimpleTimeSeriesData byIncidentResolved;
    @JsonProperty("by_incident_acknowledged")
    private SimpleTimeSeriesData byIncidentAcknowledged;
    @JsonProperty("by_alert_resolved")
    private SimpleTimeSeriesData byAlertResolved;
    @JsonProperty("by_incident_urgency")
    @JsonAlias({"by_urgency"})
    @Default
    private Map<String, Integer> byIncidentUrgency = Map.of("low", 0, "high", 0);
    @JsonProperty("by_alert_severity")
    @Default
    private Map<String, Integer> byAlertSeverity = Map.of("info", 0, "warning", 0, "error", 0, "critical", 0);
    private Map<String, Object> byService;
}