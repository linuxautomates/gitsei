package io.levelops.commons.databases.models.organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum OrgUserImportModeType {
    REPLACE,
    UPDATE;
    
    @JsonCreator
    @Nullable
    public static OrgUserImportModeType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(OrgUserImportModeType.class, value);
    }
    
    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
