package io.levelops.commons.services.velocity_productivity.models;

import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Getter
public enum VelocityWidgetReportSubType {
    VELOCITY("velocity", true, VelocityFilter.DISTINCT.velocity, null),
    VALUES_RATING_NON_MISSING("values_rating_non_missing", true, VelocityFilter.DISTINCT.values, List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.NEEDS_ATTENTION, VelocityConfigDTO.Rating.SLOW)),
    VALUES_RATING_MISSING("values_rating_missing", true, VelocityFilter.DISTINCT.values, List.of(VelocityConfigDTO.Rating.MISSING)),
    HISTOGRAM("histogram", false, VelocityFilter.DISTINCT.histogram, null),
    HISTOGRAMS("histograms", false, VelocityFilter.DISTINCT.histogram, null),
    RATING("rating", false, VelocityFilter.DISTINCT.rating, null);

    private final String displayName;
    private final boolean enablePrecalculation;
    private final VelocityFilter.DISTINCT across;
    private final List<VelocityConfigDTO.Rating> defaultRatings;

    VelocityWidgetReportSubType(String displayName, boolean enablePrecalculation, VelocityFilter.DISTINCT across, List<VelocityConfigDTO.Rating> defaultRatings) {
        this.displayName = displayName;
        this.enablePrecalculation = enablePrecalculation;
        this.across = across;
        this.defaultRatings = defaultRatings;
    }

    public static VelocityWidgetReportSubType fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(VelocityWidgetReportSubType.class, st);
    }

    @Override
    public String toString() {
        return this.getDisplayName();
    }
}
