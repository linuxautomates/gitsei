package io.levelops.commons.databases.services.pagerduty;

import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PagerDutyUtils {

    private static final String INCIDENT = "incident";
    private static final String ALERT = "alert";
    private static final String TABLE = "pd_table";
    private static final Set<PagerDutyFilter.DISTINCT> incidentFilters = Set.of(PagerDutyFilter.DISTINCT.incident_priority, PagerDutyFilter.DISTINCT.incident_created_at,
            PagerDutyFilter.DISTINCT.incident_resolved_at);
    private static final Set<String> alertSortableCols = Set.of("alert_created_at", "alert_resolved_at", "alert_severity");
    private static final Set<String> incidentSortableCols = Set.of("incident_created_at", "incident_resolved_at", "incident_priority");

    public static String sqlForPagerDutyIncidents(String company, Map<String, List<String>> conditions, String innerSelect,
                                                  boolean hasAlertFilters, PagerDutyFilter.DISTINCT across) {
        if(across.equals(PagerDutyFilter.DISTINCT.alert_created_at) ||
                across.equals(PagerDutyFilter.DISTINCT.alert_resolved_at) ||
                across.equals(PagerDutyFilter.DISTINCT.incident_resolved_at)) {
            conditions.get(TABLE).add(across + " IS NOT NULL ");
        }
        String whereCondition = "";
        if (conditions.get(TABLE).size() > 0) {
            whereCondition = " WHERE " + String.join(" AND ", conditions.get(TABLE));
        }
        String innerSelectStmt = StringUtils.isNotEmpty(innerSelect) ? innerSelect + "," : StringUtils.EMPTY;
        return " SELECT " + innerSelectStmt + " * FROM ( " + getStatusUsersJoin(company) + " SELECT " +
                getIncidentTableSelectStmt() +
                " resolve_time_status.timestamp AS incident_status_timestamp,\n" +
                " resolve_time_status.time_zone,  \n" +
                " coalesce(resolve_time_status.name,'UNASSIGNED') as user_name,\n" +
                " resolve_time_status.pd_user_id     AS user_id,\n" +
                getServicesTableSelectStmt(company, true) +
                " coalesce (resolve_time_status.timestamp, extract(epoch from now())) - i.created_at as incident_solve_time,\n" +
                " coalesce (response_time_status.timestamp, extract(epoch from now())) - i.created_at as incident_response_time," +
                " status_resolved.incident_resolved_at as incident_resolved_at," +
                " status_acknowledged.incident_acknowledged_at" +
                (hasAlertFilters ? "," + getAlertSelectStmt(company, true) : "") +
                " FROM  " + company + ".pd_incidents i " +
                " LEFT JOIN status_users resolve_time_status\n" +
                " ON         i.id = resolve_time_status.pd_incident_id\n" +
                " LEFT JOIN status_users response_time_status\n" +
                " ON         i.id = response_time_status.pd_incident_id and response_time_status.status = 'acknowledged'\n" +
                getResolvedAtJoin(company) +
                getAcknowledgedAtJoin(company) +
                getServicesTableSelectStmt(company, false) +
                (hasAlertFilters ? " LEFT JOIN " + company + ".pd_alerts alerts ON i.id = alerts.incident_id" +
                        " LEFT JOIN " + company + ".pd_alerts alerts2 ON i.id = alerts2.incident_id  AND alerts2.status = 'triggered' " : "") +
                ") z" + whereCondition;

    }

    public static String sqlForPagerDutyAlerts(String company, Map<String, List<String>> conditions
            , String innerSelect, boolean hasIncidentFilters, PagerDutyFilter.DISTINCT across) {
        if(across != null && (across.equals(PagerDutyFilter.DISTINCT.alert_created_at) ||
                across.equals(PagerDutyFilter.DISTINCT.alert_resolved_at) ||
                across.equals(PagerDutyFilter.DISTINCT.incident_resolved_at))) {
            conditions.get(TABLE).add(across + " IS NOT NULL ");
        }
        String whereCondition = "";
        if (conditions.get(TABLE).size() > 0) {
            whereCondition = " WHERE " + String.join(" AND ", conditions.get(TABLE));
        }
        String innerSelectStmt = StringUtils.isNotEmpty(innerSelect) ? innerSelect + "," : StringUtils.EMPTY;
        String alertSelectStmt = getAlertSelectStmt(company, false);
        return "SELECT " + innerSelectStmt + " * FROM   (SELECT alerts.pd_id as alert_pd_id, " +
                (hasIncidentFilters ? getIncidentTableSelectStmt()
                        + " CASE\n" +
                        "    WHEN i.status = 'resolved' THEN i.last_status_at\n" +
                        "    ELSE Extract(epoch FROM Now())\n" +
                        " END     incident_resolved_at,\n" +
                        " CASE\n" +
                        "     WHEN i.status = 'acknowledged' THEN\n" +
                        "     i.last_status_at\n" +
                        "     ELSE Extract(epoch FROM Now())\n" +
                        " END   incident_acknowledged_at,"
                        : "")
                + alertSelectStmt +
                " FROM   " + company + ".pd_alerts alerts  LEFT JOIN " + company + ".pd_alerts alerts2 " +
                " ON alerts.id = alerts2.id AND alerts.status = 'triggered'" +
                (hasIncidentFilters ? " LEFT JOIN " + company + ".pd_incidents i ON i.id = alerts.incident_id" : "") +
                " LEFT JOIN " + company + ".pd_services ps\n" +
                " ON ps.id = alerts.pd_service_id " +
                ") z " + whereCondition;
    }

    @NotNull
    private static String getAlertSelectStmt(String company, boolean fromIncidentGroupBy) {
        String servicesSelectStmt = fromIncidentGroupBy ? StringUtils.EMPTY : getServicesTableSelectStmt(company, true);
        return getAlertsTable() +
                servicesSelectStmt +
                " alerts.last_status_at AS alert_last_status_at,\n" +
                " COALESCE (alerts.last_status_at, EXTRACT(epoch FROM now())) -\n" +
                " alerts.created_at   AS alert_solve_time,\n" +
                " COALESCE (alerts2.last_status_at, EXTRACT(epoch FROM now())) -\n" +
                " alerts2.created_at    AS alert_response_time";
    }

    private static String getAlertsTable() {
        return " alerts.id as alert_id," +
                " alerts.pd_service_id  AS alert_service_id,\n" +
                " alerts.incident_id,\n" +
                " alerts.summary        AS alert_summary,\n" +
                " alerts.severity       AS alert_severity,\n" +
                " alerts.details        AS alert_details,\n" +
                " alerts.status         AS alert_status,\n" +
                " alerts.created_at     AS alert_created_at,\n" +
                " alerts.updated_at     AS alert_updated_at,\n" +
                " CASE\n" +
                "    WHEN alerts.status = 'resolved' THEN alerts.last_status_at\n" +
                "    ELSE Extract(epoch FROM Now())\n" +
                " END     alert_resolved_at," +
                " CASE\n" +
                "    WHEN alerts.status = 'acknowledged' THEN alerts.last_status_at\n" +
                "    ELSE Extract(epoch FROM Now())\n" +
                " END     alert_acknowledged_at,";
    }

    public static String getIncidentTableSelectStmt() {
        return " i.id as parent_incident_id, " +
                " i.pd_service_id,\n" +
                " i.pd_id as incident_pd_id,\n" +
                " i.summary as incident_summary,\n" +
                " i.urgency as incident_urgency,\n" +
                " i.priority as incident_priority,\n" +
                " i.details,\n" +
                " i.status as incident_status,\n" +
                " i.created_at as incident_created_at,\n" +
                " i.updated_at as incident_updated_at,\n" +
                " i.last_status_at as incident_last_status_at,\n";
    }

    public static String getStatusUsersJoinForList(String company) {
        return " WITH status_users " +
                " AS (SELECT s.pd_incident_id," +
                " array_agg(u.name) as name," +
                " array_agg(DISTINCT u.time_zone) as time_zone " +
                " FROM   " + company + ".pd_statuses s " +
                "inner join " + company + ".pd_users u " +
                "ON s.pd_user_id = u.id GROUP BY s.pd_incident_id)";
    }

    private static String getStatusUsersJoin(String company) {
        return " WITH status_users " +
                " AS (SELECT * FROM   " + company + ".pd_statuses s\n" +
                " LEFT JOIN " + company + ".pd_users u\n" +
                " ON s.pd_user_id = u.id) ";
    }

    public static String getServicesTableSelectStmt(String company, boolean getSelect) {
        if (getSelect)
            return " ps.name as  service_name,\n" +
                    " ps.integration_id as integration_id,";
        return " LEFT JOIN " + company + ".pd_services ps\n" +
                "  ON ps.id = i.pd_service_id ";
    }

    public static String getListSqlStmt(String company, Map<String, List<String>> conditions, boolean hasOfficeHoursFilter,
                                        boolean hasAlertFilters) {
        String whereCondition = "";
        if (conditions.get(TABLE).size() > 0) {
            whereCondition = " WHERE " + String.join(" AND ", conditions.get(TABLE));
        }
        String unnestTimeStamp = hasOfficeHoursFilter ? " unnest(incident_statuses.timestamps)" : " incident_statuses.timestamps";
        String unnestTimeZone = hasOfficeHoursFilter ? "unnest(status_users.time_zone)" : "status_users.time_zone";
        return "SELECT * FROM (" + getStatusUsersJoinForList(company)
                + " SELECT " + getIncidentTableSelectStmt()
                + unnestTimeStamp + " AS incident_status_timestamp,\n" +
                "       incident_statuses.status as statuses,\n"
                + unnestTimeZone + " as time_zone,\n" +
                "       status_users.NAME       AS user_names,\n" +
                "       incident_statuses.user_ids AS user_id,\n" +
                getServicesTableSelectStmt(company, true) +
                (hasAlertFilters ? getAlertsTable() : "") +
                "status_resolved.incident_resolved_at as incident_resolved_at," +
                "status_acknowledged.incident_acknowledged_at"
                + " FROM   " + company + ".pd_incidents i\n" +
                "    LEFT JOIN (\n" +
                "         SELECT   pd_incident_id,\n" +
                "                  array_agg(status)     AS  status,\n" +
                "                  array_agg(pd_user_id) AS user_ids,\n" +
                "                  array_agg(timestamp)  AS timestamps\n" +
                "         FROM     " + company + ".pd_statuses\n" +
                "         GROUP BY pd_incident_id ) AS incident_statuses \n" +
                "         ON incident_statuses.pd_incident_id = i.id\n" +
                getResolvedAtJoin(company) +
                getAcknowledgedAtJoin(company) +
                (hasAlertFilters ? " LEFT JOIN " + company + ".pd_alerts alerts ON i.id = alerts.incident_id" : "") +
                " LEFT JOIN " + company + ".pd_services ps ON ps.id = i.pd_service_id \n" +
                " LEFT JOIN status_users on status_users.pd_incident_id  = i.id ) z " + whereCondition;
    }

    public static String getResolvedAtJoin(String company) {
        return "   LEFT JOIN\n" +
                "  (SELECT   pd_incident_id,(array_agg(timestamp))[1] AS incident_resolved_at\n" +
                "  FROM     " + company + ".pd_statuses\n" +
                "  WHERE    status = 'resolved'\n" +
                "  GROUP BY pd_incident_id) as status_resolved ON \n" +
                "  status_resolved.pd_incident_id = i.id";
    }

    public static String getAcknowledgedAtJoin(String company) {
        return "   LEFT JOIN\n" +
                "  (SELECT   pd_incident_id,(array_agg(timestamp))[1] AS incident_acknowledged_at\n" +
                "  FROM     " + company + ".pd_statuses\n" +
                "  WHERE    status = 'acknowledged'\n" +
                "  GROUP BY pd_incident_id) as status_acknowledged ON \n" +
                "  status_acknowledged.pd_incident_id = i.id";
    }

    public static boolean hasAlertFilters(PagerDutyFilter filter, boolean isListQuery, String sortByKey) {
        boolean hasAlertFilter;
        hasAlertFilter = CollectionUtils.isNotEmpty(filter.getAlertSeverities())
                || CollectionUtils.isNotEmpty(filter.getAlertStatuses());
        if (filter.getAlertResolvedAt() != null && filter.getAlertResolvedAt().getRight() != null) {
            hasAlertFilter = true;
        }
        if (filter.getAlertCreatedAt() != null && filter.getAlertCreatedAt().getRight() != null) {
            hasAlertFilter = true;
        }
        if (filter.getAlertAcknowledgedAt() != null && filter.getAlertAcknowledgedAt().getRight() != null) {
            hasAlertFilter = true;
        }
        if (!isListQuery && filter.getAcross() != null && !hasAlertFilter) {
            hasAlertFilter = filter.getAcross().equals(PagerDutyFilter.DISTINCT.alert_created_at)
                    || filter.getAcross().equals(PagerDutyFilter.DISTINCT.alert_resolved_at)
                    || filter.getAcross().equals(PagerDutyFilter.DISTINCT.alert_severity);
        }
        if (alertSortableCols.contains(sortByKey) && !hasAlertFilter) {
            hasAlertFilter = true;
        }
        if(filter.getMissingFields() != null && (filter.getMissingFields().containsKey("alert_severity")
                || filter.getMissingFields().containsKey("alert_status"))) {
            hasAlertFilter = true;
        }
        boolean isIncident = filter.getIssueType().equalsIgnoreCase(INCIDENT);
        return isIncident && hasAlertFilter;
    }

    public static boolean hasIncidentFilters(PagerDutyFilter filter, String sortByKey) {
        boolean hasIncidentFilter;
        hasIncidentFilter = CollectionUtils.isNotEmpty(filter.getIncidentUrgencies())
                || CollectionUtils.isNotEmpty(filter.getIncidentStatuses()) ||
                CollectionUtils.isNotEmpty(filter.getIncidentPriorities());
        if (filter.getIncidentCreatedAt() != null && filter.getIncidentCreatedAt().getRight() != null) {
            hasIncidentFilter = true;
        }
        if (filter.getIncidentResolvedAt() != null && filter.getIncidentResolvedAt().getRight() != null) {
            hasIncidentFilter = true;
        }
        if (filter.getIncidentAcknowledgedAt() != null && filter.getIncidentAcknowledgedAt().getRight() != null) {
            hasIncidentFilter = true;
        }

        if (filter.getAcross() != null && incidentFilters.contains(filter.getAcross())) {
            hasIncidentFilter = true;
        }
        if (incidentSortableCols.contains(sortByKey) && !hasIncidentFilter) {
            hasIncidentFilter = true;
        }
        if(filter.getMissingFields() != null && filter.getMissingFields().containsKey("incident_status")) {
            hasIncidentFilter = true;
        }
        boolean isAlert = filter.getIssueType().equalsIgnoreCase(ALERT);
        return isAlert && hasIncidentFilter;
    }

    public static boolean hasOfficeFilters(PagerDutyFilter filter) {
        if (filter.getOfficeHours() != null) {
            return filter.getOfficeHours().getRight() != null && filter.getOfficeHours().getLeft() != null;
        }
        return false;
    }

    public static void checkForInvalidFilters(PagerDutyFilter filter, String sortByKey) {
        if (filter.getIssueType().equals(ALERT)) {
            if (filter.getOfficeHours() != null && filter.getOfficeHours().getLeft() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "office_hours filter cannot be used with issue_type=alert");
            }
            if (CollectionUtils.isNotEmpty(filter.getUserIds()) || filter.getAcross() == PagerDutyFilter.DISTINCT.user_id) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_ids filter cannot be used with issue_type=alert");
            }
            if (MapUtils.isNotEmpty(filter.getMissingFields()) && filter.getMissingFields().get("user_id") != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_ids filter cannot be used with issue_type=alert");
            }
            if (sortByKey.equalsIgnoreCase("user_id"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_ids filter cannot be used with issue_type=alert");
        }
    }
}
