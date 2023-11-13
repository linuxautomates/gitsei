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
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.WorkItemTestUtils.compareAggResults;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkItemsStageBounceReportServiceTest {

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
        WorkItemsStageBounceReportService workItemsStageBounceReportService = new WorkItemsStageBounceReportService(dataSource, workItemFieldsMetaService);
        IssuesMilestoneService issuesMilestoneService = new IssuesMilestoneService(dataSource);
        IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, workItemsStageBounceReportService);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        issuesMilestoneService.ensureTableExistence(COMPANY);

        String integrationId = integrationService.insert(COMPANY, Integration.builder()
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
                workItemTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, previousTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        String historiesResourcePath = "json/databases/azure_devops_workitem_history_2.json";
        IssueMgmtTestUtil.setupHistories(COMPANY, "1", workItemTimelineService, historiesResourcePath);
    }

    @Test
    public void testStageBounceReport() throws SQLException {
        var expectedResults = setupWorkItemResolutionTimeExpectedResults();
        for (Map.Entry<WorkItemsFilter, List<DbAggregationResult>> entry : expectedResults.entrySet()) {
            DbListResponse<DbAggregationResult> actualResponse = workItemService.getWorkItemsStageBounceReport(COMPANY,
                    entry.getKey().toBuilder().ingestedAt(ingestedAt)
                            .stages(List.of("To Do", "Active", "Done", "Removed", "In Progress", "New", "Resolved", "Open", "Closed", "Doing"))
                            .build(),
                    WorkItemsMilestoneFilter.builder().build(), null, true, null);
            String logger = "distinct: " + entry.getKey().getAcross() + " and calculation: " + entry.getKey().getCalculation();
            if (WorkItemsFilter.isAcrossUsers(entry.getKey().getAcross())) {
                List<DbAggregationResult> actualRecords = new ArrayList<>(actualResponse.getRecords());
                List<DbAggregationResult> expectedRecords = entry.getValue();
                DbAggregationResult[] results = expectedRecords.toArray(new DbAggregationResult[actualRecords.size()]);
                Assertions.assertThat(actualRecords).containsExactlyInAnyOrder(results);
            } else {
                compareAggResults(logger, entry.getValue(), actualResponse.getRecords());
            }
        }
    }

    private static Map<WorkItemsFilter, List<DbAggregationResult>> setupWorkItemResolutionTimeExpectedResults() {
        Map<WorkItemsFilter, List<DbAggregationResult>> expectedResults = new HashMap<>();
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.priority)
                .build();
        expectedResults.put(workItemsFilter1, List.of(
                DbAggregationResult.builder()
                        .key("_UNPRIORITIZED_")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter2 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.project)
                .build();
        expectedResults.put(workItemsFilter2, List.of(
                DbAggregationResult.builder()
                        .key("cgn-test/Agile-Project")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("cgn-test/Agile-Project")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("cgn-test/Agile-Project")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter3 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.status)
                .build();
        expectedResults.put(workItemsFilter3, List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter4 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.assignee)
                .build();
        expectedResults.put(workItemsFilter4, List.of(
                DbAggregationResult.builder()
                        .additionalKey("_UNASSIGNED_")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter5 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.workitem_type)
                .build();
        expectedResults.put(workItemsFilter5, List.of(
                DbAggregationResult.builder()
                        .key("Feature")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Bug")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Task")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Task")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter6 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.label)
                .build();
        expectedResults.put(workItemsFilter6, List.of(
                DbAggregationResult.builder()
                        .key("sampleTag")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("tag")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter7 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.fix_version)
                .build();
        expectedResults.put(workItemsFilter7, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("Closed")
                        .totalTickets(2L)
                        .mean(Double.valueOf("1.5"))
                        .median(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("In Progress")
                        .totalTickets(1L)
                        .mean(Double.valueOf("1.0"))
                        .median(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("Open")
                        .totalTickets(1L)
                        .mean(Double.valueOf("1.0"))
                        .median(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter8 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.component)
                .build();
        expectedResults.put(workItemsFilter8, List.of());
        WorkItemsFilter workItemsFilter9 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.version)
                .build();
        expectedResults.put(workItemsFilter9, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("Closed")
                        .totalTickets(2L)
                        .mean(Double.valueOf("1.5"))
                        .median(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("In Progress")
                        .totalTickets(1L)
                        .mean(Double.valueOf("1.0"))
                        .median(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("Open")
                        .totalTickets(1L)
                        .mean(Double.valueOf("1.0"))
                        .median(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter10 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.reporter)
                .build();
        expectedResults.put(workItemsFilter10, List.of(
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "srinath.chandrashekhar@levelops.io"))
                        .additionalKey("srinath.chandrashekhar@levelops.io")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(userIdentityService.getUser(COMPANY, "1", "user2@levelops.io"))
                        .additionalKey("user2@levelops.io")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter11 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.status_category)
                .build();
        expectedResults.put(workItemsFilter11, List.of(
                DbAggregationResult.builder()
                        .key("Closed")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("In Progress")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Open")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter12 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.resolution)
                .build();
        expectedResults.put(workItemsFilter12, List.of(
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(DbWorkItem.UNKNOWN)
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter13 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.attribute)
                .attributeAcross("code_area")
                .build();
        expectedResults.put(workItemsFilter13, List.of(
                DbAggregationResult.builder()
                        .key("Agile-Project-2\\agile-team-3")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Agile-Project\\agile-team-2")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Agile-Project\\agile-team-2")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Agile-Project\\agile-team-2")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter14 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.custom_field)
                .customAcross("Custom.TestCustomField1")
                .build();
        expectedResults.put(workItemsFilter14, List.of(
                DbAggregationResult.builder()
                        .key("2")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter15 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.epic)
                .build();
        expectedResults.put(workItemsFilter15, List.of(
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("_UNKNOWN_")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));

        WorkItemsFilter workItemsFilter16 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.parent_workitem_id)
                .build();
        expectedResults.put(workItemsFilter16, List.of(
                DbAggregationResult.builder()
                        .key("61")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("61")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("61")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter17 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.ticket_category)
                .build();
        expectedResults.put(workItemsFilter17, List.of(
                DbAggregationResult.builder()
                        .key("Other")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Other")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("Other")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter18 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.trend)
                .build();
        expectedResults.put(workItemsFilter18, List.of(
                DbAggregationResult.builder()
                        .key(String.valueOf(ingestedAt))
                        .additionalKey(new SimpleDateFormat("d-M-yyyy").format(currentTime))
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key(String.valueOf(ingestedAt))
                        .additionalKey(new SimpleDateFormat("d-M-yyyy").format(currentTime))
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(String.valueOf(ingestedAt))
                        .additionalKey(new SimpleDateFormat("d-M-yyyy").format(currentTime))
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter19 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                .build();
        expectedResults.put(workItemsFilter19, List.of(
                DbAggregationResult.builder()
                        .key("1620025200")
                        .additionalKey("3-5-2021")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.5"))
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620025200")
                        .additionalKey("3-5-2021")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620025200")
                        .additionalKey("3-5-2021")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter20 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.workitem_updated_at)
                .build();
        expectedResults.put(workItemsFilter20, List.of(
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620284400")
                        .additionalKey("6-5-2021")
                        .stage("Closed")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .stage("In Progress")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        WorkItemsFilter workItemsFilter21 = WorkItemsFilter.builder()
                .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                .build();
        expectedResults.put(workItemsFilter21, List.of(
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .stage("Closed")
                        .median(2L)
                        .mean(Double.valueOf("2.0"))
                        .totalTickets(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("1620370800")
                        .additionalKey("7-5-2021")
                        .stage("Open")
                        .median(1L)
                        .mean(Double.valueOf("1.0"))
                        .totalTickets(1L)
                        .build()
        ));
        return expectedResults;
    }

    @Test
    public void testStageBounceReportWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsStageBounceReport = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .stages(List.of("Closed", "Open"))
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsStageBounceReport).isNotNull();
        assertThat(workItemsStageBounceReport.getTotalCount()).isEqualTo(2);
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("_UNKNOWN_"));
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .isEqualTo(List.of("Closed"));

        workItemsStageBounceReport = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .stages(List.of("Closed", "Open"))
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.status)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsStageBounceReport).isNotNull();
        assertThat(workItemsStageBounceReport.getTotalCount()).isEqualTo(2);
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("_UNKNOWN_"));
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .isEqualTo(List.of("Closed"));

        workItemsStageBounceReport = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .stages(List.of("Closed", "Open"))
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsStageBounceReport).isNotNull();
        assertThat(workItemsStageBounceReport.getTotalCount()).isEqualTo(2);
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("_UNKNOWN_"));
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .isEqualTo(List.of("Closed"));
    }

    @Test
    public void testExcludeStagesWithStack() throws SQLException {
        DbListResponse<DbAggregationResult> workItemsStageBounceReport = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .excludeStages(List.of("Closed", "Open"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsStageBounceReport).isNotNull();
        assertThat(workItemsStageBounceReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("_UNKNOWN_"));
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .isEqualTo(List.of("In Progress"));

        workItemsStageBounceReport = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.status)
                        .excludeStages(List.of("Closed", "Open"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsStageBounceReport).isNotNull();
        assertThat(workItemsStageBounceReport.getTotalCount()).isEqualTo(1);
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("_UNKNOWN_"));
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .isEqualTo(List.of("In Progress"));

        workItemsStageBounceReport = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                        .excludeStages(List.of("In Progress"))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        assertThat(workItemsStageBounceReport).isNotNull();
        assertThat(workItemsStageBounceReport.getTotalCount()).isEqualTo(2);
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("_UNKNOWN_"));
        assertThat(workItemsStageBounceReport.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getStage).collect(Collectors.toList()))
                .isEqualTo(List.of("Closed"));
    }

    @Test
    public void testStoryPointsAgg() throws SQLException {
        DbListResponse<DbAggregationResult> result = workItemService.getWorkItemsStageBounceReport(COMPANY,
                WorkItemsFilter.builder()
                        .stages(List.of("Closed"))
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .ingestedAt(ingestedAt)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.version, true, null);
        List<DbAggregationResult> expectedResult = List.of(
                DbAggregationResult.builder()
                        .key("0")
                        .stage("Closed")
                        .median(2L)
                        .mean(2.0)
                        .totalTickets(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("2.5")
                        .stage("Closed")
                        .median(1L)
                        .mean(1.0)
                        .totalTickets(2L)
                        .build()
        );
        compareAggResults("test Story points aff", expectedResult, result.getRecords());
    }

    @Test
    public void testDrillDown() throws SQLException {
        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .stages(List.of("Open"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> listResponse = workItemService.listByFilter(COMPANY,
                workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("stages", listResponse, 1, 1, Set.of("69"));

        workItemsFilter = WorkItemsFilter.builder()
                .projects(List.of("cgn-test/Agile-Project"))
                .excludeStages(List.of("Open", "Closed"))
                .ingestedAt(ingestedAt)
                .build();
        listResponse = workItemService.listByFilter(COMPANY,
                workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("exclude stages", listResponse, 1, 1, Set.of("68"));

        workItemsFilter = WorkItemsFilter.builder()
                .integrationIds(List.of("1"))
                .ingestedAt(ingestedAt)
                .excludeStages(List.of("Closed"))
                .build();
        listResponse = workItemService.listByFilter(COMPANY,
                workItemsFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("exclude stages", listResponse, 2, 2, Set.of("68", "69"));
    }

    private void assertListResponse(String filter, DbListResponse<DbWorkItem> actualResponse,
                                    int total, int pageCount, Set<String> ids) {
        assertThat(actualResponse.getTotalCount()).as("Total count - " + filter).isEqualTo(total);
        assertThat(actualResponse.getCount()).as("Page count - " + filter).isEqualTo(pageCount);
        assertThat(getIds(actualResponse)).as("WorkItem IDs - " + filter).isEqualTo(ids);
    }

    private static void compareAggResults(String testDesc, List<DbAggregationResult> expectedList,
                                          List<DbAggregationResult> actualList) {
        log.debug("compareAggResultsForAcross: Comparing {} actual results against" +
                " {} expected results for {}", actualList.size(), expectedList.size(), testDesc);
        final Iterator<DbAggregationResult> expectedIter = expectedList.iterator();
        final Iterator<DbAggregationResult> actualIter = actualList.iterator();
        while (expectedIter.hasNext() && actualIter.hasNext()) {
            DbAggregationResult expected = expectedIter.next();
            DbAggregationResult actual = actualIter.next();
            log.info("compareAggResultsForAcross: Comparing actual value {} " +
                    "with expected value {} for fields {}", actual, expected, testDesc);
            assertThat(actual.getKey()).as("key for " + testDesc).isEqualTo(expected.getKey());
            assertThat(actual.getStage()).as("stage for " + testDesc).isEqualTo(expected.getStage());
            assertThat(actual.getAdditionalKey()).as("additional key " + testDesc).isEqualTo(expected.getAdditionalKey());
            assertThat(actual.getCount()).as("count for " + testDesc).isEqualTo(expected.getCount());
            assertThat(actual.getMean()).as("mean for " + testDesc).isEqualTo(expected.getMean());
            assertThat(actual.getMedian()).as("median for " + testDesc).isEqualTo(expected.getMedian());
        }
        assertThat((actualIter.hasNext() || expectedIter.hasNext())).as("total count for " + testDesc).isFalse();
    }

    @NotNull
    private Set<String> getIds(DbListResponse<DbWorkItem> dbWorkItemListResponse) {
        return dbWorkItemListResponse.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toSet());
    }
}
