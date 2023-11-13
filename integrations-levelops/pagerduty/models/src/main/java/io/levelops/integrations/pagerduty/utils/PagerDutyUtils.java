package io.levelops.integrations.pagerduty.utils;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Log4j2
public class PagerDutyUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    public static String formatDate(Date date){
        log.debug("formatting Date: {}", date);
        return formatDate(date.toInstant());
    }

    public static String formatDate(Long date){
        log.debug("formattind date: {}", date);
        var dateInSeconds = Integer.valueOf(date.toString().substring(0,10));
        log.debug("date in seconds: {}", dateInSeconds);
        var i = Instant.ofEpochSecond(dateInSeconds);
        return formatDate(i);
    }

    public static String formatDate(Instant date){
        log.debug("format instant: {}", date);
        var s = formatter.format(date);
        log.debug("formatted UTC: {}", s);
        return s;
    }
}