package io.levelops.api.model.organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.EnumUtils;

public enum OrgIdType {
    ORG_UUID;

    @JsonCreator
    public static OrgIdType fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(OrgIdType.class, st);
    }
}
