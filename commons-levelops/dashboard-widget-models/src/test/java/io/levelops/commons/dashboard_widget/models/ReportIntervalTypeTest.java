package io.levelops.commons.dashboard_widget.models;

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
import java.util.Map;

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
        result1.put(ReportIntervalType.LAST_7_DAYS, ImmutablePair.of(1640822400l, 1641427199l));
        result1.put(ReportIntervalType.LAST_2_WEEKS, ImmutablePair.of(1640217600l, 1641427199l));
        result1.put(ReportIntervalType.LAST_30_DAYS, ImmutablePair.of(1638835200l, 1641427199l));
        result1.put(ReportIntervalType.LAST_MONTH, ImmutablePair.of(1638316800l, 1640995199l));
        result1.put(ReportIntervalType.LAST_3_MONTH, ImmutablePair.of(1633046400l, 1640995199l));
        result1.put(ReportIntervalType.LAST_QUARTER, ImmutablePair.of(1633046400l, 1640995199l));
        result1.put(ReportIntervalType.LAST_TWO_QUARTERS, ImmutablePair.of(1625097600l, 1640995199l));



        Map<ReportIntervalType, ImmutablePair<Long, Long>> result2 = new EnumMap<ReportIntervalType, ImmutablePair<Long, Long>>(ReportIntervalType.class);
        result2.put(ReportIntervalType.LAST_7_DAYS, ImmutablePair.of(1640476800l, 1641081599l));
        result2.put(ReportIntervalType.LAST_2_WEEKS, ImmutablePair.of(1639872000l, 1641081599l));
        result2.put(ReportIntervalType.LAST_30_DAYS, ImmutablePair.of(1638489600l, 1641081599l));
        result2.put(ReportIntervalType.LAST_MONTH, ImmutablePair.of(1638316800l, 1640995199l));
        result2.put(ReportIntervalType.LAST_3_MONTH, ImmutablePair.of(1633046400l, 1640995199l));
        result2.put(ReportIntervalType.LAST_QUARTER, ImmutablePair.of(1633046400l, 1640995199l));
        result2.put(ReportIntervalType.LAST_TWO_QUARTERS, ImmutablePair.of(1625097600l, 1640995199l));



        Map<ReportIntervalType, ImmutablePair<Long, Long>> result3 = new EnumMap<ReportIntervalType, ImmutablePair<Long, Long>>(ReportIntervalType.class);
        result3.put(ReportIntervalType.LAST_7_DAYS, ImmutablePair.of(1640390400l, 1640995199l));
        result3.put(ReportIntervalType.LAST_2_WEEKS, ImmutablePair.of(1639785600l, 1640995199l));
        result3.put(ReportIntervalType.LAST_30_DAYS, ImmutablePair.of(1638403200l, 1640995199l));
        result3.put(ReportIntervalType.LAST_MONTH, ImmutablePair.of(1635724800l, 1638316799l));
        result3.put(ReportIntervalType.LAST_3_MONTH, ImmutablePair.of(1630454400l, 1638316799l));
        result3.put(ReportIntervalType.LAST_QUARTER, ImmutablePair.of(1625097600l, 1633046399l));
        result3.put(ReportIntervalType.LAST_TWO_QUARTERS, ImmutablePair.of(1617235200l, 1633046399l));


        Map<Instant, Map<ReportIntervalType, ImmutablePair<Long, Long>>> allResults = Map.of(
                Instant.ofEpochSecond(1641413262l), result1,
                Instant.ofEpochSecond(1641005373l), result2,
                Instant.ofEpochSecond(1640994573l), result3
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
                var b = t.getTimeRange(i);
                Assert.assertEquals(i.getEpochSecond() + " " + t.toString(), result.get(t), t.getTimeRange(i));
            }
        }
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        for(ReportIntervalType type : ReportIntervalType.values()) {
            DummyClass expected = new DummyClass(type);
            DummyClass actual = MAPPER.readValue(MAPPER.writeValueAsString(expected), DummyClass.class);
            Assert.assertEquals(type, actual.getData());
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