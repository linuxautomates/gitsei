package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class WorkitemsSlaHygieneReportTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    private static DataSource dataSource;
    private static IssueMgmtSprintMappingDatabaseService sprintMappingService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static WorkItemsService workItemService;
    private static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private static WorkItemFieldsMetaService workItemFieldsMetaService;
    private static String integrationId;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        sprintMappingService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, new WorkItemsReportService(dataSource, workItemFieldsMetaService),
                null, null, new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService),
                new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService),
                null, null, workItemsPrioritySLAService,
                null, null, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("Azure-Devops-test")
                .status("enabled")
                .build());
        workItemService.ensureTableExistence(company);
        sprintMappingService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);
        workItemFieldsMetaService.ensureTableExistence(company);
        workItemTimelineService.ensureTableExistence(company);
        currentTime = new Date();

        workItemFieldsMetaService.batchUpsert(company,
                List.of(DbWorkItemField.builder().custom(true).name("target time").integrationId("1").fieldType("datetime").fieldKey("customfield_10048").build(),
                        DbWorkItemField.builder().custom(true).name("story points").integrationId("1").fieldType("integer").fieldKey("customfield_10052").build(),
                        DbWorkItemField.builder().custom(true).name("custom labels").integrationId("1").fieldType("array").fieldKey("customfield_10050").build()));
    }

    @Test
    public void test() throws SQLException, BadRequestException {
        Instant beforeOneMonth = Instant.ofEpochSecond(System.currentTimeMillis()- TimeUnit.DAYS.toMillis(35));


        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("a")
                .integrationId("1")
                .project("prod")
                .workItemType("task")
                .priority("HIGH")
                .epic("E1")
                .workItemCreatedAt(Timestamp.from(Instant.now()))
                .firstCommentAt(Timestamp.from(beforeOneMonth))
                .customFields(Map.of("customfield_10048", "1628173800",
                        "customfield_10050", List.of("Magic1", "Magic2"), "customfield_10052", "7"))
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("b")
                .integrationId("1")
                .project("prod")
                .workItemType("task")
                .priority("HIGH")
                .workItemCreatedAt(Timestamp.from(Instant.now()))
                .workItemResolvedAt(Timestamp.from(beforeOneMonth))
                .customFields(Map.of("customfield_10048", "1628605800",
                        "customfield_10050", List.of("Magic2", "Magic3"), "customfield_10052", "3"))
                .epic("E1")
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("c")
                .integrationId("1")
                .project("prod")
                .workItemType("task")
                .priority("HIGH")
                .workItemCreatedAt(Timestamp.from(Instant.now()))
                .firstCommentAt(Timestamp.from(Instant.now()))
                .workItemResolvedAt(Timestamp.from(Instant.now()))
                .customFields(Map.of("customfield_10048", "1628433000",
                        "customfield_10050", List.of("Magic3", "Magic4"), "customfield_10052", "5"))
                .epic("E2")
                .build());

        workItemsPrioritySLAService.bulkUpdatePrioritySla(company, null, null, null,
                null, null, TimeUnit.DAYS.toSeconds(30), TimeUnit.DAYS.toSeconds(30));

        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        DbListResponse<DbAggregationResult> workItemsReport
                = workItemService.getWorkItemsReport(company, workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(workItemsReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsReport.getRecords().get(0).getTotalTickets()).isEqualTo(3);

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        workItemsReport
                = workItemService.getWorkItemsReport(company, workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(workItemsReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsReport.getRecords().get(0).getTotalTickets()).isEqualTo(1);

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        workItemsReport = workItemService.getWorkItemsReport(company, workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(workItemsReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsReport.getRecords().get(0).getTotalTickets()).isEqualTo(1);

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        workItemsReport
                = workItemService.getWorkItemsResolutionTimeReport(company, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(workItemsReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsReport.getRecords().get(0).getTotalTickets()).isEqualTo(1);

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        workItemsReport =
                workItemService.getWorkItemsResolutionTimeReport(company, workItemsFilter,
                        WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(workItemsReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsReport.getRecords().get(0).getTotalTickets()).isEqualTo(1);
        
        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .customFields(Map.of("customfield_10048", Map.of("$gt", "1628515166")))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();

        DbListResponse<DbWorkItem> dbWorkItemDbListResponse = workItemService.listByFilter(company, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbWorkItemDbListResponse.getRecords().size()).isEqualTo(1);
        assertThat(dbWorkItemDbListResponse.getRecords().get(0).getWorkItemId()).isEqualTo("b");

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .customFields(Map.of("customfield_10048", Map.of("$lt", "1628255966")))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        dbWorkItemDbListResponse =
                workItemService.listByFilter(company, workItemsFilter,
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbWorkItemDbListResponse.getRecords().size()).isEqualTo(1);
        assertThat(dbWorkItemDbListResponse.getRecords().get(0).getWorkItemId()).isEqualTo("a");

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .customFields(Map.of("customfield_10048",
                        Map.of("$lt", "1628515166", "$gt", "1628255966")))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        dbWorkItemDbListResponse =
                workItemService.listByFilter(company, workItemsFilter,
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbWorkItemDbListResponse.getRecords().size()).isEqualTo(1);
        assertThat(dbWorkItemDbListResponse.getRecords().get(0).getWorkItemId()).isEqualTo("c");

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .customFields(Map.of("customfield_10052",
                        Map.of("$lt", "6", "$gt", "4")))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        dbWorkItemDbListResponse =
                workItemService.listByFilter(company, workItemsFilter,
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbWorkItemDbListResponse.getRecords().size()).isEqualTo(1);
        assertThat(dbWorkItemDbListResponse.getRecords().get(0).getWorkItemId()).isEqualTo("c");
    }
}
