package io.levelops.commons.licensing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.Nullable;

public enum LicenseType {
    LIMITED_TRIAL_LICENSE("limited_trial"),
    FULL_LICENSE("full"),
    UNKNOWN("UNKNOWN");

    private final String licenseName;

    LicenseType(String licenseName) {
        this.licenseName = licenseName;
    }

    @JsonValue
    @Override
    public String toString() {
        return licenseName;
    }

    @JsonCreator
    @Nullable
    public static LicenseType fromString(@Nullable String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case "limited_trial":
                return LIMITED_TRIAL_LICENSE;
            case "full":
                return FULL_LICENSE;
            default:
                return UNKNOWN;
        }
    }
}
