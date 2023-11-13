package io.levelops.commons.dashboard_widget.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.TemporalAdjusters.previous;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WidgetMetadata.WidgetMetadataBuilder.class)
public class WidgetMetadata {
    @JsonProperty("dashBoard_time_keys")
    private final Map<String, DashboardTimeKey> dashBoardTimeKeys;

    @JsonProperty("range_filter_choice")
    private final Map<String, RangeFilterChoice> rangeFilterChoice;

    @JsonProperty("hide_stages")
    private final List<String> excludeStages;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DashboardTimeKey.DashboardTimeKeyBuilder.class)
    public static class DashboardTimeKey {
        @JsonProperty("use_dashboard_time")
        private final Boolean useDashboardTime;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RangeFilterChoice.RangeFilterChoiceBuilder.class)
    public static class RangeFilterChoice {
        @JsonProperty("type")
        private final String type;
        @JsonProperty("relative")
        private final Relative relative;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Relative.RelativeBuilder.class)
    public static class Relative {
        @JsonProperty("last")
        private final RelativeDetail last;
        @JsonProperty("next")
        private final RelativeDetail next;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RelativeDetail.RelativeDetailBuilder.class)
    public static class RelativeDetail {
        @JsonProperty("num")
        private final Object num;

        @JsonProperty("unit")
        private final String unit;
    }

    private static Long getStartOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().getEpochSecond();
    }

    private static Long getEndOfDay(LocalDate localDate) {
        return localDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    static Long calculateLastTimestamp(WidgetMetadata.RelativeDetail relativeDetail, Instant now) {
        //now = DateUtils.toEndOfDay(now);
        Object num = relativeDetail.getNum();
        Integer val = ((num == null) || (StringUtils.isBlank(num.toString()))) ? 0 : Integer.valueOf(num.toString()) -1;
        String unit = relativeDetail.getUnit();
        Instant res = null;
        LocalDate result = null;
        LocalDate date = LocalDate.ofInstant(now, ZoneId.of("UTC"));
        if("today".equals(unit)) {
            res = now;
            return DateUtils.toStartOfDay(res).getEpochSecond();
        } else if ("days".equals(unit)) {
            res = now.minus(val, ChronoUnit.DAYS);
            return DateUtils.toStartOfDay(res).getEpochSecond();
        } else if ("weeks".equals(unit)) {
            LocalDate lastSunday = (DayOfWeek.of(date.get(ChronoField.DAY_OF_WEEK)) == SUNDAY) ? date : date.with(previous(SUNDAY));
            result = lastSunday.minusWeeks(val);
            return getStartOfDay(result);
        } else if ("months".equals(unit)) {
            LocalDate firstDayOfTheMonth = date.with(TemporalAdjusters.firstDayOfMonth());
            result = firstDayOfTheMonth.minusMonths(val);
            return getStartOfDay(result);
        } else if ("quarters".equals(unit)) {
            LocalDate firstDayOfQuarter = date.with(date.getMonth().firstMonthOfQuarter())
                    .with(TemporalAdjusters.firstDayOfMonth());
            result = firstDayOfQuarter.minusMonths(val * 3);
            return getStartOfDay(result);
        }
        return null;
    }

    static Long calculateNextTimestamp(WidgetMetadata.RelativeDetail relativeDetail, Instant now) {
        //now = DateUtils.toEndOfDay(now);
        Object num = relativeDetail.getNum();
        Integer val = ((num == null) || (StringUtils.isBlank(num.toString()))) ? 0 : Integer.valueOf(num.toString());
        String unit = relativeDetail.getUnit();
        LocalDate result = null;
        LocalDate date = LocalDate.ofInstant(now, ZoneId.of("UTC"));
        if("today".equals(unit)) {
            return DateUtils.toEndOfDay(now).getEpochSecond();
        } else if ("days".equals(unit)) {
            Instant res = now.plus(val, ChronoUnit.DAYS);
            return DateUtils.toEndOfDay(res).getEpochSecond();
        } else if ("weeks".equals(unit)) {
            result = date.plusWeeks(val);
            return getEndOfDay(result);
        } else if ("months".equals(unit)) {
            result = date.plusMonths(val);
            return getEndOfDay(result);
        } else if ("quarters".equals(unit)) {
            result = date.plusMonths(val * 3);
            return getEndOfDay(result);
        }
        return null;
    }

    public static Map<String, Object> parseRelativeTimeFilters(Map<String, WidgetMetadata.RangeFilterChoice> widgetRangeFilters, Instant now) {
        if(MapUtils.isEmpty(widgetRangeFilters)) {
            return Map.of();
        }
        Map<String, Object> relativeTimeRanges = new HashMap<>();
        for(Map.Entry<String, WidgetMetadata.RangeFilterChoice> e : widgetRangeFilters.entrySet()) {
            if(!"relative".equals(e.getValue().getType())) {
                continue;
            }
            Map<String, String> timeStamps = new LinkedHashMap<>();
            timeStamps.put("$gt", String.valueOf(calculateLastTimestamp(e.getValue().getRelative().getLast(), now)));
            timeStamps.put("$lt", String.valueOf(calculateNextTimestamp(e.getValue().getRelative().getNext(), now)));
            relativeTimeRanges.put(e.getKey(), timeStamps);
        }
        return relativeTimeRanges;
    }

    public static Map<String, Object> parseDashboardTimeFilters(Map<String, WidgetMetadata.DashboardTimeKey> dashboardTimeKeys, ReportIntervalType dashboardReportInterval, Instant now) {
        if(MapUtils.isEmpty(dashboardTimeKeys) || (dashboardReportInterval == null)) {
            return Map.of();
        }
        Map<String, Object> dashboardTimeRanges = new HashMap<>();
        for(Map.Entry<String, WidgetMetadata.DashboardTimeKey> e : dashboardTimeKeys.entrySet()) {
            if ((e.getValue() == null) || (!BooleanUtils.isTrue(e.getValue().getUseDashboardTime()))) {
                continue;
            }
            ImmutablePair<Long, Long> timeRange = dashboardReportInterval.getTimeRange(now);
            Map<String, String> timeStamps = new LinkedHashMap<>();
            timeStamps.put("$gt", String.valueOf(timeRange.getLeft()));
            timeStamps.put("$lt", String.valueOf(timeRange.getRight()));
            dashboardTimeRanges.put(e.getKey(), timeStamps);
        }
        return dashboardTimeRanges;
    }


    public static Map<String, Object> parseDashboardTimeFiltersForVelocityStageTimeReport(Map<String, WidgetMetadata.DashboardTimeKey> dashboardTimeKeys, ReportIntervalType dashboardReportInterval, Instant now) {
        //Todo: fix report interval time ranges of last 7 days, 2 weeks, 30 days for all widgets instead of in stage report
        if(MapUtils.isEmpty(dashboardTimeKeys) || (dashboardReportInterval == null)) {
            return Map.of();
        }
        Map<String, Object> dashboardTimeRanges = new HashMap<>();
        for(Map.Entry<String, WidgetMetadata.DashboardTimeKey> e : dashboardTimeKeys.entrySet()) {
            if ((e.getValue() == null) || (!BooleanUtils.isTrue(e.getValue().getUseDashboardTime()))) {
                continue;
            }
            ImmutablePair<Long, Long> timeRange = dashboardReportInterval.getTimeRangeForVelocityStageTimeReport(now);
            Map<String, String> timeStamps = new LinkedHashMap<>();
            timeStamps.put("$gt", String.valueOf(timeRange.getLeft()));
            timeStamps.put("$lt", String.valueOf(timeRange.getRight()));
            dashboardTimeRanges.put(e.getKey(), timeStamps);
        }
        return dashboardTimeRanges;
    }
}
