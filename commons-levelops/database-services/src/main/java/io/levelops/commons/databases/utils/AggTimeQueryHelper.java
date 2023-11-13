package io.levelops.commons.databases.utils;

import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.Query;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Helper class for generating interval based queries
 */
public class AggTimeQueryHelper {
    private static final boolean SORT_DESC_BY_DEFAULT = false;
    private static final Set<String> DATE_FIELD_UNITS = Set.of("year", "quarter", "biweekly", "week", "month", "day", "day_of_week");

    public static Set<Long> getIngestedAtSetForInterval(final long rangeStart, final long rangeEnd, final String interval){
        // get age intervals
        var rangeFrom = Instant.ofEpochSecond(rangeStart).atZone(ZoneId.of("UTC"));
        var rangeTo = Instant.ofEpochSecond(rangeEnd).atZone(ZoneId.of("UTC"));
        var intervals = new HashSet<Long>();
        var baseTime = rangeFrom;
        var start = 0;
        var end = 0;
        switch(interval){
            case "year":
                // check if the time range starts the first day of the year and if so and only if so, include the first year of the interval
                start = rangeFrom.getDayOfYear() == 1 ? 0 : 1;
                // from the first year to be included in the range to the last year (+1 since it is non-inclusive)
                end = rangeTo.getYear() - rangeFrom.getYear() + 1;
                break;
            case "quarter":
                // check if the time range starts the first day of the month and if so and only if so, include the start first of the month
                start = Set.of(1,4,7,10).contains(rangeFrom.getMonthValue()) && rangeFrom.getDayOfMonth() == 1 ? 0 : 1;
                // from the first quarter to be included in the range to the last quarter (+1 since it is non-inclusive but only if we are not starting from 0)
                end = getNumberOfQuartersInYearByStartOfQuarter(rangeFrom, rangeTo) + (start);
                var firstQuarterMonth = getNumberOfQuartersInYearByStartOfQuarter(null, rangeFrom)-1;
                baseTime = start == 0 ? rangeFrom : Instant.parse(rangeTo.getYear() + "-" + String.format("%02d",(1 + (3*firstQuarterMonth))) + "-01T00:00:00-00:00").atZone(ZoneId.of("UTC"));
                break;
            case "month":
                // check if the time range starts the first day of the month and if so and only if so, include the starting month
                start = rangeFrom.getDayOfMonth() == 1 ? 0 : 1;
                // from the first month to be included in the range to the last month (+1 since it is non-inclusive)
                end = rangeTo.getYear() == rangeFrom.getYear() ? rangeTo.getMonthValue() - rangeFrom.getMonthValue() + 1
                    // 1 or more we substract 12 from the month on the start date then we add the months on the year of the end date and add 12 for each full year in between the start and end years
                    : (rangeTo.getYear() - rangeFrom.getYear()) >= 1 ? 12 * (rangeTo.getYear() - rangeFrom.getYear()) + rangeTo.getMonthValue() - rangeFrom.getMonthValue() +1
                    : 0; // error so stop the intervals
                break;
            case "week":
                // check if the time range starts the first day of the week and if so and only if so, include the starting week
                start = rangeFrom.getDayOfWeek() == DayOfWeek.MONDAY ? 0 : 1;
                // from the first week to be included in the range to the last week (+1 since it is non-inclusive)
                end = rangeTo.getYear() == rangeFrom.getYear() ? rangeTo.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) - rangeFrom.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + 1
                    // 1 or more we substract 52 from the week on the start date then we add the weeks on the end date and add 52 for each full year in between the start and end years
                    : (rangeTo.getYear() - rangeFrom.getYear()) >= 1 ? 52 * (rangeTo.getYear() - rangeFrom.getYear()) + rangeTo.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) - rangeFrom.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) +2 // 1 for the start year and 1 for the non-inclusive
                    : 0; // error so stop the intervals
                break;
            default:
        }
        var finalBaseTime = baseTime;
        Function<ZonedDateTime, Integer> timeSetMonth =
                "year".equalsIgnoreCase(interval)? (tmp) -> 0 :
                "quarter".equalsIgnoreCase(interval) ? (tmp) -> tmp.getMonthValue() -1 :
                "month".equalsIgnoreCase(interval) ? (tmp) -> tmp.getMonthValue() -1 :
                (tmp) -> tmp.getMonthValue() -1;
        Function<ZonedDateTime, Integer> timeSetDay =
                "year".equalsIgnoreCase(interval) ? (tmp) -> 1 :
                "quarter".equalsIgnoreCase(interval) ? (tmp) -> 1 :
                "month".equalsIgnoreCase(interval) ? (tmp) -> 1 :
                (tmp) -> (tmp.getDayOfMonth() - (tmp.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue()));
        Function<Integer, Period> increment =
                "year".equalsIgnoreCase(interval) ? (i) -> Period.ofYears(i) :
                "quarter".equalsIgnoreCase(interval) ? (i) -> Period.ofMonths(3*i) :
                "month".equalsIgnoreCase(interval) ? (i) -> Period.ofMonths(i) :
                (i) -> Period.ofWeeks(i);
        IntStream.range(start, end)
        .forEach(i -> {
            var tmp = finalBaseTime.plus(increment.apply(i));
            var g = new GregorianCalendar(TimeZone.getTimeZone(ZoneId.of("UTC")));
            g.set(tmp.getYear(), timeSetMonth.apply(tmp), timeSetDay.apply(tmp), 0, 0, 0);
            intervals.add(g.toInstant().getEpochSecond());
        });
        return intervals;
    }

    /**
     * Returns the number of "start of the quarter" that there are in between the 2 time ranges. If the "to" time range is null then we consider "end of year" for the time range end (to).
     *
     * @param rangeFrom starting timestamp. If null, start of the year from rangeTo will be used
     * @param rangeTo ending timestamp. If null end of the year from rangeFrom will be used
     * @return number of quarters
     */
    public static int getNumberOfQuartersInYearByStartOfQuarter(ZonedDateTime rangeFrom, ZonedDateTime rangeTo) {
        var diffYears = (rangeTo != null ? rangeTo.getYear() : rangeFrom.getYear()) - (rangeFrom != null ? rangeFrom.getYear() : rangeTo.getYear());
        if(diffYears < 0){
            return 0;
        }
        else if(diffYears >= 1){
            // we calculate the quarters for the start year and the quarters for the end year
            var count = getNumberOfQuartersInYearByStartOfQuarter(rangeFrom, null);
            count += getNumberOfQuartersInYearByStartOfQuarter(null, rangeTo);
            if(diffYears > 1){ // there is at least 1 full year = 4 quarters, plus the quarters from the first and last year
                count += 4 * (diffYears-1);
            }
            return count;
        }
        // same year
        var startingQuarter = 0;
        if (rangeFrom != null) {
            startingQuarter = rangeFrom.getMonthValue() == 1 && rangeFrom.getDayOfMonth() == 1 ? 0 // Jan 1srt will include first quarter
                    : (rangeFrom.getMonthValue() >= 1 && rangeFrom.getMonthValue() < 4) || (rangeFrom.getMonthValue() == 4 && rangeFrom.getDayOfMonth() == 1) ? 1 // from Jan 2nd to Apr 1st the second quarter will be included
                    : (rangeFrom.getMonthValue() >= 4 && rangeFrom.getMonthValue() < 7) || (rangeFrom.getMonthValue() == 7 && rangeFrom.getDayOfMonth() == 1) ? 2 // from Apr 2nd to Jul 1st the third quarter will be included
                    : (rangeFrom.getMonthValue() >= 7 && rangeFrom.getMonthValue() < 10) || (rangeFrom.getMonthValue() == 10 && rangeFrom.getDayOfMonth() == 1) ? 3 // from Jul 2nd to Oct 1st the 4th quarter will be included
                    : 0; // error
        }
        var endingQuarter = 4;
        if (rangeTo != null){
            endingQuarter = rangeTo.getMonthValue() >= 10 ? 4 // last quarter should be included
                    : rangeTo.getMonthValue() >= 7 ? 3 // only up to the 3rd quarter should be included
                    : rangeTo.getMonthValue() >= 4 ? 2 // only up to the second quarter should be included
                    : 1;  // only the first quarter should be included
        }
        return endingQuarter - startingQuarter;
    }

    @Value
    @Builder(toBuilder = true)
    public static class Options {
        @lombok.NonNull
        String columnName;
        @lombok.NonNull
        String across;
        @Nullable
        String interval;
        @Builder.Default
        boolean isBigInt = false;
        @Builder.Default
        boolean sortAscending = false;
        @Builder.Default
        boolean isRelative = false;
        @Builder.Default
        boolean prefixWithComma = true;
    }

    public static AggTimeQuery getAggTimeQuery(String columnName, String across, String interval, boolean isBigInt) {
        return getAggTimeQuery(Options.builder()
                .columnName(columnName)
                .across(across)
                .interval(interval)
                .isBigInt(isBigInt)
                .sortAscending(SORT_DESC_BY_DEFAULT)
                .isRelative(false)
                .build());
    }

    /**
     * Use this method to get {@link AggTimeQuery} containing different parts of a query.
     *
     * @param columnName Column name of the timestamp field (postgres dtype = bigint),
     *                   (The timestamps must be in epoch seconds)
     * @param across     Value of the across, the group by field is aliased with this name
     * @param interval   Interval for the aggregation, must be one of {@link #DATE_FIELD_UNITS}
     * @return {@link AggTimeQuery} containing different components of a query.
     */
    public static AggTimeQuery getAggTimeQuery(String columnName, String across, String interval,
                                               boolean isBigInt, boolean sortAscending) {
        return getAggTimeQuery(Options.builder()
                .columnName(columnName)
                .across(across)
                .interval(interval)
                .isBigInt(isBigInt)
                .sortAscending(sortAscending)
                .isRelative(false)
                .build());
    }

    /**
     * @deprecated replaced with {@link AggTimeQueryHelper#getAggTimeQuery(io.levelops.commons.databases.utils.AggTimeQueryHelper.Options)}
     */
    @Deprecated
    public static AggTimeQuery getAggTimeQuery(String columnName, String across, String interval,
                                               boolean isBigInt, boolean sortAscending, boolean isRelative) {
        return getAggTimeQuery(Options.builder()
                .columnName(columnName)
                .across(across)
                .interval(interval)
                .isBigInt(isBigInt)
                .sortAscending(sortAscending)
                .isRelative(isRelative)
                .build());
    }

    public static AggTimeQuery getAggTimeQuery(Options options) {
        String columnName = options.getColumnName();
        String across = options.getAcross();
        String interval = options.getInterval();
        boolean isBigInt = options.isBigInt();
        boolean sortAscending = options.isSortAscending();
        boolean isRelative = options.isRelative();

        String selectString;
        String groupByString;
        String orderByString;
        String helperColumn;

        if (StringUtils.isEmpty(interval) || !isValid(interval)) {
            interval = "day";
        }

        final String helperAlias = across + "_interval";

        if (isRelative) {
            String biWeekColumn = "";
            helperColumn = getTruncatedTimestamp(interval.toLowerCase(), columnName, isBigInt) + " as " + helperAlias;
            if (interval.equals("biweekly")) {
                helperColumn = getTruncatedTimestampForBiWeek(columnName, isBigInt) + " as " + helperAlias;
                biWeekColumn = " ::int/2 + 1 ";
                interval = "week";
            }
            selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                    + getDatePart(interval.toLowerCase(), helperAlias) + biWeekColumn + " - "
                    + getDatePart(interval.toLowerCase(), "now()") + biWeekColumn + " as interval";
            groupByString = helperAlias;
            orderByString = helperAlias;
        } else {
            switch (interval.toLowerCase()) {
                case "year":
                    helperColumn = getTruncatedTimestamp("year", columnName, isBigInt) + " as " + helperAlias;
                    selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                            + getDatePart("year", helperAlias) + " as interval";
                    groupByString = helperAlias;
                    orderByString = helperAlias;
                    break;
                case "quarter":
                    helperColumn = getTruncatedTimestamp("quarter", columnName, isBigInt) + " as " + helperAlias;
                    selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                            + "CONCAT('Q', " + getDatePart("quarter", helperAlias) + ", '-' ,"
                            + getDatePart("year", helperAlias) + ") as interval";
                    groupByString = helperAlias;
                    orderByString = helperAlias;
                    break;
                case "biweekly":
                    helperColumn = getTruncatedTimestampForBiWeek(columnName, isBigInt) + " as " + helperAlias;
                    selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                            + "CONCAT('biweekly-', " + getDatePart("week", helperAlias) + "::int/2 + 1, '-' ,"
                            + getDatePart("year", helperAlias, true) + ") as interval";
                    groupByString = helperAlias;
                    orderByString = helperAlias;
                    break;
                case "week":
                    helperColumn = getTruncatedTimestamp("week", columnName, isBigInt) + " as " + helperAlias;
                    selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                            + "CONCAT(" + getDatePart("week", helperAlias) + ", '-' ,"
                            + getDatePart("year", helperAlias, true) + ") as interval";
                    groupByString = helperAlias;
                    orderByString = helperAlias;
                    break;
                case "month":
                    helperColumn = getTruncatedTimestamp("month", columnName, isBigInt) + " as " + helperAlias;
                    selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                            + "CONCAT(" + getDatePart("month", helperAlias) + ", '-' ,"
                            + getDatePart("year", helperAlias) + ") as interval";
                    groupByString = helperAlias;
                    orderByString = helperAlias;
                    break;
                case "day":
                    helperColumn = getTruncatedTimestamp("day", columnName, isBigInt) + " as " + helperAlias;
                    selectString = "EXTRACT(EPOCH FROM " + helperAlias + ") as " + across + ", "
                            + "CONCAT(" + getDatePart("day", helperAlias) + ", '-' ,"
                            + getDatePart("month", helperAlias) + ", '-' ,"
                            + getDatePart("year", helperAlias) + ") as interval";
                    groupByString = helperAlias;
                    orderByString = helperAlias;
                    break;
                case "day_of_week":
                    helperColumn = " Rtrim(To_char(Date(" + columnName + "), 'Day'))" + " as " + helperAlias;
                    selectString = helperAlias + " as " + across + ", " +
                            helperAlias + " as interval";
                    groupByString = across;
                    orderByString = across;
                    break;
                default:
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided for aggregation. Interval: " + interval);
            }
        }

        String helperColumnPrefix = options.isPrefixWithComma() ? "," : "";
        return AggTimeQuery.builder()
                .helperColumn(helperColumnPrefix + helperColumn)
                .select(selectString)
                .groupBy(groupByString)
                .orderBy(orderByString + ((sortAscending) ? " ASC" : " DESC"))
                .intervalKey("interval")
                .intervalValue(interval)
                .build();
    }

    /**
     * @deprecated call the method by passing Options
     */
    @Deprecated
    public static Query getAggTimeQueryForTimestamp(String columnName, String across, String interval,
                                                    boolean isBigInt, boolean sortAscending, boolean isRelative) {
        return getAggTimeQueryForTimestamp(Options.builder()
                .columnName(columnName)
                .across(across)
                .interval(interval)
                .isBigInt(isBigInt)
                .sortAscending(sortAscending)
                .isRelative(isRelative)
                .build());
    }

    public static Query getAggTimeQueryForTimestamp(Options options) {
        String columnName = options.getColumnName();
        String across = options.getAcross();
        String interval = options.getInterval();
        boolean isBigInt = options.isBigInt();
        boolean sortAscending = options.isSortAscending();
        boolean isRelative = options.isRelative();
        List<Query.SelectField> selectFields = new ArrayList<>();

        if (StringUtils.isEmpty(interval) || !isValid(interval)) {
            interval = "day";
        }
        String truncatedTimestamp;
        if (interval.equals("biweekly")) {
            truncatedTimestamp = getTruncatedTimestampForBiWeek(columnName, isBigInt);
        } else {
            truncatedTimestamp = getTruncatedTimestamp(interval.toLowerCase(), columnName, isBigInt);
        }

        String epochAlias = across + "_epoch";
        Query.SelectField truncatedEpoch = Query.selectField(
                "EXTRACT(EPOCH FROM " + truncatedTimestamp + ")", epochAlias);
        Query.SelectField intervalStmt;
        if (isRelative) {
            String biWeekColumn = "";
            if (interval.equals("biweekly")) {
                interval = "week";
                biWeekColumn = " ::int/2 + 1 ";
            }
            intervalStmt = Query.selectField(
                    getDatePart(interval.toLowerCase(), truncatedTimestamp) + biWeekColumn + " - " +
                            getDatePart(interval.toLowerCase(), "now()") + biWeekColumn, "interval");
        } else {
            switch (interval.toLowerCase()) {
                case "year":
                    intervalStmt = Query.selectField(getDatePart("year", truncatedTimestamp), "interval");
                    break;
                case "quarter":
                    intervalStmt = Query.selectField("CONCAT('Q', " + getDatePart("quarter", truncatedTimestamp) + ", '-' ,"
                            + getDatePart("year", truncatedTimestamp) + ")", "interval");
                    break;
                case "biweekly":
                    intervalStmt = Query.selectField("CONCAT('biweekly-', " + getDatePart("week", truncatedTimestamp) + " :: int/2 + 1, '-' ,"
                            + getDatePart("year", truncatedTimestamp, true) + ")", "interval");
                    break;
                case "week":
                    intervalStmt = Query.selectField("CONCAT(" + getDatePart("week", truncatedTimestamp) + ", '-' ,"
                            + getDatePart("year", truncatedTimestamp, true) + ")", "interval");
                    break;
                case "month":
                    intervalStmt = Query.selectField("CONCAT(" + getDatePart("month", truncatedTimestamp) + ", '-' ,"
                            + getDatePart("year", truncatedTimestamp) + ")", "interval");
                    break;
                case "day":
                    intervalStmt = Query.selectField("CONCAT(" + getDatePart("day", truncatedTimestamp) + ", '-' ,"
                            + getDatePart("month", truncatedTimestamp) + ", '-' ,"
                            + getDatePart("year", truncatedTimestamp) + ")", "interval");
                    break;
                default:
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided for aggregation. Interval: " + interval);
            }
        }
        selectFields.add(truncatedEpoch);
        selectFields.add(intervalStmt);

        List<Query.GroupByField> groupByFields = new ArrayList<>();
        groupByFields.add(Query.groupByField(epochAlias));
        groupByFields.add(Query.groupByField("interval"));

        return Query.builder().select(selectFields)
                .groupBy(groupByFields)
                .orderBy(Query.sortByField(epochAlias, ((sortAscending) ? " ASC" : " DESC"), false))
                .build();
    }

    public static boolean isValid(String interval) {
        return DATE_FIELD_UNITS.contains(interval.toLowerCase());
    }

    private static String getTruncatedTimestamp(String interval, String columnName, Boolean isBigInt) {
        if (isValid(interval)) {
            if (Boolean.TRUE.equals(isBigInt)) {
                return "date_trunc('" + interval + "',to_timestamp(" + columnName + "))";
            } else {
                return "date_trunc('" + interval + "', " + columnName + ")";
            }
        }
        return null;
    }

    private static String getTruncatedTimestampForBiWeek(String columnName, Boolean isBigInt) {
        String dateSql;
        if (Boolean.TRUE.equals(isBigInt)) {
            dateSql = "to_timestamp(" + columnName + ")";
        } else {
            dateSql = columnName;
        }
        String yearSql = "date_part('YEAR'," + dateSql + ")";
        String yearCorrection = getYearCorrectionForWeek(dateSql);
        String weekSql = "(((Date_part('WEEK'," + dateSql + ") + 1)::integer / 2) * 2 - 1)";
        return "to_date( concat(" + yearSql + yearCorrection + "," + weekSql + "), 'iyyyiw')";
    }

    /**
     * ISO considers that the first week of the year must contain Jan 4 so Jan 1 to 4 may belong the week 52 or 53 of previous year.
     * In such case, we need to subtract 1 from the year.
     * E.g. 2005-01-01 is in week 53 of 2004 (not 2005!)
     *
     * https://www.postgresql.org/docs/8.1/functions-datetime.html#FUNCTIONS-DATETIME-EXTRACT
     */
    private static String getYearCorrectionForWeek(String dateSql) {
        return " - (case when (date_part('WEEK'," + dateSql + ") > 10) and (date_part('month', " + dateSql + ") = 1) then 1 else 0 end)";
    }

    private static String getDatePart(String interval, String timestampColumn) {
        return getDatePart(interval, timestampColumn, false);
    }

    private static String getDatePart(String interval, String timestampColumn, boolean correctForWeek) {
        if (isValid(interval)) {
            return "(date_part('" + interval + "'," + timestampColumn + ")"
                    + (correctForWeek? getYearCorrectionForWeek(timestampColumn) : "")
                    + ")";
        }
        return null;
    }

    @NotNull
    public static ImmutablePair<Long, Long> getTimeRangeForStacks(DbAggregationResult row, String aggInterval) {
        Calendar cal = Calendar.getInstance();
        long startTimeInSeconds = Long.parseLong(row.getKey());
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if (aggInterval.equals(AGG_INTERVAL.month.toString())) {
            cal.add(Calendar.MONTH, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.day.toString())) {
            cal.add(Calendar.DATE, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.year.toString())) {
            cal.add(Calendar.YEAR, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.quarter.toString())) {
            cal.add(Calendar.MONTH, 3);
        } else {
            cal.add(Calendar.DATE, 7);
        }
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
        return ImmutablePair.of(startTimeInSeconds, endTimeInSeconds);
    }

    public static Query getAggTimeQueryForTimestampForList(String interval, boolean isBigInt) {
        String columnName = "ingested_at";
        List<Query.SelectField> selectFields = new ArrayList<>();
        String truncatedTimestamp = getTruncatedTimestamp(interval.toLowerCase(), columnName, isBigInt);
        if (interval.equals("biweekly")) {
            truncatedTimestamp = getTruncatedTimestampForBiWeek(columnName, isBigInt);
        }

        String epochAlias = "trend_epoch";
        Query.SelectField truncatedEpoch = Query.selectField(
                "EXTRACT(EPOCH FROM " + truncatedTimestamp + ")", epochAlias);
        selectFields.add(truncatedEpoch);
        return Query.builder().select(selectFields)
                .build();
    }

    public static Query getAggTimeQueryForTimestampForFilter(String interval, boolean isBigInt) {
        String columnName = ":wi_ingested_at_for_trend";
        List<Query.SelectField> selectFields = new ArrayList<>();
        String truncatedTimestamp = getTruncatedTimestamp(interval.toLowerCase(), columnName, isBigInt);
        if (interval.equals("biweekly")) {
            truncatedTimestamp = getTruncatedTimestampForBiWeek(columnName, isBigInt);
        }

        String epochAlias = "trend_epoch";
        Query.SelectField truncatedEpoch = Query.selectField(
                "EXTRACT(EPOCH FROM " + truncatedTimestamp + ")", epochAlias);
        selectFields.add(truncatedEpoch);
        return Query.builder().select(selectFields)
                .build();
    }

    public static AggTimeQuery getAggTimeQueryForList(Options options) {
        String columnName = options.getColumnName();
        String across = options.getAcross();
        String interval = options.getInterval();
        boolean isBigInt = options.isBigInt();
        String selectString;
        String helperColumn;
        final String helperAlias = across + "_interval";

        switch (interval.toLowerCase()) {
            case "year":
                helperColumn = getTruncatedTimestamp("year", columnName, isBigInt) + " as " + helperAlias;
                selectString = getDatePart("year", getTruncatedTimestamp("year", columnName, isBigInt)) + " as interval";
                break;
            case "quarter":
                helperColumn = getTruncatedTimestamp("quarter", columnName, isBigInt) + " as " + helperAlias;
                selectString = "CONCAT('Q', " + getDatePart("quarter", getTruncatedTimestamp("quarter", columnName, isBigInt)) + ", '-' ,"
                        + getDatePart("year", getTruncatedTimestamp("quarter", columnName, isBigInt)) + ") as interval";
                break;
            case "biweekly":
                helperColumn = getTruncatedTimestampForBiWeek(columnName, isBigInt) + " as " + helperAlias;
                selectString = "CONCAT('biweekly-', " + getDatePart("week", getTruncatedTimestampForBiWeek(columnName, isBigInt)) + "::int/2 + 1, '-' ,"
                        + getDatePart("year", getTruncatedTimestampForBiWeek(columnName, isBigInt), true) + ") as interval";
                break;
            case "week":
                helperColumn = getTruncatedTimestamp("week", columnName, isBigInt) + " as " + helperAlias;
                selectString = "CONCAT(" + getDatePart("week", getTruncatedTimestamp("week", columnName, isBigInt)) + ", '-' ,"
                        + getDatePart("year", getTruncatedTimestamp("week", columnName, isBigInt), true) + ") as interval";
                break;
            case "month":
                helperColumn = getTruncatedTimestamp("month", columnName, isBigInt) + " as " + helperAlias;
                selectString = "CONCAT(" + getDatePart("month", getTruncatedTimestamp("month", columnName, isBigInt)) + ", '-' ,"
                        + getDatePart("year", getTruncatedTimestamp("month", columnName, isBigInt)) + ") as interval";
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided for filter. Interval: " + interval);
        }
        return AggTimeQuery.builder()
                .helperColumn(helperColumn)
                .select(selectString)
                .build();
    }

    public static String getIntervalFilterString(String interval) {
        String intervalFilterString;
        String helperAlias = "Date_trunc('" + interval.toLowerCase() + "', To_timestamp(:jira_ingested_at_for_trend))";
        switch (interval.toLowerCase()) {
            case "year":
                intervalFilterString = getDatePart("year", helperAlias);
                break;
            case "quarter":
                intervalFilterString = "CONCAT('Q', " + getDatePart("quarter", helperAlias) + ", '-' ,"
                        + getDatePart("year", helperAlias) + ")";
                break;
            case "biweekly":
                helperAlias = "Date_trunc('week', To_timestamp(:jira_ingested_at_for_trend))";
                intervalFilterString = "CONCAT('biweekly-', " + getDatePart("week", helperAlias) + "::int/2 + 1, '-' ,"
                        + getDatePart("year", helperAlias, true) + ")";
                break;
            case "week":
                intervalFilterString = "CONCAT(" + getDatePart("week", helperAlias) + ", '-' ,"
                        + getDatePart("year", helperAlias, true) + ")";
                break;
            case "month":
                intervalFilterString = "CONCAT(" + getDatePart("month", helperAlias) + ", '-' ,"
                        + getDatePart("year", helperAlias) + ")";
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interval provided for filter. Interval: " + interval);
        }
        return intervalFilterString;
    }

    /**
     * Model class for returning different parts of an interval based aggregation query
     */
    @Value
    @Builder
    public static class AggTimeQuery {
        String select; // the select statement to be added to the parent query
        String groupBy; // the group by field
        String orderBy; // the order by field

        /**
         * definition of the helper column which is used in the {@link #select} statement,
         * must be used with the internal select statements
         */
        String helperColumn;

        String intervalKey; // field alias containing a string representation of the interval
        String intervalValue;
    }
}
