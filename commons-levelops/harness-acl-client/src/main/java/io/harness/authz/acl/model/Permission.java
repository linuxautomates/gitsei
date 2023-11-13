package io.harness.authz.acl.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public enum Permission {
    ACCOUNT_CONFIG_CREATE("sei_seiconfigurationsettings_create"),
    ACCOUNT_CONFIG_VIEW("sei_seiconfigurationsettings_view"),
    ACCOUNT_CONFIG_EDIT("sei_seiconfigurationsettings_edit"),
    ACCOUNT_CONFIG_DELETE("sei_seiconfigurationsettings_delete"),

    COLLECTIONS_CREATE("sei_seicollections_create"),
    COLLECTIONS_VIEW("sei_seicollections_view"),
    COLLECTIONS_EDIT("sei_seicollections_edit"),
    COLLECTIONS_DELETE("sei_seicollections_delete"),

    INSIGHTS_CREATE("sei_seiinsights_create"),
    INSIGHTS_VIEW("sei_seiinsights_view"),
    INSIGHTS_EDIT("sei_seiinsights_edit"),
    INSIGHTS_DELETE("sei_seiinsights_delete"),
    COLLECTION_INSIGHT_VIEW("sei_seicollections_view,sei_seiinsights_view");

    private final String permission;
    Permission(String permission) {
        this.permission = permission;
    }

    @JsonCreator
    @Nullable
    public static Permission fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(Permission.class, value);
    }

    public static List<Permission> getPermissionListForResource(ResourceType resourceType){

        switch (resourceType){
            case SEI_COLLECTIONS:
                return List.of(COLLECTIONS_CREATE, COLLECTIONS_EDIT, COLLECTIONS_VIEW, COLLECTIONS_DELETE);
            case SEI_INSIGHTS:
                return List.of(INSIGHTS_CREATE, INSIGHTS_EDIT, INSIGHTS_VIEW, INSIGHTS_DELETE);
            case SEI_CONFIGURATION_SETTINGS:
                return List.of(ACCOUNT_CONFIG_CREATE, ACCOUNT_CONFIG_EDIT, ACCOUNT_CONFIG_EDIT, ACCOUNT_CONFIG_DELETE);
            default:
                return List.of();
        }

    }
}
