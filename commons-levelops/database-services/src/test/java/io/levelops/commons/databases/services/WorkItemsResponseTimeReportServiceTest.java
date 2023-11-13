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
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.WorkItemTestUtils.compareAggResults;

@Log4j2
@SuppressWarnings("unused")
public class WorkItemsResponseTimeReportServiceTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static String integrationId;
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
        integrationId = integrationService.insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azure test")
                .status("enabled")
                .build());
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
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
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);

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
    }

    @Test
    public void testWorkItemReponseTimeFilters() throws SQLException {
        var expectedResults = setupWorkItemReponseTimeFiltersExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsReponseTimeReport(COMPANY,
                    entry.getKey().toBuilder().ingestedAt(ingestedAt).build(),WorkItemsMilestoneFilter.builder().build(),null,  true, null);
            System.out.println(entry.getKey() + " -> " + actualResponse);
            String logger = "distinct: " + entry.getKey().getAcross() + " and calculation: " + entry.getKey().getCalculation();
            if (WorkItemsFilter.isAcrossUsers(entry.getKey().getAcross())) {
                List<DbAggregationResult> actualRecords = actualResponse.getRecords().stream().map(dbAggregationResult ->
                                dbAggregationResult.toBuilder().min(null).max(null).mean(null).median(null).build())
                        .collect(Collectors.toList());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                compareAggResults(logger, entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupWorkItemReponseTimeFiltersExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.assignee)
                .build();
        expectedResults.put(workItemsFilter1, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").totalTickets(3L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter2 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        expectedResults.put(workItemsFilter2, List.of(
                DbAggregationResult.builder().key("In Progress").median(9325750L).min(9325750L).max(9325750L)
                        .totalTickets(1L).build(),
                DbAggregationResult.builder().key("Closed").median(9325734L).min(9325734L).max(9325772L)
                        .totalTickets(2L).build(),
                DbAggregationResult.builder().key("Open").median(9325734L).min(9325734L).max(9325734L)
                        .totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter3 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.project)
                .build();
        expectedResults.put(workItemsFilter3, List.of(
                DbAggregationResult.builder().key("cgn-test/Agile-Project").median(9325978L).min(9325978L).max(9326016L)
                        .totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter4 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                .build();
        expectedResults.put(workItemsFilter4, List.of(
                DbAggregationResult.builder().key("61").median(9426563L).min(9426563L).max(9426563L)
                        .totalTickets(3L).build()));
        WorkItemsFilter workItemsFilter5 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.workitem_type)
                .build();
        expectedResults.put(workItemsFilter5, List.of(
                DbAggregationResult.builder().key("Task").median(9326228L).min(9326228L).max(9326251L)
                        .totalTickets(2L).build(),
                DbAggregationResult.builder().key("Bug").median(9326213L).min(9326213L).max(9326213L)
                        .totalTickets(1L).build(),
                DbAggregationResult.builder().key("Feature").median(9326213L).min(9326213L).max(9326213L)
                        .totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter6 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.priority)
                .build();
        expectedResults.put(workItemsFilter6, List.of(
                DbAggregationResult.builder().key("2").median(9326413L).min(9326413L).max(9326435L)
                        .totalTickets(2L).build(),
                DbAggregationResult.builder().key("1").median(9326397L).min(9326397L).max(9326397L)
                        .totalTickets(1L).build(),
                DbAggregationResult.builder().key("_UNPRIORITIZED_").median(9326397L).min(9326397L).max(9326397L)
                        .totalTickets(1L).build()));
        WorkItemsFilter workItemsFilter7 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.epic)
                .build();
        expectedResults.put(workItemsFilter7, List.of(
                DbAggregationResult.builder().key("_UNKNOWN_").median(9329532L).min(9329532L).max(9329570L)
                        .totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter8 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.reporter)
                .build();
        expectedResults.put(workItemsFilter8, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io").totalTickets(1L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(3L).build()));
        WorkItemsFilter workItemsFilter9 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.resolution)
                .build();
        expectedResults.put(workItemsFilter9, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).median(9691217L).min(9691217L).max(9691217L)
                        .totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter10 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.version)
                .build();
        expectedResults.put(workItemsFilter10, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter11 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.fix_version)
                .build();
        expectedResults.put(workItemsFilter11, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter12 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                .build();
        expectedResults.put(workItemsFilter12, List.of(
                DbAggregationResult.builder().key("1620025200").additionalKey("3-5-2021").median(9327524L).min(9327524L).max(9327562L)
                        .totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter13 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.workitem_updated_at)
                .build();
        expectedResults.put(workItemsFilter13, List.of(
                DbAggregationResult.builder().key("1620284400").additionalKey("6-5-2021").median(9329729L).min(9329729L).max(9329729L)
                        .totalTickets(1L).build(),
                DbAggregationResult.builder().key("1620370800").additionalKey("7-5-2021").median(9327770L).min(9327770L).max(9327786L)
                        .totalTickets(3L).build()));
        WorkItemsFilter workItemsFilter14 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                .build();
        expectedResults.put(workItemsFilter14, List.of(
                DbAggregationResult.builder().key("1620370800").additionalKey("7-5-2021").median(9329232L).min(9329232L).max(9329254L)
                        .totalTickets(2L).build()));
        WorkItemsFilter workItemsFilter15 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.trend)
                .build();
        expectedResults.put(workItemsFilter15, List.of(
                DbAggregationResult.builder().key("1627369200").additionalKey("27-7-2021").median(9328263L).min(9328263L).max(9328301L)
                        .totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter16 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.trend)
                .build();
        expectedResults.put(workItemsFilter16, List.of(
                DbAggregationResult.builder()
                        .key(String.valueOf(ingestedAt))
                        .additionalKey(new SimpleDateFormat("d-M-yyyy").format(currentTime))
                        .median(9328263L).min(9328263L).max(9328301L)
                        .totalTickets(4L).build()));
        WorkItemsFilter workItemsFilter17 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.response_time)
                .across(WorkItemsFilter.DISTINCT.story_points)
                .build();
        expectedResults.put(workItemsFilter17, List.of(
                DbAggregationResult.builder().key("2.5").totalTickets(1L).build(),
                DbAggregationResult.builder().key("0").totalTickets(1L).build(),
                DbAggregationResult.builder().key("3").totalTickets(2L).build()));
        return expectedResults;
    }

    @Test
    public void testAgeReportWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsResponseTimeReport = workItemService.getWorkItemsReponseTimeReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.response_time)
                        .across(WorkItemsFilter.DISTINCT.workitem_type)
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.project, false, null);
        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder()
                        .key("Task")
                        .median(9422395L)
                        .min(9422395L)
                        .max(9419886L)
                        .totalTickets(2L)
                        .stacks(
                        List.of(
                                DbAggregationResult.builder()
                                        .key("cgn-test/Agile-Project")
                                        .additionalKey("Task")
                                        .median(9422395L)
                                        .min(9422395L)
                                        .max(9419886L)
                                        .totalTickets(2L)
                                        .build()
                        )).build(),
                DbAggregationResult.builder()
                        .key("Bug")
                        .median(9419847L)
                        .min(9419847L)
                        .max(9419847L)
                        .totalTickets(1L)
                        .stacks(
                        List.of(
                                DbAggregationResult.builder()
                                        .key("cgn-test/Agile-Project")
                                        .additionalKey("Bug")
                                        .median(9419847L)
                                        .min(9419847L)
                                        .max(9419847L)
                                        .totalTickets(1L)
                                        .build()
                        )).build(),
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .median(9419847L)
                        .min(9419847L)
                        .max(9419847L)
                        .totalTickets(1L)
                        .stacks(
                        List.of(
                                DbAggregationResult.builder()
                                        .key("cgn-test/Agile-Project")
                                        .additionalKey("_UNKNOWN_")
                                        .median(9419847L)
                                        .min(9419847L)
                                        .max(9419847L)
                                        .totalTickets(1L)
                                        .build()
                        )).build()
        );
        verifyRecords(workItemsResponseTimeReport.getRecords(), expected, true);
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
        Assert.assertEquals(a.getCount(), e.getCount());
        Assert.assertEquals(a.getSum(), e.getSum());
        Assert.assertEquals(a.getTotalTickets(), e.getTotalTickets());
        verifyRecords(a.getStacks(), e.getStacks(), true);
    }
}
