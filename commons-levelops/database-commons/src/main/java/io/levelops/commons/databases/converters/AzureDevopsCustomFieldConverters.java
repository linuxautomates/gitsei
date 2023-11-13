package io.levelops.commons.databases.converters;

import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.Fields;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;

import static io.levelops.commons.databases.models.database.jira.DbJiraField.TYPE_DATE;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.TYPE_DATETIME;

@Log4j2
public class AzureDevopsCustomFieldConverters {

    private static final int MAX_WIDTH = 50;

    public static Date parseDate(Object value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(String.valueOf(value));
        } catch (ParseException e) {
            log.error("Failed to parse Date from value={}", value, e);
            return null;
        }
    }

    public static Long parseDateTime(Object value) {
        try {
            return DateUtils.toEpochSecond(DateUtils.parseDateTime(String.valueOf(value)));
        } catch (DateTimeParseException e) {
            log.error("Failed to parse DateTime from value={}", value, e);
            return null;
        }
    }

    @Nullable
    public static Object parseValue(@Nullable Object value, @Nullable String fieldType) {
        if (Objects.isNull(value)) {
            return null;
        }

        // -- dates
        if (TYPE_DATE.equalsIgnoreCase(fieldType)) {
            return AzureDevopsCustomFieldConverters.parseDate(value);
        } else if (TYPE_DATETIME.equalsIgnoreCase(fieldType)) {
            return AzureDevopsCustomFieldConverters.parseDateTime(value);
        }

        String parsedValue = null;
        if (value instanceof String) {
            parsedValue = (String) value;
        } else if (value instanceof Number || value instanceof Boolean) {
            parsedValue = String.valueOf(value);
        } else if (value instanceof Fields.AuthorizationDetail) {
            parsedValue = ((Fields.AuthorizationDetail) value).getUniqueName();
        }
        return StringUtils.truncate(parsedValue, MAX_WIDTH);
    }

}
