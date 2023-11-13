package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

@Log4j2
@Getter
public enum ESAggInterval {
    YEAR(CalendarInterval.Year, "yyyy", null, false),
    QUARTER(CalendarInterval.Quarter, "q-yyyy", null, true),
    //BIWEEKLY(null, null, "ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(doc['w_resolved_at'].value.millis), ZoneId.of('UTC')); int y = zdt.getYear(); int w = (zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)/2)+1; return(y.toString() + \\\"-\\\" + w.toString());", true),
    BIWEEKLY(CalendarInterval.Week, "ww-yyyy", null, true), //Bi Weekly is painFULL in ES. Mainly to allign with DB. Easier approach is to do weekly & merge in Java Side.
    WEEK(CalendarInterval.Week, "ww-yyyy", null, true),
    MONTH(CalendarInterval.Month, "MM-yyyy", null, true),
    DAY(CalendarInterval.Day, "dd-MM-yyyy", null, false),
    DAY_OF_WEEK(null, null, "doc['w_created_at'].value.dayOfWeekEnum.getDisplayName(TextStyle.SHORT, Locale.ROOT)", false);

    private final CalendarInterval calendarInterval;
    private final String format;
    private final String source;
    private final Boolean supportsBA;

    ESAggInterval(CalendarInterval calendarInterval, String format, String source, Boolean supportsBA) {
        this.calendarInterval = calendarInterval;
        this.format = format;
        this.source = source;
        this.supportsBA = supportsBA;
    }

    @JsonCreator
    @Nullable
    public static ESAggInterval fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ESAggInterval.class, value);
    }

    public static ESAggInterval fromStringWithDefault(@Nullable String value, ESAggInterval defaultAggInterval) {
        return EnumUtils.getEnumIgnoreCase(ESAggInterval.class, value, defaultAggInterval);
    }

    public String formatEpochSeconds(Long epochSeconds) {
        Instant i = Instant.ofEpochSecond(epochSeconds);
        LocalDate localDate = LocalDate.ofInstant(i, ZoneId.of("UTC"));
        if (this.getCalendarInterval() == CalendarInterval.Year) {
            int year = localDate.getYear();
            String formattedDate = String.valueOf(year);
            return formattedDate;
        } else if (this.getCalendarInterval() == CalendarInterval.Quarter) {
            int quarter = localDate.get(IsoFields.QUARTER_OF_YEAR);
            int year = localDate.getYear();
            String formattedDate = quarter + "-" + year;
            return formattedDate;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.getFormat())
                    .withZone(ZoneId.of("UTC"));
            String formattedDate = formatter.format(i);
            return formattedDate;
        }
    }
}
