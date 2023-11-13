package io.levelops.commons.databases.services.dev_productivity.utils;

import com.google.common.math.DoubleMath;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;

import java.math.RoundingMode;

public class MeanUtils {

    private static final double DAYS_TO_SECONDS_DOUBLE = 86400d;

    public static Double convertMeanDaysToSecondsOrDefault(DevProductivityProfile.Feature feature, Double mean) {
        if (mean == null) {
            Long defaultValue = (feature != null && feature.getFeatureType() != null) ? feature.getFeatureType().getDefaultValue() : null;
            return defaultValue != null ? Double.valueOf(defaultValue) : null;
        }
        return DAYS_TO_SECONDS_DOUBLE * mean;
    }

    public static Long roundMeanDoubleUpToLong(Double meanSeconds) {
        if (meanSeconds == null) {
            return null;
        }
        return DoubleMath.roundToLong(meanSeconds, RoundingMode.UP);
    }

}
