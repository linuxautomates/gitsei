package io.levelops.commons.dates;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class DateUtils {
    private final static Pattern zoneCorrection = Pattern.compile("^[\\+\\-][0-9]{4}$");
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");


    // region EPOCH
    public static Long toEpochSecond(@Nullable Long epochSecondOrMs) {
        if (epochSecondOrMs == null) {
            return null;
        }
        // 30000000000L in seconds is year 2920; and in ms, it is 1970
        return (epochSecondOrMs < 30000000000L) ? epochSecondOrMs : epochSecondOrMs / 1000;
    }

    public static Instant fromEpochSecond(@Nullable Long epochSec, Instant defaultValue) {
        return epochSec != null ? Instant.ofEpochSecond(epochSec) : defaultValue;
    }

    @Nullable
    public static Instant fromEpochSecond(@Nullable Long epochSec) {
        return fromEpochSecond(epochSec, null);
    }

    @Nullable
    public static Long toEpochSecond(@Nullable Instant epochSec) {
        return epochSec != null ? epochSec.getEpochSecond() : null;
    }

    @Nullable
    public static Long toEpochSecond(@Nullable Date date) {
        return date != null ? toEpochSecond(date.toInstant()) : null;
    }

    /**
     * Truncate date up to given field and return epoch in seconds
     * @param field e.g. Calendar.DATE
     */
    @Nullable
    public static Long truncate(@Nullable Date date, int field) {
        return date != null
                ? org.apache.commons.lang3.time.DateUtils.truncate(date, field).toInstant().getEpochSecond()
                : null;
    }

    @Nullable
    public static Long truncate(@Nullable Instant instant, int field) {
        return truncate(toDate(instant), field);
    }

    @Nullable
    public static Long truncate(@Nullable Long epochSecond, int field) {
        return truncate(fromEpochSecondToDate(epochSecond), field);
    }

    @Nullable
    public static Date fromEpochSecondToDate(@Nullable Long epochSec) {
        Instant instant = fromEpochSecond(epochSec);
        return instant != null ? Date.from(instant) : null;
    }

    public static Date fromEpochSecondToDate(@Nullable Long epochSec, Date defaultValue) {
        Instant instant = fromEpochSecond(epochSec);
        return instant != null ? Date.from(instant) : defaultValue;
    }

    public static Timestamp fromEpochSecondToTimestamp(@Nullable Long epochSec) {
        return toTimestamp(fromEpochSecond(epochSec));
    }

    @Nullable
    public static Long toEpochSecond(@Nullable LocalDateTime localDateTime, @Nonnull ZoneId zoneId) {
        Validate.notNull(zoneId, "zoneId cannot be null.");
        return localDateTime != null ? localDateTime.atZone(zoneId).toEpochSecond() : null;
    }

    @Nullable
    public static Long toEpochSecond(@Nullable LocalDateTime localDateTime) {
        return toEpochSecond(localDateTime, ZoneId.systemDefault());
    }

    //endregion

    // region CASTING
    public static Instant toInstant(@Nullable Date date, Instant defaultInstant) {
        return date != null ? date.toInstant() : defaultInstant;
    }

    public static Instant toInstant(@Nullable Date date, Date defaultDate) {
        return toInstant(date != null ? date : defaultDate);
    }

    @Nullable
    public static Instant toInstant(@Nullable Date date) {
        return date != null ? date.toInstant() : null;
    }

    @Nonnull
    public static Instant toInstant(@Nullable Timestamp ts, @Nonnull Instant defaultValue) {
        return ts != null ? ts.toInstant() : defaultValue;
    }

    @Nullable
    public static Instant toInstant(@Nullable Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    public static Date toDate(@Nullable Instant instant, Date defaultDate) {
        return instant != null ? Date.from(instant) : defaultDate;
    }

    public static Date toDate(@Nullable Instant instant, Instant defaultInstant) {
        return toDate(instant != null ? instant : defaultInstant);
    }

    public static Date toDate(@Nullable Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }

    public static Instant toStartOfDay(Instant dt) {
        LocalDateTime d = LocalDateTime.ofInstant(dt, UTC_ZONE_ID);
        return d.with(LocalTime.MIN).atZone(UTC_ZONE_ID).toInstant();
    }

    public static Instant toEndOfDay(Instant dt) {
        LocalDateTime d = LocalDateTime.ofInstant(dt, UTC_ZONE_ID);
        return d.with(LocalTime.MAX).atZone(UTC_ZONE_ID).toInstant();
    }

    @Nonnull
    public static Timestamp toTimestamp(@Nullable Instant instant, @Nonnull Instant defaultValue) {
        return toTimestamp(instant != null ? instant : defaultValue);
    }

    @Nullable
    public static Timestamp toTimestamp(@Nullable Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    @Nullable
    public static Timestamp toTimestamp(@Nullable Date date) {
        return toTimestamp(toInstant(date));
    }
    // endregion

    // region COMPARISON

    public static boolean isBetween(@Nullable Instant i,
                                    @Nullable Instant from, boolean fromInclusive,
                                    @Nullable Instant to, boolean toInclusive) {
        if (i == null) {
            return false;
        }
        if (from == null && to == null) {
            return true;
        }
        if (to == null) {
            return i.isAfter(from) || (fromInclusive && i.equals(from));
        }
        if (from == null) {
            return i.isBefore(to) || (toInclusive && i.equals(to));
        }
        if (i.isAfter(from) && i.isBefore(to)) {
            return true;
        }
        return (toInclusive && i.equals(to)) || (fromInclusive && i.equals(from));
    }

    public static boolean isBetween(@Nullable Date d,
                                    @Nullable Date from, boolean fromInclusive,
                                    @Nullable Date to, boolean toInclusive) {
        return isBetween(toInstant(d), toInstant(from), fromInclusive, toInstant(to), toInclusive);
    }

    public static boolean isLongerThan(Instant a, Instant b, long duration, TemporalUnit timeUnit) {
        if (a == null || b == null) {
            return true;
        }
        return Duration.between(a, b).abs().compareTo(Duration.of(duration, timeUnit)) > 0;
    }

    @Nullable
    public static Instant parseDateTime(@Nullable final String dateTime) {
        if (StringUtils.isBlank(dateTime)) {
            return null;
        }
        try {
            return Instant.parse(dateTime);
        } catch (DateTimeParseException e) {
            // FIX for unparsable dates of the type 2020-08-24T00:31:14.497+0000 
            // that are parsable by converting to 2020-08-24T00:31:14.497+00:00 
            if (StringUtils.isNotBlank(dateTime) && dateTime.length() > 5) {
                var zone = dateTime.substring(dateTime.length() - 5);
                if (zoneCorrection.matcher(zone).matches()) {
                    zone = zone.substring(0, 3) + ":" + zone.substring(3);
                    return Instant.parse(dateTime.substring(0, dateTime.length() - 5) + zone);
                }
            }
            throw e;
        }
    }

    @Nullable
    public static Date parseDateTimeToDate(@Nullable String dateTime) {
        return DateUtils.toDate(parseDateTime(dateTime));
    }

    // endregion

    @Nullable
    public static Instant latest(@Nullable Instant a, @Nullable Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    @Nullable
    public static Instant earliest(@Nullable Instant a, @Nullable Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? b : a;
    }

    // region partitions

    public static List<ImmutablePair<Long, Long>> getDailyPartition(Instant from, Instant to) {
        return getPartition(from, to, ChronoField.HOUR_OF_DAY, 1, ChronoUnit.DAYS, 1);
    }

    public static List<ImmutablePair<Long, Long>> getWeeklyPartition(Instant from, Instant to) {
        return getPartition(from, to, ChronoField.DAY_OF_WEEK, 1, ChronoUnit.WEEKS, 1);
    }

    public static List<ImmutablePair<Long, Long>> getMonthlyPartition(Instant from, Instant to) {
        return getPartition(from, to, ChronoField.DAY_OF_MONTH, 1, ChronoUnit.MONTHS, 1);
    }

    public static List<ImmutablePair<Long, Long>> getYearlyPartition(Instant from, Instant to) {
        return getPartition(from, to, ChronoField.MONTH_OF_YEAR, 1, ChronoUnit.YEARS, 1);
    }

    protected static List<ImmutablePair<Long, Long>> getPartition(Instant from, Instant to, TemporalField adjuster, long adjustedValue, TemporalUnit intervalUnit, long interval) {
        List<ImmutablePair<Long, Long>> partition = new ArrayList<>();
        ZonedDateTime start = from.atZone(ZoneId.of("UTC")).with(adjuster, adjustedValue);
        ZonedDateTime end = to.atZone(ZoneId.of("UTC"));
        ZonedDateTime current = start;
        while (current.isBefore(end)) {
            ZonedDateTime next = current
                    .plus(interval, intervalUnit)
                    .with(adjuster, adjustedValue);
            partition.add(ImmutablePair.of(current.toEpochSecond(), next.toEpochSecond()));
            current = next;
        }
        return partition;
    }

    // endregion

    /**
     * Converts date to ISO String (such as "2023-02-28T07:01:51Z").
     */
    @Nullable
    public static String toString(@Nullable Date date) {
        return toString(toInstant(date));
    }

    /**
     * Converts instant to ISO String (such as "2023-02-28T07:01:51Z").
     */
    @Nullable
    public static String toString(@Nullable Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    public static String getWeeklyFormat(Long epochSecond) {
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.of("UTC"));
        int weekOfTheYear = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        int year = date.getYear();
        String weeklyFormat = String.format("%02d-%04d", weekOfTheYear, year);
        return weeklyFormat;
    }

}
