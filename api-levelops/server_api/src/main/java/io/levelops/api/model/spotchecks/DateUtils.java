package io.levelops.api.model.spotchecks;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Date;

public class DateUtils {

    private static final String GITLAB_SPOT_CHECK_REQUEST_DATE_PATTERN = "MM/dd/yyyy";
    private static final String GITLAB_CLOUD_DATE_PATTERN = "MM/dd/yyyy HH:mm:ss";
    public static String gitlabDateToString(Date d) {
        if (d == null) {
            return "";
        }
        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date according to the chosen pattern
        DateFormat df = new SimpleDateFormat(GITLAB_CLOUD_DATE_PATTERN);
        return df.format(d);
    }

    public static Date requestStrToDate (final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        Instant i = LocalDate.parse(input, DateTimeFormatter.ofPattern(GITLAB_SPOT_CHECK_REQUEST_DATE_PATTERN))
                .atStartOfDay()
                .atZone(ZoneOffset.UTC)
                .toInstant();
        Date d = Date.from(i);
        return d;
    }
}
