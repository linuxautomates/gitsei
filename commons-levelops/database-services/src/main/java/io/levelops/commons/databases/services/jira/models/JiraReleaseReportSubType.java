package io.levelops.commons.databases.services.jira.models;

import lombok.Getter;

@Getter
public enum JiraReleaseReportSubType {
    JIRA_RELEASE_TABLE_REPORT("jira_release_table_report", true),
    JIRA_RELEASE_TABLE_REPORT_VALUES("jira_release_table_report_values", true);

    private final String displayName;
    private final boolean enablePrecalculation;

    JiraReleaseReportSubType(String displayName,
                             boolean enablePrecalculation) {
        this.displayName = displayName;
        this.enablePrecalculation = enablePrecalculation;
    }
}
