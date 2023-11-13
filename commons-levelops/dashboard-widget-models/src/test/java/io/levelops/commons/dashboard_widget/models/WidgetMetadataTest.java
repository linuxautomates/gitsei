package io.levelops.commons.dashboard_widget.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class WidgetMetadataTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerializeDeserialize() throws JsonProcessingException {
        String serialized = "{ \"order\": 1, \"width\": \"full\", \"hidden\": false, \"weights\": { \"IDLE\": 20, \"NO_ASSIGNEE\": 20, \"NO_DUE_DATE\": 30, \"NO_COMPONENTS\": 20, \"POOR_DESCRIPTION\": 8 }, \"children\": [], \"description\": \"Desc\", \"max_records\": 20, \"widget_type\": \"graph\", \"custom_hygienes\": [], \"filter_tab_order\": { \"customfield_10024\": 6, \"customfield_10040\": 4, \"customfield_10048\": 5, \"jira_issue_created_at\": 1, \"jira_issue_updated_at\": 2, \"jira_issue_resolved_at\": 3 }, \"dashBoard_time_keys\": { \"jira_issue_resolved_at\": { \"use_dashboard_time\": true } }, \"range_filter_choice\": { \"jira_issue_created_at\": { \"type\": \"relative\", \"relative\": { \"last\": { \"num\": \"90\", \"unit\": \"days\" }, \"next\": { \"unit\": \"today\" } } }, \"jira_issue_updated_at\": { \"type\": \"absolute\", \"relative\": { \"last\": { \"num\": 105, \"unit\": \"days\" }, \"next\": { \"num\": -14, \"unit\": \"days\" } } } } }";
        WidgetMetadata widgetMetadata = MAPPER.readValue(serialized, WidgetMetadata.class);
        Assert.assertNotNull(widgetMetadata);

        Assert.assertTrue(MapUtils.isNotEmpty(widgetMetadata.getDashBoardTimeKeys()));
        Assert.assertEquals(1, MapUtils.size(widgetMetadata.getDashBoardTimeKeys()));
        Assert.assertEquals(true, widgetMetadata.getDashBoardTimeKeys().get("jira_issue_resolved_at").getUseDashboardTime());

        Assert.assertTrue(MapUtils.isNotEmpty(widgetMetadata.getRangeFilterChoice()));
        Assert.assertEquals(2, MapUtils.size(widgetMetadata.getRangeFilterChoice()));
        Assert.assertEquals(true, widgetMetadata.getRangeFilterChoice().containsKey("jira_issue_created_at"));
        Assert.assertEquals(true, widgetMetadata.getRangeFilterChoice().containsKey("jira_issue_updated_at"));

        WidgetMetadata.RangeFilterChoice rfc1 = widgetMetadata.getRangeFilterChoice().get("jira_issue_created_at");
        Assert.assertEquals("relative", rfc1.getType());
        WidgetMetadata.Relative relative1 = rfc1.getRelative();
        Assert.assertEquals("90", relative1.getLast().getNum().toString());
        Assert.assertEquals("days", relative1.getLast().getUnit());
        Assert.assertNull(relative1.getNext().getNum());
        Assert.assertEquals("today", relative1.getNext().getUnit());

        WidgetMetadata.RangeFilterChoice rfc2 = widgetMetadata.getRangeFilterChoice().get("jira_issue_updated_at");
        Assert.assertEquals("absolute", rfc2.getType());
        WidgetMetadata.Relative relative2 = rfc2.getRelative();
        Assert.assertEquals("105", relative2.getLast().getNum().toString());
        Assert.assertEquals("days", relative2.getLast().getUnit());
        Assert.assertEquals("-14", relative2.getNext().getNum().toString());
        Assert.assertEquals("days", relative2.getNext().getUnit());
    }
    @Test
    public void testCalculateLastTimestamp() {
        Instant now = Instant.ofEpochSecond(1660520468l);
        Assert.assertEquals(1660435200l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(null).unit("today").build() ,now).longValue());
        Assert.assertEquals(1660262400l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("days").build() ,now).longValue());
        Assert.assertEquals(1659225600l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("weeks").build() ,now).longValue());
        Assert.assertEquals(1654041600l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("months").build() ,now).longValue());
        Assert.assertEquals(1640995200l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("quarters").build() ,now).longValue());
    }

    @Test
    public void testCalculateNextTimestamp() {
        Instant now = Instant.ofEpochSecond(1660520468l);
        Assert.assertEquals(1660780799l, WidgetMetadata.calculateNextTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("days").build() ,now).longValue());
    }

    @Test
    public void testCalculateLastTimestamp2() {
        Instant now = Instant.ofEpochSecond(1663206550l);
        Assert.assertEquals(1663200000l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(null).unit("today").build() ,now).longValue());
        Assert.assertEquals(1663027200l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("days").build() ,now).longValue());
        Assert.assertEquals(1661644800l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("weeks").build() ,now).longValue());
        Assert.assertEquals(1656633600l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("months").build() ,now).longValue());
        Assert.assertEquals(1648771200l, WidgetMetadata.calculateLastTimestamp(WidgetMetadata.RelativeDetail.builder().num(2).unit("quarters").build() ,now).longValue());
    }

    @Test
    public void testCalculateNextTimestamp2() {
        Instant now = Instant.ofEpochSecond(1663206550l);
        Assert.assertEquals(1663286399l, WidgetMetadata.calculateNextTimestamp(WidgetMetadata.RelativeDetail.builder().num(null).unit("today").build() ,now).longValue());
        Assert.assertEquals(1663545599l, WidgetMetadata.calculateNextTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("days").build() ,now).longValue());
        Assert.assertEquals(1665100799l, WidgetMetadata.calculateNextTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("weeks").build() ,now).longValue());
        Assert.assertEquals(1671148799l, WidgetMetadata.calculateNextTimestamp(WidgetMetadata.RelativeDetail.builder().num(3).unit("months").build() ,now).longValue());
        Assert.assertEquals(1678924799l, WidgetMetadata.calculateNextTimestamp(WidgetMetadata.RelativeDetail.builder().num(2).unit("quarters").build() ,now).longValue());
    }

    @RunWith(Parameterized.class)
    public static class TestTimeRangeFilter {
        ReportIntervalType intervalType;
        String lowerTimeRangeLimit;
        String upperTimeRangeLimit;
        final static Long PRECALCULATED_AT = 1691476960L;

        public TestTimeRangeFilter(ReportIntervalType intervalType, String lowerTimeRangeLimit, String upperTimeRangeLimit) {
            this.intervalType = intervalType;
            this.lowerTimeRangeLimit = lowerTimeRangeLimit;
            this.upperTimeRangeLimit = upperTimeRangeLimit;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            // PreCalculation on 1691476960 - Aug 8, 2023
            // Last 7 days 1690848000 - Aug 1, 2023 12:00:00 AM to 1691452799 - Aug 7, 2023 11:59:59 PM
            // Last 2 weeks 1690243200 - July 25, 2023 12:00:00 AM to 1691452799 - Aug 7, 2023 11:59:59 PM
            // Last 30 days 1688860800 - July 9, 2023 12:00:00 AM to 1691452799 - Aug 7, 2023 11:59:59 PM
            // Last month 1688169600- July 1, 2023 12:00:00 AM to 1690847999 - July 31, 2023 11:59:59 PM
            // Last 3 months 1682899200 - May 1, 2023 12:00:00 AM to 1690847999 - July 31, 2023 11:59:59 PM
            // Last quarter 1680307200 - April 1, 2023 12:00:00 AM to 1688169599- June 30, 2023 11:59:59 PM
            // Last 2 quarters 1672531200 - January 1, 2023 12:00:00 AM to 1688169599- June 30, 2023 11:59:59 PM
            return Arrays.asList(new Object[][] {
                    {ReportIntervalType.LAST_7_DAYS, "1690848000", "1691452799"},
                    {ReportIntervalType.LAST_2_WEEKS, "1690243200", "1691452799"},
                    {ReportIntervalType.LAST_30_DAYS, "1688860800", "1691452799"},
                    {ReportIntervalType.LAST_MONTH, "1688169600", "1690847999"},
                    {ReportIntervalType.LAST_3_MONTH, "1682899200", "1690847999"},
                    {ReportIntervalType.LAST_QUARTER, "1680307200", "1688169599"},
                    {ReportIntervalType.LAST_TWO_QUARTERS, "1672531200", "1688169599"}
            });
        }

        @Test
        public void testDashboardTimeFiltersForVelocityStageTimeReport() throws JsonProcessingException {
            WidgetMetadata.DashboardTimeKey dashboardTimeKey = WidgetMetadata.DashboardTimeKey.builder()
                    .useDashboardTime(true)
                    .build();
            WidgetMetadata widgetMetadata = WidgetMetadata.builder()
                    .dashBoardTimeKeys(Map.of("issue_resolved_at", dashboardTimeKey))
                    .build();
            Map<String, Object> timeRangeFilter =  WidgetMetadata.parseDashboardTimeFiltersForVelocityStageTimeReport(
                    widgetMetadata.getDashBoardTimeKeys(),
                    intervalType,
                    Instant.ofEpochSecond(PRECALCULATED_AT)
            );

            Assert.assertEquals(lowerTimeRangeLimit.toString(), ((Map)timeRangeFilter.get("issue_resolved_at")).get("$gt"));
            Assert.assertEquals(upperTimeRangeLimit.toString(), ((Map)timeRangeFilter.get("issue_resolved_at")).get("$lt"));
        }
    }
}