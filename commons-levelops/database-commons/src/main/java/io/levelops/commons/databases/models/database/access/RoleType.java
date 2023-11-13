package io.levelops.commons.databases.models.database.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum RoleType {
    DEMO,
    ADMIN,
    AUDITOR,
    TRIGGERS,
    INGESTION,
    LIMITED_USER,
    RESTRICTED_USER,
    ASSIGNED_ISSUES_USER,
    PUBLIC_DASHBOARD,
    SUPER_ADMIN,
    TENANT_ADMIN,
    ORG_ADMIN_USER;

    @JsonCreator
    @Nullable
    public static RoleType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(RoleType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
