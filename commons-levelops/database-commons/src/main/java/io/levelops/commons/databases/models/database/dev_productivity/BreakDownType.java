package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.EnumUtils;

public enum BreakDownType {
    JIRA_ISSUES,
    WORKITEM_ISSUES,
    SCM_COMMITS,
    SCM_PRS,
    SCM_CONTRIBUTIONS,
    SONAR_ISSUES;

    @JsonCreator
    public static BreakDownType fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(BreakDownType.class, st);
    }
}
