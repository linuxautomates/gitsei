package io.levelops.commons.tenant_management.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import java.util.concurrent.TimeUnit;

@Getter
public enum TaskType {
    TENANT_CONFIG_SYNC(TimeUnit.MINUTES.toSeconds(3l)),
    TENANT_INDEX_TYPE_CONFIG_SYNC(TimeUnit.MINUTES.toSeconds(3l)),
    TENANT_INDEX_SNAPSHOT_SYNC(TimeUnit.MINUTES.toSeconds(3l)),
    TENANT_INDEX_SNAPSHOT_SCHEDULE(TimeUnit.MINUTES.toSeconds(3l)),
    TENANT_INDEX_SNAPSHOT_DELETE(TimeUnit.HOURS.toSeconds(1l)),
    MONITOR_INTEGRATION_TRACKERS(TimeUnit.MINUTES.toSeconds(10l));

    private final Long taskIntervalInSecs;

    TaskType(Long taskIntervalInSecs) {
        this.taskIntervalInSecs = taskIntervalInSecs;
    }

    @JsonCreator
    public static TaskType fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(TaskType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

}
