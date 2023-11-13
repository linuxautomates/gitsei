package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsSprintMappingFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class IssueMgmtSprintCountTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemTimelineService workItemTimelineService;
    private static WorkItemsReportService workItemsReportGenerator;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemsSprintMetricReportService workItemsSprintMetricReportService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    private static DataSource dataSource;
    private static String integrationId;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static UserIdentityService userIdentityService;
    private static IntegrationTrackingService integrationTrackingService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static Date currentTime;
    private static Long ingestedAt;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);

        workItemTimelineService = new WorkItemTimelineService(dataSource);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportGenerator = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsSprintMetricReportService = new WorkItemsSprintMetricReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportGenerator,
                workItemsStageTimesReportService, workItemsAgeReportService, workItemsResolutionTimeReportService,
                workItemsResponseTimeReportService, workItemsSprintMetricReportService,
                null, workItemsPrioritySLAService, null, null,
                workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        integrationId = integrationService.insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azure test")
                .status("enabled")
                .build());
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        workItemTimelineService.ensureTableExistence(COMPANY);
        issuesMilestoneService.ensureTableExistence(COMPANY);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(COMPANY);
        integrationTrackingService.ensureTableExistence(COMPANY);
        currentTime = new Date();
        String iterationsResourcePath = "json/databases/azure_devops_iterations_2.json";
        String historiesResourcePath = "json/databases/azure_devops_workitem_history_2.json";
        String workItemsResourcePath = "json/databases/azure_devops_work_items_2.json";
        //read json
        IssueMgmtTestUtil.setup(COMPANY, integrationId, workItemService, workItemTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime, historiesResourcePath, iterationsResourcePath, workItemsResourcePath, List.of(), List.of(), List.of(), userIdentityService);
    }

    @Test
    public void testStageTimesReport() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsTimelineFilter.builder()
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsTimelineFilter.builder()
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testSprintMetricsReport() throws SQLException {
        Assertions.assertThat(workItemService.getSprintMetricsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                WorkItemsSprintMappingFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(workItemService.getSprintMetricsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                WorkItemsSprintMappingFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testSprintMetricsCountReport() throws SQLException {
        Assertions.assertThat(workItemService.getSprintMetricsReportCount(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping)
                        .calculation(WorkItemsFilter.CALCULATION.sprint_mapping)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                WorkItemsSprintMappingFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(workItemService.getSprintMetricsReportCount(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping)
                        .calculation(WorkItemsFilter.CALCULATION.sprint_mapping)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                WorkItemsSprintMappingFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testWorkItemsList() throws SQLException {
        Assertions.assertThat(workItemService.listByFilter(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                null,
                0, 10000).getTotalCount()).isEqualTo(60);
        Assertions.assertThat(workItemService.listByFilter(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                null,
                0, 10000).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testResponseTimeReportService() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testResolutionTimeReport() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testTicketsReport() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testAgeReport() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(3)
                        .build(),
                null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testSprintsList() throws SQLException {
        Assertions.assertThat(issuesMilestoneService.listByFilter(COMPANY,
                WorkItemsMilestoneFilter.builder().build(),
                0, 10000).getTotalCount()).isEqualTo(57);
        Assertions.assertThat(issuesMilestoneService.listByFilter(COMPANY,
                WorkItemsMilestoneFilter.builder()
                        .sprintCount(1)
                        .build(),
                0, 10000).getRecords().size()).isEqualTo(1);
    }
}
