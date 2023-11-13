package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum TriggerType {
    UNKNOWN,
    MANUAL,
    SCHEDULED("clock-circle"),
    COMPONENT_EVENT;

    @Getter
    private final String icon;

    TriggerType() {
        this("levelops");
    }

    TriggerType(String icon) {
        this.icon = icon;
    }

    @Nullable
    @JsonCreator
    public static TriggerType fromString(final String type){
        return EnumUtils.getEnumIgnoreCase(TriggerType.class, type, UNKNOWN);
    }

    @Override
    @JsonValue
    public String toString(){
        return super.toString().toLowerCase();
    }
}