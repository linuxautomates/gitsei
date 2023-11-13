package io.levelops.integrations.github.models;

import org.apache.commons.lang3.EnumUtils;

public enum GithubEventType {
    GITHUB_ISSUE,
    GITHUB_PING,
    GITHUB_PR,
    GITHUB_PROJECT,
    GITHUB_PROJECT_CARD,
    GITHUB_PROJECT_COLUMN,
    GITHUB_PUSH,
    GITHUB_REPOSITORY,
    UNKNOWN_EVENT;

    public static GithubEventType fromString(final String eventType) {
        return EnumUtils.getEnumIgnoreCase(GithubEventType.class, eventType);
    }
}
