package io.levelops.commons.services.velocity_productivity.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class VelocityWidgetReportSubTypeTest {
    @Test
    public void testToString() {
        Set<String> expected = Set.of("velocity", "values_rating_non_missing", "values_rating_missing", "histogram", "histograms", "rating");
        Set<String> actual = Arrays.asList(VelocityWidgetReportSubType.values()).stream().map(VelocityWidgetReportSubType::toString).collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testEnablePrecalculate() {
        Set<String> expected = Set.of("velocity", "values_rating_non_missing", "values_rating_missing");
        Set<String> actual = Arrays.asList(VelocityWidgetReportSubType.values()).stream().filter(VelocityWidgetReportSubType::isEnablePrecalculation).map(VelocityWidgetReportSubType::toString).collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }
}