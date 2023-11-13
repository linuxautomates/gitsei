package io.levelops.integrations.jira.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JiraCustomFieldConverter {

    private static final int MAX_WIDTH = 50;
    private static final int DO_NOT_TRUNCATE = -1;
    private static final String SPRINT_FORMAT = "com.atlassian.greenhopper.service.sprint";

    public static String parseValue(Object value) {
        return parseValue(value, MAX_WIDTH);
    }

    public static String parseValueWithoutTruncation(Object value) {
        return parseValue(value, DO_NOT_TRUNCATE);
    }

    /**
     * @param value
     * @param maxWidth - if maxWidth is less than 0, function will not truncate & return original, else function will truncate to width specified
     * @return
     */
    public static String parseValue(Object value, int maxWidth) {
        if (value == null) {
            return null;
        }
        String parsedValue = null;
        if (value instanceof String) {
            parsedValue = (String) value;
            if (StringUtils.startsWith(parsedValue, SPRINT_FORMAT)) {
                parsedValue = parseSprint(parsedValue);
            }
        } else if (value instanceof Number) {
            parsedValue = String.valueOf(value);
        } else if (value instanceof Map) {
            parsedValue = parseMapValue((Map<?, ?>) value);
        }
        return (maxWidth <= DO_NOT_TRUNCATE) ? parsedValue : StringUtils.truncate(parsedValue, maxWidth);
    }

    private static String parseMapValue(@Nonnull Map<?, ?> o) {
        //if its a user object and not a simple 'value' object
        var parsedValue = ObjectUtils.firstNonNull(o.get("value"), o.get("emailAddress"), o.get("displayName"),
                o.get("name"));
        if (parsedValue instanceof String) {
            return (String) parsedValue;
        }
        if (parsedValue instanceof Number) {
            return String.valueOf(parsedValue);
        }
        return null;
    }

    public static Object serializeValue(String value, String type, String name, String items) {
        if (value == null) {
            return null;
        }
        if (name.equalsIgnoreCase("sprint")) {
            return Integer.parseInt(value);
        } else if (name.equalsIgnoreCase("approvers")) {
            return Arrays.stream(StringUtils.split(value, ","))
                    .map(x -> Map.of("id", x)).collect(Collectors.toList());
        } else {
            switch (StringUtils.defaultString(type).toLowerCase()) {
                case "option":
                    return Map.of("value", value);
                case "number":
                    return Math.round(Float.parseFloat(value));
                case "array":
                    if (items.equalsIgnoreCase("option")) {
                        return Arrays.stream(StringUtils.split(value, ","))
                                .map(x -> Map.of("value", x)).collect(Collectors.toList());
                    } else {
                        return Arrays.stream(StringUtils.split(value, ","))
                                .map(x -> Map.of("id", x)).collect(Collectors.toList());
                    }
                case "option-with-child":
                    List<String> parentChildData = Arrays.stream(StringUtils.split(value, ":"))
                            .collect(Collectors.toList());
                    return Map.of("value", parentChildData.get(0),
                            "child", Map.of("value", parentChildData.get(1)));
                default:
                case "string":
                    return value;
            }
        }
    }

    public static String parseOptionWithChild(@Nonnull Map<?, ?> o) throws JsonProcessingException {
        //if its a user object and not a simple 'value' object
        String option = (String) ObjectUtils.firstNonNull(o.get("value"), "");
        var child = o.get("child");
        String childValue = "";
        if (child instanceof Map) {
            childValue = (String) ((Map<?, ?>) child).get("value");
        }
        return StringUtils.strip(DefaultObjectMapper.get()
                .writeValueAsString(Map.of(option, childValue)), "{}");
    }

    public static Date parseDate(Object value) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse((String) value);
    }

    public static Date parseDateTime(Object value) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse((String) value);
    }

    /**
     * https://community.atlassian.com/t5/Jira-questions/Sprint-field-value-REST-API/qaq-p/229495#M469401
     * The method is used to parse the name of sprint when the sprint is a string instead of map.
     *
     * @param value
     * @return name of the sprint
     */
    private static String parseSprint(String value) {
        final Pattern NAME_PATTERN = Pattern.compile("(name=[^,]*)", Pattern.CASE_INSENSITIVE);
        Matcher sprintMatcher = NAME_PATTERN.matcher(value);
        while (sprintMatcher.find()) {
            String[] fields = sprintMatcher.group().split("=");
            if (fields.length > 1 && !StringUtils.isEmpty(fields[1])) {
                return fields[1];
            }
        }
        return null;
    }

    public static String parseTeam(Map<?, ?> o) {
        if (o == null) {
            return null;
        }
        Object title = o.get("title");
        if (title != null) {
            return title.toString();
        }
        Object id = o.get("id");
        if (id != null) {
            return id.toString();
        }
        return null;
    }
}
