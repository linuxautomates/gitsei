package io.levelops.commons.databases.services;

import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
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
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkItemsStoryPointReportServiceTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemTimelineService workItemTimelineService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static DataSource dataSource;
    private static String integrationId;
    private static Long ingestedAt;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        IntegrationService integrationService = new IntegrationService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
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
        currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        String workitemsResourcePath = "json/databases/azure_devops_work_items.json";
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, currentTime,
                workitemsResourcePath, List.of(), List.of(), List.of("date", "datetime"), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, List.of(), List.of(), List.of("date", "datetime"), userIdentityService);
    }

    @Test
    public void testStoryPointsReport() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.status)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null).getTotalCount()).isEqualTo(3);

        DbListResponse<DbAggregationResult> storyPointsReport = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.assignee)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(storyPointsReport.getTotalCount()).isEqualTo(2);
        assertThat(storyPointsReport.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").totalTickets(3L).totalStoryPoints(8L).totalUnestimatedTickets(0L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").totalTickets(1L).totalStoryPoints(0L).totalUnestimatedTickets(1L).build()
        );

        storyPointsReport = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.status)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(storyPointsReport.getTotalCount()).isEqualTo(3);
        assertThat(storyPointsReport.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Closed").totalTickets(2L).totalStoryPoints(2L).totalUnestimatedTickets(1L).build(),
                DbAggregationResult.builder().key("Open").totalTickets(1L).totalStoryPoints(3L).totalUnestimatedTickets(0L).build(),
                DbAggregationResult.builder().key("In Progress").totalTickets(1L).totalStoryPoints(3L).totalUnestimatedTickets(0L).build()
        );

        storyPointsReport = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.fix_version)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(storyPointsReport.getTotalCount()).isEqualTo(1);
        assertThat(storyPointsReport.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .totalTickets(4L)
                        .totalStoryPoints(8L)
                        .totalUnestimatedTickets(1L)
                        .build()
        );

        storyPointsReport = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.fix_version)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(storyPointsReport.getTotalCount()).isEqualTo(1);
        assertThat(storyPointsReport.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .totalTickets(4L)
                        .totalStoryPoints(8L)
                        .totalUnestimatedTickets(1L)
                        .build()
        );

        storyPointsReport = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                        .aggInterval("day")
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .sort(Map.of(WorkItemsFilter.CALCULATION.story_point_report.toString(), SortingOrder.DESC))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(storyPointsReport.getTotalCount()).isEqualTo(1);
        assertThat(storyPointsReport.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1620025200").additionalKey("3-5-2021").totalTickets(4L).totalStoryPoints(8L).totalUnestimatedTickets(1L).build()
        );

        storyPointsReport = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .sort(Map.of(WorkItemsFilter.CALCULATION.story_point_report.toString(), SortingOrder.DESC))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        List<Long> results = storyPointsReport.getRecords().stream().map(DbAggregationResult::getTotalStoryPoints).collect(Collectors.toList());
        Collections.reverse(results);
        assertThat(storyPointsReport.getTotalCount()).isEqualTo(1);
        assertThat(results).isSorted();
    }

    @Test
    public void testAggByStoryPoints() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .calculation(WorkItemsFilter.CALCULATION.story_point_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getRecords().get(0).getKey()).isEqualTo("3");
        assertThat(result.getRecords().get(0).getTotalTickets()).isEqualTo(2);
        assertThat(result.getRecords().get(1).getKey()).isEqualTo("2.5");
        assertThat(result.getRecords().get(1).getTotalTickets()).isEqualTo(1);
        assertThat(result.getRecords().get(2).getKey()).isEqualTo("0");
        assertThat(result.getRecords().get(2).getTotalTickets()).isEqualTo(1);
    }

    @Test
    public void testEffortReport() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.status)
                        .calculation(WorkItemsFilter.CALCULATION.effort_report)
                        .integrationIds(List.of(integrationId))
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getTotalEffort)).containsExactly(30L, 20L, -10L);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getTotalUnestimatedTickets)).containsExactly(0L, 1L, 0L);
    }

}
