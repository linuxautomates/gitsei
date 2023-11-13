package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.BooleanUtils;

import java.util.concurrent.TimeUnit;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FeatureResponse.FeatureResponseBuilder.class)
public class FeatureResponse {
    @JsonProperty("section_order")
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private final Integer sectionOrder;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("order")
    private final Integer order;

    @JsonProperty("integrations_absent")
    Boolean integrationsAbsent;

    @JsonProperty("median")
    Long median;
    @JsonProperty("min")
    Long min;
    @JsonProperty("max")
    Long max;
    @JsonProperty("count")
    Long count;
    @JsonProperty("sum")
    Long sum;
    @JsonProperty("mean")
    Double mean;

    @JsonProperty("feature_unit")
    String featureUnit;

    @JsonProperty("result")
    private final Long result; //counts or time in secs

    //Score out of 100 e.g. 35/100
    @JsonProperty("score")
    private final Integer score;

    //Rating
    @JsonProperty("rating")
    private final DevProductivityProfile.Rating rating;

    @JsonProperty("enabled")
    private Boolean enabled;

    /*
    More stuff will be added here later
     */

    private static FeatureResponse.FeatureResponseBuilder constructBuilder(final Integer sectionOrder, final DevProductivityProfile.Feature feature, Long valueCountOrInSeconds, Boolean integrationsAbsent, ReportIntervalType interval) {
        return constructBuilder(sectionOrder, feature, valueCountOrInSeconds, integrationsAbsent, null, null, interval);
    }

    private static FeatureResponse.FeatureResponseBuilder constructBuilder(final Integer sectionOrder, final DevProductivityProfile.Feature feature, Long valueCountOrInSeconds, Boolean integrationsAbsent, Long count, Double mean, ReportIntervalType interval) {
        FeatureResponseBuilder responseBuilder = FeatureResponse.builder()
                .sectionOrder(sectionOrder).order(feature.getOrder())
                .name(feature.getName())
                .description(feature.getDescription()).featureUnit(feature.getFeatureUnit())
                .result(valueCountOrInSeconds)
                .count(count)
                .mean(mean)
                .integrationsAbsent(integrationsAbsent)
                .enabled(feature.getEnabled());
        if(interval != null && feature.getFeatureType().isDependsOnTimeInterval()){
            //change the label based on feature type
            responseBuilder = responseBuilder.name(feature.getFeatureType().getDisplayTextForTimeInterval(interval));
        }
        if(BooleanUtils.isTrue(feature.getEnabled())){
            //extrapolate number based on feature type
            Long value = (interval != null && valueCountOrInSeconds != null && feature.getFeatureType().isDependsOnTimeInterval())
                    ? Long.valueOf(Math.round(valueCountOrInSeconds * interval.getMultiplicationFactor())) : valueCountOrInSeconds;
            responseBuilder = responseBuilder.score(feature.calculateScore(value))
                    .rating(feature.calculateRating(value));
        }
        return responseBuilder;
    }

    public static FeatureResponse.FeatureResponseBuilder constructBuilder(final Integer sectionOrder, final DevProductivityProfile.Feature feature, Long valueCountOrInSeconds, ReportIntervalType interval) {
        return constructBuilder(sectionOrder, feature, valueCountOrInSeconds, null, interval);
    }

    public static FeatureResponse.FeatureResponseBuilder constructBuilder(final Integer sectionOrder, final DevProductivityProfile.Feature feature, Long valueCountOrInSeconds) {
        return constructBuilder(sectionOrder, feature, valueCountOrInSeconds, null);
    }

    public static FeatureResponse.FeatureResponseBuilder constructBuilder(final Integer sectionOrder, final DevProductivityProfile.Feature feature, Long valueCountOrInSeconds, Long count, Double mean, ReportIntervalType interval) {
        return constructBuilder(sectionOrder, feature, valueCountOrInSeconds, null, count, mean, interval);
    }

    public static FeatureResponse.FeatureResponseBuilder constructIntegrationsAbsentBuilder(final Integer sectionOrder, final DevProductivityProfile.Feature feature) {
        return constructBuilder(sectionOrder, feature, feature.getFeatureType().getDefaultValue(), true, null);
    }
}
