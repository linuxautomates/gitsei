package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class SonarQubeIssueFilter {

    List<String> severities;
    List<String> types;
    List<String> statuses;
    List<String> projects;
    List<String> organizations;
    List<String> authors;
    List<String> components;
    List<String> tags;
    List<String> integrationIds;
    Map<String, Map<String, String>> partialMatch;

    @NonNull
    Long ingestedAt;

    DISTINCT distinct;
    CALCULATION calculation;

    public enum DISTINCT {
        project,
        type,
        severity,
        status,
        organization,
        trend,
        author,
        tag,
        component,
        none;

        public static SonarQubeIssueFilter.DISTINCT fromString(String across) {
            return EnumUtils.getEnumIgnoreCase(SonarQubeIssueFilter.DISTINCT.class, across);
        }
    }

    public enum CALCULATION {
        effort,
        issue_count;
        public static SonarQubeIssueFilter.CALCULATION fromString(String calculation){
            return EnumUtils.getEnumIgnoreCase(SonarQubeIssueFilter.CALCULATION.class,calculation);
        }
    }
}
