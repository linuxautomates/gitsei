package io.harness.authz.acl.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum ResourceType {
    ACCOUNT,
    ORGANIZATION,
    PROJECT,
    SEI_CONFIGURATION_SETTINGS,
    SEI_COLLECTIONS,
    SEI_INSIGHTS,
    SEI_COLLECTIONS_INSIGHTS;

    @JsonCreator
    @Nullable
    public static ResourceType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ResourceType.class, value);
    }
}
