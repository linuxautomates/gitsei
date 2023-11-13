package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

import static io.levelops.commons.databases.models.database.dev_productivity.Constants.HIGH;
import static io.levelops.commons.databases.models.database.dev_productivity.Constants.LOW;
import static io.levelops.commons.databases.models.database.dev_productivity.Constants.MEDIUM;

@Getter
public enum ReportIntervalType {
    LAST_WEEK(true, HIGH, null, null,null, 2l, ChronoUnit.HOURS, 4l, ChronoUnit.HOURS, 4.0, " in one week"),
    LAST_TWO_WEEKS(true, HIGH, null, null,null, 2l, ChronoUnit.HOURS, 4l, ChronoUnit.HOURS, 2.0, " in two weeks"),
    LAST_TWO_MONTHS(true, HIGH, null, null,null, 4l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    LAST_THREE_MONTHS(true, HIGH, null, null,null, 4l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    LAST_MONTH(true, HIGH, null, null,null, 4l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    LAST_QUARTER(true, HIGH, null, null, null, 4l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    LAST_TWO_QUARTERS(true, HIGH, null, null, null, 4l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    LAST_TWELVE_MONTHS(true, HIGH, null, null, null, 4l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    PAST_YEAR(true, MEDIUM, null, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_JAN(true, LOW, Month.JANUARY, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_FEB(true, LOW, Month.FEBRUARY, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_MAR(true, LOW, Month.MARCH, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_APR(true, LOW, Month.APRIL, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_MAY(true, LOW, Month.MAY, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_JUN(true, LOW, Month.JUNE, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_JUL(true, LOW, Month.JULY, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_AUG(true, LOW, Month.AUGUST, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_SEP(true, LOW, Month.SEPTEMBER, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_OCT(true, LOW, Month.OCTOBER, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_NOV(true, LOW, Month.NOVEMBER, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    MONTH_DEC(true, LOW, Month.DECEMBER, null, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    PAST_QUARTER_ONE(true, MEDIUM, null, 1, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS,1.0, " per month"),
    PAST_QUARTER_TWO(true, MEDIUM, null, 2, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    PAST_QUARTER_THREE(true, MEDIUM, null, 3, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    PAST_QUARTER_FOUR(true, MEDIUM, null, 4, null, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    PAST_TWO_QUARTERS_ONE(true, MEDIUM, null,2, 1, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month"),
    PAST_TWO_QUARTERS_TWO(true, MEDIUM, null,4, 2, 24l, ChronoUnit.HOURS, 7l, ChronoUnit.DAYS, 1.0, " per month");

    private final boolean enabled;
    private final int calculationPriority;
    private final Month month;
    private final Integer quarter;
    private final Integer half;
    private final Long devProdProximitySkipRecomputeDurationValue;
    private final ChronoUnit devProdProximitySkipRecomputeDurationUnit;
    private final Long devProdSkipRecomputeDurationValue;
    private final ChronoUnit devProdSkipRecomputeDurationUnit;
    //Use this to extrapolate numbers in case of intervals smaller than a month to fit to "per month" - applicable only to "per month" features
    private final Double multiplicationFactor;
    private final String labelSuffix;

    ReportIntervalType(boolean enabled, int calculationPriority, Month month, Integer quarter, Integer half, Long devProdProximitySkipRecomputeDurationValue, ChronoUnit devProdProximitySkipRecomputeDurationUnit, Long devProdSkipRecomputeDurationValue, ChronoUnit devProdSkipRecomputeDurationUnit, Double multiplicationFactor, String labelSuffix) {
        this.enabled = enabled;
        this.calculationPriority = calculationPriority;
        this.month = month;
        this.quarter = quarter;
        this.half = half;
        this.devProdProximitySkipRecomputeDurationValue = devProdProximitySkipRecomputeDurationValue;
        this.devProdProximitySkipRecomputeDurationUnit = devProdProximitySkipRecomputeDurationUnit;
        this.devProdSkipRecomputeDurationValue = devProdSkipRecomputeDurationValue;
        this.devProdSkipRecomputeDurationUnit = devProdSkipRecomputeDurationUnit;
        this.multiplicationFactor = multiplicationFactor;
        this.labelSuffix = labelSuffix;
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
    public IntervalTimeRange getIntervalTimeRange(Instant instant) {
        IntervalTimeRange result = null;

        LocalDate date = LocalDate.ofInstant(instant, ZoneId.of("UTC"));
        LocalDate firstDayOfPrevMonth = date.with(TemporalAdjusters.firstDayOfMonth()).minusDays(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfPrevMonth = firstDayOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate lastDayOfPrevQuarter = getLastDayOfPrevQuarter(date);
        LocalDate lastDayOfPrevTwoQuarter = getLastDayOfPrevQuarter(lastDayOfPrevQuarter);
        LocalDate firstDayOfLastTwoWeeks = date.minusDays(date.getDayOfWeek().getValue()+13);
        LocalDate firstDayOfLastWeek = date.minusDays(date.getDayOfWeek().getValue()+6);
        LocalDate lastDayOfLastWeek = date.minusDays(date.getDayOfWeek().getValue());
        LocalDate firstDayOfLastTwoMonths = firstDayOfPrevMonth.minusDays(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate firstDayOfLastThreeMonths = firstDayOfLastTwoMonths.minusDays(1).with(TemporalAdjusters.firstDayOfMonth());

        int year = 0;
        int weekOfTheYear = 0;
        LocalDate from;
        LocalDate to;

        switch (this) {
            case LAST_WEEK:
                year = date.getYear();
                weekOfTheYear = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                result = IntervalTimeRange.builder()
                        .timeRange(ImmutablePair.of(getStartOfDay(firstDayOfLastWeek),getEndOfDay(lastDayOfLastWeek)))
                        .weekOfTheYear(weekOfTheYear)
                        .year(year)
                        .build();
                break;
            case LAST_TWO_WEEKS:
                year = date.getYear();
                weekOfTheYear = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                result = IntervalTimeRange.builder()
                        .timeRange(ImmutablePair.of(getStartOfDay(firstDayOfLastTwoWeeks),getEndOfDay(lastDayOfLastWeek)))
                        .weekOfTheYear(weekOfTheYear)
                        .year(year)
                        .build();
                break;
            case LAST_TWO_MONTHS:
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(firstDayOfLastTwoMonths),getEndOfDay(lastDayOfPrevMonth)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case LAST_THREE_MONTHS:
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(firstDayOfLastThreeMonths),getEndOfDay(lastDayOfPrevMonth)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case LAST_MONTH:
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(firstDayOfPrevMonth), getEndOfDay(lastDayOfPrevMonth)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case LAST_QUARTER:
                LocalDate firstDayOfLastQuarter = lastDayOfPrevQuarter.with(lastDayOfPrevQuarter.getMonth().firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastDayOfLastQuarter = firstDayOfLastQuarter.plusMonths(2)
                        .with(TemporalAdjusters.lastDayOfMonth());
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(firstDayOfLastQuarter), getEndOfDay(lastDayOfLastQuarter)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case LAST_TWO_QUARTERS:
                from = lastDayOfPrevTwoQuarter.with(lastDayOfPrevTwoQuarter.getMonth().firstMonthOfQuarter())
                        .with(TemporalAdjusters.firstDayOfMonth());
                to = lastDayOfPrevQuarter.with(TemporalAdjusters.lastDayOfMonth());
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(from), getEndOfDay(to)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case PAST_YEAR:
                year = date.getYear() - 1;
                from = LocalDate.of(year, Month.JANUARY, 1);
                to = LocalDate.of(year, Month.DECEMBER, 31);;
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(from), getEndOfDay(to)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case LAST_TWELVE_MONTHS:
                from = lastDayOfPrevMonth.minusMonths(11).with(TemporalAdjusters.firstDayOfMonth());
                to = lastDayOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(from), getEndOfDay(to)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case PAST_QUARTER_ONE:
            case PAST_QUARTER_TWO:
            case PAST_QUARTER_THREE:
            case PAST_QUARTER_FOUR:
                year = (date.get(IsoFields.QUARTER_OF_YEAR) > this.getQuarter()) ? date.getYear() : date.getYear() - 1;
                LocalDate firstDayOfFirstMonth = LocalDate.of(year, Month.of( ((this.getQuarter() -1) * 3) + 1), 1); //First day of the first month of the quarter, e.g. Jan 01 or Apr 01 etc.
                LocalDate firstDayOfLastMonth = LocalDate.of(year, Month.of( ((this.getQuarter() -1) * 3) + 3), 1); //First day  of the last month of the quarter, e.g. Mar 01 or Jun 01 etc.

                from = firstDayOfFirstMonth.with(TemporalAdjusters.firstDayOfMonth());
                to = firstDayOfLastMonth.with(TemporalAdjusters.lastDayOfMonth());
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(from), getEndOfDay(to)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;
            case PAST_TWO_QUARTERS_ONE:
            case PAST_TWO_QUARTERS_TWO:
                year = (date.get(IsoFields.QUARTER_OF_YEAR) > this.getQuarter()) ? date.getYear() : date.getYear() - 1;
                firstDayOfFirstMonth = LocalDate.of(year, Month.of( ((this.getHalf() -1) * 6) + 1), 1); //First day of the first month of the half, e.g. Jan 01 or Jul 01 etc.
                firstDayOfLastMonth = LocalDate.of(year, Month.of( ((this.getHalf() -1) * 6) + 6), 1); //First day  of the last month of the quarter, e.g. May 01 or Dec 01 etc.

                from = firstDayOfFirstMonth.with(TemporalAdjusters.firstDayOfMonth());
                to = firstDayOfLastMonth.with(TemporalAdjusters.lastDayOfMonth());
                result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(from), getEndOfDay(to)))
                        .weekOfTheYear(-1).year(-1)
                        .build();
                break;

            default:
                if(this.getMonth() != null) {
                    year = (date.getMonthValue() > this.getMonth().getValue()) ? date.getYear() : date.getYear() - 1;
                    LocalDate effectiveDate = LocalDate.of(year, this.getMonth(), 1); //First of the month, e.g. Jan 01 or Feb 01 etc.

                    from = effectiveDate.with(TemporalAdjusters.firstDayOfMonth());
                    to = effectiveDate.with(TemporalAdjusters.lastDayOfMonth());
                    result = IntervalTimeRange.builder().timeRange(ImmutablePair.of(getStartOfDay(from), getEndOfDay(to)))
                            .weekOfTheYear(-1).year(-1)
                            .build();
                }
                break;
        }
        return result;
    }
}
