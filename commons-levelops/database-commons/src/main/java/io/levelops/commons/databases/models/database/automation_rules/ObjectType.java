package io.levelops.commons.databases.models.database.automation_rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum ObjectType {
    JIRA_ISSUE("Jira Issue"),
    SCM_PULL_REQUEST("Scm Pull Request",true),
    GIT_PULL_REQUEST("Github Pull Request");

    private final String name;
    private final Boolean disabled;

    ObjectType(String name) {
        this(name, false);
    }

    ObjectType(String name, Boolean disabled) {
        this.name = name;
        this.disabled = disabled;
    }

    @JsonCreator
    @Nullable
    public static ObjectType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ObjectType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
