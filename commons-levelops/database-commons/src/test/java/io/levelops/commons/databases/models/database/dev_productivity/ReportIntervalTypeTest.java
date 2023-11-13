package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReportIntervalTypeTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromString() {
        for(ReportIntervalType type : ReportIntervalType.values()) {
            Assert.assertEquals(type, ReportIntervalType.fromString(type.toString()));
        }
    }
    
    public static Map<Instant, Map<ReportIntervalType, ImmutablePair<Long, Long>>> getExpected() {
        Map<ReportIntervalType, ImmutablePair<Long, Long>> result1 = new EnumMap<ReportIntervalType, ImmutablePair<Long, Long>>(ReportIntervalType.class);
        result1.put(ReportIntervalType.LAST_WEEK, ImmutablePair.of(1640563200l, 1641167999l));
        result1.put(ReportIntervalType.LAST_TWO_WEEKS, ImmutablePair.of(1639958400l, 1641167999l));
        result1.put(ReportIntervalType.LAST_MONTH, ImmutablePair.of(1638316800l, 1640995199l));
        result1.put(ReportIntervalType.LAST_QUARTER, ImmutablePair.of(1633046400l, 1640995199l));
        result1.put(ReportIntervalType.LAST_TWO_QUARTERS, ImmutablePair.of(1625097600l, 1640995199l));
        result1.put(ReportIntervalType.LAST_TWELVE_MONTHS, ImmutablePair.of(1609459200l, 1640995199l));
        result1.put(ReportIntervalType.PAST_YEAR, ImmutablePair.of(1609459200l, 1640995199l));
        result1.put(ReportIntervalType.MONTH_JAN, ImmutablePair.of(1609459200l, 1612137599l));
        result1.put(ReportIntervalType.MONTH_FEB, ImmutablePair.of(1612137600l, 1614556799l));
        result1.put(ReportIntervalType.MONTH_MAR, ImmutablePair.of(1614556800l, 1617235199l));
        result1.put(ReportIntervalType.MONTH_APR, ImmutablePair.of(1617235200l, 1619827199l));
        result1.put(ReportIntervalType.MONTH_MAY, ImmutablePair.of(1619827200l, 1622505599l));
        result1.put(ReportIntervalType.MONTH_JUN, ImmutablePair.of(1622505600l, 1625097599l));
        result1.put(ReportIntervalType.MONTH_JUL, ImmutablePair.of(1625097600l, 1627775999l));
        result1.put(ReportIntervalType.MONTH_AUG, ImmutablePair.of(1627776000l, 1630454399l));
        result1.put(ReportIntervalType.MONTH_SEP, ImmutablePair.of(1630454400l, 1633046399l));
        result1.put(ReportIntervalType.MONTH_OCT, ImmutablePair.of(1633046400l, 1635724799l));
        result1.put(ReportIntervalType.MONTH_NOV, ImmutablePair.of(1635724800l, 1638316799l));
        result1.put(ReportIntervalType.MONTH_DEC, ImmutablePair.of(1638316800l, 1640995199l));
        result1.put(ReportIntervalType.PAST_QUARTER_ONE, ImmutablePair.of(1609459200l, 1617235199l));
        result1.put(ReportIntervalType.PAST_QUARTER_TWO, ImmutablePair.of(1617235200l, 1625097599l));
        result1.put(ReportIntervalType.PAST_QUARTER_THREE, ImmutablePair.of(1625097600l, 1633046399l));
        result1.put(ReportIntervalType.PAST_QUARTER_FOUR, ImmutablePair.of(1633046400l, 1640995199l));
        result1.put(ReportIntervalType.PAST_TWO_QUARTERS_ONE, ImmutablePair.of(1609459200l, 1625097599l));
        result1.put(ReportIntervalType.PAST_TWO_QUARTERS_TWO, ImmutablePair.of(1625097600l,1640995199l));
        result1.put(ReportIntervalType.LAST_TWO_MONTHS, ImmutablePair.of(1635724800l, 1640995199l));
        result1.put(ReportIntervalType.LAST_THREE_MONTHS, ImmutablePair.of(1633046400l, 1640995199l));


        Map<ReportIntervalType, ImmutablePair<Long, Long>> result2 = new EnumMap<ReportIntervalType, ImmutablePair<Long, Long>>(ReportIntervalType.class);
        result2.put(ReportIntervalType.LAST_WEEK, ImmutablePair.of(1639958400l, 1640563199l));
        result2.put(ReportIntervalType.LAST_TWO_WEEKS, ImmutablePair.of(1639353600l, 1640563199l));
        result2.put(ReportIntervalType.LAST_TWO_MONTHS, ImmutablePair.of(1633046400l, 1638316799l));
        result2.put(ReportIntervalType.LAST_THREE_MONTHS, ImmutablePair.of(1630454400l, 1638316799l));
        result2.put(ReportIntervalType.LAST_MONTH, ImmutablePair.of(1635724800l, 1638316799l));
        result2.put(ReportIntervalType.LAST_QUARTER, ImmutablePair.of(1625097600l, 1633046399l));
        result2.put(ReportIntervalType.LAST_TWO_QUARTERS, ImmutablePair.of(1617235200l, 1633046399l));
        result2.put(ReportIntervalType.LAST_TWELVE_MONTHS, ImmutablePair.of(1606780800l, 1638316799l));
        result2.put(ReportIntervalType.PAST_YEAR, ImmutablePair.of(1577836800l, 1609459199l));
        result2.put(ReportIntervalType.MONTH_JAN, ImmutablePair.of(1609459200l, 1612137599l));
        result2.put(ReportIntervalType.MONTH_FEB, ImmutablePair.of(1612137600l, 1614556799l));
        result2.put(ReportIntervalType.MONTH_MAR, ImmutablePair.of(1614556800l, 1617235199l));
        result2.put(ReportIntervalType.MONTH_APR, ImmutablePair.of(1617235200l, 1619827199l));
        result2.put(ReportIntervalType.MONTH_MAY, ImmutablePair.of(1619827200l, 1622505599l));
        result2.put(ReportIntervalType.MONTH_JUN, ImmutablePair.of(1622505600l, 1625097599l));
        result2.put(ReportIntervalType.MONTH_JUL, ImmutablePair.of(1625097600l, 1627775999l));
        result2.put(ReportIntervalType.MONTH_AUG, ImmutablePair.of(1627776000l, 1630454399l));
        result2.put(ReportIntervalType.MONTH_SEP, ImmutablePair.of(1630454400l, 1633046399l));
        result2.put(ReportIntervalType.MONTH_OCT, ImmutablePair.of(1633046400l, 1635724799l));
        result2.put(ReportIntervalType.MONTH_NOV, ImmutablePair.of(1635724800l, 1638316799l));
        result2.put(ReportIntervalType.MONTH_DEC, ImmutablePair.of(1606780800l, 1609459199l));
        result2.put(ReportIntervalType.PAST_QUARTER_ONE, ImmutablePair.of(1609459200l, 1617235199l));
        result2.put(ReportIntervalType.PAST_QUARTER_TWO, ImmutablePair.of(1617235200l, 1625097599l));
        result2.put(ReportIntervalType.PAST_QUARTER_THREE, ImmutablePair.of(1625097600l, 1633046399l));
        result2.put(ReportIntervalType.PAST_QUARTER_FOUR, ImmutablePair.of(1601510400l, 1609459199l));
        result2.put(ReportIntervalType.PAST_TWO_QUARTERS_ONE, ImmutablePair.of(1609459200l,1625097599l));
        result2.put(ReportIntervalType.PAST_TWO_QUARTERS_TWO, ImmutablePair.of(1593561600l, 1609459199l));

        Map<ReportIntervalType, ImmutablePair<Long, Long>> result3 = new EnumMap<ReportIntervalType, ImmutablePair<Long, Long>>(ReportIntervalType.class);
        result3.putAll(result1);
        result3.put(ReportIntervalType.LAST_WEEK, ImmutablePair.of(1639958400l, 1640563199l));
        result3.put(ReportIntervalType.LAST_TWO_WEEKS, ImmutablePair.of(1639353600l, 1640563199l));


        Map<Instant, Map<ReportIntervalType, ImmutablePair<Long, Long>>> allResults = Map.of(
                Instant.ofEpochSecond(1641413262l), result1,
                Instant.ofEpochSecond(1640994573l), result2,
                Instant.ofEpochSecond(1641005373l), result3
        );
        return allResults;
    }

    @Test
    public void testGetTimeRange() {
        //ImmutablePair<Long, Long> res = ReportIntervalType.PAST_QUARTER_FOUR.getTimeRange(Instant.ofEpochSecond(1641413262l));
        Map<Instant, Map<ReportIntervalType, ImmutablePair<Long, Long>>> allResults = getExpected();
        for(Instant i : allResults.keySet()) {
            Map<ReportIntervalType, ImmutablePair<Long, Long>> result = allResults.get(i);
            for(ReportIntervalType t : ReportIntervalType.values()) {
                var a = result.get(t);
                var b = t.getIntervalTimeRange(i).getTimeRange();
                Assert.assertEquals(i.getEpochSecond() + " " + t.toString(), result.get(t), t.getIntervalTimeRange(i).getTimeRange());
            }
        }
    }

    @Test
    public void testGetTimeRange2() {
        IntervalTimeRange i = ReportIntervalType.LAST_WEEK.getIntervalTimeRange(Instant.ofEpochSecond(1690233061l));
        Assert.assertEquals(i.getWeekOfTheYear(),new Integer(30));
        Assert.assertEquals(i.getYear(), new Integer(2023));
        Assert.assertEquals(i.getTimeRange(), ImmutablePair.of(1689552000l,1690156799l));
        i = ReportIntervalType.LAST_TWO_WEEKS.getIntervalTimeRange(Instant.ofEpochSecond(1690233061l));
        Assert.assertEquals(i.getWeekOfTheYear(),new Integer(30));
        Assert.assertEquals(i.getYear(), new Integer(2023));
        Assert.assertEquals(i.getTimeRange(), ImmutablePair.of(1688947200l,1690156799l));
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        for(ReportIntervalType type : ReportIntervalType.values()) {
            DummyClass expected = new DummyClass(type);
            DummyClass actual = MAPPER.readValue(MAPPER.writeValueAsString(expected), DummyClass.class);
            Assert.assertEquals(type, actual.getData());
        }
    }
    @Test
    public void testCalculationPriority() {
        for(ReportIntervalType type : ReportIntervalType.values()) {
            if(type.toString().startsWith("LAST_")) {
                Assert.assertEquals(1, type.getCalculationPriority());
            } else if(type.toString().startsWith("PAST_")) {
                Assert.assertEquals(2, type.getCalculationPriority());
            } else if(type.toString().startsWith("MONTH_")) {
                Assert.assertEquals(3, type.getCalculationPriority());
            } else {
                throw  new RuntimeException("Not Supported");
            }
        }
    }

    public static class DummyClass{
        private ReportIntervalType data;

        public DummyClass() {
        }

        public DummyClass(ReportIntervalType data) {
            this.data = data;
        }

        public ReportIntervalType getData() {
            return data;
        }
        public void setData(ReportIntervalType data) {
            this.data = data;
        }
    }

    @Test
    public void test() {
        Instant n = Instant.now();
        LocalDate f = LocalDate.ofInstant(n, ZoneOffset.UTC);
        System.out.println(n);
        System.out.println(f);

        System.out.println(n.getEpochSecond());
        System.out.println(f.atStartOfDay(ZoneOffset.UTC).toEpochSecond());
    }
}