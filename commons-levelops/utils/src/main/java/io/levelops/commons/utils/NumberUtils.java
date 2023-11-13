package io.levelops.commons.utils;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class NumberUtils {

    public static Integer toInteger(@Nullable String value, @Nullable Integer defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    @Nullable
    public static Integer toInteger(@Nullable String value) {
        return toInteger(value, null);
    }

    public static Float toFloat(@Nullable String value, @Nullable Float defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }
}
