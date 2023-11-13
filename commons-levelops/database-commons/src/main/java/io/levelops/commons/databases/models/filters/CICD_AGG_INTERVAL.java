package io.levelops.commons.databases.models.filters;

import org.apache.commons.lang3.EnumUtils;

public enum CICD_AGG_INTERVAL {
    day,
    biweekly,
    week,
    month,
    quarter,
    year;

    public static CICD_AGG_INTERVAL fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(CICD_AGG_INTERVAL.class, st);
    }
}