package io.levelops.commons.databases.services.pagerduty;

import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.ingestion.models.IntegrationType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PagerDutyConditionBuilder {

    private static final String PD_TABLE = "pd_table";

    private static final String afterHoursTime =
            "( incident_status_timestamp > extract(epoch from to_timestamp(''{0} {3}:00'', ''YYYY-MM-DD HH24:MI:SS'') at time zone ''UTC'' at time zone time_zone) " +
                    "AND incident_status_timestamp < extract(epoch from to_timestamp(''{1} {2}:00'', ''YYYY-MM-DD HH24:MI:SS'') at time zone ''UTC'' at time zone time_zone))";
    private static final String INCIDENT = "incident";

    public static Map<String, List<String>> createWhereClauseAndUpdateParams(String company, MapSqlParameterSource params,
                                                                             PagerDutyFilter filter, boolean isListQuery,
                                                                             OUConfiguration ouConfig, String issueType) {
        List<String> tableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            tableConditions.add("integration_id IN (:integrationIds)");
            params.addValue("integrationIds", filter.getIntegrationIds().stream()
                    .map(Integer::parseInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getIncidentPriorities())) {
            tableConditions.add("incident_priority IN (:incidentPriority)");
            params.addValue("incidentPriority", filter.getIncidentPriorities());
        }
        if (CollectionUtils.isNotEmpty(filter.getIncidentUrgencies())) {
            tableConditions.add("incident_urgency IN (:incidentUrgency)");
            params.addValue("incidentUrgency", filter.getIncidentUrgencies());
        }
        if (CollectionUtils.isNotEmpty(filter.getIncidentStatuses())) {
            tableConditions.add("incident_status IN (:incidentStatus)");
            params.addValue("incidentStatus", filter.getIncidentStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getAlertStatuses())) {
            tableConditions.add("alert_status IN (:alertStatus)");
            params.addValue("alertStatus", filter.getAlertStatuses());
        }
        if (MapUtils.isNotEmpty(filter.getMissingFields())) {
            Map<PagerDutyFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields = new EnumMap<>(
                    PagerDutyFilter.MISSING_BUILTIN_FIELD.class);
            filter.getMissingFields().forEach((field, shouldBeMissing) -> {
                Optional.ofNullable(PagerDutyFilter.MISSING_BUILTIN_FIELD.fromString(field))
                        .ifPresent(builtinField -> missingBuiltinFields.put(builtinField, shouldBeMissing));

            });
            tableConditions.addAll(getMissingFieldsClause(missingBuiltinFields, isListQuery));
        }
        if (CollectionUtils.isNotEmpty(filter.getUserNames())) {
            tableConditions.add("user_name = 'UNASSIGNED'");
        }
        if (CollectionUtils.isNotEmpty(filter.getUserIds())) {
            if (CollectionUtils.isNotEmpty(filter.getUserIds()) || OrgUnitHelper.doesOuConfigHavePagerDutyUsers(ouConfig)) {
                // OU: user_ids
                if (OrgUnitHelper.doesOuConfigHavePagerDutyUsers(ouConfig)) {
                    var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params.getValues(), IntegrationType.PAGERDUTY);
                    if (StringUtils.isNotBlank(usersSelect)) {
                        if (isListQuery)
                            tableConditions.add(MessageFormat.format("{0} && ARRAY[{0}]::uuid[] IN (SELECT id FROM ({1}) l)", "user_id", usersSelect));
                        else
                            tableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", "user_id", usersSelect));
                    }
                } else if (CollectionUtils.isNotEmpty(filter.getUserIds())) {
                    if (isListQuery) {
                        tableConditions.add("user_id && ARRAY[ :userIds ]::uuid[]");
                    } else {
                        tableConditions.add("user_id IN (:userIds)");
                    }
                    params.addValue("userIds", filter.getUserIds().stream().map(UUID::fromString).collect(Collectors.toList()));
                }
            }

        }
        if (CollectionUtils.isNotEmpty(filter.getPdServiceIds())) {
            String key = "pd_service_id";
            if (!issueType.equals(INCIDENT)) {
                key = "alert_service_id";
            }
            tableConditions.add(key + " IN (:pdServiceIds)");
            params.addValue("pdServiceIds", filter.getPdServiceIds().stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getAlertSeverities())) {
            tableConditions.add("alert_severity IN (:alertSeverity)");
            params.addValue("alertSeverity", filter.getAlertSeverities());
        }
        if (filter.getAlertCreatedAt() != null) {
            if (filter.getAlertCreatedAt().getLeft() != null) {
                tableConditions.add("alert_created_at > " + filter.getAlertCreatedAt().getLeft());
            }
            if (filter.getAlertCreatedAt().getRight() != null) {
                tableConditions.add("alert_created_at < " + filter.getAlertCreatedAt().getRight());
            }
        }
        if (filter.getAlertResolvedAt() != null) {
            if (filter.getAlertResolvedAt().getLeft() != null) {
                tableConditions.add("alert_resolved_at > " + filter.getAlertResolvedAt().getLeft());
            }
            if (filter.getAlertResolvedAt().getRight() != null) {
                tableConditions.add("alert_resolved_at < " + filter.getAlertResolvedAt().getRight());
            }
        }
        if (filter.getIncidentAcknowledgedAt() != null) {
            if (filter.getIncidentAcknowledgedAt().getLeft() != null) {
                tableConditions.add("incident_acknowledged_at > " + filter.getIncidentAcknowledgedAt().getLeft());
            }
            if (filter.getIncidentAcknowledgedAt().getRight() != null) {
                tableConditions.add("incident_acknowledged_at < " + filter.getIncidentAcknowledgedAt().getRight());
            }
        }
        if (filter.getAlertAcknowledgedAt() != null) {
            if (filter.getAlertAcknowledgedAt().getLeft() != null) {
                tableConditions.add("alert_acknowledged_at > " + filter.getAlertAcknowledgedAt().getLeft());
            }
            if (filter.getAlertAcknowledgedAt().getRight() != null) {
                tableConditions.add("alert_acknowledged_at < " + filter.getAlertAcknowledgedAt().getRight());
            }
        }
        if (filter.getOfficeHours() != null) {
            if (filter.getOfficeHours().getRight() != null || filter.getOfficeHours().getLeft() != null) {
                var activeInstant = Instant.ofEpochSecond(filter.getOfficeHoursFrom());
                var toInstant = Instant.ofEpochSecond(filter.getOfficeHoursTo());
                DateTimeFormatter userTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
                final String startShift = ObjectUtils.firstNonNull(filter.getOfficeHours().getLeft(), "09:00");
                final String endShift = ObjectUtils.firstNonNull(filter.getOfficeHours().getRight(), "17:00");
                while (activeInstant.isBefore(toInstant)) {
                    var initialDay = userTimeFormater.format(activeInstant);
                    activeInstant = activeInstant.plus(Duration.ofDays(1));
                    var nextDay = userTimeFormater.format(activeInstant);
                    tableConditions.add(MessageFormat.format(afterHoursTime, initialDay, nextDay, startShift, endShift));
                }
            }
        }
        if (filter.getIncidentCreatedAt() != null) {
            if (filter.getIncidentCreatedAt().getLeft() != null) {
                tableConditions.add("incident_created_at > " + filter.getIncidentCreatedAt().getLeft());
            }
            if (filter.getIncidentCreatedAt().getRight() != null) {
                tableConditions.add("incident_created_at <" + filter.getIncidentCreatedAt().getRight());
            }
        }
        if (filter.getIncidentResolvedAt() != null) {
            if (filter.getIncidentResolvedAt().getLeft() != null) {
                tableConditions.add("incident_resolved_at > " + filter.getIncidentResolvedAt().getLeft());
            }
            if (filter.getIncidentResolvedAt().getRight() != null) {
                tableConditions.add("incident_resolved_at < " + filter.getIncidentResolvedAt().getRight());
            }
        }
        return Map.of(PD_TABLE, tableConditions);
    }

    private static List<String> getMissingFieldsClause(Map<PagerDutyFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields,
                                                       boolean isListQuery) {
        List<String> missingFieldConditions = new ArrayList<>();
        if (MapUtils.isNotEmpty(missingBuiltinFields)) {
            missingFieldConditions.addAll(missingBuiltinFields.entrySet().stream()
                    .map(missingBuiltinField -> {
                        String clause;
                        final boolean shouldBeMissing = Boolean.TRUE.equals(missingBuiltinField.getValue());
                        PagerDutyFilter.MISSING_BUILTIN_FIELD key = missingBuiltinField.getKey();
                        switch (key) {
                            case user_id:
                                if (isListQuery) {
                                    clause = " array_length( user_id, 1) IS " + (shouldBeMissing ? " NULL " : "NOT NULL ");
                                } else {
                                    clause = " user_id IS " + (shouldBeMissing ? " NULL " : "NOT NULL ");
                                }
                                break;
                            case priority:
                                clause = " incident_priority  " + (shouldBeMissing ? " = '' " : " != '' ");
                                break;
                            case alert_severity:
                                clause = " alert_severity  IS " + (shouldBeMissing ? " NULL " : "NOT NULL ");
                                break;
                            case incident_status:
                            case alert_status:
                                clause = " " + key  +"  " + (shouldBeMissing ? " = '' " : " != '' ");
                                break;
                            case incident_created_at:
                            case alert_created_at:
                            case incident_resolved_at:
                            case alert_resolved_at:
                                clause = " " + key  +" IS " + (shouldBeMissing ? " NULL " : " NOT NULL ");
                                break;
                            default:
                                return null;
                        }
                        return clause;
                    })
                    .collect(Collectors.toList()));
        }
        return missingFieldConditions;
    }

}
