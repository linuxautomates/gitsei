package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;

@Value
@Builder(toBuilder = true)
@Log4j2
public class PagerDutyFilter {
    DISTINCT across;
    AGG_INTERVAL aggInterval;
    CALCULATION calculation;
    List<String> integrationIds;
    Map<String, SortingOrder> sort;


    List<String> alertStatuses;
    List<String> alertSeverities;
    ImmutablePair<Long, Long> alertCreatedAt;
    ImmutablePair<Long, Long> alertResolvedAt;
    ImmutablePair<Long, Long> alertAcknowledgedAt;

    List<String> incidentStatuses;
    List<String> incidentUrgencies;
    List<String> incidentPriorities;
    ImmutablePair<Long, Long> incidentCreatedAt;
    ImmutablePair<Long, Long> incidentResolvedAt;
    ImmutablePair<Long, Long> incidentAcknowledgedAt;

    String issueType;
    List<String> userIds;
    List<String> userNames;
    List<String> pdServiceIds;
    ImmutablePair<String, String> officeHours;
    Long officeHoursFrom;
    Long OfficeHoursTo;
    Map<String, Boolean> missingFields;

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (issueType != null)
            dataToHash.append(",issueType=").append(issueType);
        if (officeHoursFrom != null)
            dataToHash.append(",officeHoursFrom=").append(officeHoursFrom);
        if (OfficeHoursTo != null)
            dataToHash.append(",OfficeHoursTo=").append(OfficeHoursTo);
        if (CollectionUtils.isNotEmpty(userIds)) {
            ArrayList<String> tempList = new ArrayList<>(userIds);
            Collections.sort(tempList);
            dataToHash.append(",userIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(incidentUrgencies)) {
            ArrayList<String> tempList = new ArrayList<>(incidentUrgencies);
            Collections.sort(tempList);
            dataToHash.append(",incidentUrgencies=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(incidentPriorities)) {
            ArrayList<String> tempList = new ArrayList<>(incidentPriorities);
            Collections.sort(tempList);
            dataToHash.append(",incidentPriorities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(incidentStatuses)) {
            ArrayList<String> tempList = new ArrayList<>(incidentStatuses);
            Collections.sort(tempList);
            dataToHash.append(",incidentStatuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(alertStatuses)) {
            ArrayList<String> tempList = new ArrayList<>(alertStatuses);
            Collections.sort(tempList);
            dataToHash.append(",alertStatuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(alertSeverities)) {
            ArrayList<String> tempList = new ArrayList<>(alertSeverities);
            Collections.sort(tempList);
            dataToHash.append(",alertSeverities=").append(String.join(",", tempList));
        }
        if (aggInterval != null)
            dataToHash.append(",aggInterval=").append(aggInterval);
        if (CollectionUtils.isNotEmpty(pdServiceIds)) {
            ArrayList<String> tempList = new ArrayList<>(pdServiceIds);
            Collections.sort(tempList);
            dataToHash.append(",pdServiceIds=").append(String.join(",", tempList));
        }
        if (incidentCreatedAt != null) {
            dataToHash.append(",incidentCreatedAt=");
            if (incidentCreatedAt.getLeft() != null)
                dataToHash.append(incidentCreatedAt.getLeft()).append("-");
            if (incidentCreatedAt.getRight() != null)
                dataToHash.append(incidentCreatedAt.getRight());
        }
        hashDataMapOfStrings(dataToHash, "missingFields", missingFields);
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (incidentResolvedAt != null) {
            dataToHash.append(",incidentResolvedAt=");
            if (incidentResolvedAt.getLeft() != null)
                dataToHash.append(incidentResolvedAt.getLeft()).append("-");
            if (incidentResolvedAt.getRight() != null)
                dataToHash.append(incidentResolvedAt.getRight());
        }
        if (alertCreatedAt != null) {
            dataToHash.append(",alertCreatedAt=");
            if (alertCreatedAt.getLeft() != null)
                dataToHash.append(alertCreatedAt.getLeft()).append("-");
            if (alertCreatedAt.getRight() != null)
                dataToHash.append(alertCreatedAt.getRight());
        }
        if (alertAcknowledgedAt != null) {
            dataToHash.append(",alertAcknowledgedAt=");
            if (alertAcknowledgedAt.getLeft() != null)
                dataToHash.append(alertAcknowledgedAt.getLeft()).append("-");
            if (alertAcknowledgedAt.getRight() != null)
                dataToHash.append(alertAcknowledgedAt.getRight());
        }
        if (incidentAcknowledgedAt != null) {
            dataToHash.append(",incidentAcknowledgedAt=");
            if (incidentAcknowledgedAt.getLeft() != null)
                dataToHash.append(incidentAcknowledgedAt.getLeft()).append("-");
            if (incidentAcknowledgedAt.getRight() != null)
                dataToHash.append(incidentAcknowledgedAt.getRight());
        }
        if (alertResolvedAt != null) {
            dataToHash.append(",alertResolvedAt=");
            if (alertResolvedAt.getLeft() != null)
                dataToHash.append(alertResolvedAt.getLeft()).append("-");
            if (alertResolvedAt.getRight() != null)
                dataToHash.append(alertResolvedAt.getRight());
        }
        if (officeHours != null) {
            dataToHash.append(",officeHours=");
            if (officeHours.getLeft() != null)
                dataToHash.append(officeHours.getLeft()).append("-");
            if (officeHours.getRight() != null)
                dataToHash.append(officeHours.getRight());
        }
        if (MapUtils.isNotEmpty(sort)) {
            TreeSet<String> fields = new TreeSet<>(sort.keySet());
            dataToHash.append(",sort=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(sort.get(field).toString().toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public static PagerDutyFilterBuilder fromDefaultListRequest(DefaultListRequest filter,
                                                                PagerDutyFilter.CALCULATION calc,
                                                                PagerDutyFilter.DISTINCT across) throws BadRequestException {
        List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
        ImmutablePair<Long, Long> incidentCreatedAt = getTimeRange(filter, "incident_created_at");
        ImmutablePair<Long, Long> incidentResolvedAt = getTimeRange(filter, "incident_resolved_at");
        ImmutablePair<Long, Long> incidentAcknowledgedAt = getTimeRange(filter, "incident_acknowledged_at");
        ImmutablePair<Long, Long> alertCreatedAt = getTimeRange(filter, "alert_created_at");
        ImmutablePair<Long, Long> alertResolvedAt = getTimeRange(filter, "alert_resolved_at");
        ImmutablePair<Long, Long> alertAcknowledgedAt = getTimeRange(filter, "alert_acknowledged_at");
        ImmutablePair<String, String> officeHours = getOfficeHourRange(filter, "office_hours");
        Long officeHoursFrom = StringUtils.isNotEmpty((String) filter.getFilter().getOrDefault("from", StringUtils.EMPTY))
                ? Long.parseLong(String.valueOf(filter.getFilter().get("from"))) : Instant.now().minus(7, ChronoUnit.DAYS).getEpochSecond();
        Long officeHoursTo = StringUtils.isNotEmpty((String) filter.getFilter().getOrDefault("to", StringUtils.EMPTY))
                ? Long.parseLong(String.valueOf(filter.getFilter().get("to"))) : Instant.now().getEpochSecond();
        PagerDutyFilterBuilder bldr = PagerDutyFilter.builder()
                .integrationIds(integrationIds)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .incidentCreatedAt(incidentCreatedAt)
                .alertCreatedAt(alertCreatedAt)
                .incidentResolvedAt(incidentResolvedAt)
                .alertResolvedAt(alertResolvedAt)
                .incidentAcknowledgedAt(incidentAcknowledgedAt)
                .alertAcknowledgedAt(alertAcknowledgedAt)
                .officeHours(officeHours)
                .officeHoursFrom(officeHoursFrom)
                .OfficeHoursTo(officeHoursTo)
                .userIds(getListOrDefault(filter.getFilter(), "user_ids"))
                .pdServiceIds(getListOrDefault(filter.getFilter(), "pd_service_ids"))
                .missingFields(MapUtils.emptyIfNull(
                        (Map<String, Boolean>) filter.getFilter().get("missing_fields")))
                .incidentPriorities(getListOrDefault(filter.getFilter(), "incident_priorities"))
                .incidentUrgencies(getListOrDefault(filter.getFilter(), "incident_urgencies"))
                .issueType(StringUtils.defaultIfEmpty((String) filter.getFilter().get("issue_type"), "incident"))
                .incidentStatuses(getListOrDefault(filter.getFilter(), "incident_statuses"))
                .alertStatuses(getListOrDefault(filter.getFilter(), "alert_statuses"))
                .alertSeverities(getListOrDefault(filter.getFilter(), "alert_severities"));
        if (across != null) {
            bldr.across(across);
        }
        if (calc != null) {
            bldr.calculation(calc);
        }
        log.info("pagerDutyIncidentFilter = {}", bldr.build());
        return bldr;
    }

    private static ImmutablePair<String, String> getOfficeHourRange(DefaultListRequest filter, String field) {
        Map<String, String> officeHourRange = filter.getFilterValue(field, Map.class).orElse(Map.of());
        String start = officeHourRange.get("$from") != null ? officeHourRange.get("$from") : null;
        String end = officeHourRange.get("$to") != null ? officeHourRange.get("$to") : null;
        return ImmutablePair.of(start, end);
    }
    public enum DISTINCT {
        incident_priority,
        status,
        alert_severity,
        user_id,
        pd_service,
        //these are across time
        incident_created_at,
        alert_created_at,
        alert_resolved_at,
        incident_resolved_at;

        public static PagerDutyFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(PagerDutyFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        response_time,
        resolution_time;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public enum MISSING_BUILTIN_FIELD {
        user_id, priority, alert_severity, incident_status, alert_status,
        incident_created_at, alert_created_at, alert_resolved_at, incident_resolved_at;

        public static PagerDutyFilter.MISSING_BUILTIN_FIELD fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(PagerDutyFilter.MISSING_BUILTIN_FIELD.class, st);
        }
    }
}
