package io.levelops.commons.databases.services.jira.models;

import lombok.Getter;

@Getter
public enum VelocityStageTimesReportSubType {
    VELOCITY_STAGE_TIME_REPORT("velocity_stage_time_report", true),
    VELOCITY_STAGE_TIME_REPORT_VALUES("velocity_stage_time_report_values", true);

    private final String displayName;
    private final boolean enablePrecalculation;

    VelocityStageTimesReportSubType(String displayName,
                             boolean enablePrecalculation) {
        this.displayName = displayName;
        this.enablePrecalculation = enablePrecalculation;
    }
}
