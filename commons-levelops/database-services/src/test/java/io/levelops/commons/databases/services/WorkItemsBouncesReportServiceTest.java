package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
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

@Log4j2
public class WorkItemsBouncesReportServiceTest {
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
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        String historiesResourcePath = "json/databases/azure_devops_workitem_history_2.json";
        IssueMgmtTestUtil.setupHistories(COMPANY, "1", workItemTimelineService, historiesResourcePath);
    }

    @Test
    public void testBouncesTimeReport() throws SQLException {
        var expectedResults = setupWorkItemBouncesTimeExpectedResults();

        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsBouncesReport(COMPANY,
                    entry.getKey().toBuilder().ingestedAt(ingestedAt).build(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            String logger = "distinct: " + entry.getKey().getAcross() + " and calculation: " + entry.getKey().getCalculation();
            if (WorkItemsFilter.isAcrossUsers(entry.getKey().getAcross())) {
                List<DbAggregationResult> actualRecords = new ArrayList<>(actualResponse.getRecords());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                WorkItemTestUtils.compareAggResults(logger, entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupWorkItemBouncesTimeExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.priority)
                .build();
        expectedResults.put(workItemsFilter1, List.of(
                DbAggregationResult.builder()
                        .key("1")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("_UNPRIORITIZED_")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter2 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.project)
                .build();
        expectedResults.put(workItemsFilter2, List.of(
                DbAggregationResult.builder()
                        .key("cgn-test/Agile-Project")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter3 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        expectedResults.put(workItemsFilter3, List.of(
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
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter4 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.assignee)
                .build();
        expectedResults.put(workItemsFilter4, List.of(
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(3L)
                        .build(),
                DbAggregationResult.builder()
                        .additionalKey("_UNASSIGNED_")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter5 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.workitem_type)
                .build();
        expectedResults.put(workItemsFilter5, List.of(
                DbAggregationResult.builder()
                        .key("Bug")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Feature")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Task")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter6 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.label)
                .build();
        expectedResults.put(workItemsFilter6, List.of(
                DbAggregationResult.builder()
                        .key("sampleTag")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("tag")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter7 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.fix_version)
                .build();
        expectedResults.put(workItemsFilter7, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter8 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.component)
                .build();
        expectedResults.put(workItemsFilter8, List.of());
        WorkItemsFilter workItemsFilter9 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.version)
                .build();
        expectedResults.put(workItemsFilter9, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter10 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.reporter)
                .build();
        expectedResults.put(workItemsFilter10, List.of(
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter11 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.status_category)
                .build();
        expectedResults.put(workItemsFilter11, List.of(
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
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter12 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.resolution)
                .build();
        expectedResults.put(workItemsFilter12, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter13 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.attribute)
                .attributeAcross("code_area")
                .build();
        expectedResults.put(workItemsFilter13, List.of(
                DbAggregationResult.builder()
                        .key("Agile-Project-2\\agile-team-3")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Agile-Project\\agile-team-2")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter14 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.custom_field)
                .customAcross("Custom.TestCustomField1")
                .build();
        expectedResults.put(workItemsFilter14, List.of(
                DbAggregationResult.builder()
                        .key("1")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter15 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.epic)
                .build();
        expectedResults.put(workItemsFilter15, List.of(
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(4L)
                        .build()
        ));

        WorkItemsFilter workItemsFilter16 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                .build();
        expectedResults.put(workItemsFilter16, List.of(
                DbAggregationResult.builder()
                        .key("61")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter17 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.ticket_category)
                .build();
        expectedResults.put(workItemsFilter17, List.of(
                DbAggregationResult.builder()
                        .key("Other")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter18 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.trend)
                .build();
        expectedResults.put(workItemsFilter18, List.of(
                DbAggregationResult.builder()
                        .key(String.valueOf(ingestedAt))
                        .additionalKey(new SimpleDateFormat("d-M-yyyy").format(currentTime))
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter19 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                .build();
        expectedResults.put(workItemsFilter19, List.of(
                DbAggregationResult.builder()
                        .key("1620025200")
                        .additionalKey("3-5-2021")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(4L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter20 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.workitem_updated_at)
                .build();
        expectedResults.put(workItemsFilter20, List.of(
                DbAggregationResult.builder()
                        .key("1620284400")
                        .additionalKey("6-5-2021")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(3L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter21 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                .build();
        expectedResults.put(workItemsFilter21, List.of(
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter22 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.bounces)
                .across(WorkItemsFilter.DISTINCT.stage)
                .build();
        expectedResults.put(workItemsFilter22, List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(3L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build()
        ));
        return expectedResults;
    }

    @Test
    public void testBounceReportWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsBouncesReport = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsBouncesReport).isNotNull();
        assertThat(workItemsBouncesReport.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testExcludeStages() throws SQLException {
        var expectedResults = setupWorkItemBouncesTimeExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsBouncesReport(COMPANY,
                    entry.getKey().toBuilder().excludeStages(
                            List.of("Done", "Resolved", "To Do")
                    ).build(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            Assert.assertNotNull(actualResponse);
        }
    }

    @Test
    public void testExcludeStagesWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsBouncesReport = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .excludeStages(List.of("Done", "Resolved", "To Do"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsBouncesReport).isNotNull();

        workItemsBouncesReport = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.status)
                        .excludeStages(List.of("Done", "Resolved", "To Do"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsBouncesReport).isNotNull();

        workItemsBouncesReport = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                        .excludeStages(List.of("Done", "Resolved", "To Do"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsBouncesReport).isNotNull();
    }

    @Test
    public void testStoryPointsAgg() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsBouncesReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.hops)
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);

        System.out.println(result.getRecords());

        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("0")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2.5")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("3")
                        .median(0L)
                        .min(0L)
                        .max(0L)
                        .totalTickets(2L)
                        .build()
        );

        WorkItemTestUtils.compareAggResults("test Story points aff", expectedResult, result.getRecords());
    }
}