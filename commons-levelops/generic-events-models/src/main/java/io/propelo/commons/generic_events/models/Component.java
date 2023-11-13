package io.propelo.commons.generic_events.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum Component {
    JIRA,
    SCM_PR,
    SCM_COMMIT,
    CICD_JOB_RUN,
    WORK_ITEM;

    @Nullable
    @JsonCreator
    public static Component fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(Component.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
