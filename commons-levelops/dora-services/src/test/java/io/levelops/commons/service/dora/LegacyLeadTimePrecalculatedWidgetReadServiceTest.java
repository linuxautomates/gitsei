package io.levelops.commons.service.dora;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.VelocityStageResult;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DoraWidgetReportSubType;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsValuesResultFilterSortService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

public class LegacyLeadTimePrecalculatedWidgetReadServiceTest {
    private static final String company = "test";
    private static final boolean DISABLE_PRE_CALCULATION_RESULT = false;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    private static DashboardWidgetService dashboardWidgetService;
    private static WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;
    private static VelocityAggsValuesResultFilterSortService velocityAggsValuesResultFilterSortService;

    private static LegacyLeadTimePrecalculatedWidgetReadService legacyLeadTimePrecalculatedWidgetReadService;

    @BeforeClass
    public static void setUp() {
        dashboardWidgetService = Mockito.mock(DashboardWidgetService.class);
        widgetPrecalculatedResultsDBService = Mockito.mock(WidgetPrecalculatedResultsDBService.class);
        velocityAggsValuesResultFilterSortService = Mockito.mock(VelocityAggsValuesResultFilterSortService.class);

        legacyLeadTimePrecalculatedWidgetReadService = new LegacyLeadTimePrecalculatedWidgetReadService(
                mapper, dashboardWidgetService, widgetPrecalculatedResultsDBService, velocityAggsValuesResultFilterSortService
        );
    }

    @Test
    public void testGetNewVelocityAggsForLeadTime() throws JsonProcessingException, SQLException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .widgetId(widgetId)
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        List<DbAggregationResult> leadTimeResponse = List.of(
                DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
        );
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(leadTimeResponse)
                .reportSubType(DoraWidgetReportSubType.LEAD_TIME_CALCULATION.getDisplayName())
                .listRequest(originalRequest)
                .build();
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );
        doReturn(dbListResponse).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(DoraWidgetReportSubType.LEAD_TIME_CALCULATION.getDisplayName())), any(), any()
        );

        // execute
        Optional<List<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForLeadTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Optional<List<DbAggregationResult>> expectedResult = Optional.of(
                mapper.readValue(dbListResponse.getRecords().get(0).getReport().toString(),
                        mapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class))
        );
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testGetNewVelocityAggsForLeadTimeNoPreCalcStored() throws JsonProcessingException, SQLException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .widgetId(widgetId)
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        doReturn(null).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(DoraWidgetReportSubType.LEAD_TIME_CALCULATION.getDisplayName())), any(), any()
        );

        // execute
        Optional<List<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForLeadTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Assert.assertEquals(Optional.empty(), result);
    }

    @Test
    public void testGetNewVelocityAggsForLeadTimeWidgetIdInFilter() throws JsonProcessingException, SQLException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(Map.of("widget_id", widgetId))
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        List<DbAggregationResult> leadTimeResponse = List.of(
                DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
        );
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(leadTimeResponse)
                .reportSubType(DoraWidgetReportSubType.LEAD_TIME_CALCULATION.getDisplayName())
                .listRequest(originalRequest)
                .build();
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );
        doReturn(dbListResponse).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(DoraWidgetReportSubType.LEAD_TIME_CALCULATION.getDisplayName())), any(), any()
        );

        // execute
        Optional<List<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForLeadTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Optional<List<DbAggregationResult>> expectedResult = Optional.of(
                mapper.readValue(dbListResponse.getRecords().get(0).getReport().toString(),
                        mapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class))
        );
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testGetNewVelocityAggsForMeanTime() throws JsonProcessingException, SQLException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .widgetId(widgetId)
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        List<DbAggregationResult> leadTimeResponse = List.of(
                DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
        );
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(leadTimeResponse)
                .reportSubType(DoraWidgetReportSubType.MEAN_TIME_CALCULATION.getDisplayName())
                .listRequest(originalRequest)
                .build();
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );
        doReturn(dbListResponse).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(DoraWidgetReportSubType.MEAN_TIME_CALCULATION.getDisplayName())), any(), any()
        );

        // execute
        Optional<List<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForMeanTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Optional<List<DbAggregationResult>> expectedResult = Optional.of(
                mapper.readValue(dbListResponse.getRecords().get(0).getReport().toString(),
                        mapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class))
        );
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testGetNewVelocityAggsForMeanTimeNoWidgetId() throws JsonProcessingException {
        // configure
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .ouIds(Set.of(10))
                .build();

        // execute
        Optional<List<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForMeanTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Assert.assertEquals(Optional.empty(), result);
    }

    @Test
    public void testGetVelocityValuesForLeadTime() throws JsonProcessingException, SQLException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .widgetId(widgetId)
                .filter(
                        Map.of(
                                "value_jira_issue_types", List.of("l1"),
                                "calculation", VelocityFilter.CALCULATION.pr_velocity.toString(),
                                "ratings", List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.NEEDS_ATTENTION, VelocityConfigDTO.Rating.SLOW)
                        )
                )
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        List<DbAggregationResult> leadTimeResponse = List.of(
                        DbAggregationResult.builder()
                                .key("some-stage")
                                .data(List.of(DbAggregationResult.builder()
                                                .count(10L)
                                                .p90(80L)
                                                .p95(84L)
                                                .mean(5.0)
                                                .median(2L)
                                        .velocityStageResult(VelocityStageResult.builder()
                                                .lowerLimitValue(4L)
                                                .lowerLimitUnit(TimeUnit.DAYS)
                                                .upperLimitValue(11L)
                                                .upperLimitUnit(TimeUnit.DAYS)
                                                .rating(VelocityConfigDTO.Rating.GOOD)
                                                .build())
                                        .build())
                )
                .build()
        );
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(leadTimeResponse)
                .reportSubType(DoraWidgetReportSubType.LEAD_TIME_VALUES_CALCULATION_RATING_NON_MISSING.getDisplayName())
                .listRequest(originalRequest)
                .build();
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );
        doReturn(dbListResponse).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(DoraWidgetReportSubType.LEAD_TIME_VALUES_CALCULATION_RATING_NON_MISSING.getDisplayName())), any(), any()
        );

        DbAggregationResult expectedResult = DbAggregationResult.builder()
                .key("some-stage")
                .count(10L)
                .p90(80L)
                .p95(84L)
                .mean(5.0)
                .median(2L)
                .velocityStageResult(VelocityStageResult.builder()
                        .lowerLimitValue(4L)
                        .lowerLimitUnit(TimeUnit.DAYS)
                        .upperLimitValue(11L)
                        .upperLimitUnit(TimeUnit.DAYS)
                        .rating(VelocityConfigDTO.Rating.GOOD)
                        .build())
                .build();
        when(velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(any(), eq(originalRequest))).thenReturn(DbListResponse.of(List.of(expectedResult), 1));

        // execute
        Optional<DbListResponse<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getVelocityValuesForLeadTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Assert.assertEquals(Optional.of(DbListResponse.of(List.of(expectedResult), 1)), result);
    }

    @Test
    public void testGetVelocityValuesForMeanTime() throws JsonProcessingException, SQLException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .widgetId(widgetId)
                .filter(
                        Map.of(
                                "value_jira_issue_types", List.of("l1"),
                                "calculation", VelocityFilter.CALCULATION.pr_velocity.toString()
                        )
                )
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        List<DbAggregationResult> meanTimeResponse = List.of(
                DbAggregationResult.builder()
                        .key("some-stage")
                        .data(List.of(DbAggregationResult.builder()
                                .count(10L)
                                .p90(80L)
                                .p95(84L)
                                .mean(5.0)
                                .median(2L)
                                .velocityStageResult(VelocityStageResult.builder()
                                        .lowerLimitValue(4L)
                                        .lowerLimitUnit(TimeUnit.DAYS)
                                        .upperLimitValue(11L)
                                        .upperLimitUnit(TimeUnit.DAYS)
                                        .rating(VelocityConfigDTO.Rating.GOOD)
                                        .build())
                                .build())
                        )
                        .build()
        );
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(meanTimeResponse)
                .reportSubType(DoraWidgetReportSubType.MEAN_TIME_VALUES_CALCULATION_RATING_NON_MISSING.getDisplayName())
                .listRequest(originalRequest)
                .build();
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );
        doReturn(dbListResponse).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(DoraWidgetReportSubType.MEAN_TIME_VALUES_CALCULATION_RATING_NON_MISSING.getDisplayName())), any(), any()
        );

        DbAggregationResult expectedResult = DbAggregationResult.builder()
                .key("some-stage")
                .count(10L)
                .p90(80L)
                .p95(84L)
                .mean(5.0)
                .median(2L)
                .velocityStageResult(VelocityStageResult.builder()
                        .lowerLimitValue(4L)
                        .lowerLimitUnit(TimeUnit.DAYS)
                        .upperLimitValue(11L)
                        .upperLimitUnit(TimeUnit.DAYS)
                        .rating(VelocityConfigDTO.Rating.GOOD)
                        .build())
                .build();
        when(velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(any(), eq(originalRequest))).thenReturn(DbListResponse.of(List.of(expectedResult), 1));

        // execute
        Optional<DbListResponse<DbAggregationResult>> result = legacyLeadTimePrecalculatedWidgetReadService.getVelocityValuesForMeanTime(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Assert.assertEquals(
                Optional.of(DbListResponse.of(List.of(expectedResult), 1)),
                result
        );
    }
}
