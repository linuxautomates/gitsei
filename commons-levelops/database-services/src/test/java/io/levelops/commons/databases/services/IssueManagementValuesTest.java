package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class IssueManagementValuesTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemsReportService workItemsReportService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static Long ingestedAt;
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        integrationService = new IntegrationService(dataSource);
        WorkItemsMetadataService workItemsMetadataService = new WorkItemsMetadataService(dataSource);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, null, null,
                null, null, null,
                null, null, workItemsPrioritySLAService, null, null,
                workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        workItemService.ensureTableExistence(COMPANY);
        workItemsMetadataService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);

        integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        Date currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        String workitemsResourcePath = "json/databases/azure_devops_work_items.json";
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, currentTime,
                workitemsResourcePath, List.of(), List.of(), List.of("date", "datetime"), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, List.of(), List.of(), List.of("date", "datetime"), userIdentityService);
        IssueMgmtTestUtil.setupStatusMetadata(COMPANY, "1", workItemsMetadataService);
    }

    @Test
    public void testValues() throws SQLException {
        var expectedResults = setupValuesExpectedResults();
        WorkItemsFilter.WorkItemsFilterBuilder valuesFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .calculation(WorkItemsFilter.CALCULATION.issue_count);
        for (Map.Entry<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemsReportService.generateReport(COMPANY,
                    valuesFilter.across(entry.getKey()).ingestedAt(ingestedAt).build(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            WorkItemTestUtils.compareAggResults(entry.getKey().name(), entry.getValue(), actualResponse.getRecords());
        }
    }

    private static Map<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> setupValuesExpectedResults() {
        Map<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> expectedResults = new HashMap<>();
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_type, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(2L).build(),
                DbAggregationResult.builder().key("Bug").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Feature").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.project, List.of(
                DbAggregationResult.builder().key("cgn-test/Agile-Project").totalTickets(4L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.assignee, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").totalTickets(3L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.component, List.of());
        expectedResults.put(WorkItemsFilter.DISTINCT.version, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.fix_version, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.label, List.of(
                DbAggregationResult.builder().key("sampleTag").totalTickets(1L).build(),
                DbAggregationResult.builder().key("tag").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.priority, List.of(
                DbAggregationResult.builder().key("2").totalTickets(2L).build(),
                DbAggregationResult.builder().key("1").totalTickets(1L).build(),
                DbAggregationResult.builder().key("_UNPRIORITIZED_").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.reporter, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(3L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.resolution, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.status, List.of(
                DbAggregationResult.builder().key("status-0").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-1").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-2").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-3").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-4").totalTickets(0L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.status_category, List.of(
                DbAggregationResult.builder().key("Closed").totalTickets(2L).build(),
                DbAggregationResult.builder().key("In Progress").totalTickets(1L).build(),
                DbAggregationResult.builder().key("Open").totalTickets(1L).build()
        ));
        return expectedResults;
    }
}
