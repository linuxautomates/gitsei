package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.pagerduty.DbPDIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPdAlert;
import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class DBPagerdutyConverters {

    private static final String INCIDENT = "incident";

    public static RowMapper<Map<String, Object>> distinctRowMapper(ObjectMapper mapper, String calculation) {
        return (rs, row) -> {
            var aggregations = ParsingUtils.parseMap(mapper, "aggregations", String.class, Object.class, rs.getString("aggregations"));
            if (calculation.equalsIgnoreCase("trend")) {
                return Map.of(
                        "id", rs.getString("pd_service_id"),
                        "name", rs.getString("name"),
                        "type", rs.getString("type"),
                        "timestamp", TimeUnit.MILLISECONDS.toSeconds(rs.getDate("trend").getTime()),
                        "aggregations", aggregations.entrySet()
                                .stream()
                                .map(item -> Map.of("key", item.getKey(), "count", item.getValue()))
                                .collect(Collectors.toList())
                );
            } else {
                return Map.of(
                        "id", rs.getString("pd_service_id"),
                        "name", rs.getString("name"),
                        "type", rs.getString("type"),
                        "aggregations", aggregations.entrySet()
                                .stream()
                                .map(item -> Map.of("key", item.getKey(), "count", item.getValue()))
                                .collect(Collectors.toList())
                );
            }
        };
    }

    public static RowMapper<DbAggregationResult> distinctPdRowMapper(PagerDutyFilter.DISTINCT across,
                                                                     String key, String issueType) {
        return (rs, row) -> {
            switch (across) {
                case user_id:
                    return setCalculationComponent(key, "user_name", rs);
                case pd_service:
                    if (issueType.equalsIgnoreCase(INCIDENT))
                        return setCalculationComponent(key + "_id", "service_name", rs);
                    return setCalculationComponent("alert_service_id", "service_name", rs);
                case incident_created_at:
                case incident_resolved_at:
                case alert_created_at:
                case alert_resolved_at:
                    return setCalculationComponent(key, "interval", rs);
                case status:
                    if (issueType.equalsIgnoreCase(INCIDENT))
                        return setCalculationComponent("incident_" + key, null, rs);
                    return setCalculationComponent("alert_" + key, null, rs);
                case incident_priority:
                default:
                    return setCalculationComponent(key, null, rs);
            }

        };
    }

    private static DbAggregationResult setCalculationComponent(String key, String additionalKey, ResultSet rs) throws SQLException {
        return DbAggregationResult.builder()
                .key(StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(rs.getString(key)) ? rs.getString(key) : "NA")
                .additionalKey(StringUtils.isNotEmpty(additionalKey) ? rs.getString(additionalKey) : "NA")
                .max(rs.getLong("mx"))
                .min(rs.getLong("mn"))
                .median(rs.getLong("median"))
                .count(rs.getLong("ct"))
                .mean(rs.getDouble("mean"))
                .build();
    }

    public static RowMapper<DbPDIncident> incidentListMapper(ObjectMapper mapper) {
        return (rs, rowNumber) -> DbPDIncident.builder()
                .id((UUID) rs.getObject("parent_incident_id"))
                .pdServiceId((UUID) rs.getObject("pd_service_id"))
                .pdId(rs.getString("incident_pd_id"))
                .summary(rs.getString("incident_summary"))
                .urgency(rs.getString("incident_urgency"))
                .priority(rs.getString("incident_priority"))
                .details(ParsingUtils.parseJsonObject(mapper, "details", rs.getString("details")))
                .status(rs.getString("incident_status"))
                .incidentStatuses(columnPresent(rs, "statuses") ? Arrays.asList((String[]) rs.getArray("statuses").getArray()) : List.of())
                .createdAt(Instant.ofEpochSecond(rs.getLong("incident_created_at")))
                .updatedAt(Instant.ofEpochSecond(rs.getLong("incident_updated_at")))
                .lastStatusAt(Instant.ofEpochSecond(rs.getLong("incident_last_status_at")))
                .incidentAcknowledgedAt(Instant.ofEpochSecond(rs.getLong("incident_acknowledged_at")))
                .userNames(columnPresent(rs, "user_names") ? Arrays.asList((String[]) rs.getArray("user_names").getArray()) : List.of())
                .userids(columnPresent(rs, "user_ids") ? Arrays.asList((UUID[]) rs.getArray("user_id").getArray()) : List.of())
                .serviceName(columnPresent(rs, "service_name") ? rs.getString("service_name") : null)
                .integrationId(columnPresent(rs, "integration_id") ? rs.getString("integration_id") : null)
                .incidentResolvedAt(columnPresent(rs, "incident_resolved_at") ? Instant.ofEpochSecond(rs.getLong("incident_resolved_at")) : null)
                .build();
    }

    public static RowMapper<DbPdAlert> alertListMapper(ObjectMapper mapper) {
        return (rs, rowNumber) -> DbPdAlert.builder()
                .id((UUID) rs.getObject("alert_id"))
                .pdServiceId((UUID) rs.getObject("alert_service_id"))
                .pdId(rs.getString("alert_pd_id"))
                .incidentId((UUID) rs.getObject("incident_id"))
                .summary(rs.getString("alert_summary"))
                .severity(rs.getString("alert_severity"))
                .details(ParsingUtils.parseJsonObject(mapper, "alert_details", rs.getString("alert_details")))
                .status(rs.getString("alert_status"))
                .createdAt(Instant.ofEpochSecond(rs.getLong("alert_created_at")))
                .updatedAt(Instant.ofEpochSecond(rs.getLong("alert_updated_at")))
                .alertAcknowledgedAt(Instant.ofEpochSecond(rs.getLong("alert_acknowledged_at")))
                .lastStatusAt(Instant.ofEpochSecond(rs.getLong("alert_last_status_at")))
                .serviceName(rs.getString("service_name"))
                .integrationId(rs.getString("integration_id"))
                .build();
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }
}
