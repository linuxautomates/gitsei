package io.levelops.aggregations.functions;

import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryField.Operation;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.queryops.QueryGroup.GroupOperator;
import io.levelops.commons.databases.services.temporary.PagerDutyQueryTable;
import io.levelops.integrations.pagerduty.models.PagerDutyAlert;
import io.levelops.integrations.pagerduty.models.PagerDutyIncident;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.STRING;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.CONTAINS;;

@Log4j2
public class PagerDutyAggQuery {
    private static DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));

    //#region Alerts

    public static int countAlertsBySeverity(final PagerDutyQueryTable table, final String severity) throws SQLException {
        return countAlerts(table, severity, null, null, null);
    }

    public static List<PagerDutyAlert> getAlertsBySeverity(final PagerDutyQueryTable table, 
                                                            final String severity, 
                                                            final int page, 
                                                            final int pageSize) throws SQLException {
        return getAlerts(table, severity, null, null, page, pageSize);
    }

    public static int countAlertsBySummaryContents(final PagerDutyQueryTable table, final String content) 
        throws SQLException {
        return countAlerts(table, null, content, null, null);
    }

    public static List<PagerDutyAlert> getAlertsBySummaryContents(final PagerDutyQueryTable table, 
                                                                    final String content, 
                                                                    final int page, 
                                                                    final int pageSize) 
        throws SQLException {
        return getAlerts(table, null, content, null, page, pageSize);
    }

    public static List<PagerDutyAlert> getAlertsByStatus(final PagerDutyQueryTable table, 
                                                                    final PagerDutyAlert.Status status,
                                                                    final Long from,
                                                                    final Long to,
                                                                    final int page, 
                                                                    final int pageSize) 
        throws SQLException {
        return getAlerts(table, null, null, status, from, to, page, pageSize);
    }

    /**
     * Retrieves incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param severity Severity to match in the query
     * @param content text to search for in the incident's description
     * @return number of incidents that matched the search criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static int countAlerts(final PagerDutyQueryTable table, final String severity, final String content, final Long from, final Long to) 
        throws SQLException {
        List<QueryField> queries = new ArrayList<>();
        queries.add(new QueryField("ingestion_data_type", "ALERT"));
        if (!Strings.isBlank(severity)) {
            queries.add(new QueryField("severity", severity));
        }
        if (!Strings.isBlank(content)) {
            queries.add(new QueryField("summary", STRING, CONTAINS, content));
        }
        if (from != null && from > 0L) {
            queries.add(new QueryField("created_at", STRING, Operation.PREFIX, DAY_DATE_FORMATTER.format(Instant.ofEpochMilli(from))));
        }
        // if (to != null && to > 0L) {
        //     queries.add(new QueryField("created_at", NUMBER, LESS_THAN, to));
        // }
        return table.countRows(List.of(new QueryGroup(queries, GroupOperator.AND)), true);
    }

    /**
     * Retrieves incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param severity Severity to match in the query
     * @param content text to search for in the incident's description
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static List<PagerDutyAlert> getAlerts(final PagerDutyQueryTable table, 
                                                final String severity, 
                                                final String content, 
                                                final PagerDutyAlert.Status status,
                                                final int page, final int pageSize) throws SQLException {
        return getAlerts(table, severity, content, status, null, null, page, pageSize);
    }

    /**
     * Retrieves incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param severity Severity to match in the query
     * @param content text to search for in the incident's description
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static List<PagerDutyAlert> getAlerts(final PagerDutyQueryTable table, 
                                                final String severity, 
                                                final String content, 
                                                final PagerDutyAlert.Status status,
                                                final Long from,
                                                final Long to,
                                                final int page, final int pageSize) throws SQLException {
        log.debug("GetAlerts: table={}, severity={}, content={}, status={}, from={}, to={}, page={}, pageSize={}", table, severity, content, status, from, to, page, pageSize);
        List<QueryField> queries = new ArrayList<>();
        queries.add(new QueryField("ingestion_data_type", "ALERT"));
        if (!Strings.isBlank(severity)) {
            queries.add(new QueryField("severity", severity));
        }
        if (!Strings.isBlank(content)) {
            queries.add(new QueryField("description", STRING, CONTAINS, content));
        }
        if (status != null) {
            queries.add(new QueryField("status", status.toString()));
        }
        if (from != null && from > 0L) {
            queries.add(new QueryField("resolved_at", STRING, Operation.PREFIX, DAY_DATE_FORMATTER.format(Instant.ofEpochMilli(from))));
        }
        // if (to != null && to > 0L) {
        //     queries.add(new QueryField("resolved_at", NUMBER, LESS_THAN, to));
        // }
        return table.getRows(List.of(new QueryGroup(queries, GroupOperator.AND)), true, page, pageSize).stream().map(item -> (PagerDutyAlert)item).collect(Collectors.toList());
    }

    //#endregion Alerts

    //#region Incidents

    /**
     * Retrieves a set of unique values for incident.priority
     * @param table The temp table to use
     * @return Set of unique priorities
     * @throws SQLException if there are issues querying the db
     */
    public static Set<String> getUniqueIncidentPriorities(final PagerDutyQueryTable table) 
        throws SQLException {
        return new HashSet<>(table.distinctValues(
            new QueryField(
                "priority", 
                QueryField.FieldType.STRING,
                QueryField.Operation.NON_NULL_CHECK, 
                Collections.emptyList(), 
                null)));
    }

    /** 
     * Counts the incidents that match the urgency specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @return number of incidents that match the criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static int countIncidentsByUrgency(final PagerDutyQueryTable table, final String urgency) throws SQLException {
        return countIncidents(table, urgency, null, null, null, null);
    }

    /** 
     * Retrieves incidents by looking up for the urgency specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static List<PagerDutyIncident> getIncidentsByUrgency(final PagerDutyQueryTable table, final String urgency, final Long from, final Long to, final int page, final int pageSize) 
        throws SQLException {
        return getIncidents(table, urgency, null, null, null, from, to, page, pageSize);
    }

    /** 
     * Retrieves incidents by looking up for the urgency specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static List<PagerDutyIncident> getIncidentsByUrgency(final PagerDutyQueryTable table, final String urgency, final int page, final int pageSize) 
        throws SQLException {
        return getIncidents(table, urgency, null, null, null, page, pageSize);
    }

    /** 
     * Counts the incidents that match the priority specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param priority Priority to match in the query
     * @return number of incidents that match the criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static int countIncidentsByPriority(final PagerDutyQueryTable table, final String priority) throws SQLException {
        return countIncidents(table, null, priority, null, null, null);
    }

    /** 
     * Retrieves incidents by looking up for the priority specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param priority Priority to match in the query
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static List<PagerDutyIncident> getIncidentsByPriority(final PagerDutyQueryTable table, final String priority, final Long from, final Long to, final int page, final int pageSize) 
        throws SQLException {
        return getIncidents(table, null, priority, null, null, from, to, page, pageSize);
    }

    /** 
     * Retrieves incidents by looking up for the priority specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param priority Priority to match in the query
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static List<PagerDutyIncident> getIncidentsByPriority(final PagerDutyQueryTable table, final String priority, final int page, final int pageSize) 
        throws SQLException {
        return getIncidents(table, null, priority, null, null, page, pageSize);
    }

    /** 
     * Counts the incidents that match the urgency and priority specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @param priority Priority to match in the query
     * @return number of incidents that match the criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static int countIncidentsByUrgencyAndPriority(final PagerDutyQueryTable table, final String urgency, final String priority) 
        throws SQLException {
        return countIncidents(table, urgency, priority, null, null, null);
    }

    /** 
     * Retrieves incidents by looking up for the urgency and priority specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @param priority Priority to match in the query
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static List<PagerDutyIncident> getIncidentsByUrgencyAndPriority(final PagerDutyQueryTable table, 
                                                                            final String urgency, 
                                                                            final String priority, 
                                                                            final int page, 
                                                                            final int pageSize) throws SQLException {
        return getIncidents(table, urgency, priority, null, null, page, pageSize);
    }

    /** 
     * Retrieves incidents by looking up for the urgency and priority specified.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param status match the status in the incident
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException exception if there is any issue while querying the temptable
     * 
     */
    public static List<PagerDutyIncident> getIncidentsByStatus(final PagerDutyQueryTable table, 
                                                                            final PagerDutyIncident.Status status, 
                                                                            final Long from,
                                                                            final Long to,
                                                                            final int page,
                                                                            final int pageSize) throws SQLException {
        return getIncidents(table, null, null, null, status, from, to, page, pageSize);
    }

    /**
     * Counts incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param content text to search for in the incident's description
     * @return number of incidents that match the criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static int countIncidentsBySummaryContents(final PagerDutyQueryTable table, final String content) 
        throws SQLException {
        return countIncidents(table, null, null, content, null, null);
    }

    /**
     * Retrieves incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @param priority Priority to match in the query
     * @param content text to search for in the incident's description
     * @param from Issue creation time - start of the time frame to analyze.
     * @param to Issue creation time - end of the time frame to analyze.
     * @return number of incidents that matched the search criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static int countIncidents(final PagerDutyQueryTable table, final String urgency, final String priority, final String content, final Long from, final Long to)
        throws SQLException {
        List<QueryField> queries = new ArrayList<>();
        queries.add(new QueryField("ingestion_data_type", "INCIDENT"));
        if (!Strings.isBlank(urgency)) {
            queries.add(new QueryField("urgency", urgency));
        }
        if (!Strings.isBlank(priority)) {
            queries.add(new QueryField("priority", priority));
        }
        if (!Strings.isBlank(content)) {
            queries.add(new QueryField("description", STRING, CONTAINS, content));
        }
        if (from != null && from > 0L) {
            queries.add(new QueryField("created_at", STRING, Operation.PREFIX, DAY_DATE_FORMATTER.format(Instant.ofEpochMilli(from))));
        }
        // if (to != null && to > 0L) {
        //     queries.add(new QueryField("created_at", NUMBER, LESS_THAN, to));
        // }
        
        return table.countRows(List.of(new QueryGroup(queries, GroupOperator.AND)), true);
    }

    /**
     * Retrieves incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @param priority Priority to match in the query
     * @param content text to search for in the incident's description
     * @param status the status of the incident to match
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static List<PagerDutyIncident> getIncidents(final PagerDutyQueryTable table, 
                                                        final String urgency, 
                                                        final String priority, 
                                                        final String content, 
                                                        final PagerDutyIncident.Status status,
                                                        final int page, final int pageSize) throws SQLException {
        return getIncidents(table, urgency,priority, content, status, null, null, page, pageSize);
    }

    /**
     * Retrieves incidents that contain the specified search criteria.
     * 
     * @param table The Query Table object to be used to query fot the data.
     * @param urgency Urgency to match in the query
     * @param priority Priority to match in the query
     * @param content text to search for in the incident's description
     * @param status the status of the incident to match
     * @param from the starting point of the 'updated at' field to fetch incidents
     * @param to the max time of the 'updated at' field to fetch incidents
     * @return List of PagerDutyEntities that matched the search criteria
     * @throws SQLException if something goes wrong during the querying
     */
    public static List<PagerDutyIncident> getIncidents(final PagerDutyQueryTable table, 
                                                        final String urgency, 
                                                        final String priority, 
                                                        final String content, 
                                                        final PagerDutyIncident.Status status,
                                                        final Long from,
                                                        final Long to,
                                                        final int page, final int pageSize) throws SQLException {
        List<QueryField> queries = new ArrayList<>();
        queries.add(new QueryField("ingestion_data_type", "INCIDENT"));
        if (!Strings.isBlank(urgency)) {
            queries.add(new QueryField("urgency", urgency));
        }
        if (!Strings.isBlank(priority)) {
            queries.add(new QueryField("priority", priority));
        }
        if (!Strings.isBlank(content)) {
            queries.add(new QueryField("description", STRING, CONTAINS, content));
        }
        if (status != null) {
            queries.add(new QueryField("status", status.toString()));
        }
        if (from != null && from > 0L) {
            queries.add(new QueryField("last_status_change_at", STRING, Operation.PREFIX, DAY_DATE_FORMATTER.format(Instant.ofEpochMilli(from))));
        }
        // if (to != null && to > 0L) {
        //     queries.add(new QueryField("last_status_change_at", NUMBER, LESS_THAN, to));
        // }
        return table.getRows(List.of(new QueryGroup(queries, GroupOperator.AND)), true, page, pageSize).stream().map(item -> (PagerDutyIncident)item).collect(Collectors.toList());
    }

    //#endregion Incidents

    public void countUnplannedEvents(){

    }

    public void getTimeSpentInUnplannedEvents(){

    }
}