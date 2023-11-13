package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemPrioritySLA;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@Log4j2
public class WorkItemsReportServiceTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static Date currentTime;
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
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        integrationService = new IntegrationService(dataSource);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsMetadataService workItemsMetadataService = new WorkItemsMetadataService(dataSource);
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
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        workItemsMetadataService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);

        integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test1")
                .status("enabled")
                .build());

        //read json
        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_item_fields.json");
        List<WorkItemField> workItemFields = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, WorkItemField.class));
        List<DbWorkItemField> customFieldProperties = workItemFields.stream()
                .map(field -> DbWorkItemField.fromAzureDevopsWorkItemField("1", field))
                .filter(dbWorkItemField -> BooleanUtils.isTrue(dbWorkItemField.getCustom()))
                .collect(Collectors.toList());
        List<IntegrationConfig.ConfigEntry> customFieldConfig = List.of(IntegrationConfig.ConfigEntry.builder()
                        .key("Custom.TestCustomField1")
                        .build(),
                IntegrationConfig.ConfigEntry.builder()
                        .key("Custom.TestCustomField2")
                        .build());
        currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        String workitemsResourcePath = "json/databases/azure_devops_work_items.json";
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, currentTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        IssueMgmtTestUtil.setupStatusMetadata(COMPANY, "1", workItemsMetadataService);
    }

    @Test
    public void testWorkItemsReport() throws SQLException {
        var expectedResults = setupAcrossExpectedResults();
        WorkItemsFilter.WorkItemsFilterBuilder prioritiesFilter = WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .priorities(List.of("2"));
        for (Map.Entry<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemsReportService.generateReport(COMPANY,
                    prioritiesFilter.across(entry.getKey()).build(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            if (WorkItemsFilter.isAcrossUsers(entry.getKey())) {
                List<DbAggregationResult> actualRecords = new ArrayList<>(actualResponse.getRecords());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                WorkItemTestUtils.compareAggResults(entry.getKey().name(), entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    @Test
    public void testTicketCategoryCustomFields() throws SQLException {
        var expectedResults = setupAcrossExpectedResults();
        WorkItemsFilter filter = WorkItemsFilter.builder()
                .across(WorkItemsFilter.DISTINCT.ticket_category)
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .ticketCategorizationFilters(List.of(
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .id("1")
                                .name("cat1")
                                .index(1)
                                .filter(WorkItemsFilter.builder()
                                        .customFields(Map.of("Custom.TestCustomField1", List.of("1")))
                                        .build())
                                .build(),
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .id("2")
                                .name("cat2")
                                .index(2)
                                .filter(WorkItemsFilter.builder()
                                        .customFields(Map.of("Custom.TestCustomField1", List.of("2")))
                                        .build())
                                .build()
                ))
                .build();
        DbListResponse<DbAggregationResult> actualResponse = workItemsReportService.generateReport(COMPANY,
                filter, WorkItemsMilestoneFilter.builder().build(), null, true, null);
        DefaultObjectMapper.prettyPrint(actualResponse);
        WorkItemTestUtils.compareAggResults("ticket_category",
                List.of(
                        DbAggregationResult.builder().key("Other").totalTickets(2L).build(),
                        DbAggregationResult.builder().key("cat1").totalTickets(1L).build(),
                        DbAggregationResult.builder().key("cat2").totalTickets(1L).build()
                ), actualResponse.getRecords());
    }

    @Test
    public void prioritiesListTest() throws SQLException {
        workItemsPrioritySLAService.insert(COMPANY, DbWorkItemPrioritySLA.builder()
                        .integrationId("1")
                        .project("cgn-test/Agile-Project")
                        .workitemType("Task")
                        .priority("2")
                .build());
        List<DbWorkItemPrioritySLA> dbWorkItemPrioritySLAS = workItemsPrioritySLAService.listPrioritySla(COMPANY, List.of("1"),
                List.of("cgn-test/Agile-Project"), List.of("Task"), List.of("2")).getRecords();
        assertNotNull(dbWorkItemPrioritySLAS);
        assertNotNull(dbWorkItemPrioritySLAS.get(0));

        dbWorkItemPrioritySLAS.forEach(dbWorkItemPrioritySLA -> {
            assertThat(dbWorkItemPrioritySLA.getIntegrationId()).isEqualTo("1");
            assertThat(dbWorkItemPrioritySLA.getProject()).isEqualTo("cgn-test/Agile-Project");
            assertThat(dbWorkItemPrioritySLA.getWorkitemType()).isEqualTo("Task");
            assertThat(dbWorkItemPrioritySLA.getPriority()).isEqualTo("2");
        });
    }

    private static Map<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> setupAcrossExpectedResults() {
        Map<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> expectedResults = new HashMap<>();
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_type, List.of(
                DbAggregationResult.builder().key("Task").totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_created_at, List.of(
                DbAggregationResult.builder().key("1620025200").totalTickets(2L).additionalKey("3-5-2021").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_updated_at, List.of(
                DbAggregationResult.builder().key("1620284400").totalTickets(1L).additionalKey("6-5-2021").build(),
                DbAggregationResult.builder().key("1620370800").totalTickets(1L).additionalKey("7-5-2021").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_resolved_at, List.of());
        expectedResults.put(WorkItemsFilter.DISTINCT.epic, List.of(
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(2L).build()
        ));
        final Long ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        final String key = String.valueOf(ingestedAt);
        String addlnKey = new SimpleDateFormat("d-M-yyyy").format(currentTime);
        expectedResults.put(WorkItemsFilter.DISTINCT.trend, List.of(
                DbAggregationResult.builder().key(key)
                        .totalTickets(2L).additionalKey(addlnKey).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.parent_workitem_id, List.of(
                DbAggregationResult.builder().key("61").totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.ticket_category, List.of(
                DbAggregationResult.builder().key("Other").totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.project, List.of(
                DbAggregationResult.builder().key("cgn-test/Agile-Project").totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.assignee, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.component, List.of());
        expectedResults.put(WorkItemsFilter.DISTINCT.version, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.fix_version, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.label, List.of(
                DbAggregationResult.builder().key("tag").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.priority, List.of(
                DbAggregationResult.builder().key("2").totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.reporter, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(1L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.resolution, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(2L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.status, List.of(
                DbAggregationResult.builder().key("status-0").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-1").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-2").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-3").totalTickets(0L).build(),
                DbAggregationResult.builder().key("status-4").totalTickets(0L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.status_category, List.of(
                DbAggregationResult.builder().key("Closed").totalTickets(1L).build(),
                DbAggregationResult.builder().key("In Progress").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.stage, List.of(
                DbAggregationResult.builder().key("Closed").totalTickets(1L).build(),
                DbAggregationResult.builder().key("In Progress").totalTickets(1L).build()
        ));
        return expectedResults;
    }

    @Test
    public void testAttributes() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse1 = workItemsReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.attribute)
                        .attributeAcross("code_area")
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(dbListResponse1.getTotalCount()).isEqualTo(2);
    }

    @Test
    public void testCustomFields() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse1 = workItemsReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.custom_field)
                        .customAcross("Custom.TestCustomField1")
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(dbListResponse1.getTotalCount()).isEqualTo(2);
    }

    @Test
    public void testStoryPointsAgg() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemsReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getRecords().get(0).getKey()).isEqualTo("3");
        assertThat(result.getRecords().get(0).getTotalTickets()).isEqualTo(2);
        assertThat(result.getRecords().get(1).getKey()).isEqualTo("0");
        assertThat(result.getRecords().get(1).getTotalTickets()).isEqualTo(1);
        assertThat(result.getRecords().get(2).getKey()).isEqualTo("2.5");
        assertThat(result.getRecords().get(2).getTotalTickets()).isEqualTo(1);
    }

    @Test
    public void testValues() throws SQLException {
        WorkItemsFilter.WorkItemsFilterBuilder workItemsFilter1 = WorkItemsFilter.builder().integrationIds(List.of("1"));
        List<String> fields = List.of("workitem_type", "project");
        List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
        for (String field : fields) {
            response.add(Map.of(field, workItemsReportService.generateReport(COMPANY,
                    workItemsFilter1
                            .across(WorkItemsFilter.DISTINCT.fromString(field))
                            .calculation(WorkItemsFilter.CALCULATION.issue_count)
                            .ingestedAt(ingestedAt)
                            .build(), WorkItemsMilestoneFilter.builder().build(), null, true, null).getRecords()));//no limit.
        }
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get(0).get("workitem_type").size()).isEqualTo(3);
        assertThat(response.get(1).get("project").size()).isEqualTo(1);
    }

    @Test
    public void testParentWorkItemType() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemsReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.none)
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getRecords().get(0).getTotalTickets()).isEqualTo(4);

        result = workItemsReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.none)
                        .parentWorkItemTypes(List.of("Feature"))
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getRecords().get(0).getTotalTickets()).isEqualTo(3);

        result = workItemsReportService.generateReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.none)
                        .parentWorkItemTypes(List.of("Epic"))
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, false, null);
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getRecords().get(0).getTotalTickets()).isEqualTo(0);
    }
}
