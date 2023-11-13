package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum JiraDeploymentType {
    CLOUD,
    SERVER;

    @JsonCreator
    @Nullable
    public static JiraDeploymentType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(JiraDeploymentType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}