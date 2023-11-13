package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;


import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
public class WorkItemsResolutionTimeReportServiceTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static final ObjectMapper m = DefaultObjectMapper.get();
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
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"),userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"),userIdentityService);
        String historiesResourcePath = "json/databases/azure_devops_workitem_history_2.json";
        IssueMgmtTestUtil.setupHistories(COMPANY, "1", workItemTimelineService, historiesResourcePath);
    }

    @Test
    public void testResolutionTimeReport() throws SQLException {
        var expectedResults = setupWorkItemResolutionTimeExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                    entry.getKey().toBuilder().ingestedAt(ingestedAt).build(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            String logger = "distinct: " + entry.getKey().getAcross() + " and calculation: " + entry.getKey().getCalculation();
            if (WorkItemsFilter.isAcrossUsers(entry.getKey().getAcross())) {
                List<DbAggregationResult> actualRecords = actualResponse.getRecords().stream().map(dbAggregationResult ->
                                dbAggregationResult.toBuilder().min(null).max(null).mean(null).median(null).p90(null).build())
                        .collect(Collectors.toList());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                WorkItemTestUtils.compareAggResults(logger, entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    @Test
    public void testResolutionTimeReportSorting() throws SQLException {
        List<String> metricList = new ArrayList<>();
        metricList.add("90th_percentile_resolution_time");
        Map<String, SortingOrder> sort=new HashMap<>();
        sort.put("resolution_time",SortingOrder.DESC);
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("metric", metricList);
        filterMap.put("sort_xaxis", "label_high-low");
        OUConfiguration ouConfig = OUConfiguration.builder()
                .request(
                        DefaultListRequest.builder()
                                .filter(
                                        filterMap
                                )
                                .build()
                )
                .build();
        WorkItemsFilter filter=WorkItemsFilter.builder().across(WorkItemsFilter.DISTINCT.assignee).ingestedAt(ingestedAt).build();
        DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsResolutionTimeReport(COMPANY, filter, WorkItemsMilestoneFilter.builder().build(), null, true, ouConfig);
        assertThat(actualResponse.getTotalCount()).isEqualTo(2);
          }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupWorkItemResolutionTimeExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.priority)
                .build();
        expectedResults.put(workItemsFilter1, List.of(
                DbAggregationResult.builder()
                        .key("2")
                        .median(9395485L)
                        .min(9395485L)
                        .max(9395507L)
                        .mean(Double.valueOf("9395496"))
                        .p90(9395505L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("_UNPRIORITIZED_")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter2 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.project)
                .build();
        expectedResults.put(workItemsFilter2, List.of(
                DbAggregationResult.builder()
                        .key("cgn-test/Agile-Project")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395500L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter3 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        expectedResults.put(workItemsFilter3, List.of(
                DbAggregationResult.builder()
                        .key("In Progress")
                        .median(9395485L)
                        .min(9395485L)
                        .max(9395485L)
                        .mean(Double.valueOf("9395485"))
                        .p90(9395485L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Closed")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864393"))
                        .p90(8489284L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter4 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.assignee)
                .build();
        expectedResults.put(workItemsFilter4, List.of(
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar")
                        .totalTickets(3L)
                        .build(),
                DbAggregationResult.builder()
                        .additionalKey("_UNASSIGNED_")
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter5 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.workitem_type)
                .build();
        expectedResults.put(workItemsFilter5, List.of(
                DbAggregationResult.builder()
                        .key("Task")
                        .median(9395485L)
                        .min(9395485L)
                        .max(9395507L)
                        .mean(Double.valueOf("9395496"))
                        .p90(9395505L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Bug")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Feature")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter6 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.label)
                .build();
        expectedResults.put(workItemsFilter6, List.of(
                DbAggregationResult.builder()
                        .key("tag")
                        .median(9395485L)
                        .min(9395485L)
                        .max(9395485L)
                        .mean(Double.valueOf("9395485"))
                        .p90(9395485L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("sampleTag")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter7 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.fix_version)
                .build();
        expectedResults.put(workItemsFilter7, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter8 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.component)
                .build();
        expectedResults.put(workItemsFilter8, List.of());
        WorkItemsFilter workItemsFilter9 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.version)
                .build();
        expectedResults.put(workItemsFilter9, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter10 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.reporter)
                .build();
        expectedResults.put(workItemsFilter10, List.of(
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io")
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io")
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter11 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.status_category)
                .build();
        expectedResults.put(workItemsFilter11, List.of(
                DbAggregationResult.builder()
                        .key("In Progress")
                        .median(9395485L)
                        .min(9395485L)
                        .max(9395485L)
                        .mean(Double.valueOf("9395485"))
                        .p90(9395485L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Closed")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864393"))
                        .p90(8489284L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter12 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.resolution)
                .build();
        expectedResults.put(workItemsFilter12, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .median(333278L)
                        .min(333278L)
                        .max(9694506L)
                        .mean(Double.valueOf("5013886"))
                        .p90(9694499L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter13 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.attribute)
                .attributeAcross("code_area")
                .build();
        expectedResults.put(workItemsFilter13, List.of(
                DbAggregationResult.builder()
                        .key("Agile-Project\\agile-team-2")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395500L)
                        .totalTickets(3L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Agile-Project-2\\agile-team-3")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter14 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.custom_field)
                .customAcross("Custom.TestCustomField1")
                .build();
        expectedResults.put(workItemsFilter14, List.of(
                DbAggregationResult.builder()
                        .key("1")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter15 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.epic)
                .build();
        expectedResults.put(workItemsFilter15, List.of(
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395500L)
                        .totalTickets(4L)
                        .build()
        ));

        WorkItemsFilter workItemsFilter16 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                .build();
        expectedResults.put(workItemsFilter16, List.of(
                DbAggregationResult.builder()
                        .key("61")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395500L)
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter17 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.ticket_category)
                .build();
        expectedResults.put(workItemsFilter17, List.of(
                DbAggregationResult.builder()
                        .key("Other")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395500L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter18 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.trend)
                .build();
        expectedResults.put(workItemsFilter18, List.of(
                DbAggregationResult.builder()
                        .key(String.valueOf(ingestedAt))
                        .additionalKey(new SimpleDateFormat("d-M-yyyy").format(currentTime))
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395501L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter19 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                .build();
        expectedResults.put(workItemsFilter19, List.of(
                DbAggregationResult.builder()
                        .key("1620025200")
                        .additionalKey("3-5-2021")
                        .median(333278L)
                        .min(333278L)
                        .max(9395507L)
                        .mean(Double.valueOf("4864387"))
                        .p90(9395500L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter20 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.workitem_updated_at)
                .build();
        expectedResults.put(workItemsFilter20, List.of(
                DbAggregationResult.builder()
                        .key("1620284400")
                        .additionalKey("6-5-2021")
                        .median(9399717L)
                        .min(9399717L)
                        .max(9399717L)
                        .mean(Double.valueOf("9399717"))
                        .p90(9399717L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .median(333278L)
                        .min(333278L)
                        .max(9399695L)
                        .mean(Double.valueOf("3355417"))
                        .p90(7586411L)
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter21 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                .build();
        expectedResults.put(workItemsFilter21, List.of(
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .mean(Double.valueOf("333278"))
                        .p90(333278L)
                        .totalTickets(2L)
                        .build()
        ));
        return expectedResults;
    }

    @Test
    public void testResolutionTimeReportWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsResolutionTimeReport = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsResolutionTimeReport).isNotNull();
        assertThat(workItemsResolutionTimeReport.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testExcludeStages() throws SQLException {
        var expectedResults = setupWorkItemResolutionTimeExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                    entry.getKey().toBuilder().excludeStages(
                            List.of("Done", "Resolved", "To Do")
                    ).build(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            Assert.assertNotNull(actualResponse);
        }
    }

    @Test
    public void testExcludeStagesWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsResolutionTimeReport = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .excludeStages(List.of("Done", "Resolved", "To Do"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsResolutionTimeReport).isNotNull();

        workItemsResolutionTimeReport = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                        .across(WorkItemsFilter.DISTINCT.status)
                        .excludeStages(List.of("Done", "Resolved", "To Do"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsResolutionTimeReport).isNotNull();

        workItemsResolutionTimeReport = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                        .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                        .excludeStages(List.of("Done", "Resolved", "To Do"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsResolutionTimeReport).isNotNull();
    }

    @Test
    public void testStoryPointsAgg() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsResolutionTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.resolution_time)
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);

        System.out.println(result.getRecords());

        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("2.5")
                        .median(10721273L)
                        .min(10721273L)
                        .max(10721273L)
                        .p90(10721273L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("0")
                        .median(333278L)
                        .min(333278L)
                        .max(333278L)
                        .p90(333278L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("3")
                        .median(333278L)
                        .min(333278L)
                        .max(10721251L)
                        .p90(9682453L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test Story points aff", expectedResult, result.getRecords());
    }

}
