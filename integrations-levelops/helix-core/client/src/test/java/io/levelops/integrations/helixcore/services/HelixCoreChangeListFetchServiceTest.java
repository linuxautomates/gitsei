package io.levelops.integrations.helixcore.services;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HelixCoreChangeListFetchServiceTest {

    @Test
    public void testGetIntervals() {
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");

        Instant from = Instant.ofEpochMilli(1646267100000l);
        Instant to = Instant.ofEpochMilli(1646270700000l);
        List<ImmutablePair<LocalDate, LocalDate>> intervals = HelixCoreChangeListFetchService.getIntervals(from, to, zoneId, 1);
        Assert.assertNotNull(intervals);
        Assert.assertEquals(1, intervals.size());

        from = Instant.ofEpochMilli(1646292600000l);
        to = Instant.ofEpochMilli(1646296200000l);
        intervals = HelixCoreChangeListFetchService.getIntervals(from, to, zoneId, 1);
        Assert.assertNotNull(intervals);
        Assert.assertEquals(2, intervals.size());

        to = Instant.ofEpochSecond(1646949720l);
        from = to.minus(90, ChronoUnit.DAYS);
        intervals = HelixCoreChangeListFetchService.getIntervals(from, to, zoneId, 1);
        Assert.assertNotNull(intervals);
        Assert.assertEquals(91, intervals.size());
    }
}