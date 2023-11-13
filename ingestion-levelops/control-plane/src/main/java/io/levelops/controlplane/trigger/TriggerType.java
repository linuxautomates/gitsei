package io.levelops.controlplane.trigger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum TriggerType {
    TEST,
    JIRA;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    @JsonCreator
    @Nullable
    public static TriggerType fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(TriggerType.class, value);
    }
}
