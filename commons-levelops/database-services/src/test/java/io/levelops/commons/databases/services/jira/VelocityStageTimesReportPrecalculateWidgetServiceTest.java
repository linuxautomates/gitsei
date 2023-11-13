package io.levelops.commons.databases.services.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.jira.models.VelocityStageTimesReportSubType;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VelocityStageTimesReportPrecalculateWidgetServiceTest {
    private static final String company = "test";
    private static ObjectMapper objectMapper;
    private static JiraIssueService jiraIssueService;
    private static OrgUnitHelper orgUnitHelper;
    private static DashboardWidgetService dashboardWidgetService;
    private static VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    private static JiraFilterParser jiraFilterParser;
    private static WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;
    private static VelocityStageTimesReportPrecalculateWidgetService velocityStageTimesReportPrecalculateWidgetService;

    @BeforeClass
    public static void setup() {
        objectMapper = DefaultObjectMapper.get();
        jiraIssueService = Mockito.mock(JiraIssueService.class);
        orgUnitHelper = Mockito.mock(OrgUnitHelper.class);
        dashboardWidgetService = Mockito.mock(DashboardWidgetService.class);
        velocityConfigsDatabaseService = Mockito.mock(VelocityConfigsDatabaseService.class);
        jiraFilterParser = Mockito.mock(JiraFilterParser.class);
        widgetPrecalculatedResultsDBService = Mockito.mock(WidgetPrecalculatedResultsDBService.class);
        velocityStageTimesReportPrecalculateWidgetService = new VelocityStageTimesReportPrecalculateWidgetService(objectMapper,
                jiraIssueService, orgUnitHelper,
                dashboardWidgetService, velocityConfigsDatabaseService,
                jiraFilterParser, widgetPrecalculatedResultsDBService);
    }

    @Test
    public void testCalculationOfTimeSpentInStageReport() throws IOException, SQLException {
        DefaultListRequest request = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/velocity_stage_time_request.json"), DefaultListRequest.class);
        VelocityConfig velocityConfig = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/workflow_profile.json"), VelocityConfig.class);
        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouRefId(33490)
                .request(request)
                .build();
        DbListResponse<DbAggregationResult> expected = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/velocity_stage_time_response.json"), objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, DbAggregationResult.class));
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), eq(IntegrationType.JIRA), any())).thenReturn(ouConfig);
        when(velocityConfigsDatabaseService.get(anyString(), anyString())).thenReturn(Optional.ofNullable(velocityConfig));
        when(jiraIssueService.stackedGroupBy(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(expected);

        Optional<DbListResponse<DbAggregationResult>> actual = velocityStageTimesReportPrecalculateWidgetService.calculateTimeSpentInStageReport(company,
                request,
                false);

        Assert.assertEquals(expected, actual.get());
    }

    @Test
    public void testCalculationOfTimeSpentInStageReportValues() throws IOException, SQLException, BadRequestException {
        DefaultListRequest request = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/velocity_stage_time_drilldown_request.json"), DefaultListRequest.class);
        VelocityConfig velocityConfig = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/workflow_profile.json"), VelocityConfig.class);
        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouRefId(33490)
                .request(request)
                .build();
        DbListResponse<DbJiraIssue> expected = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/velocity_stage_time_drilldown_response.json"), objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, DbJiraIssue.class));
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), eq(IntegrationType.JIRA), any())).thenReturn(ouConfig);
        when(velocityConfigsDatabaseService.get(anyString(), anyString())).thenReturn(Optional.ofNullable(velocityConfig));
        when(jiraIssueService.list(anyString(), any(JiraSprintFilter.class), any(), any(), any(), any(),any(), anyInt(), anyInt())).thenReturn(expected);

        Optional<DbListResponse<DbJiraIssue>> actual = velocityStageTimesReportPrecalculateWidgetService.calculateTimeSpentInStageReportValues(company,
                request,
                false);

        Assert.assertEquals(expected, actual.get());
    }

    @Test
    public void testVelocityStageTimeReportPreCalculation() throws IOException, BadRequestException, SQLException {
        DefaultListRequest request = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/velocity_stage_time_request.json"), DefaultListRequest.class);
        Widget widget = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/widget.json"), Widget.class);
        Dashboard dashboard = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/dashboard.json"), Dashboard.class);
        DbListResponse<DbAggregationResult> expected = objectMapper.readValue(ResourceUtils.getResourceAsString("json/databases/jira/velocity_stage_time_response.json"), objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, DbAggregationResult.class));
        WidgetPrecalculatedReport widgetPrecalculatedReport = WidgetPrecalculatedReport.builder()
                .report(expected.getRecords())
                .calculatedAt(Instant.ofEpochSecond(1686700800))
                .build();
        when(widgetPrecalculatedResultsDBService.listByFilter(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(DbListResponse.of(List.of(widgetPrecalculatedReport), 1));
        when(dashboardWidgetService.getWidget("test", widget.getId(), null)).thenReturn(Optional.ofNullable(widget));
        when(dashboardWidgetService.get(anyString(), anyString())).thenReturn(Optional.ofNullable(dashboard));

        Optional<DbListResponse<DbAggregationResult>> actual = velocityStageTimesReportPrecalculateWidgetService.getVelocityStageTimeReportPreCalculation(company,
                request,
                VelocityStageTimesReportSubType.VELOCITY_STAGE_TIME_REPORT,
                false);

        Assert.assertEquals(expected, actual.get());
    }
}
