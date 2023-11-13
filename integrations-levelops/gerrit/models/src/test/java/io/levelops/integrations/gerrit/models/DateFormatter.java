package io.levelops.integrations.gerrit.models;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateFormatter {

    public static String formatDates(String body) {
        Pattern datePattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{9}");
        Matcher matcher = datePattern.matcher(body);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        for (String match : matches) {
            body = body.replace(match, match.replace(" ", "T"));
        }
        return body;
    }
}
