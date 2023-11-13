package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class WorkItemsAllReportsDedupeTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static IntegrationService integrationService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;

    private static WorkItemsBouncesReportService workItemsBouncesReportService;

    private static WorkItemsHopsReportService workItemsHopsReportService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static Date currentTime;
    private static Long ingestedAt;
    private static String integrationId;
    private static List<DbWorkItem> workItems;

    private static List<DbWorkItemHistory> workItemTimeLines;

    private static List<DbIssuesMilestone> workItemMilestones;
    private static UserIdentityService userIdentityService;
    private static IntegrationTrackingService integrationTrackingService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        workItemsHopsReportService = new WorkItemsHopsReportService(dataSource,workItemFieldsMetaService);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        IssuesMilestoneService milestoneService = new IssuesMilestoneService(dataSource);
        IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService, workItemsBouncesReportService,
                workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemFieldsMetaService.ensureTableExistence(COMPANY);
        milestoneService.ensureTableExistence(COMPANY);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(COMPANY);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource,DefaultObjectMapper.get());
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(COMPANY);
        integrationTrackingService.ensureTableExistence(COMPANY);

        integrationId = integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);

        workItems = List.of(getWorkItem("LEV-1", toTimeStamp(1629347837l) , 5.0F, "Low", "Done","BUG","E1",Map.of("ReleaseDate",1669107736l)),
                getWorkItem("LEV-2", toTimeStamp(1635595199l) , 6.0F, "High", "Done","BUG","E2",Map.of("ReleaseDate",1669107636l)),
                getWorkItem("LEV-3", toTimeStamp(1629347906l) , 5.0F, "Medium", "Done","BUG","E1",Map.of("ReleaseDate",1669107536l)),
                getWorkItem("LEV-4", toTimeStamp(1629347906l) , 6.0F, "Highest", "Done","BUG","E2",Map.of("ReleaseDate",1669107436l)));

        workItems.forEach(workItem -> {
            try {
                workItemService.insert(COMPANY,workItem);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        workItemMilestones = List.of(getWorkItemMileStone("sprint-1",toTimeStamp(1625097600l), toTimeStamp(1630411199l)),
                getWorkItemMileStone("sprint-2",toTimeStamp(1630454400l),toTimeStamp(1635681599l)));

        workItemMilestones.forEach(mileStone -> {
            try {
                milestoneService.insert(COMPANY, mileStone);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        workItemTimeLines = List.of(getWorkItemHistory("LEV-1","status","Open",toTimeStamp(1625616000l), toTimeStamp(1626307200l)),
                getWorkItemHistory("LEV-1","status","In Progress",toTimeStamp(1626307201l), toTimeStamp(1627603200l)),
                getWorkItemHistory("LEV-1","status","Closed",toTimeStamp(1627603200l), toTimeStamp(1631664000l)),
                getWorkItemHistory("LEV-1","sprint","LEV\\sprint-1",toTimeStamp(1625097600l), toTimeStamp(ingestedAt)),
                getWorkItemHistory("LEV-1","sprint","LEV\\sprint-2",toTimeStamp(1630454400l), toTimeStamp(ingestedAt)),
                getWorkItemHistory("LEV-1","sprint","LEV\\sprint-1",toTimeStamp(1630454400l), toTimeStamp(ingestedAt)),
                getWorkItemHistory("LEV-2","status","Open",toTimeStamp(1629158400l), toTimeStamp(1631145600l)),
                getWorkItemHistory("LEV-2","status","In Progress",toTimeStamp(1631145601l), toTimeStamp(1633737600l)),
                getWorkItemHistory("LEV-2","status","Closed",toTimeStamp(1633737601l), toTimeStamp(1635552000l)),
                getWorkItemHistory("LEV-2","sprint","LEV\\sprint-1",toTimeStamp(1625097600l), toTimeStamp(ingestedAt)),
                getWorkItemHistory("LEV-2","sprint","LEV\\sprint-2",toTimeStamp(1630454400l), toTimeStamp(ingestedAt)));

        workItemTimeLines.forEach( timeLine -> {
            try {
                workItemTimelineService.insert(COMPANY, timeLine);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        integrationTrackingService.upsert(COMPANY, IntegrationTracker.builder()
                .integrationId(integrationId)
                .latestIngestedAt(ingestedAt)
                .build());
    }

    private static DbWorkItemHistory getWorkItemHistory(String key, String fieldType, String fieldValue, Timestamp startTime, Timestamp endTime) {
        return DbWorkItemHistory.builder()
                .workItemId(key)
                .fieldType(fieldType)
                .fieldValue(fieldValue)
                .integrationId(integrationId)
                .startDate(startTime)
                .endDate(endTime)
                .build();
    }

    private static DbIssuesMilestone getWorkItemMileStone(String name, Timestamp startTime, Timestamp endTime) {
        return DbIssuesMilestone.builder()
                .name(name)
                .integrationId(Integer.valueOf(integrationId))
                .state("past")
                .fieldType("sprint")
                .fieldValue(name)
                .parentFieldValue("LEV")
                .projectId("LEV")
                .startDate(startTime)
                .endDate(endTime)
                .build();
    }

    private static Timestamp toTimeStamp(long l) {
        return DateUtils.toTimestamp(DateUtils.fromEpochSecond(l));
    }

    private static DbWorkItem getWorkItem(String key, Timestamp issueResolvedAt, float storyPoints, String priority, String statusCategory, String issueType, String parentWorkItemId, Map<String, Object> customFields) {
        return DbWorkItem.builder()
                .workItemId(key)
                .assignee("shiva")
                .integrationId(integrationId)
                .workItemResolvedAt(issueResolvedAt)
                .status("DONE")
                .statusCategory(statusCategory)
                .ingestedAt(ingestedAt)
                .project("test-project")
                .descSize(1)
                .customFields(customFields)
                .priority(priority)
                .reporter("xyz")
                .workItemType(issueType)
                .hops(0)
                .bounces(0)
                .numAttachments(0)
                .workItemCreatedAt(toTimeStamp(18870l))
                .workItemUpdatedAt(issueResolvedAt)
                .parentWorkItemId(parentWorkItemId)
                .storyPoint(storyPoints)
                .build();
    }

    @Test
    public void testTicketsReport() throws SQLException, IOException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.stage)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), WorkItemsFilter.DISTINCT.version, true, null);

        DefaultObjectMapper.prettyPrint(result);

        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test tickets report - across stage with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("High")
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Low")
                        .totalTickets(1L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test tickets report - across priority with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("LEV\\sprint-1")
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test tickets report - across sprint with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("E1")
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("E2")
                        .totalTickets(1L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test tickets report - across epic with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("E1")
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("E2")
                        .totalTickets(1L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test tickets report - across epic with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.trend)
                        .aggInterval("month")
                        .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.issue_count)
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .ingestedAt(ingestedAt)
                        .customFields(Map.of("ReleaseDate",Map.of("$gt","1669107735","$lt","1669107737")))
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);
    }

    @Test
    public void testStoryPointsReport() throws SQLException, IOException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .across(WorkItemsFilter.DISTINCT.stage)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), WorkItemsFilter.DISTINCT.version, true, null);

        DefaultObjectMapper.prettyPrint(result);

        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .totalTickets(2L)
                        .totalStoryPoints(11L)
                        .totalUnestimatedTickets(0L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .totalTickets(2L)
                        .totalStoryPoints(11L)
                        .totalUnestimatedTickets(0L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test stroy points report - across stage with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

//        DefaultObjectMapper.prettyPrint(result);

       expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("E2")
                        .totalTickets(1L)
                        .totalStoryPoints(6L)
                        .totalUnestimatedTickets(0L)
                        .build(),
                DbAggregationResult.builder()
                        .key("E1")
                        .totalTickets(1L)
                        .totalStoryPoints(5L)
                        .totalUnestimatedTickets(0L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test story points report - across epic with sprint filter", expectedResult, result.getRecords());
    }

    @Test
    public void testBouncesAndHopsReportWithSprintFilter() throws SQLException, IOException {

        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.bounces)
                        .across(WorkItemsFilter.DISTINCT.stage)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), WorkItemsFilter.DISTINCT.version, true, null);

        DefaultObjectMapper.prettyPrint(result);

        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test bounces report - across stage with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.bounces)
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("High")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Low")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test bounces report - across priority with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.bounces)
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("LEV\\sprint-1")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test bounces report - across sprint with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsHopsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.stage)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), WorkItemsFilter.DISTINCT.version, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test hops report - across stage with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsHopsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("High")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Low")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test hops report - across priority with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsHopsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("LEV\\sprint-1")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test hops report - across priority with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsHopsReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.trend)
                        .aggInterval("month")
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

    }

    @Test
    public void testAgeReport() throws SQLException, IOException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .across(WorkItemsFilter.DISTINCT.stage)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), WorkItemsFilter.DISTINCT.version, true, null);

        DefaultObjectMapper.prettyPrint(result);

        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test age report - across stage with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("High")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Low")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test age report - across priority with sprint filter", expectedResult, result.getRecords());

        result = workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sprint-1"))
                        .build(), null, true, null);

        DefaultObjectMapper.prettyPrint(result);

        expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("LEV\\sprint-1")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test age report - across sprint with sprint filter", expectedResult, result.getRecords());
    }
}
