package io.levelops.commons.databases.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScmIssueCorrelationUtil {

    private static final Pattern WORKITEM_PATTERN = Pattern.compile("(\b?\\d+\b?)");
    /**
     * From Jira: "Project keys must start with an uppercase letter, followed by one or more uppercase alphanumeric characters."
     */
    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("((?<!([A-Za-z]{1,10})-?)[A-Za-z][A-Za-z\\d]+-\\d+)");

    public static List<String> extractWorkitems(String... messages) {
        return getIssueKeysOrIds(messages, WORKITEM_PATTERN);
    }

    public static List<String> extractJiraKeys(String... messages) {
        return getIssueKeysOrIds(messages, JIRA_KEY_PATTERN);
    }

    @NotNull
    private static ArrayList<String> getIssueKeysOrIds(String[] messages, Pattern issuePattern) {
        Set<String> workitemIds = new HashSet<>();
        if (ArrayUtils.isNotEmpty(messages)) {
            for (String message : messages) {
                Matcher m = issuePattern.matcher(message);
                while (m.find())
                    workitemIds.add(m.group().toUpperCase());
            }
        }
        return new ArrayList<>(workitemIds);
    }
}
