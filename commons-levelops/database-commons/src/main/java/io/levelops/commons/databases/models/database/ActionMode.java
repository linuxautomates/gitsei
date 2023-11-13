package io.levelops.commons.databases.models.database;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.apache.commons.lang3.EnumUtils;

public enum ActionMode{
    AUTO,
    MANUAL;

    @JsonCreator
    @Nullable
    public static ActionMode fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ActionMode.class, value);
    }
    
    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}