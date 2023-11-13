package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.WorkItemTestUtils.compareAggResults;

@Log4j2
public class WorkItemsFirstAssigneeReportServiceTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static Long ingestedAt;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        IssuesMilestoneService issuesMilestoneService = new IssuesMilestoneService(dataSource);
        IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        WorkItemsReportService workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsAgeReportService workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsStageTimesReportService workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResponseTimeReportService workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);

        integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        workItemTimelineService.ensureTableExistence(COMPANY);
        issuesMilestoneService.ensureTableExistence(COMPANY);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(COMPANY);

        String historiesResourcePath = "json/databases/azure_devops_history_4.json";
        String workItemsResourcePath = "json/databases/azure_devops_work_items_4.json";
        String iterationsResourcePath = "json/databases/azure_devops_iterations_3.json";

        IssueMgmtTestUtil.setup(COMPANY, "1", workItemService, workItemTimelineService,
                issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime,
                historiesResourcePath, iterationsResourcePath, workItemsResourcePath,
                null, null, null, userIdentityService);
    }

    @Test
    public void testWorkItemFirstAssigneeFilters() throws SQLException {
        var expectedResults = setupWorkItemFirstAssigneeFiltersExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            WorkItemsFilter workItemsFilter = entry.getKey().toBuilder().ingestedAt(ingestedAt).build();
            WorkItemsTimelineFilter timelineFilter = WorkItemsTimelineFilter.builder().build();
            WorkItemsMilestoneFilter milestoneFilter = WorkItemsMilestoneFilter.builder().build();
            DbListResponse<DbAggregationResult> actualResponse = workItemService
                    .getFirstAssigneeReport(COMPANY, workItemsFilter, timelineFilter, milestoneFilter, false, null);
            String logger = "distinct: " + entry.getKey().getAcross() + " and calculation: " + entry.getKey().getCalculation();
            if (WorkItemsFilter.isAcrossUsers(entry.getKey().getAcross())) {
                List<DbAggregationResult> actualRecords = new ArrayList<>(actualResponse.getRecords());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                compareAggResults(logger, entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupWorkItemFirstAssigneeFiltersExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter filterWithFirstAssigneeAcross = WorkItemsFilter.builder()
                .priorities(List.of("1"))
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.first_assignee)
                .build();
        expectedResults.put(filterWithFirstAssigneeAcross, List.of(
                DbAggregationResult.builder().key("praveen@cognitree.com").median(320522L).min(320522L).max(320522L).
                        totalTickets(1L).build()));
        WorkItemsFilter filterWithPriorityAcross = WorkItemsFilter.builder()
                .priorities(List.of("1"))
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.priority)
                .build();
        expectedResults.put(filterWithPriorityAcross, List.of(
                DbAggregationResult.builder().key("1").median(320522L).min(320522L).max(320522L).
                        totalTickets(1L).build()));
        WorkItemsFilter filterWithVersionAcross = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.version)
                .build();
        expectedResults.put(filterWithVersionAcross, List.of(
                DbAggregationResult.builder().key("_UNKNOWN_").median(320522L).min(320522L).max(320522L).
                        totalTickets(2L).build()));
        WorkItemsFilter filterWithSprintAcross = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.sprint)
                .build();
        expectedResults.put(filterWithSprintAcross, List.of(
                DbAggregationResult.builder().key("project-test-1\\azure devopos integration").median(320522L).min(320522L).max(320522L).
                        totalTickets(3L).build()
        ));
        WorkItemsFilter filterWithLabelAcross = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.label)
                .build();
        expectedResults.put(filterWithLabelAcross, List.of(
                DbAggregationResult.builder().key("sampleTag").median(320522L).min(320522L).max(320522L).
                        totalTickets(1L).build()));
        WorkItemsFilter filterWithComponentAcross = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.component)
                .build();
        expectedResults.put(filterWithComponentAcross, List.of());
        WorkItemsFilter filterWithStatusAcross = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        expectedResults.put(filterWithStatusAcross, List.of(
                DbAggregationResult.builder().key("To Do").median(320522L).min(320522L).max(320522L).
                        totalTickets(1L).build(),
                DbAggregationResult.builder().key("Open").median(320522L).min(320522L).max(320522L).
                        totalTickets(1L).build()));
        return expectedResults;
    }

    @Test
    public void testWorkItemFirstAssigneeMilestoneFilters() throws SQLException {
    Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsMilestoneFilter milestoneFilter = WorkItemsMilestoneFilter.builder()
                .names(List.of("azure devopos integration"))
                .integrationIds(List.of("1"))
                .build();
        WorkItemsFilter filterWithSprintAcross = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.sprint)
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbAggregationResult> actualResponse = workItemService
                .getFirstAssigneeReport(COMPANY, filterWithSprintAcross, WorkItemsTimelineFilter.builder().build(), milestoneFilter, false, null);
        expectedResults.put(filterWithSprintAcross, List.of(
                DbAggregationResult.builder().key("project-test-1\\azure devopos integration").median(320522L).min(320522L).max(320522L).
                        totalTickets(3L).build()
        ));
        String logger = "distinct: " + filterWithSprintAcross.getAcross() + " and calculation: " + filterWithSprintAcross.getCalculation();
        compareAggResults(logger, expectedResults.get(filterWithSprintAcross), actualResponse.getRecords());
    }

    @Test
    public  void testWorkItemFirstAssigneeSortFilters() throws SQLException {
        WorkItemsFilter filterWithFirstAssigneeAcross1 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.first_assignee)
                .sort(Map.of("first_assignee", SortingOrder.ASC))
                .ingestedAt(ingestedAt)
                .build();
        WorkItemsFilter filterWithFirstAssigneeAcross2 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.first_assignee)
                .sort(Map.of("first_assignee", SortingOrder.DESC))
                .ingestedAt(ingestedAt)
                .build();
        WorkItemsFilter filterWithFirstAssigneeAcross3 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.first_assignee)
                .sort(Map.of("assign_to_resolve", SortingOrder.ASC))
                .ingestedAt(ingestedAt)
                .build();
        WorkItemsFilter filterWithFirstAssigneeAcross4 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.assign_to_resolve)
                .across(WorkItemsFilter.DISTINCT.first_assignee)
                .sort(Map.of("assign_to_resolve", SortingOrder.DESC))
                .ingestedAt(ingestedAt)
                .build();
        ArrayList<String> expectedResults1 = new ArrayList<>(List.of("chaitanya@cognitree.com", "praveen@cognitree.com"));
        ArrayList<String> expectedResults2 = new ArrayList<>(List.of("praveen@cognitree.com", "chaitanya@cognitree.com"));
        DbListResponse<DbAggregationResult> actualResponse1 = workItemService
                .getFirstAssigneeReport(COMPANY, filterWithFirstAssigneeAcross1, WorkItemsTimelineFilter.builder().build(),
                        WorkItemsMilestoneFilter.builder().build(), false, null);
        Assertions.assertThat(actualResponse1.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(expectedResults1);
        DbListResponse<DbAggregationResult> actualResponse2 = workItemService
                .getFirstAssigneeReport(COMPANY, filterWithFirstAssigneeAcross2, WorkItemsTimelineFilter.builder().build(),
                        WorkItemsMilestoneFilter.builder().build(), false, null);
        Assertions.assertThat(actualResponse2.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(expectedResults2);
        DbListResponse<DbAggregationResult> actualResponse3 = workItemService
                .getFirstAssigneeReport(COMPANY, filterWithFirstAssigneeAcross3, WorkItemsTimelineFilter.builder().build(),
                        WorkItemsMilestoneFilter.builder().build(), false, null);
        Assertions.assertThat(actualResponse3.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(expectedResults2);
        DbListResponse<DbAggregationResult> actualResponse4 = workItemService
                .getFirstAssigneeReport(COMPANY, filterWithFirstAssigneeAcross4, WorkItemsTimelineFilter.builder().build(),
                        WorkItemsMilestoneFilter.builder().build(), false, null);
        Assertions.assertThat(actualResponse4.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(expectedResults1);
    }
}