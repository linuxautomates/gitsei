package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.ActivityLog;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ActivityLogsControllerTest {

    @Test
    public void sanitizeActivityLog() {
        ActivityLog output = ActivityLogsController.sanitizeActivityLog(ActivityLog.builder()
                .targetItemType(ActivityLog.TargetItemType.DASHBOARD)
                .action(ActivityLog.Action.EDITED)
                .details(Map.of("some", "details"))
                .build());
        assertThat(output).isNotNull();
        assertThat(output.getDetails()).isEqualTo(Map.of("some", "details"));


        output = ActivityLogsController.sanitizeActivityLog(ActivityLog.builder()
                .targetItemType(ActivityLog.TargetItemType.USER)
                .action(ActivityLog.Action.EDITED)
                .details(Map.of("some", "details"))
                .build());
        assertThat(output).isNotNull();
        assertThat(output.getDetails()).isEmpty(); // DO NOT TOUCH THIS TEST WITHOUT READING SEI-2608
    }

}