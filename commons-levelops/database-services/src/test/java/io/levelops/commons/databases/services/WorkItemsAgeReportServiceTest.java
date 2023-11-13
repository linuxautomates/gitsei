package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
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
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
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
public class WorkItemsAgeReportServiceTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static final ObjectMapper m = DefaultObjectMapper.get();
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
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                null, null, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);

        integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_items.json");
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_item_fields.json");
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
        Long ingestedAt = Long.valueOf("1627452844");
        Date currentTime = new Date();
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItems().forEach(
                            workItem -> {
                                DbWorkItem dbWorkItem = DbWorkItem.fromAzureDevOpsWorkItem("1", project,
                                        currentTime, workItem, customFieldConfig, customFieldProperties, Map.of(), null);
                                try {
                                    dbWorkItem = IssueMgmtTestUtil.upsertAndPopulateDbWorkItemIds(COMPANY, dbWorkItem, userIdentityService);
                                } catch (SQLException e) {
                                    log.warn("setup: error populting assignee and reporter Ids", e);
                                }
                                try {
                                    workItemService.insert(COMPANY, dbWorkItem.toBuilder().ingestedAt(ingestedAt).build());
                                } catch (SQLException e) {
                                    log.warn("setupAzureDevopsWorkItems: error inserting project: " + dbWorkItem.getId(), e);
                                }
                            });
                });
    }

    @Test
    public void testWorkItemAgeFilters() throws SQLException {
        var expectedResults = setupWorkItemAgeFiltersExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsAgeReport(COMPANY,
                    entry.getKey(), WorkItemsMilestoneFilter.builder().build(), null, true, null);
            String logger = "distinct: " + entry.getKey().getAcross() + " and calculation: " + entry.getKey().getCalculation();
            if (WorkItemsFilter.isAcrossUsers(entry.getKey().getAcross())) {
                List<DbAggregationResult> actualRecords = actualResponse.getRecords().stream().map(dbAggregationResult ->
                        dbAggregationResult.toBuilder().mean(null).build()).collect(Collectors.toList());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                compareAggResults(logger, entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupWorkItemAgeFiltersExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.assignee)
                .build();
        expectedResults.put(workItemsFilter1, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").median(85L).min(85L).max(85L).p90(85L).totalTickets(3L)
                        .totalStoryPoints(8L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").median(85L).min(85L).max(85L).p90(85L).totalTickets(1L)
                        .totalStoryPoints(0L).build()));
        WorkItemsFilter workItemsFilter2 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        expectedResults.put(workItemsFilter2, List.of(
                DbAggregationResult.builder().key("In Progress").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(3L).build(),
                DbAggregationResult.builder().key("Closed").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(2L).totalStoryPoints(2L).build(),
                DbAggregationResult.builder().key("Open").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(3L).build()));
        WorkItemsFilter workItemsFilter3 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.project)
                .build();
        expectedResults.put(workItemsFilter3, List.of(
                DbAggregationResult.builder().key("cgn-test/Agile-Project").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(4L).totalStoryPoints(8L).build()));
        WorkItemsFilter workItemsFilter4 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                .build();
        expectedResults.put(workItemsFilter4, List.of(
                DbAggregationResult.builder().key("61").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L)
                        .totalTickets(3L)
                        .totalStoryPoints(8L).build()));
        WorkItemsFilter workItemsFilter5 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.workitem_type)
                .build();
        expectedResults.put(workItemsFilter5, List.of(
                DbAggregationResult.builder().key("Task").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(2L).totalStoryPoints(5L).build(),
                DbAggregationResult.builder().key("Bug").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(3L).build(),
                DbAggregationResult.builder().key("Feature").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(0L).build()));
        WorkItemsFilter workItemsFilter6 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.priority)
                .build();
        expectedResults.put(workItemsFilter6, List.of(
                DbAggregationResult.builder().key("2").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(2L).totalStoryPoints(5L).build(),
                DbAggregationResult.builder().key("1").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(3L).build(),
                DbAggregationResult.builder().key("_UNPRIORITIZED_").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(0L).build()));
        WorkItemsFilter workItemsFilter7 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.epic)
                .build();
        expectedResults.put(workItemsFilter7, List.of(
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(4L).totalStoryPoints(8L).build()
        ));
        WorkItemsFilter workItemsFilter8 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.reporter)
                .build();
        expectedResults.put(workItemsFilter8, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io").median(85L).min(85L).max(85L)
                        .p90(85L).totalTickets(1L).totalStoryPoints(2L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").median(85L).min(85L).max(85L)
                        .p90(85L).totalTickets(3L).totalStoryPoints(6L).build()));
        WorkItemsFilter workItemsFilter9 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.resolution)
                .build();
        expectedResults.put(workItemsFilter9, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(4L).totalStoryPoints(8L).build()));
        WorkItemsFilter workItemsFilter10 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.version)
                .build();
        expectedResults.put(workItemsFilter10, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter11 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.fix_version)
                .build();
        expectedResults.put(workItemsFilter11, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter12 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                .build();
        expectedResults.put(workItemsFilter12, List.of(
                DbAggregationResult.builder().key("1620025200").additionalKey("3-5-2021").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(4L).totalStoryPoints(8L).build()));
        WorkItemsFilter workItemsFilter13 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.workitem_updated_at)
                .build();
        expectedResults.put(workItemsFilter13, List.of(
                DbAggregationResult.builder().key("1620284400").additionalKey("6-5-2021").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(1L).totalStoryPoints(8L).build(),
                DbAggregationResult.builder().key("1620370800").additionalKey("7-5-2021").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(3L).totalStoryPoints(6L).build())
        );
        WorkItemsFilter workItemsFilter14 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                .build();
        expectedResults.put(workItemsFilter14, List.of(
                DbAggregationResult.builder().key("1620370800").additionalKey("7-5-2021").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(2L).totalStoryPoints(3L).build()));
        WorkItemsFilter workItemsFilter15 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.age)
                .across(WorkItemsFilter.DISTINCT.trend)
                .build();
        expectedResults.put(workItemsFilter15, List.of(
                DbAggregationResult.builder().key("1627369200").additionalKey("27-7-2021").median(85L).min(85L).max(85L)
                        .mean(Double.valueOf("85")).p90(85L).totalTickets(4L).totalStoryPoints(8L).build()));
        return expectedResults;
    }

    @Test
    public void testAgeReportWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsAgeReport = workItemService.getWorkItemsAgeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.age)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.version, true, null);

        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .median(85L)
                        .min(85L)
                        .max(85L)
                        .mean(Double.valueOf("85"))
                        .p90(85L)
                        .totalTickets(4L)
                        .totalStoryPoints(8L).stacks(
                                List.of(
                                        DbAggregationResult.builder()
                                                .key(DbWorkItem.UNKNOWN)
                                                .additionalKey(DbWorkItem.UNKNOWN)
                                                .median(85L)
                                                .min(85L)
                                                .max(85L)
                                                .mean(Double.valueOf("85"))
                                                .p90(85L)
                                                .totalTickets(4L)
                                                .totalStoryPoints(8L)
                                                .build()
                                )).build()
        );
        verifyRecords(workItemsAgeReport.getRecords(), expected, true);
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
        Assert.assertEquals(e.getMin(), a.getMin());
        Assert.assertEquals(e.getMax(), a.getMax());
        Assert.assertEquals(Math.floor(e.getMean()), Math.floor(a.getMean()), 0.0);
        Assert.assertEquals(e.getP90(), a.getP90());
        Assert.assertEquals(e.getTotalTickets(), a.getTotalTickets());
        Assert.assertEquals(e.getTotalStoryPoints(), a.getTotalStoryPoints());
        verifyRecords(a.getStacks(), e.getStacks(), true);
    }
}