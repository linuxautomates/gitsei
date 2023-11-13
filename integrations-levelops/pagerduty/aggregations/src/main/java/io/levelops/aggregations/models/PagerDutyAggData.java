package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.integrations.pagerduty.models.PagerDutyAlert;
import io.levelops.integrations.pagerduty.models.PagerDutyIncident;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true, chain = true)
@ToString
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyAggData.PagerDutyAggDataBuilderImpl.class)
public class PagerDutyAggData extends AggData {
    // incident
    @JsonProperty("agg_incidents")
    private Map<String, Integer> aggIncidents;
    // incident.urgency (high, low)
    @JsonProperty("agg_incidents_by_urgency")
    private Map<String, Integer> aggIncidentsByUrgency;
    // incident.priority.summary (distinct)
    @JsonProperty("agg_incidents_by_priority")
    private Map<String, Integer> aggIncidentsByPriority;
    // incident.urgency
    // incident.priority.summary
    @JsonProperty("agg_incidents_by_urgency_priority")
    private Map<String, Integer> aggIncidentsByUrgencyPriority;
    // alert
    @JsonProperty("agg_alerts")
    private Map<String, Integer> aggAlerts;
    // alert.severity (info, warning, error, critical)
    @JsonProperty("agg_alerts_by_severity")
    private Map<String, Integer> aggAlertsBySeverity;
    @JsonProperty("latest_incidents_by_urgency")
    private Map<String, List<PagerDutyIncident>> latestIncidentsByUrgency;
    @JsonProperty("latest_incidents_by_priority")
    private Map<String, List<PagerDutyIncident>> latestIncidentsByPriority;
    @JsonProperty("latest_alerts_by_severity")
    private Map<String, List<PagerDutyAlert>> latestAlertsBySeverity;
    @JsonProperty("time_series")
    private Set<PagerDutyTimeSeries> timeSeries;

    @JsonPOJOBuilder(withPrefix = "")
    static final class PagerDutyAggDataBuilderImpl extends PagerDutyAggDataBuilder<PagerDutyAggData, PagerDutyAggDataBuilderImpl> {
    }
}