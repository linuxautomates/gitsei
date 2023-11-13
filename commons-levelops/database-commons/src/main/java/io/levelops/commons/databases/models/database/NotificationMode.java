package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum NotificationMode {
    SLACK,
    EMAIL;

    @JsonCreator
    @Nullable
    public static NotificationMode fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(NotificationMode.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
