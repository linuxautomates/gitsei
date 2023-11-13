package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkItemsStageTimesReportServiceTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemTimelineService workItemTimelineService;
    private static WorkItemsReportService workItemsReportGenerator;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemsSprintMetricReportService workItemsSprintMetricReportService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static DataSource dataSource;
    private static String integrationId;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static Date currentTime;
    private static Long ingestedAt;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);

        workItemTimelineService = new WorkItemTimelineService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportGenerator = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        workItemService = new WorkItemsService(dataSource, workItemsReportGenerator, workItemsStageTimesReportService,
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
        issuesMilestoneService.ensureTableExistence(COMPANY);

        //read json
        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_items_2.json");
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

        currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        setUpWorkitems(enrichedProjectDataPaginatedResponse, customFieldConfig, customFieldProperties, dbWorkItems, currentTime);

        List<DbWorkItem> dbWorkItemsSecondSet = new ArrayList<>();
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        setUpWorkitems(enrichedProjectDataPaginatedResponse, customFieldConfig, customFieldProperties, dbWorkItemsSecondSet, previousTime);

        input = ResourceUtils.getResourceAsString("json/databases/azure_devops_workitem_history_2.json");
        PaginatedResponse<EnrichedProjectData> historyResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));

        String iterationsResourcePath = "json/databases/azure_devops_iterations_2.json";
        IssueMgmtTestUtil.setupIterations(COMPANY, integrationId, issuesMilestoneService, iterationsResourcePath);

        historyResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItemHistories().stream()
                            .filter(history -> history.getFields() != null && history.getFields().getChangedDate() != null)
                            .sorted(Comparator.comparing(history -> history.getFields().getChangedDate().getNewValue()))
                            .forEach(workItemHistory -> {
                                List<DbWorkItemHistory> dbWorkItemHistories = DbWorkItemHistory
                                        .fromAzureDevopsWorkItemHistories(integrationId, workItemHistory, new Date());
                                dbWorkItemHistories.forEach(dbWorkItemHistory -> {
                                    try {
                                        Optional<DbWorkItemHistory> lastEvent = workItemTimelineService
                                                .getLastEvent(COMPANY, Integer.valueOf(integrationId),
                                                        dbWorkItemHistory.getFieldType(),
                                                        dbWorkItemHistory.getWorkItemId());
                                        if (lastEvent.isPresent()) {
                                            String changedDate = workItemHistory.getFields().getChangedDate().getNewValue();
                                            DbWorkItemHistory lastEventUpdatedHistory = lastEvent.get().toBuilder()
                                                    .endDate(Timestamp.from(DateUtils.parseDateTime(changedDate)))
                                                    .build();

                                            workItemTimelineService.updateEndDate(COMPANY, lastEventUpdatedHistory);//update
                                        }
                                        workItemTimelineService.insert(COMPANY, dbWorkItemHistory);//insert except endTime.
                                    } catch (Exception ex) {
                                        log.warn("setupAzureDevopsWorkItems: error inserting project: " + workItemHistory.getId()
                                                + " for project id: " + project.getId(), ex);
                                    }
                                });
                            });
                }
        );
    }

    private static void setUpWorkitems(PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse,
                                       List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                       List<DbWorkItemField> customFieldProperties,
                                       List<DbWorkItem> dbWorkItems, Date fetchTime) {
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItems().forEach(
                            workItem -> {
                                DbWorkItem dbWorkItem = DbWorkItem.fromAzureDevOpsWorkItem("1", project,
                                        fetchTime, workItem, customFieldConfig, customFieldProperties, Map.of(), null);
                                try {
                                    dbWorkItem = IssueMgmtTestUtil.upsertAndPopulateDbWorkItemIds(COMPANY, dbWorkItem, userIdentityService);
                                } catch (SQLException e) {
                                    log.warn("setUpWorkitems: error populting assignee and reporter Ids", e);
                                }
                                dbWorkItems.add(dbWorkItem);
                            });
                });
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

    @Test
    public void testStageTimesReport() throws SQLException {
        var expectedResults = setupAcrossExpectedResults();
        WorkItemsFilter.WorkItemsFilterBuilder workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"));
        WorkItemsTimelineFilter workItemHistoryFilter = WorkItemsTimelineFilter.builder().integrationIds(List.of("1")).build();
        for (Map.Entry<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsStageTimesReport(COMPANY,
                    workItemsFilter.across(entry.getKey()).ingestedAt(ingestedAt).build(), workItemHistoryFilter, WorkItemsMilestoneFilter.builder().build(), null, true, null);
            List<DbAggregationResult> actualRecords = actualResponse.getRecords().stream().map(dbAggregationResult ->
                            dbAggregationResult.toBuilder().min(null).max(null).mean(null).median(null).build())
                    .collect(Collectors.toList());
            List<DbAggregationResult> expectedRecords = entry.getValue();
            DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
            Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
        }
    }

    private Map<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> setupAcrossExpectedResults() {
        Map<WorkItemsFilter.DISTINCT, List<DbAggregationResult>> expectedResults = new HashMap<>();
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_type, List.of(
                DbAggregationResult.builder().key("Issue").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("Issue").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("Task").totalTickets(3L).stage("To Do").build(),
                DbAggregationResult.builder().key("Epic").totalTickets(6L).stage("To Do").build(),
                DbAggregationResult.builder().key("Issue").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("Issue").totalTickets(8L).stage("To Do").build(),
                DbAggregationResult.builder().key("AgileTest").totalTickets(4L).stage("New").build(),
                DbAggregationResult.builder().key("Bug").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("Bug").totalTickets(10L).stage("New").build(),
                DbAggregationResult.builder().key("Epic").totalTickets(4L).stage("New").build(),
                DbAggregationResult.builder().key("Task").totalTickets(7L).stage("New").build(),
                DbAggregationResult.builder().key("Task").totalTickets(2L).stage("Closed").build(),
                DbAggregationResult.builder().key("User Story").totalTickets(9L).stage("New").build(),
                DbAggregationResult.builder().key("Task").totalTickets(3L).stage("Active").build(),
                DbAggregationResult.builder().key("Bug").totalTickets(3L).stage("Active").build(),
                DbAggregationResult.builder().key("User Story").totalTickets(3L).stage("Closed").build(),
                DbAggregationResult.builder().key("User Story").totalTickets(4L).stage("Active").build(),
                DbAggregationResult.builder().key("Epic").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("User Story").totalTickets(2L).stage("Resolved").build(),
                DbAggregationResult.builder().key("Bug").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("User Story").totalTickets(1L).stage("Removed").build(),
                DbAggregationResult.builder().key("Epic").totalTickets(1L).stage("Resolved").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_created_at, List.of(
                DbAggregationResult.builder().key("1618815600").additionalKey("19-4-2021").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("1622098800").additionalKey("27-5-2021").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("1622530800").additionalKey("1-6-2021").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("1623913200").additionalKey("17-6-2021").totalTickets(5L).stage("To Do").build(),
                DbAggregationResult.builder().key("1623999600").additionalKey("18-6-2021").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("1626332400").additionalKey("15-7-2021").totalTickets(6L).stage("To Do").build(),
                DbAggregationResult.builder().key("1626591600").additionalKey("18-7-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1626591600").additionalKey("18-7-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1626764400").additionalKey("20-7-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628146800").additionalKey("5-8-2021").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("1628406000").additionalKey("8-8-2021").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("1628665200").additionalKey("11-8-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1628751600").additionalKey("12-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628838000").additionalKey("13-8-2021").totalTickets(2L).stage("New").build(),
                DbAggregationResult.builder().key("1629097200").additionalKey("16-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1629183600").additionalKey("17-8-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1627196400").additionalKey("25-7-2021").totalTickets(4L).stage("New").build(),
                DbAggregationResult.builder().key("1622098800").additionalKey("27-5-2021").totalTickets(3L).stage("To Do").build(),
                DbAggregationResult.builder().key("1628665200").additionalKey("11-8-2021").totalTickets(7L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(2L).stage("Closed").build(),
                DbAggregationResult.builder().key("1627887600").additionalKey("2-8-2021").totalTickets(7L).stage("New").build(),
                DbAggregationResult.builder().key("1620025200").additionalKey("3-5-2021").totalTickets(2L).stage("Closed").build(),
                DbAggregationResult.builder().key("1620630000").additionalKey("10-5-2021").totalTickets(2L).stage("New").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(4L).stage("New").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(3L).stage("Active").build(),
                DbAggregationResult.builder().key("1628406000").additionalKey("8-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(2L).stage("New").build(),
                DbAggregationResult.builder().key("1620630000").additionalKey("10-5-2021").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("1629183600").additionalKey("17-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(2L).stage("Active").build(),
                DbAggregationResult.builder().key("1620630000").additionalKey("10-5-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1628406000").additionalKey("8-8-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(2L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1627887600").additionalKey("2-8-2021").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(1L).stage("Removed").build(),
                DbAggregationResult.builder().key("1620284400").additionalKey("6-5-2021").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("1620025200").additionalKey("3-5-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1620630000").additionalKey("10-5-2021").totalTickets(1L).stage("Resolved").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_updated_at, List.of(
                DbAggregationResult.builder().key("1621839600").additionalKey("24-5-2021").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("1622098800").additionalKey("27-5-2021").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("1622530800").additionalKey("1-6-2021").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("1623913200").additionalKey("17-6-2021").totalTickets(5L).stage("To Do").build(),
                DbAggregationResult.builder().key("1623999600").additionalKey("18-6-2021").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1626332400").additionalKey("15-7-2021").totalTickets(6L).stage("To Do").build(),
                DbAggregationResult.builder().key("1626591600").additionalKey("18-7-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1626678000").additionalKey("19-7-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1627196400").additionalKey("25-7-2021").totalTickets(3L).stage("New").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("1628665200").additionalKey("11-8-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1628838000").additionalKey("13-8-2021").totalTickets(2L).stage("New").build(),
                DbAggregationResult.builder().key("1629097200").additionalKey("16-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1629183600").additionalKey("17-8-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1622098800").additionalKey("27-5-2021").totalTickets(3L).stage("To Do").build(),
                DbAggregationResult.builder().key("1628665200").additionalKey("11-8-2021").totalTickets(7L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(3L).stage("Closed").build(),
                DbAggregationResult.builder().key("1627282800").additionalKey("26-7-2021").totalTickets(2L).stage("New").build(),
                DbAggregationResult.builder().key("1627887600").additionalKey("2-8-2021").totalTickets(4L).stage("New").build(),
                DbAggregationResult.builder().key("1628751600").additionalKey("12-8-2021").totalTickets(3L).stage("Closed").build(),
                DbAggregationResult.builder().key("1627282800").additionalKey("26-7-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1628751600").additionalKey("12-8-2021").totalTickets(7L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(3L).stage("New").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("1629183600").additionalKey("17-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(3L).stage("Active").build(),
                DbAggregationResult.builder().key("1625036400").additionalKey("30-6-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1627887600").additionalKey("2-8-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1628751600").additionalKey("12-8-2021").totalTickets(2L).stage("Active").build(),
                DbAggregationResult.builder().key("1628751600").additionalKey("12-8-2021").totalTickets(2L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1625036400").additionalKey("30-6-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1625036400").additionalKey("30-6-2021").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1627282800").additionalKey("26-7-2021").totalTickets(1L).stage("Removed").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("Resolved").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.workitem_resolved_at, List.of(
                DbAggregationResult.builder().key("1621839600").additionalKey("24-5-2021").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(3L).stage("Closed").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(3L).stage("New").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("1627887600").additionalKey("2-8-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1628578800").additionalKey("10-8-2021").totalTickets(3L).stage("Active").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("1629356400").additionalKey("19-8-2021").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1624345200").additionalKey("22-6-2021").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1627887600").additionalKey("2-8-2021").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("1620370800").additionalKey("7-5-2021").totalTickets(2L).stage("Closed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.epic, List.of(
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(34L).stage("New").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key("_UNKNOWN_").totalTickets(1L).stage("Removed").build()

        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.parent_workitem_id, List.of(
                DbAggregationResult.builder().key("111").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("112").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("115").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("136").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("36").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("136").totalTickets(3L).stage("New").build(),
                DbAggregationResult.builder().key("90").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("90").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("126").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("107").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("107").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("90").totalTickets(1L).stage("Removed").build(),
                DbAggregationResult.builder().key("107").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("126").totalTickets(1L).stage("Resolved").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.ticket_category, List.of(
                DbAggregationResult.builder().key("Other").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("Other").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("Other").totalTickets(34L).stage("New").build(),
                DbAggregationResult.builder().key("Other").totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key("Other").totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key("Other").totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key("Other").totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key("Other").totalTickets(1L).stage("Removed").build()
        ));
        final Long ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        final String key = String.valueOf(ingestedAt);
        String addlnKey = new SimpleDateFormat("d-M-yyyy").format(currentTime);
        expectedResults.put(WorkItemsFilter.DISTINCT.trend, List.of(
                DbAggregationResult.builder().key(key).totalTickets(1L).additionalKey(addlnKey).stage("Done").build(),
                DbAggregationResult.builder().key(key).totalTickets(1L).additionalKey(addlnKey).stage("Doing").build(),
                DbAggregationResult.builder().key(key).totalTickets(17L).additionalKey(addlnKey).stage("To Do").build(),
                DbAggregationResult.builder().key(key).totalTickets(7L).additionalKey(addlnKey).stage("Closed").build(),
                DbAggregationResult.builder().key(key).totalTickets(34L).additionalKey(addlnKey).stage("New").build(),
                DbAggregationResult.builder().key(key).totalTickets(11L).additionalKey(addlnKey).stage("Active").build(),
                DbAggregationResult.builder().key(key).totalTickets(4L).additionalKey(addlnKey).stage("Resolved").build(),
                DbAggregationResult.builder().key(key).totalTickets(1L).additionalKey(addlnKey).stage("Removed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.project, List.of(
                DbAggregationResult.builder().key("cgn-test/project-test-11").stage("Done").totalTickets(1L).build(),
                DbAggregationResult.builder().key("cgn-test/project-test-11").stage("Doing").totalTickets(1L).build(),
                DbAggregationResult.builder().key("cgn-test/project-test-11").stage("To Do").totalTickets(10L).build(),
                DbAggregationResult.builder().key("cgn-test/project-test-9").stage("To Do").totalTickets(6L).build(),
                DbAggregationResult.builder().key("cgn-test/tfvc-project-2").stage("Active").totalTickets(1L).build(),
                DbAggregationResult.builder().key("cgn-test/tfvc-project-2").stage("New").totalTickets(1L).build(),
                DbAggregationResult.builder().key("cgn-test/project-test-5").stage("To Do").totalTickets(1L).build(),
                DbAggregationResult.builder().key("cgn-test/Agile-Project").stage("New").totalTickets(33L).build(),
                DbAggregationResult.builder().key("cgn-test/Agile-Project").stage("Closed").totalTickets(7L).build(),
                DbAggregationResult.builder().key("cgn-test/Agile-Project").stage("Active").totalTickets(10L).build(),
                DbAggregationResult.builder().key("cgn-test/Agile-Project").stage("Resolved").totalTickets(4L).build(),
                DbAggregationResult.builder().key("cgn-test/Agile-Project").stage("Removed").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.assignee, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "praveen@cognitree.com"))
                        .additionalKey("praveen@cognitree.com").stage("Done").totalTickets(1L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").stage("Doing").totalTickets(1L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").stage("To Do").totalTickets(15L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "gaurav@cognitree.com"))
                        .additionalKey("gaurav@cognitree.com").stage("To Do").totalTickets(1L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "praveen@cognitree.com"))
                        .additionalKey("praveen@cognitree.com").stage("To Do").totalTickets(1L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").stage("New").totalTickets(17L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "praveen@cognitree.com"))
                        .additionalKey("praveen@cognitree.com").stage("New").totalTickets(6L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "gaurav@cognitree.com"))
                        .additionalKey("gaurav@cognitree.com").stage("New").totalTickets(4L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").stage("Closed").totalTickets(2L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "praveen@cognitree.com"))
                        .additionalKey("praveen@cognitree.com").stage("Active").totalTickets(2L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").stage("Closed").totalTickets(3L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").stage("Active").totalTickets(6L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "praveen@cognitree.com"))
                        .additionalKey("praveen@cognitree.com").stage("Closed").totalTickets(2L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").stage("New").totalTickets(7L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "gaurav@cognitree.com"))
                        .additionalKey("gaurav@cognitree.com").stage("Active").totalTickets(2L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").stage("Active").totalTickets(1L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar").stage("Resolved").totalTickets(2L).build(),
                DbAggregationResult.builder().additionalKey("_UNASSIGNED_").stage("Resolved").totalTickets(1L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "gaurav@cognitree.com"))
                        .additionalKey("gaurav@cognitree.com").stage("Resolved").totalTickets(1L).build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "gaurav@cognitree.com"))
                        .additionalKey("gaurav@cognitree.com").stage("Removed").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.component, List.of());
        expectedResults.put(WorkItemsFilter.DISTINCT.fix_version, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(34L).stage("New").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(1L).stage("Removed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.label, List.of(
                DbAggregationResult.builder().key("TestTag").totalTickets(2L).stage("To Do").build(),
                DbAggregationResult.builder().key("sample tag").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("TestTag").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("sample tag").stage("New").totalTickets(2L).build(),
                DbAggregationResult.builder().key("TestTag").stage("New").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sample tag 2").stage("New").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sample tag").stage("Active").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sample tag 2").stage("Active").totalTickets(1L).build(),
                DbAggregationResult.builder().key("TestTag").stage("Removed").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sample tag").stage("Resolved").totalTickets(1L).build(),
                DbAggregationResult.builder().key("sample tag 2").stage("Resolved").totalTickets(1L).build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.priority, List.of(
                DbAggregationResult.builder().key("2").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("2").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("2").totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key("_UNPRIORITIZED_").totalTickets(4L).stage("New").build(),
                DbAggregationResult.builder().key("2").totalTickets(29L).stage("New").build(),
                DbAggregationResult.builder().key("2").totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key("2").totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key("2").totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key("3").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("2").totalTickets(1L).stage("Removed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.reporter, List.of(
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(34L).stage("New").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io").totalTickets(1L).stage("Removed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.resolution, List.of(
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(34L).stage("New").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key(DbWorkItem.UNKNOWN).totalTickets(1L).stage("Removed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.status, List.of(
                DbAggregationResult.builder().key("Done").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("Doing").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("To Do").totalTickets(17L).stage("To Do").build(),
                DbAggregationResult.builder().key("Closed").totalTickets(7L).stage("Closed").build(),
                DbAggregationResult.builder().key("New").totalTickets(34L).stage("New").build(),
                DbAggregationResult.builder().key("Active").totalTickets(11L).stage("Active").build(),
                DbAggregationResult.builder().key("Resolved").totalTickets(4L).stage("Resolved").build(),
                DbAggregationResult.builder().key("Removed").totalTickets(1L).stage("Removed").build()
        ));
        expectedResults.put(WorkItemsFilter.DISTINCT.status_category, List.of(
                DbAggregationResult.builder().key("Done").totalTickets(1L).stage("Done").build(),
                DbAggregationResult.builder().key("Doing").totalTickets(1L).stage("Doing").build(),
                DbAggregationResult.builder().key("To Do").totalTickets(16L).stage("To Do").build(),
                DbAggregationResult.builder().key("Active").totalTickets(4L).stage("Active").build(),
                DbAggregationResult.builder().key("Closed").totalTickets(5L).stage("Closed").build(),
                DbAggregationResult.builder().key("Active").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("New").totalTickets(25L).stage("New").build(),
                DbAggregationResult.builder().key("Resolved").totalTickets(3L).stage("New").build(),
                DbAggregationResult.builder().key("Removed").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("Removed").totalTickets(1L).stage("New").build(),
                DbAggregationResult.builder().key("Closed").totalTickets(3L).stage("New").build(),
                DbAggregationResult.builder().key("Resolved").totalTickets(1L).stage("Closed").build(),
                DbAggregationResult.builder().key("Active").totalTickets(2L).stage("New").build(),
                DbAggregationResult.builder().key("Doing").totalTickets(1L).stage("To Do").build(),
                DbAggregationResult.builder().key("Closed").totalTickets(3L).stage("Active").build(),
                DbAggregationResult.builder().key("New").totalTickets(2L).stage("Active").build(),
                DbAggregationResult.builder().key("New").totalTickets(1L).stage("Resolved").build(),
                DbAggregationResult.builder().key("Resolved").totalTickets(1L).stage("Active").build(),
                DbAggregationResult.builder().key("Resolved").totalTickets(3L).stage("Resolved").build(),
                DbAggregationResult.builder().key("Removed").totalTickets(1L).stage("Removed").build()
        ));
        return expectedResults;
    }

    @Test
    public void testCustomAndAttributes() throws SQLException {
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.attribute)
                        .attributeAcross("code_area")
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(21);
        Assertions.assertThat(workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.custom_field)
                        .customAcross("Custom.TestCustomField1")
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                null, false, null).getTotalCount()).isEqualTo(2);
    }


    @Test
    public void testStack() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .acrossLimit(1)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.status, false, null);
        DefaultObjectMapper.prettyPrint(result.getRecords());

        List<DbAggregationResult> expected = List.of(
                DbAggregationResult.builder()
                        .key("2")
                        .stage("Done")
                        .totalTickets(1L)
                        .stacks(
                                List.of(
                                        DbAggregationResult.builder()
                                                .key("Done")
                                                .totalTickets(1L)
                                                .stage("Done")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Doing")
                                                .totalTickets(1L)
                                                .stage("Doing")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("To Do")
                                                .totalTickets(16L)
                                                .stage("To Do")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Active")
                                                .totalTickets(4L)
                                                .stage("Active")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Closed")
                                                .totalTickets(5L)
                                                .stage("Closed")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("New")
                                                .totalTickets(20L)
                                                .stage("New")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Active")
                                                .totalTickets(1L)
                                                .stage("Closed")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Resolved")
                                                .totalTickets(3L)
                                                .stage("New")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Removed")
                                                .totalTickets(1L)
                                                .stage("Active")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Removed")
                                                .totalTickets(1L)
                                                .stage("New")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Closed")
                                                .totalTickets(3L)
                                                .stage("New")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Resolved")
                                                .totalTickets(1L)
                                                .stage("Closed")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Active")
                                                .totalTickets(2L)
                                                .stage("New")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Doing")
                                                .totalTickets(1L)
                                                .stage("To Do")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Closed")
                                                .totalTickets(3L)
                                                .stage("Active")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("New")
                                                .totalTickets(2L)
                                                .stage("Active")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("New")
                                                .totalTickets(1L)
                                                .stage("Resolved")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Resolved")
                                                .totalTickets(1L)
                                                .stage("Active")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Resolved")
                                                .totalTickets(3L)
                                                .stage("Resolved")
                                                .build(),
                                        DbAggregationResult.builder()
                                                .key("Removed")
                                                .totalTickets(1L)
                                                .stage("Removed")
                                                .build()
                                )).build()
        );
        verifyRecords(result.getRecords(), expected, true);
    }

    @Test
    public void testAggByStoryPoints() throws SQLException {
        DbListResponse<DbAggregationResult> sprintMetricsReport = workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(sprintMetricsReport.getTotalCount()).isEqualTo(18);
    }

    @Test
    public void testSlaTimeFields() throws SQLException {
        DbListResponse<DbAggregationResult> stageTimesReport = workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time))
                        .across(WorkItemsFilter.DISTINCT.none)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(stageTimesReport.getTotalCount()).isEqualTo(8);

        stageTimesReport = workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                        .across(WorkItemsFilter.DISTINCT.none)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(stageTimesReport.getTotalCount()).isEqualTo(8);

        stageTimesReport = workItemService.getWorkItemsStageTimesReport(COMPANY,
                WorkItemsFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time))
                        .across(WorkItemsFilter.DISTINCT.sprint)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsTimelineFilter.builder().build(),
                WorkItemsMilestoneFilter.builder().build(), null, false, null);
        assertThat(stageTimesReport.getTotalCount()).isEqualTo(0);
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
        Assert.assertEquals(a.getStage(), e.getStage());
        Assert.assertEquals(e.getTotalTickets(), a.getTotalTickets());
        verifyRecords(a.getStacks(), e.getStacks(), true);
    }
}
