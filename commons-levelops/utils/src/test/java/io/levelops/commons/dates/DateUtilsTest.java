package io.levelops.commons.dates;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DateUtilsTest {

    @Test
    public void epoch() {
        assertThat(DateUtils.fromEpochSecond(1573238245L)).isEqualTo("2019-11-08T18:37:25Z");
        assertThat(DateUtils.toEpochSecond(Instant.parse("2019-11-08T18:37:25Z"))).isEqualTo(1573238245L);
        assertThat(DateUtils.fromEpochSecondToDate(1573238245L)).isEqualTo("2019-11-08T18:37:25Z");
        assertThat(DateUtils.toEpochSecond(Date.from(Instant.parse("2019-11-08T18:37:25Z")))).isEqualTo(1573238245L);
    }

    @Test
    public void isBetween() {
        Instant a = Instant.ofEpochSecond(10);
        Instant b = Instant.ofEpochSecond(20);
        Instant c = Instant.ofEpochSecond(30);
        assertThat(DateUtils.isBetween(a, null, false, null, false)).isTrue();
        assertThat(DateUtils.isBetween(b, a, false, c, false)).isTrue();
        assertThat(DateUtils.isBetween(a, b, false, c, false)).isFalse();
        assertThat(DateUtils.isBetween(c, a, false, b, false)).isFalse();


        assertThat(DateUtils.isBetween(a, a, false, b, false)).isFalse();
        assertThat(DateUtils.isBetween(b, a, false, b, false)).isFalse();

        assertThat(DateUtils.isBetween(a, a, true, b, false)).isTrue();
        assertThat(DateUtils.isBetween(b, a, true, b, true)).isTrue();

        assertThat(DateUtils.isBetween(a, a, false, null, false)).isFalse();
        assertThat(DateUtils.isBetween(a, null, false, a, false)).isFalse();
        assertThat(DateUtils.isBetween(a, a, true, null, false)).isTrue();
        assertThat(DateUtils.isBetween(a, null, false, a, true)).isTrue();
    }

    @Test
    public void isLongerThan() {
        Instant a = Instant.ofEpochSecond(10);
        Instant b = Instant.ofEpochSecond(20);
        assertThat(DateUtils.isLongerThan(a, b, 1, ChronoUnit.SECONDS)).isTrue();
        assertThat(DateUtils.isLongerThan(a, b, 1000, ChronoUnit.SECONDS)).isFalse();
        assertThat(DateUtils.isLongerThan(a, null, 1000, ChronoUnit.SECONDS)).isTrue();
        assertThat(DateUtils.isLongerThan(null, a, 1000, ChronoUnit.SECONDS)).isTrue();
        assertThat(DateUtils.isLongerThan(a, b, -1, ChronoUnit.SECONDS)).isTrue();
        assertThat(DateUtils.isLongerThan(b, a, 1, ChronoUnit.SECONDS)).isTrue();
    }

    @Test
    public void testParseDateTime() {
        Instant output = DateUtils.parseDateTime("2020-08-24T00:31:14.497+0000");
        assertThat(output).isNotNull();
        assertThat(output.getEpochSecond()).isEqualTo(1598229074L);
        assertThat(output.getNano()).isEqualTo(497000000L);

        output = DateUtils.parseDateTime("2020-08-24T00:31:14.497-0000");
        assertThat(output).isNotNull();
        assertThat(output.getEpochSecond()).isEqualTo(1598229074L);
        assertThat(output.getNano()).isEqualTo(497000000L);

        output = DateUtils.parseDateTime("2020-08-12T15:29:37.2875904Z");
        assertThat(output).isNotNull();
        assertThat(output.getEpochSecond()).isEqualTo(1597246177L);
        assertThat(output.getNano()).isEqualTo(287590400L);

        output = DateUtils.parseDateTime("2020-08-12T15:29:37.28Z");
        assertThat(output).isNotNull();
        assertThat(output.getEpochSecond()).isEqualTo(1597246177L);
        assertThat(output.getNano()).isEqualTo(280000000L);
    }

    @Test
    public void testParseDateTimeToDate() {
        String input = "2020-08-24T00:31:14.497+0000";
        assertThat(DateUtils.toInstant(DateUtils.parseDateTimeToDate(input))).isEqualTo(DateUtils.parseDateTime(input));
    }

    @Test
    public void latest() {
        Instant a = Instant.ofEpochSecond(10);
        Instant b = Instant.ofEpochSecond(20);
        assertThat(DateUtils.latest(a, b)).isEqualTo(b);
        assertThat(DateUtils.latest(b, a)).isEqualTo(b);
        assertThat(DateUtils.latest(null, a)).isEqualTo(a);
        assertThat(DateUtils.latest(a, null)).isEqualTo(a);
        assertThat(DateUtils.latest(a, a)).isEqualTo(a);
        assertThat(DateUtils.latest(null, null)).isNull();
    }

    @Test
    public void earliest() {
        Instant a = Instant.ofEpochSecond(10);
        Instant b = Instant.ofEpochSecond(20);
        assertThat(DateUtils.earliest(a, b)).isEqualTo(a);
        assertThat(DateUtils.earliest(b, a)).isEqualTo(a);
        assertThat(DateUtils.earliest(null, a)).isEqualTo(a);
        assertThat(DateUtils.earliest(a, null)).isEqualTo(a);
        assertThat(DateUtils.earliest(a, a)).isEqualTo(a);
        assertThat(DateUtils.earliest(null, null)).isNull();
    }

    @Test
    public void toEpochSeconds() {
        assertThat(DateUtils.toEpochSecond(1616085070123L)).isEqualTo(1616085070L);
        assertThat(DateUtils.toEpochSecond(1616085070L)).isEqualTo(1616085070L);
    }

    @Test
    public void getWeeklyPartition() {
        List<ImmutablePair<Long, Long>> partition = DateUtils.getWeeklyPartition(
                DateUtils.fromEpochSecond(1619086400L),
                DateUtils.fromEpochSecond(1620086400L));
        assertThat(partition).containsExactly(
                ImmutablePair.of(1618827200L, 1619432000L),
                ImmutablePair.of(1619432000L, 1620036800L),
                ImmutablePair.of(1620036800L, 1620641600L)
        );
    }

    @Test
    public void getMonthlyPartition() {
        List<ImmutablePair<Long, Long>> partition = DateUtils.getMonthlyPartition(
                DateUtils.fromEpochSecond(1615086400L),
                DateUtils.fromEpochSecond(1620514805L));
        assertThat(partition).containsExactly(
                ImmutablePair.of(1614568000L, 1617246400L),
                ImmutablePair.of(1617246400L, 1619838400L),
                ImmutablePair.of(1619838400L, 1622516800L)
        );
    }

    @Test
    public void testStartAndEndOfDay() {
        Instant d = Instant.ofEpochSecond(1624488477);
        Assert.assertEquals(Instant.ofEpochSecond(1624406400), DateUtils.toStartOfDay(d));
        Assert.assertEquals(Instant.ofEpochSecond(1624492799).getEpochSecond(), DateUtils.toEndOfDay(d).getEpochSecond());
    }

    @Test
    public void testMonthlyPartition2() {
        Instant from = Instant.ofEpochSecond(1629347800L);
        Instant to = Instant.ofEpochSecond(1631774300L);
        List<ImmutablePair<Long, Long>> result = DateUtils.getMonthlyPartition(from, to);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testFromEpochSecondsToTimestamp() {
        assertThat(DateUtils.fromEpochSecondToTimestamp(1629347800L).getTime()).isEqualTo(1629347800000L);
        assertThat(DateUtils.fromEpochSecondToTimestamp(null)).isNull();
    }

    @Test
    public void testToTimestamp() {
        assertThat(DateUtils.toTimestamp(Date.from(Instant.ofEpochSecond(1629347800L))).getTime()).isEqualTo(1629347800000L);
        assertThat(DateUtils.toTimestamp((Date) null)).isNull();
    }

    @Test
    public void testToString() {
        assertThat(DateUtils.toString(Instant.ofEpochSecond(1677567711))).isEqualTo("2023-02-28T07:01:51Z");
        assertThat(DateUtils.toString(DateUtils.toDate(Instant.ofEpochSecond(1677567711)))).isEqualTo("2023-02-28T07:01:51Z");
    }

    @Test
    public void testToEpochSecondLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(1678151462, 0, ZoneOffset.UTC);
        assertThat(DateUtils.toEpochSecond(localDateTime, ZoneId.ofOffset("", ZoneOffset.UTC))).isEqualTo(1678151462);
    }

    @Test
    public void truncate() {
        assertThat(DateUtils.truncate(1678151462L, Calendar.DATE)).isEqualTo(1678089600L);
        assertThat(DateUtils.truncate(DateUtils.fromEpochSecond(1678151462L), Calendar.DATE)).isEqualTo(1678089600L);
    }

    @Test
    public void testGetWeeklyFormat() {
        Map<Long, String> dataSet = new HashMap<>();
        dataSet.put(1672531200l, "01-2023");
        dataSet.put(1682294400l, "17-2023");
        dataSet.put(1684108800l, "20-2023");
        dataSet.put(1684713600l, "21-2023");
        dataSet.put(1685318400l, "22-2023");
        dataSet.put(1685923200l, "23-2023");
        dataSet.put(1686528000l, "24-2023");
        dataSet.put(1687132800l, "25-2023");
        dataSet.put(1687737600l, "26-2023");
        dataSet.put(1704067200l, "01-2024");
        dataSet.put(1740787200l, "09-2025");

        for(Map.Entry<Long,String> entry : dataSet.entrySet()) {
            Assert.assertEquals(DateUtils.getWeeklyFormat(entry.getKey()), entry.getValue());
        }
    }
}