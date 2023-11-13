package io.levelops.aggregations.helpers;

import io.levelops.aggregations.functions.PagerDutyAggQuery;
import io.levelops.aggregations.models.SimpleTimeSeriesData;
import io.levelops.commons.databases.services.temporary.PagerDutyQueryTable;
import io.levelops.integrations.pagerduty.models.PagerDutyAlert;
import io.levelops.integrations.pagerduty.models.PagerDutyAlert.Severity;
import io.levelops.integrations.pagerduty.models.PagerDutyIncident;
import io.levelops.integrations.pagerduty.models.PagerDutyIncident.Urgency;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class PagerDutyAggregator {
    private static final int MAX_PAGES = 100;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * Aggregate incidents by prioriy distinct values.
     * 
     * @param table The temp table to be used
     * @return A map of priorities and counts of incidents with the priorities as the keys.
     */
    public static Map<String, Integer> aggregateIncidentsByPriority(final PagerDutyQueryTable table) {
        try {
            Set<String> priorities = PagerDutyAggQuery.getUniqueIncidentPriorities(table);
            Map<String, Integer> results = new HashMap<>();
            for (String priority: priorities) {
                results.put(priority, PagerDutyAggQuery.countIncidentsByPriority(table, priority));
            }
            return results;
        } catch (SQLException e) {
            log.error("Exception while running aggregations of incindents by priority.", e);
        }
        return null;
    }

    /**
     * Aggregate incidents by urgency distinct values.
     * 
     * @param table The temp table to be used
     * @return A map of urgencies and counts of incidents with the urgencies as the keys.
     */
    public static Map<String, Integer> aggregateIncidentsByUrgency(final PagerDutyQueryTable table) {
        try {
            Map<String, Integer> results = new HashMap<>();
            for (Urgency urgency:Urgency.values()) {
                results.put(urgency.toString(), PagerDutyAggQuery.countIncidentsByUrgency(table, urgency.toString()));
            }
            return results;
        } catch (SQLException e) {
            log.error("Exception while running aggregations of incindents by urgency.", e);
        }
        return null;
    }

    /**
     * Aggregate alerts by severity distinct values.
     * 
     * @param table The temp table to be used
     * @return A map of severities and counts of alerts with the severities as the keys.
     */
    public static Map<String, Integer> aggregateAlertsBySeverity(final PagerDutyQueryTable table) {
        try {
            Map<String, Integer> results = new HashMap<>();
            for (Severity severity:Severity.values()) {
                results.put(severity.toString(), PagerDutyAggQuery.countAlertsBySeverity(table, severity.toString()));
            }
            return results;
        } catch (SQLException e) {
            log.error("Exception while running aggregations of alerts by severity.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the priority values as keys and a list of the latest 100 incidents for the corresponding priority.
     */
    public static Map<String, List<PagerDutyIncident>> getLatestIncidentsByPriority(final PagerDutyQueryTable table) {
        try {
            Set<String> priorities = PagerDutyAggQuery.getUniqueIncidentPriorities(table);
            Map<String, List<PagerDutyIncident>> results = new HashMap<>();
            for (String priority: priorities) {
                results.put(priority, PagerDutyAggQuery.getIncidentsByPriority(table, priority, DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
            }
            return results;
        } catch (SQLException e) {
            log.error("Exception while running aggregations of incindents by priority.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the urgency values as keys and a list of the latest 100 incidents for the corresponding urgency.
     */
    public static Map<String, List<PagerDutyIncident>> getLatestIncidentsByUrgency(final PagerDutyQueryTable table) {
        try {
            Map<String, List<PagerDutyIncident>> results = new HashMap<>();
            for (Urgency urgency:Urgency.values()) {
                results.put(urgency.toString(), PagerDutyAggQuery.getIncidentsByUrgency(table, urgency.toString(), DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
            }
            return results;
        } catch (SQLException e) {
            log.error("Exception while running aggregations of incindents by urgency.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the severity values as keys and a list of the latest 100 alerts for the corresponding severity.
     */
    public static Map<String, List<PagerDutyAlert>> getLatestAlertsBySeverity(final PagerDutyQueryTable table) {
        try {
            Map<String, List<PagerDutyAlert>> results = new HashMap<>();
            for (Severity severity:Severity.values()) {
                results.put(severity.toString(), PagerDutyAggQuery.getAlertsBySeverity(table, severity.toString(), DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
            }
            return results;
        } catch (SQLException e) {
            log.error("Exception while running aggregations of alerts by severity.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the stats for the lead time for incident resolution
     */
    public static Map<String, Integer> getAlertsBySeverityTimeSeriesStats(final PagerDutyQueryTable table, final Instant from, final Instant to) {
        try {
                return Map.of(
                    "info", PagerDutyAggQuery.countAlerts(table, PagerDutyAlert.Severity.info.toString(), null, from.toEpochMilli(), to.toEpochMilli()),
                    "warning", PagerDutyAggQuery.countAlerts(table, PagerDutyAlert.Severity.warning.toString(), null, from.toEpochMilli(), to.toEpochMilli()),
                    "error", PagerDutyAggQuery.countAlerts(table, PagerDutyAlert.Severity.error.toString(), null, from.toEpochMilli(), to.toEpochMilli()),
                    "critical", PagerDutyAggQuery.countAlerts(table, PagerDutyAlert.Severity.critical.toString(), null, from.toEpochMilli(), to.toEpochMilli())
                );
        } catch (SQLException e) {
            log.error("Exception while running aggregations of alerts by severity time series stats.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the stats for the lead time for incident resolution
     */
    public static Map<String, Integer> getIncidentsByUrgencyTimeSeriesStats(final PagerDutyQueryTable table, final Instant from, final Instant to) {
        try {
                return Map.of(
                    "low", PagerDutyAggQuery.countIncidents(table, PagerDutyIncident.Urgency.low.toString(), null, null, from.toEpochMilli(), to.toEpochMilli()),
                    "high", PagerDutyAggQuery.countIncidents(table, PagerDutyIncident.Urgency.high.toString(), null, null, from.toEpochMilli(), to.toEpochMilli())
                );
        } catch (SQLException e) {
            log.error("Exception while running aggregations of incindents by urgency time series stats.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the stats for the lead time for incident resolution
     */
    public static SimpleTimeSeriesData getAlertsResolvedLeadTimeStats(final PagerDutyQueryTable table, final Instant from, final Instant to) {
        try {
            var min = new Long[]{0L};
            var max = new Long[]{0L};
            var mean = new Long[]{0L};
            for(var page=0; page < MAX_PAGES; page++) {
                var alerts = PagerDutyAggQuery.getAlertsByStatus(table, PagerDutyAlert.Status.RESOLVED, from.toEpochMilli(), to.toEpochMilli(), page, DEFAULT_PAGE_SIZE);
                if (CollectionUtils.isEmpty(alerts)) {
                    break;
                }
                var pageMean = alerts.stream().map(entity -> (PagerDutyAlert)entity).filter(alert -> !alert.getCreatedAt().toInstant().isBefore(from) ).mapToLong(alert -> {
                    var time = alert.getResolvedAt().getTime() - alert.getCreatedAt().getTime();
                    max[0] = time > max[0] ? time: max[0];
                    min[0] = min[0] > time ? time: min[0];
                    return time;
                })
                .sum()/alerts.size();
                mean[0] = mean[0] > 0 ? (mean[0] + pageMean)/2 : pageMean;
                if (alerts.size() < DEFAULT_PAGE_SIZE){
                    break;
                }
            }
            return SimpleTimeSeriesData.builder()
                .low(Long.valueOf(min[0]/1000).intValue())
                .high(Long.valueOf(max[0]/1000).intValue())
                .medium(Long.valueOf(mean[0]/1000).intValue())
                .build();
        } catch (SQLException e) {
            log.error("Exception while running aggregations of alert lead time by status.", e);
        }
        return null;
    }

    /**
     * Helper method to get a map that contains the stats for the lead time for incident resolution
     */
    public static SimpleTimeSeriesData getIncidentsResolvedLeadTimeStats(final PagerDutyQueryTable table, final Instant from, final Instant to) {
        try {
            var min = new Long[]{0L};
            var max = new Long[]{0L};
            var mean = new Long[]{0L};
            for(var page=0; page < MAX_PAGES; page++) {
                var incidents = PagerDutyAggQuery.getIncidentsByStatus(table, PagerDutyIncident.Status.RESOLVED, from.toEpochMilli(), null, page, DEFAULT_PAGE_SIZE);
                if (CollectionUtils.isEmpty(incidents)) {
                    break;
                }
                var pageMean = incidents.stream().map(entity -> (PagerDutyIncident)entity).filter(incident -> incident.getLastStatusChangeAt() != null ).mapToLong(incident -> {
                    var time = incident.getLastStatusChangeAt().getTime() - incident.getCreatedAt().getTime();
                    max[0] = time > max[0] ? time: max[0];
                    min[0] = min[0] > time ? time: min[0];
                    return time;
                })
                .sum()/incidents.size();
                mean[0] = mean[0] > 0 ? (mean[0] + pageMean)/2 : pageMean;
                if (incidents.size() < DEFAULT_PAGE_SIZE){
                    break;
                }
            }
            return SimpleTimeSeriesData.builder()
                .low(Long.valueOf(min[0]/1000).intValue())
                .high(Long.valueOf(max[0]/1000).intValue())
                .medium(Long.valueOf(mean[0]/1000).intValue())
                .build();
        } catch (SQLException e) {
            log.error("Exception while running aggregations of incindents lead time by status.", e);
        }
        return null;
    }
}