package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.databases.services.WorkItemTestUtils.compareAggResults;

@Log4j2
public class WorkItemsHygieneReportServiceTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemTimelineService workItemTimelineService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static Date currentTime;
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
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        integrationService = new IntegrationService(dataSource);
        workItemTimelineService = new WorkItemTimelineService(dataSource);
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
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        workItemService.ensureTableExistence(COMPANY);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        currentTime = new Date();

        integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        //read json
        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_items_hygiene_report.json");
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));

        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItems().forEach(
                            workItem -> {
                                DbWorkItem dbWorkItem = DbWorkItem.fromAzureDevOpsWorkItem("1", project,
                                        currentTime, workItem, List.of(), List.of(), Map.of(), null);
                                try {
                                    dbWorkItem = IssueMgmtTestUtil.upsertAndPopulateDbWorkItemIds(COMPANY, dbWorkItem, userIdentityService);
                                } catch (SQLException e) {
                                    log.warn("setUpWorkitems: error populting assignee and reporter Ids", e);
                                }
                                dbWorkItems.add(dbWorkItem);
                            });
                });
        dbWorkItems = setupWorkItemForTest(dbWorkItems);
        dbWorkItems.stream().takeWhile(Objects::nonNull).forEach(
                dbAzureDevopsWorkItem -> {
                    try {
                        workItemService.insert(COMPANY, dbAzureDevopsWorkItem);
                    } catch (SQLException e) {
                        log.warn("setupAzureDevopsWorkItems: error inserting project: " + dbAzureDevopsWorkItem.getId(), e);
                    }
                }
        );
    }

    private static ArrayList<DbWorkItem> setupWorkItemForTest(List<DbWorkItem> dbWorkItems) {
        ArrayList<DbWorkItem> updatedDbWorkItems = new ArrayList<>();
        Timestamp timestamp_30days_before
                = new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(22));
        Timestamp timestamp_45days_before
                = new Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(45));

        for (DbWorkItem dbWorkItem : dbWorkItems) {
            Timestamp workItemDueAt = timestamp_30days_before;
            Timestamp workItemUpdatedAt = timestamp_30days_before;
            int descSize = 50;
            List<String> components = List.of("Product", "test");
            String assignee = dbWorkItem.getAssignee();

            switch (dbWorkItem.getWorkItemId()) {
                case "76":
                    workItemUpdatedAt = timestamp_45days_before;
                    break;
                case "75":
                    assignee = DbWorkItem.UNASSIGNED;
                    break;
                case "74":
                    assignee = DbWorkItem.UNASSIGNED;
                    workItemDueAt = null;
                    break;
                case "73":
                    workItemDueAt = null;
                    break;
                case "72":
                    descSize = 25;
                    break;
                case "71":
                    descSize = 3;
                    break;
                case "70":
                    components = Collections.emptyList();
                    break;
            }

            dbWorkItem = dbWorkItem.toBuilder()
                    .workItemUpdatedAt(workItemUpdatedAt)
                    .workItemDueAt(workItemDueAt)
                    .descSize(descSize)
                    .components(components)
                    .assignee(assignee)
                    .build();
            updatedDbWorkItems.add(dbWorkItem);
        }
        return updatedDbWorkItems;
    }

    @Test
    public void testHygieneFilters() throws SQLException {
        var expectedResults = setupHygieneFiltersExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemsReportService.generateReport(COMPANY,
                    entry.getKey(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
            Map<WorkItemsFilter.EXTRA_CRITERIA, Object> criteria = entry.getKey().getHygieneCriteriaSpecs();
            compareAggResults(criteria.toString(), entry.getValue(), actualResponse.getRecords());
        }
    }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupHygieneFiltersExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.no_assignee))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(2L).build()));
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.poor_description))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter1, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter2 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.poor_description))
                .hygieneCriteriaSpecs(Map.of(WorkItemsFilter.EXTRA_CRITERIA.poor_description, "35"))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter2, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(2L).build()));
        WorkItemsFilter workItemsFilter3 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.idle))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter3, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter4 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.idle))
                .hygieneCriteriaSpecs(Map.of(WorkItemsFilter.EXTRA_CRITERIA.idle, "15"))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter4, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(10L).build()));
        WorkItemsFilter workItemsFilter5 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.no_due_date))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter5, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(2L).build()));
        WorkItemsFilter workItemsFilter6 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.no_components))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter6, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter7 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.no_due_date, WorkItemsFilter.EXTRA_CRITERIA.no_assignee))
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        expectedResults.put(workItemsFilter7, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(1L).build()));
        List<WorkItemsFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(
                WorkItemsFilter.TicketCategorizationFilter.builder()
                        .name("Unassigned tickets")
                        .index(1)
                        .filter(WorkItemsFilter.builder()
                                .missingFields(Map.of("assignee_id", true))
                                .build())
                        .build());
        WorkItemsFilter workItemsFilter8 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .ticketCategorizationFilters(ticketCategorizationFilters)
                .across(WorkItemsFilter.DISTINCT.ticket_category).build();
        expectedResults.put(workItemsFilter8, List.of(
                DbAggregationResult.builder().key("Unassigned tickets").totalTickets(10L).build())
        );
        WorkItemsFilter workItemsFilter9 = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .ticketCategories(List.of("Unassigned tickets"))
                .ticketCategorizationFilters(ticketCategorizationFilters)
                .across(WorkItemsFilter.DISTINCT.ticket_category).build();
        expectedResults.put(workItemsFilter9, List.of(
                DbAggregationResult.builder().key("Unassigned tickets").totalTickets(10L).build()));
        return expectedResults;
    }

    @Test
    public void testStack() throws SQLException {
        List<WorkItemsFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(
                WorkItemsFilter.TicketCategorizationFilter.builder()
                        .name("Unassigned tickets")
                        .index(1)
                        .filter(WorkItemsFilter.builder()
                                .missingFields(Map.of("assignee_id", true))
                                .build())
                        .build());

        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .ticketCategorizationFilters(ticketCategorizationFilters)
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();
        DbListResponse<DbAggregationResult> dbListResponse
                = workItemsReportService.generateReport(COMPANY, workItemsFilter, WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.ticket_category, false, null);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder().key("Task").totalTickets(10L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Unassigned tickets").additionalKey("Task").totalTickets(10L).build()
                        )).build()
        );
        verifyRecords(dbListResponse.getRecords(), expected, true);

        WorkItemsFilter workItemsFilterWithStack = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .across(WorkItemsFilter.DISTINCT.workitem_created_at).build();
        dbListResponse
                = workItemsReportService.generateReport(COMPANY, workItemsFilterWithStack, WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.ticket_category, false, null);
        expected = List.of(
                DbAggregationResult.builder().key("1620025200").totalTickets(10L).stacks(
                        List.of(
                                DbAggregationResult.builder().key("Other").additionalKey("1620025200").totalTickets(10L).build()
                        )).build()
        );
        verifyRecords(dbListResponse.getRecords(), expected, true);

        ticketCategorizationFilters = List.of(
                WorkItemsFilter.TicketCategorizationFilter.builder()
                        .name("Unassigned tickets")
                        .index(1)
                        .filter(WorkItemsFilter.builder()
                                .missingFields(Map.of("assignee_id", true))
                                .build())
                        .build());

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .ticketCategories(List.of("Unassigned tickets"))
                .ticketCategorizationFilters(ticketCategorizationFilters)
                .across(WorkItemsFilter.DISTINCT.workitem_type).build();

        DbListResponse<DbWorkItem> dbWorkItemDbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null,
                0, 20);
        Assert.assertEquals(10, dbWorkItemDbListResponse.getRecords().size());
    }

    private void verifyRecords(List<DbAggregationResult> a, List<DbAggregationResult> e, boolean ignoreKey) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<Object, DbAggregationResult> actualMap = convertListToMap(a, ignoreKey);
        Map<Object, DbAggregationResult> expectedMap = convertListToMap(e, ignoreKey);
        for (Object key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key), ignoreKey);
        }
    }

    private Map<Object, DbAggregationResult> convertListToMap(List<DbAggregationResult> lst, boolean ignoreKey) {
        Map<Object, DbAggregationResult> map = new HashMap<>();
        for (int i = 0; i < lst.size(); i++) {
            if (ignoreKey) {
                map.put(i, lst.get(i));
            } else {
                map.put(AcrossUniqueKey.fromDbAggregationResult(lst.get(i)), lst.get(i));
            }
        }
        return map;
    }

    private void verifyRecord(DbAggregationResult a, DbAggregationResult e, boolean ignoreKey) {
        Assert.assertEquals((e == null), (a == null));
        if (e == null) {
            return;
        }
        if (!ignoreKey) {
            Assert.assertEquals(a.getKey(), e.getKey());
        }
        Assert.assertEquals(a.getMedian(), e.getMedian());
        Assert.assertEquals(a.getMin(), e.getMin());
        Assert.assertEquals(a.getMax(), e.getMax());
        Assert.assertEquals(a.getCount(), e.getCount());
        Assert.assertEquals(a.getSum(), e.getSum());
        Assert.assertEquals(a.getTotalTickets(), e.getTotalTickets());
        verifyRecords(a.getStacks(), e.getStacks(), true);
    }
}