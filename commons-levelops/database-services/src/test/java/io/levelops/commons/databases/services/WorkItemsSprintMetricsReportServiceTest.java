package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsSprintMappingFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Log4j2
@SuppressWarnings("unused")
public class WorkItemsSprintMetricsReportServiceTest {
    public static final String COMPANY = "test";
    private static final String SUB_TASK_ISSUE_TYPE = "SUB-TASK";
    private static final String CLOSED_STATUS_CATEGORY = "Closed";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static WorkItemsService workItemService;
    private static WorkItemTimelineService workItemTimelineService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    private static WorkItemsReportService workItemsReportGenerator;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemsSprintMetricReportService workItemsSprintMetricReportService;
    private static DataSource dataSource;
    private static String integrationId;
    private static Long ingestedAt;
    private static UserIdentityService userIdentityService;

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
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportGenerator,
                workItemsStageTimesReportService, workItemsAgeReportService, workItemsResolutionTimeReportService,
                workItemsResponseTimeReportService, workItemsSprintMetricReportService,
                null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
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

        String historiesResourcePath = "json/databases/azure_devops_workitem_history_2.json";
        IssueMgmtTestUtil.setupHistories(COMPANY, "1", workItemTimelineService, historiesResourcePath);
        String iterationsResourcePath = "json/databases/azure_devops_iterations_2.json";
        IssueMgmtTestUtil.setupIterations(COMPANY, "1", issuesMilestoneService, iterationsResourcePath);
        Date currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        String workitemsResourcePath = "json/databases/azure_devops_work_items_2.json";
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime,
                workitemsResourcePath, List.of(), List.of(), List.of(), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, previousTime,
                workitemsResourcePath, List.of(), List.of(), List.of(), userIdentityService);
    }

    @Test
    public void testSprintMetrics() throws SQLException {
        DbListResponse<DbAggregationResult> sprintMetricsReport = workItemService.getSprintMetricsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .build(),
                WorkItemsSprintMappingFilter
                        .builder()
                        .ignorableWorkitemType(false)
                        .build(), null, false, null);
        Assertions.assertThat(sprintMetricsReport).isNotNull();
        DefaultObjectMapper.prettyPrint(sprintMetricsReport.getRecords());
        Assertions.assertThat(sprintMetricsReport.getRecords().get(0).getSprintId()).isEqualTo("project-test-11\\April sprint 2");
        Assertions.assertThat(sprintMetricsReport.getRecords().get(0).getSprintName()).isEqualTo("April sprint 2");
        Assertions.assertThat(sprintMetricsReport.getRecords().get(0).getIssueMgmtSprintMappingAggResults().size()).isGreaterThan(0);
    }

    @Test
    public void testSprintCountReport() throws SQLException {
        DbListResponse<DbAggregationResult> sprintMetricsCountReport = workItemsSprintMetricReportService.generateCountReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .build(),
                WorkItemsSprintMappingFilter
                        .builder()
                        .ignorableWorkitemType(false)
                        .build(), null, false, null);
        Assertions.assertThat(sprintMetricsCountReport).isNotNull();

    }
}
