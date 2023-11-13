package io.levelops.commons.databases.services;

import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.WorkItemTestUtils.compareAggResults;

@Log4j2
public class WorkItemSprintFilterTest {
    public static final String COMPANY = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static Date currentTime;
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
    private static UserIdentityService userIdentityService;
    private static IntegrationTrackingService integrationTrackingService;

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
        workItemService = new WorkItemsService(dataSource, workItemsReportGenerator, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
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
        IssueMgmtTestUtil.setup(COMPANY, integrationId, workItemService, workItemTimelineService,
                issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime,
                historiesResourcePath, iterationsResourcePath, workItemsResourcePath,
                null, null, null, userIdentityService);
    }

    @Test
    public void ticketsReportSprintFilterTest() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .build(), null, false, null).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2", "Unknown"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeNames(List.of("Sprint-Test1"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeFieldValues(List.of("5360cecb-39f5-481d-ac2c-81e7b6a93e11"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .fieldValues(List.of("d05739a9-42a8-4c7c-bcc4-f98c6e8d389b"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeParentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .parentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .states(List.of("current"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .startedAtRange(ImmutablePair.of(1620009306L, 1621019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .endedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .completedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2"))
                        .completedAtRange(ImmutablePair.of(1620009306L, 1623019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void responseTimeReportSprintFilterTest() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .build(), null, false, null).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2", "Unknown"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeNames(List.of("Sprint-Test1"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeFieldValues(List.of("5360cecb-39f5-481d-ac2c-81e7b6a93e11"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .fieldValues(List.of("d05739a9-42a8-4c7c-bcc4-f98c6e8d389b"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeParentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .parentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .states(List.of("current"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .startedAtRange(ImmutablePair.of(1620009306L, 1621019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .endedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .completedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2"))
                        .completedAtRange(ImmutablePair.of(1620009306L, 1623019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testAgeReport() throws SQLException {
        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.assignee)
                .build();
        WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter
                .builder()
                .names(List.of("sprint-2"))
                .build();
        List<DbAggregationResult> expectedResults = List.of();
        DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsAgeReport(COMPANY,
                workItemsFilter, workItemsMilestoneFilter, null, true, null);
        String logger = "distinct: " + workItemsFilter.getAcross() + " and calculation: " + workItemsFilter.getCalculation();
        compareAggResults(logger, expectedResults, actualResponse.getRecords());

        workItemsFilter = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        workItemsMilestoneFilter = WorkItemsMilestoneFilter
                .builder()
                .names(List.of("sprint-2", "Unknown"))
                .build();
        expectedResults = List.of();
        actualResponse = workItemService.getWorkItemsAgeReport(COMPANY,
                workItemsFilter, workItemsMilestoneFilter, null, true, null);
        logger = "distinct: " + workItemsFilter.getAcross() + " and calculation: " + workItemsFilter.getCalculation();
        compareAggResults(logger, expectedResults, actualResponse.getRecords());

        workItemsFilter = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.project)
                .build();
        workItemsMilestoneFilter = WorkItemsMilestoneFilter
                .builder()
                .fieldValues(List.of("d05739a9-42a8-4c7c-bcc4-f98c6e8d389b"))
                .build();
        expectedResults = List.of();
        actualResponse = workItemService.getWorkItemsAgeReport(COMPANY,
                workItemsFilter, workItemsMilestoneFilter, null, true, null);
        logger = "distinct: " + workItemsFilter.getAcross() + " and calculation: " + workItemsFilter.getCalculation();
        compareAggResults(logger, expectedResults, actualResponse.getRecords());

        workItemsFilter = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                .build();
        workItemsMilestoneFilter = WorkItemsMilestoneFilter
                .builder()
                .excludeFieldValues(List.of("5360cecb-39f5-481d-ac2c-81e7b6a93e11"))
                .build();
        expectedResults = List.of();
        actualResponse = workItemService.getWorkItemsAgeReport(COMPANY,
                workItemsFilter, workItemsMilestoneFilter, null, true, null);
        logger = "distinct: " + workItemsFilter.getAcross() + " and calculation: " + workItemsFilter.getCalculation();
        compareAggResults(logger, expectedResults, actualResponse.getRecords());

        workItemsFilter = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.workitem_type)
                .build();
        workItemsMilestoneFilter = WorkItemsMilestoneFilter
                .builder()
                .parentFieldValues(List.of("Agile-Project"))
                .build();
        expectedResults = List.of();
        actualResponse = workItemService.getWorkItemsAgeReport(COMPANY,
                workItemsFilter, workItemsMilestoneFilter, null, true, null);
        logger = "distinct: " + workItemsFilter.getAcross() + " and calculation: " + workItemsFilter.getCalculation();
        compareAggResults(logger, expectedResults, actualResponse.getRecords());

        Assertions.assertThat(workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .startedAtRange(ImmutablePair.of(1620009306L, 1621019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .endedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .completedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2"))
                        .completedAtRange(ImmutablePair.of(1620009306L, 1623019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void resolutionTimeReportSprintsFilterTest() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .build(), null, false, null).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-2"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-2", "Unknown"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeNames(List.of("Sprint-Test1"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeFieldValues(List.of("5360cecb-39f5-481d-ac2c-81e7b6a93e11"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .fieldValues(List.of("d05739a9-42a8-4c7c-bcc4-f98c6e8d389b"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeParentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .parentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .states(List.of("current"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .startedAtRange(ImmutablePair.of(1620009306L, 1621019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .endedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .completedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-2"))
                        .completedAtRange(ImmutablePair.of(1620009306L, 1623019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .excludeStages(List.of("Done", "Resolved"))
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-2"))
                        .completedAtRange(ImmutablePair.of(1620009306L, 1623019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .excludeStages(List.of("Done", "Resolved"))
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-2"))
                        .completedAtRange(ImmutablePair.of(1620009306L, 1623019306L))
                        .build(), WorkItemsFilter.DISTINCT.assignee, false, null).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void stageTimesReportSprintFilterTest() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .build(), null, false, null).getTotalCount()).isEqualTo(15);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes(new ArrayList<>(List.of("sprint"))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes((new ArrayList<>(List.of("sprint")))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .names(List.of("sprint-2", "Unknown"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes((new ArrayList<>(List.of("sprint")))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeNames(List.of("Sprint-Test1"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes((new ArrayList<>(List.of("sprint")))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeFieldValues(List.of("5360cecb-39f5-481d-ac2c-81e7b6a93e11"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes((new ArrayList<>(List.of("sprint")))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .fieldValues(List.of("d05739a9-42a8-4c7c-bcc4-f98c6e8d389b"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes((new ArrayList<>(List.of("sprint")))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .excludeParentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes(new ArrayList<>(List.of("sprint"))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .parentFieldValues(List.of("Agile-Project"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(7);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes(new ArrayList<>(List.of("sprint"))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .states(List.of("current"))
                        .build(), null, false, null).getTotalCount()).isEqualTo(5);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes(new ArrayList<>(List.of("sprint"))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .startedAtRange(ImmutablePair.of(1620009306L, 1621019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes(new ArrayList<>(List.of("sprint"))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .endedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .build(),
                WorkItemsTimelineFilter.builder().fieldTypes(new ArrayList<>(List.of("sprint"))).build(),
                WorkItemsMilestoneFilter
                        .builder()
                        .completedAtRange(ImmutablePair.of(1620009306L, 1622019306L))
                        .build(), null, false, null).getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testSortyBySprintTimeFields() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemsReportGenerator.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_start_date", SortingOrder.ASC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsReportGenerator.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_end_date", SortingOrder.DESC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsStageTimesReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_start_date", SortingOrder.ASC))
                        .build(), WorkItemsMilestoneFilter.builder().build(),
                WorkItemsTimelineFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsStageTimesReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_end_date", SortingOrder.DESC))
                        .build(), WorkItemsMilestoneFilter.builder().build(),
                WorkItemsTimelineFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsAgeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_start_date", SortingOrder.ASC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsAgeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_end_date", SortingOrder.DESC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsResolutionTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_start_date", SortingOrder.ASC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsResolutionTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_end_date", SortingOrder.DESC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsResponseTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_start_date", SortingOrder.ASC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        result = workItemsResponseTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .sort(Map.of("milestone_end_date", SortingOrder.DESC))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        Assertions.assertThat(result).isNotNull();

        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .sort(Map.of("milestone_start_date", SortingOrder.fromString("DESC")))
                .across(WorkItemsFilter.DISTINCT.fromString("project"))
                .build();
        DbListResponse<DbWorkItem> dbListResponse1 = workItemService.listByFilter(COMPANY,
                workItemsFilter1, WorkItemsMilestoneFilter.builder()
                        .names(List.of("Agile-Project\\sprint-2", "project-test-5\\Sprint 2")).build(), null, 0, 100);
        Assertions.assertThat(dbListResponse1).isNotNull();
    }

    @Test
    public void testMilestonePartialMatch() throws SQLException {
        WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_sprint_name", Map.of("$begins", "Ap")))
                .build();
        DbListResponse<DbAggregationResult> result = workItemsReportGenerator.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(), workItemsMilestoneFilter, null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();
      /*  String key = result.getRecords().get(0).getKey();
        String sprint_name  = key.split("\\\\")[1];
        Assertions.assertThat(sprint_name.startsWith("Ap")).isTrue();*/

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_sprint_name", Map.of("$ends", "2")))
                .build();
        result = workItemsReportGenerator.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(), workItemsMilestoneFilter, null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();
        /*String key = result.getRecords().get(0).getKey();
        String sprint_name  = key.split("\\\\")[1];
        Assertions.assertThat(sprint_name.endsWith("2")).isTrue();*/

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_parent_field_value", Map.of("$contains", "Agile")))
                .build();
        result = workItemsReportGenerator.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(), workItemsMilestoneFilter, null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();
        //Assertions.assertThat(result.getRecords().get(0).getKey().contains("Agile")).isTrue();

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_parent_field_value", Map.of("$begins", "pro")))
                .build();
        result = workItemsStageTimesReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(), workItemsMilestoneFilter,
                WorkItemsTimelineFilter.builder().build(), null, false, null);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_sprint_name", Map.of("$begins", "$azs")))
                .build();
        result = workItemsResolutionTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(),workItemsMilestoneFilter, null, false, null);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_sprint_name", Map.of("$ends", "st")))
                .build();
        result = workItemsResolutionTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(),workItemsMilestoneFilter, null, false, null);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();
       /* key = result.getRecords().get(0).getKey();
        sprint_name  = key.split("\\\\")[1];
        Assertions.assertThat(sprint_name.endsWith("st")).isTrue();*/

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_milestone_full_name", Map.of("$contains", "Agil")))
                .build();
        result = workItemsResolutionTimeReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .build(),workItemsMilestoneFilter, null, false, null);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getRecords()).isEmpty();
        //Assertions.assertThat(result.getRecords().get(0).getKey().contains("Agil")).isTrue();

        workItemsMilestoneFilter = WorkItemsMilestoneFilter.builder()
                .partialMatch(Map.of("workitem_sprint_name", Map.of("$ends", "st")))
                .build();
        DbListResponse<DbWorkItem> dbListResponse1 = workItemService.listByFilter(COMPANY,
                WorkItemsFilter.builder().build(), workItemsMilestoneFilter, null, 0, 100);
        Assertions.assertThat(dbListResponse1).isNotNull();
    }
    
}

