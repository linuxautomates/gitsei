package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.EnumUtils;

public enum IdType {
    INTEGRATION_USER_IDS,
    OU_USER_IDS,
    ORG_IDS;

    @JsonCreator
    public static IdType fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(IdType.class, st);
    }

}
