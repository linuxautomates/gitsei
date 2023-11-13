package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class CxSastIssueFilter {

    DISTINCT across;
    CALCULATION calculation;
    AGG_INTERVAL aggInterval;
    Integer acrossLimit;
    List<String> integrationIds;
    List<String> scanIds;
    List<String> statuses;
    Boolean falsePositive;
    List<String> severities;
    List<String> assignees;
    List<String> states;
    List<String> projects;
    List<String> languages;
    List<String> categories;
    List<String> issueNames;
    List<String> issueGroups;
    List<String> files;

    public enum DISTINCT {
        state,
        severity,
        status,
        assignee,
        project,
        issue_name,
        issue_group,
        trend,
        file,
        language,
        none;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CxSastIssueFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CxSastIssueFilter.CALCULATION.class, st);
        }
    }
}
