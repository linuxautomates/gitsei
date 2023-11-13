package io.levelops.commons.databases.services;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CiCdDateUtils {

    public static int extractDataComponentForDbResults(Date date, int dateComponent, boolean isIntervalQuarter) {
        Calendar calendar = getPGCompatibleCalendar();
        calendar.setTime(date);
        if (isIntervalQuarter) {
            return (calendar.get(dateComponent) / 3) + 1;
        }
        if(dateComponent == 2) {
            return calendar.get(dateComponent) + 1;
        }
        return calendar.get(dateComponent);
    }

    /**
     * By definition, ISO weeks start on Mondays and the first week of a year contains January 4 of that year.
     * In other words, the first Thursday of a year is in week 1 of that year.
     * {@see https://tapoueh.org/blog/2017/06/postgresql-and-the-calendar/}
     */
    @NotNull
    public static Calendar getPGCompatibleCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }
}
