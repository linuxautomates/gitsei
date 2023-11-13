package io.levelops.commons.databases.models.filters;

import org.apache.commons.lang3.EnumUtils;

public enum AGG_INTERVAL {
    day,
    day_of_week,
    biweekly,
    week,
    month,
    quarter,
    two_quarters,
    year;

    public static AGG_INTERVAL fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(AGG_INTERVAL.class, st);
    }
}