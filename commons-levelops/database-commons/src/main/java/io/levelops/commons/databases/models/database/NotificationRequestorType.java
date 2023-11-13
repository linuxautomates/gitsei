package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum NotificationRequestorType {
    RUNBOOK,
    SLACK_USER,
    USER;

    @JsonCreator
    @Nullable
    public static NotificationRequestorType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(NotificationRequestorType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
