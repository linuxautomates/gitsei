package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

/**
 * Used in conjunction with Content Type to identify what a field contains.
 * See KvField
 */
public enum ValueType {
    NONE, // for variables with no FE visible value
    STRING, // right now field data is always stored as string (even for integers, boolean, etc.)
    JSON_BLOB,
    JSON_ARRAY;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    @JsonCreator
    @Nullable
    public static ValueType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ValueType.class, value);
    }
}