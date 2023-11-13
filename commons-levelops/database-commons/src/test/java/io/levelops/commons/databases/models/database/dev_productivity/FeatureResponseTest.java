package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class FeatureResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        FeatureResponse expected = FeatureResponse.builder()
                .sectionOrder(0)
                .name("Bugs Per 100 LOC")
                .description("Bugs Per 100 LOC")
                .featureUnit("Bugs")
                .order(2)
                .mean(77.00)
                .score(72)
                .rating(DevProductivityProfile.Rating.GOOD)
                .build();

        String serialized = MAPPER.writeValueAsString(expected);
        FeatureResponse actual = MAPPER.readValue(serialized, FeatureResponse.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testConstructBuilder() {
        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .name("Percentage of Legacy Rework").description("Percentage of Legacy Rework")
                .order(0)
                .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                .enabled(true)
                .maxValue(100L)
                .lowerLimitPercentage(25).upperLimitPercentage(75)
                .build();
        FeatureResponse featureResponse = FeatureResponse.constructBuilder(0, feature, 10L).build();
        Assert.assertNotNull(featureResponse);
        Assert.assertNotNull(featureResponse.getScore());
        Assert.assertNotNull(featureResponse.getRating());

        feature = feature.toBuilder().enabled(false).build();
        featureResponse = FeatureResponse.constructBuilder(0, feature, 10L).build();
        Assert.assertNotNull(featureResponse);
        Assert.assertNull(featureResponse.getScore());
        Assert.assertNull(featureResponse.getRating());

    }

    @Test
    public void testConstructIntegrationsAbsentBuilder() {
        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .name("Percentage of Legacy Rework").description("Percentage of Legacy Rework")
                .order(0)
                .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                .maxValue(100L)
                .lowerLimitPercentage(25).upperLimitPercentage(75)
                .build();
        FeatureResponse featureResponse = FeatureResponse.constructIntegrationsAbsentBuilder(0, feature).build();
        Assert.assertNotNull(featureResponse);
    }
}