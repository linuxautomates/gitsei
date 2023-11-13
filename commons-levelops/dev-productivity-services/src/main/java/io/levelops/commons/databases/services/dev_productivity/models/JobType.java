package io.levelops.commons.databases.services.dev_productivity.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

public enum JobType {
    USER_DEV_PRODUCTIVITY_REPORTS,
    ORG_DEV_PRODUCTIVITY_REPORTS,
    OU_ORG_USER_MAPPINGS;

    @JsonCreator
    public static JobType fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(JobType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
