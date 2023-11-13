package io.levelops.commons.databases.models.database.dev_productivity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Constants {
    public static final Integer HIGHEST = 0;
    public static final Integer HIGH = 1;
    public static final Integer MEDIUM = 2;
    public static final Integer LOW = 3;
    public static final Integer LOWEST = 4;

    public static final Set<DevProductivityProfile.FeatureType> ALL_RELEASED_FEATURE_TYPES = Arrays.asList(DevProductivityProfile.FeatureType.values()).stream()
            .filter(f -> f.isReleased())
            .collect(Collectors.toSet());
}
