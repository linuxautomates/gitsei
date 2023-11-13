package io.levelops.integrations.zendesk.converters;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZendeskCustomFieldConverter {

    private static final int MAX_WIDTH = 50;

    public static String parseValue(Object value) {
        return parseValue(value, MAX_WIDTH);
    }

    public static String parseValue(Object value, int maxWidth) {
        if (value == null) {
            return null;
        }
        String parsedValue = null;
        if (value instanceof String) {
            parsedValue = (String) value;
        } else if (value instanceof Number || value instanceof Boolean) {
            parsedValue = String.valueOf(value);
        }
        return StringUtils.truncate(parsedValue, maxWidth);
    }

    public static Date parseDate(Object value) throws ParseException {
        if (value == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd").parse((String) value);
    }

    public static Date parseDateTime(Object value) throws ParseException {
        if (value == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse((String) value);
    }
}
