package io.levelops.commons.dashboard_widget.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.TimeUnit;

@Getter
public enum ReportIntervalType {
    LAST_7_DAYS(true, 12l, ChronoUnit.HOURS, true, 23l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS),
    LAST_2_WEEKS(true, 12l, ChronoUnit.HOURS, true, 23l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS),
    LAST_30_DAYS(true, 12l, ChronoUnit.HOURS, true, 23l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS, true, 12l, ChronoUnit.HOURS),
    LAST_MONTH(true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS),
    LAST_3_MONTH(true, 1l, ChronoUnit.DAYS, false, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS),
    LAST_QUARTER(true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS),
    LAST_TWO_QUARTERS(true, 1l, ChronoUnit.DAYS, false, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS, true, 1l, ChronoUnit.DAYS);

    private final boolean velocityEnabled;
    private final Long velocitySkipRecomputeDurationValue;
    private final ChronoUnit velocitySkipRecomputeDurationUnit;
    private final boolean baEnabled;
    private final Long baSkipRecomputeDurationValue;
    private final ChronoUnit baSkipRecomputeDurationUnit;
    private final boolean doraEnabled;
    private final Long doraSkipRecomputeDurationValue;
    private final ChronoUnit doraSkipRecomputeDurationUnit;

    private final boolean jiraReleaseEnabled;
    private final Long jiraReleaseSkipRecomputeDurationValue;
    private final ChronoUnit jiraReleaseSkipRecomputeDurationUnit;

    private final boolean velocityStageTimeReportEnabled;
    private final Long velocityStageTimeReportSkipRecomputeValue;
    private final ChronoUnit velocityStageTimeReportSkipRecomputeDurationUnit;

    ReportIntervalType(boolean velocityEnabled, Long velocitySkipRecomputeDurationValue, ChronoUnit velocitySkipRecomputeDurationUnit,
                       boolean baEnabled, Long baSkipRecomputeDurationValue, ChronoUnit baSkipRecomputeDurationUnit,
                       boolean doraEnabled, Long doraSkipRecomputeDurationValue, ChronoUnit doraSkipRecomputeDurationUnit,
                       boolean jiraReleaseEnabled, Long jiraReleaseSkipRecomputeDurationValue, ChronoUnit jiraReleaseSkipRecomputeDurationUnit,
                       boolean velocityStageTimeReportEnabled, Long velocityStageTimeReportSkipRecomputeValue, ChronoUnit velocityStageTimeReportSkipRecomputeDurationUnit) {
        this.velocityEnabled = velocityEnabled;
        this.velocitySkipRecomputeDurationValue = velocitySkipRecomputeDurationValue;
        this.velocitySkipRecomputeDurationUnit = velocitySkipRecomputeDurationUnit;
        this.baEnabled = baEnabled;
        this.baSkipRecomputeDurationValue = baSkipRecomputeDurationValue;
        this.baSkipRecomputeDurationUnit = baSkipRecomputeDurationUnit;
        this.doraEnabled = doraEnabled;
        this.doraSkipRecomputeDurationValue = doraSkipRecomputeDurationValue;
        this.doraSkipRecomputeDurationUnit = doraSkipRecomputeDurationUnit;
        this.jiraReleaseEnabled = jiraReleaseEnabled;
        this.jiraReleaseSkipRecomputeDurationValue = jiraReleaseSkipRecomputeDurationValue;
        this.jiraReleaseSkipRecomputeDurationUnit = jiraReleaseSkipRecomputeDurationUnit;
        this.velocityStageTimeReportEnabled = velocityStageTimeReportEnabled;
        this.velocityStageTimeReportSkipRecomputeValue = velocityStageTimeReportSkipRecomputeValue;
        this.velocityStageTimeReportSkipRecomputeDurationUnit = velocityStageTimeReportSkipRecomputeDurationUnit;
    }

    @JsonCreator
    @Nullable
    public static ReportIntervalType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ReportIntervalType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

    private static Long getStartOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().getEpochSecond();
    }

    private static Long getEndOfDay(LocalDate localDate) {
        return localDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    private static LocalDate getLastDayOfPrevQuarter(LocalDate localDate) {
        return localDate.with(localDate.getMonth().firstMonthOfQuarter())
                .with(TemporalAdjusters.firstDayOfMonth())
                .minusDays(1);
    }

    public ImmutablePair<Long, Long> getTimeRange(Instant instant) {
        ImmutablePair<Long, Long> result = null;

        LocalDate date = LocalDate.ofInstant(instant, ZoneId.of("UTC"));
        LocalDate firstDayOfPrevMonth = date.with(TemporalAdjusters.firstDayOfMonth()).minusDays(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfPrevMonth = firstDayOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate lastDayOfPrevQuarter = getLastDayOfPrevQuarter(date);
        LocalDate lastDayOfPrevTwoQuarter = getLastDayOfPrevQuarter(lastDayOfPrevQuarter);

        int year = 0;
        LocalDate from;
        LocalDate to;

        switch (this) {
            case LAST_7_DAYS:
                result = ImmutablePair.of(getStartOfDay(date.minusDays(6)), getEndOfDay(date));
                break;
            case LAST_2_WEEKS:
                result = ImmutablePair.of(getStartOfDay(date.minusDays(13)), getEndOfDay(date));
                break;
            case LAST_30_DAYS:
                result = ImmutablePair.of(getStartOfDay(date.minusDays(29)), getEndOfDay(date));
                break;
            case LAST_MONTH:
                result = ImmutablePair.of(getStartOfDay(firstDayOfPrevMonth), getEndOfDay(lastDayOfPrevMonth));
                break;
            case LAST_3_MONTH:
                LocalDate firstDayOfLast3Months = firstDayOfPrevMonth.minusMonths(2).with(TemporalAdjusters.firstDayOfMonth());
                result = ImmutablePair.of(getStartOfDay(firstDayOfLast3Months), getEndOfDay(lastDayOfPrevMonth));
                break;
            case LAST_QUARTER:
                LocalDate firstDayOfLastQuarter = lastDayOfPrevQuarter.with(lastDayOfPrevQuarter.getMonth().firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastDayOfLastQuarter = firstDayOfLastQuarter.plusMonths(2)
                        .with(TemporalAdjusters.lastDayOfMonth());
                result = ImmutablePair.of(getStartOfDay(firstDayOfLastQuarter), getEndOfDay(lastDayOfLastQuarter));
                break;
            case LAST_TWO_QUARTERS:
                from = lastDayOfPrevTwoQuarter.with(lastDayOfPrevTwoQuarter.getMonth().firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth());
                to = lastDayOfPrevQuarter.with(TemporalAdjusters.lastDayOfMonth());
                result = ImmutablePair.of(getStartOfDay(from), getEndOfDay(to));
                break;
            default:
                result = null;
                break;
        }
        return result;
    }

    public ImmutablePair<Long, Long> getTimeRangeForVelocityStageTimeReport(Instant instant) {
        ImmutablePair<Long, Long> result = null;

        LocalDate date = LocalDate.ofInstant(instant, ZoneId.of("UTC"));
        LocalDate firstDayOfPrevMonth = date.with(TemporalAdjusters.firstDayOfMonth()).minusDays(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfPrevMonth = firstDayOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate lastDayOfPrevQuarter = getLastDayOfPrevQuarter(date);
        LocalDate lastDayOfPrevTwoQuarter = getLastDayOfPrevQuarter(lastDayOfPrevQuarter);

        int year = 0;
        LocalDate from;
        LocalDate to;

        switch (this) {
            case LAST_7_DAYS:
                result = ImmutablePair.of(getStartOfDay(date.minusDays(7)), getEndOfDay(date.minusDays(1)));
                break;
            case LAST_2_WEEKS:
                result = ImmutablePair.of(getStartOfDay(date.minusDays(14)), getEndOfDay(date.minusDays(1)));
                break;
            case LAST_30_DAYS:
                result = ImmutablePair.of(getStartOfDay(date.minusDays(30)), getEndOfDay(date.minusDays(1)));
                break;
            case LAST_MONTH:
                result = ImmutablePair.of(getStartOfDay(firstDayOfPrevMonth), getEndOfDay(lastDayOfPrevMonth));
                break;
            case LAST_3_MONTH:
                LocalDate firstDayOfLast3Months = firstDayOfPrevMonth.minusMonths(2).with(TemporalAdjusters.firstDayOfMonth());
                result = ImmutablePair.of(getStartOfDay(firstDayOfLast3Months), getEndOfDay(lastDayOfPrevMonth));
                break;
            case LAST_QUARTER:
                LocalDate firstDayOfLastQuarter = lastDayOfPrevQuarter.with(lastDayOfPrevQuarter.getMonth().firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastDayOfLastQuarter = firstDayOfLastQuarter.plusMonths(2)
                        .with(TemporalAdjusters.lastDayOfMonth());
                result = ImmutablePair.of(getStartOfDay(firstDayOfLastQuarter), getEndOfDay(lastDayOfLastQuarter));
                break;
            case LAST_TWO_QUARTERS:
                from = lastDayOfPrevTwoQuarter.with(lastDayOfPrevTwoQuarter.getMonth().firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth());
                to = lastDayOfPrevQuarter.with(TemporalAdjusters.lastDayOfMonth());
                result = ImmutablePair.of(getStartOfDay(from), getEndOfDay(to));
                break;
            default:
                result = null;
                break;
        }
        return result;
    }
}
