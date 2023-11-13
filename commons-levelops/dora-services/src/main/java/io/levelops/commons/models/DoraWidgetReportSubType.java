package io.levelops.commons.models;

import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import lombok.Getter;

import java.util.List;

@Getter
public enum DoraWidgetReportSubType {
    LEAD_TIME_CALCULATION("lead_time_calculation", true, VelocityFilter.DISTINCT.velocity, null),
    LEAD_TIME_VALUES_CALCULATION_RATING_MISSING("lead_time_values_calculation_rating_missing", true, VelocityFilter.DISTINCT.values, null),
    LEAD_TIME_VALUES_CALCULATION_RATING_NON_MISSING("lead_time_values_calculation_rating_non_missing", true, VelocityFilter.DISTINCT.values, List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.NEEDS_ATTENTION, VelocityConfigDTO.Rating.SLOW)),

    MEAN_TIME_CALCULATION("mean_time_calculation", true, VelocityFilter.DISTINCT.velocity, null),
    MEAN_TIME_VALUES_CALCULATION_RATING_MISSING("mean_time_calculation_values_calculation_rating_missing", true, VelocityFilter.DISTINCT.values, null),
    MEAN_TIME_VALUES_CALCULATION_RATING_NON_MISSING("mean_time_calculation_values_calculation_rating_non_missing", true, VelocityFilter.DISTINCT.values, List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.NEEDS_ATTENTION, VelocityConfigDTO.Rating.SLOW));

    private final String displayName;
    private final boolean enablePrecalculation;
    private final VelocityFilter.DISTINCT across;
    private final List<VelocityConfigDTO.Rating> defaultRatings;

    DoraWidgetReportSubType(String displayName,
                            boolean enablePrecalculation,
                            VelocityFilter.DISTINCT across,
                            List<VelocityConfigDTO.Rating> defaultRatings) {
        this.displayName = displayName;
        this.enablePrecalculation = enablePrecalculation;
        this.across = across;
        this.defaultRatings = defaultRatings;
    }
}
