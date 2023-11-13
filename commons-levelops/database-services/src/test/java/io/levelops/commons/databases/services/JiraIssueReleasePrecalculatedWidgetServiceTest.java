package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.services.jira.JiraIssueReleasePrecalculatedWidgetService;
import io.levelops.commons.databases.services.jira.models.JiraReleaseReportSubType;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class JiraIssueReleasePrecalculatedWidgetServiceTest {
    private static final String company = "test";
    private static final boolean DISABLE_PRE_CALCULATION_RESULT = false;

    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static DashboardWidgetService dashboardWidgetService;
    private static WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;

    private static JiraIssueReleasePrecalculatedWidgetService jiraIssueReleasePrecalculatedWidgetService;

    @BeforeClass
    public static void setUp() {
        dashboardWidgetService = Mockito.mock(DashboardWidgetService.class);
        widgetPrecalculatedResultsDBService = Mockito.mock(WidgetPrecalculatedResultsDBService.class);

        jiraIssueReleasePrecalculatedWidgetService = new JiraIssueReleasePrecalculatedWidgetService(
                mapper, dashboardWidgetService, widgetPrecalculatedResultsDBService
        );
    }

    @Test
    public void testJiraReleaseTableReport() throws JsonProcessingException, SQLException {
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

        List<JiraReleaseResponse> jiraReleaseReport = List.of(
                JiraReleaseResponse.builder()
                        .name("abc")
                        .averageTime(1234567L)
                        .issueCount(12)
                        .project("PROJ")
                        .releaseEndTime(1234567L)
                        .build()
        );
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(jiraReleaseReport)
                .reportSubType(null)
                .listRequest(originalRequest)
                .calculatedAt(Instant.EPOCH)
                .build();
        DbListResponse<WidgetPrecalculatedReport> precalculatedResult = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );
        doReturn(precalculatedResult).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(JiraReleaseReportSubType.JIRA_RELEASE_TABLE_REPORT.getDisplayName())), any(), any()
        );

        // execute
        Optional<DbListResponse<JiraReleaseResponse>> result = jiraIssueReleasePrecalculatedWidgetService.jiraReleaseTableReport(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Assert.assertEquals(
                Optional.of(
                        DbListResponse.of(
                                jiraReleaseReport,
                                jiraReleaseReport.size(),
                                Instant.EPOCH.getEpochSecond()
                        )
                ),
                result
        );
    }

    @Test
    public void testJiraReleaseTableReportDrillDown() throws JsonProcessingException, SQLException, BadRequestException {
        // configure
        String widgetId = UUID.randomUUID().toString();
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .widgetId(widgetId)
                .filter(Map.of("fix_versions", List.of("release1.0")))
                .ouIds(Set.of(10))
                .build();

        String dashboardId = UUID.randomUUID().toString();
        Widget widget = Widget.builder().dashboardId(dashboardId).build();
        Dashboard dashboard = Dashboard.builder().metadata(Map.of("dashboard_time_range_filter", "LAST_7_DAYS")).build();
        doReturn(Optional.of(widget)).when(dashboardWidgetService).getWidget(company, widgetId, null);
        doReturn(Optional.of(dashboard)).when(dashboardWidgetService).get(company, dashboardId);

        DbJiraIssue jiraIssue1 = DbJiraIssue.builder()
                .fixVersion("release1.0")
                .issueCreatedAt(123234L)
                .key("PROJ-1")
                .id(UUID.randomUUID().toString())
                .velocityStageTotalTime(22222L)
                .priority("P2")
                .build();
        DbJiraIssue jiraIssue2 = DbJiraIssue.builder()
                .fixVersion("release1.0")
                .issueCreatedAt(123456L)
                .key("PROJ-2")
                .id(UUID.randomUUID().toString())
                .velocityStageTotalTime(33333L)
                .priority("P3")
                .build();
        DbJiraIssue jiraIssue3 = DbJiraIssue.builder()
                .fixVersion("release2.0")
                .issueCreatedAt(23456L)
                .key("PROJ-3")
                .id(UUID.randomUUID().toString())
                .velocityStageTotalTime(11111L)
                .priority("P1")
                .build();
        List<DbJiraIssue> dbResponse = List.of(jiraIssue1, jiraIssue2, jiraIssue3);
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(dbResponse)
                .reportSubType(null)
                .listRequest(originalRequest)
                .calculatedAt(Instant.EPOCH)
                .build();
        DbListResponse precalculatedResult = DbListResponse.of(
                List.of(widgetPrecalculatedReport), 1
        );

        doReturn(precalculatedResult).when(widgetPrecalculatedResultsDBService).listByFilter(
                eq(company), eq(0), eq(100), any(), any(), any(), eq(List.of(JiraReleaseReportSubType.JIRA_RELEASE_TABLE_REPORT_VALUES.getDisplayName())), any(), any()
        );

        // execute
        Optional<DbListResponse<DbJiraIssue>> result = jiraIssueReleasePrecalculatedWidgetService.jiraReleaseTableReportDrillDown(
                company, originalRequest, DISABLE_PRE_CALCULATION_RESULT
        );

        // assert
        Assert.assertEquals(
                Optional.of(
                        DbListResponse.of(
                                List.of(jiraIssue1, jiraIssue2),
                                2
                        )
                ),
                result
        );
    }
}
