package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class WorkItemServiceTest {

    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static IntegrationService integrationService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static Date currentTime;
    private static Long ingestedAt;
    private static Long olderIngestedAt;
    private static String integrationId;
    private static UserIdentityService userIdentityService;
    private static IntegrationTrackingService integrationTrackingService;

    /*
    Tests-
    0. Import json and map to List<WorkItems>
    1. Insert
    2. Filter
        - includes
        - excludes
     */
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
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService, null,
                null, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemFieldsMetaService.ensureTableExistence(COMPANY);
        IssuesMilestoneService issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issuesMilestoneService.ensureTableExistence(COMPANY);
        integrationTrackingService.ensureTableExistence(COMPANY);

        integrationId = integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
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
        olderIngestedAt = DateUtils.truncate(previousTime, Calendar.DATE);
        String iterationsResource = "json/databases/azure_devops_iterations_2.json";
        IssueMgmtTestUtil.setupIterations(COMPANY, "1", issuesMilestoneService, iterationsResource);
        IssueMgmtTestUtil.setupWorkItems(COMPANY, "1", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
    }

    @Test
    public void testDefault() throws SQLException {
        DbWorkItem dbWorkItem = workItemService.get(COMPANY, "1", "61");
        Assert.assertEquals("_UNASSIGNED_", dbWorkItem.getAssignee());
        Assert.assertEquals("_UNPRIORITIZED_", dbWorkItem.getPriority());
        Assert.assertEquals("Feature", dbWorkItem.getWorkItemType());
        Assert.assertEquals(List.of(), dbWorkItem.getSprintIds());
        Assert.assertEquals("_UNKNOWN_", dbWorkItem.getEpic());
        Assert.assertEquals(Float.valueOf(0f), dbWorkItem.getStoryPoint());
        Assert.assertTrue(StringUtils.isNotBlank(dbWorkItem.getProjectId()));
        Assert.assertEquals(4, workItemService.getIssuesCount(COMPANY, List.of(1), ingestedAt).intValue());
        Assert.assertEquals(0, workItemService.getIssuesCount(COMPANY, List.of(2), ingestedAt).intValue());
        Assert.assertEquals(4, workItemService.getIssuesCount(COMPANY, List.of(1), olderIngestedAt).intValue());
        Assert.assertEquals(0, workItemService.getIssuesCount(COMPANY, List.of(2), olderIngestedAt).intValue());
        Assert.assertEquals(0, workItemService.getIssuesCount(COMPANY, List.of(1), 1673222400l).intValue());
    }

    @Test
    public void testIdsInsertion() throws SQLException {
        DbWorkItem dbWorkItem = workItemService.get(COMPANY, "1", "67");
        Optional<String> integrationUserId1 = userIdentityService.getUserByDisplayName(COMPANY, integrationId, dbWorkItem.getAssignee());
        String assigneeId = dbWorkItem.getAssigneeId();
        integrationUserId1.ifPresent(s -> assertThat(s).isEqualTo(assigneeId));
        Optional<String> integrationUserId2 = userIdentityService.getUserByDisplayName(COMPANY, integrationId, dbWorkItem.getReporter());
        String reporterId = dbWorkItem.getReporterId();
        integrationUserId2.ifPresent(s -> assertThat(s).isEqualTo(reporterId));
    }

    @Test
    public void testListByIngestedAt() throws SQLException {
        List<DbWorkItem> dbWorkItems = workItemService.list(COMPANY, ingestedAt, null, 0, 250).getRecords();
        Assertions.assertThat(dbWorkItems.size()).isEqualTo(4);
        dbWorkItems.forEach(dbWorkItem1 -> {
            Assertions.assertThat(dbWorkItem1.getIngestedAt()).isEqualTo(ingestedAt);
        });
    }

    @Test
    public void testFirstAssigneeDrilldown() throws SQLException {
        DbListResponse<DbWorkItem> dbListResponse = workItemService.listByFilter(COMPANY, WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of(integrationId))
                        .missingFields(Map.of("first_assignee", true)).build(),
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(3);

        dbListResponse = workItemService.listByFilter(COMPANY, WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of(integrationId))
                        .missingFields(Map.of("first_assignee", true)).build(),
                WorkItemsMilestoneFilter.builder().states(List.of("current", "future")).build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(0);

        dbListResponse = workItemService.listByFilter(COMPANY, WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of(integrationId))
                        .missingFields(Map.of("first_assignee", true)).build(),
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, false);
        assertThat(dbListResponse.getCount()).isEqualTo(3);

        dbListResponse = workItemService.listByFilter(COMPANY, WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of(integrationId))
                        .missingFields(Map.of("first_assignee", true)).build(),
                WorkItemsMilestoneFilter.builder().states(List.of("current", "future")).build(), null, 0, 100, false);
        assertThat(dbListResponse.getCount()).isEqualTo(0);

        dbListResponse = workItemService.listByFilter(COMPANY, WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of(integrationId))
                        .missingFields(Map.of("first_assignee", true)).build(),
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, false);
        assertThat(dbListResponse.getCount()).isEqualTo(3);

        dbListResponse = workItemService.listByFilter(COMPANY, WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .integrationIds(List.of(integrationId))
                        .missingFields(Map.of("first_assignee", true)).build(),
                WorkItemsMilestoneFilter.builder().states(List.of("current", "future")).build(), null, 0, 100, false);
        assertThat(dbListResponse.getCount()).isEqualTo(0);
    }

    @Test
    public void testFilterTicketCategoriesWithSlaTimeColumns() throws SQLException {
        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .ticketCategorizationFilters(List.of(
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                .build(),
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(1)
                                .name("cat2")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                .build()
                ))
                .ticketCategories(List.of("cat1", "cat2"))
                .build();
        DbListResponse<DbWorkItem> dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(3);
        dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter.toBuilder().stages(List.of("Closed")).build(),
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(1);

        workItemsFilter = WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .ticketCategorizationFilters(List.of(
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                .build(),
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(1)
                                .name("cat2")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                .build()
                ))
                .ticketCategories(List.of("cat1"))
                .build();
        dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(1);

        workItemsFilter = WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .ticketCategorizationFilters(List.of(
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                .build(),
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(1)
                                .name("cat2")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                .build()
                ))
                .ticketCategories(List.of("unknown"))
                .build();
        dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(0);

        workItemsFilter = WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .ticketCategorizationFilters(List.of(
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                .build(),
                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                .index(1)
                                .name("cat2")
                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                .build()
                ))
                .ticketCategories(List.of("unknown"))
                .excludeStages(List.of("Open", "Closed"))
                .build();
        dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(0);

        dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter,
                WorkItemsMilestoneFilter.builder().states(List.of("current", "future")).build(), null, 0, 100, true);
        assertThat(dbListResponse.getCount()).isEqualTo(0);
    }

    @Test
    public void testFilterPriority() throws SQLException {
        WorkItemsFilter inclPriorFilter = WorkItemsFilter.builder()
                .priorities(List.of("1", "2"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclPriorResp = workItemService.listByFilter(COMPANY,
                inclPriorFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl priorities", inclPriorResp, 3, 3, Set.of("67", "68", "69"));

        WorkItemsFilter exclPriorFilter = WorkItemsFilter.builder()
                .excludePriorities(List.of("1", "2"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclPriorResp = workItemService
                .listByFilter(COMPANY, exclPriorFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl priorities", exclPriorResp, 1, 1, Set.of("61"));

        WorkItemsFilter inclExclPriorFilter = WorkItemsFilter.builder()
                .priorities(List.of("1"))
                .excludePriorities(List.of("2"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclPriorResp = workItemService
                .listByFilter(COMPANY, inclExclPriorFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl priorities", inclExclPriorResp, 1, 1, Set.of("69"));
    }

    @Test
    public void testFilterMissingFields() throws SQLException {
        WorkItemsFilter inclMissingFields = WorkItemsFilter.builder()
                .missingFields(Map.of("Custom.TestCustomField1", Boolean.TRUE))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclMissingFieldsResp = workItemService.listByFilter(COMPANY,
                inclMissingFields, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("incl custom missing fields", inclMissingFieldsResp, 2, 2, Set.of("67", "68"));

        WorkItemsFilter exclMissingFields = WorkItemsFilter.builder()
                .missingFields(Map.of("Custom.TestCustomField1", Boolean.FALSE))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclMissingFieldsResp = workItemService
                .listByFilter(COMPANY, exclMissingFields, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("excl custom missing fields", exclMissingFieldsResp, 2, 2, Set.of("69", "61"));

        WorkItemsFilter inclMissingPriorityFields = WorkItemsFilter.builder()
                .missingFields(Map.of("priority", Boolean.FALSE))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclMissingPriorityFieldsResp = workItemService
                .listByFilter(COMPANY, inclMissingPriorityFields, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("incl priorities missing fields", inclMissingPriorityFieldsResp, 3, 3, Set.of("67", "68", "69"));

        WorkItemsFilter exclMissingPriorityFields = WorkItemsFilter.builder()
                .missingFields(Map.of("priority", Boolean.TRUE))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclMissingPrioritiesFieldsResp = workItemService
                .listByFilter(COMPANY, exclMissingPriorityFields, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("excl priorities missing fields", exclMissingPrioritiesFieldsResp, 1, 1, Set.of("61"));
    }

    @Test
    public void testPartialMatchForProjects() throws SQLException {
        WorkItemsFilter partialMatchFilterForProjectsBegin = WorkItemsFilter.builder().partialMatch(Map.of("workitem_project", Map.of("$begins", "cg"))).build();
        DbListResponse<DbWorkItem> partialMatchReponseForProjectsBegin = workItemService.listByFilter(COMPANY,
                partialMatchFilterForProjectsBegin, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Projects Begins", partialMatchReponseForProjectsBegin, 8, 8, Set.of("67", "68", "69", "61"));

        WorkItemsFilter partialMatchFilterForProjectsEnds = WorkItemsFilter.builder().partialMatch(Map.of("workitem_project", Map.of("$ends", "ct"))).build();
        DbListResponse<DbWorkItem> partialMatchReponseForProjectsEnds = workItemService
                .listByFilter(COMPANY, partialMatchFilterForProjectsEnds, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Projects Ends", partialMatchReponseForProjectsEnds, 8, 8, Set.of("67", "68", "69", "61"));

        WorkItemsFilter partialMatchFilterForProjectsContains= WorkItemsFilter.builder().partialMatch(Map.of("workitem_project", Map.of("$contains", "cgn"))).build();
        DbListResponse<DbWorkItem> partialMatchReponseForProjectsContains = workItemService
                .listByFilter(COMPANY, partialMatchFilterForProjectsContains, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Projects Contains", partialMatchReponseForProjectsContains, 8, 8, Set.of("67", "68", "69", "61"));
    }

    @Test
    public void testPartialMatchForVersions() throws SQLException {
        WorkItemsFilter partialMatchFilterForVersionsBegin = WorkItemsFilter.builder().partialMatch(Map.of("workitem_versions", Map.of("$begins", "_UN"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForVersionsBegin = workItemService.listByFilter(COMPANY,
                partialMatchFilterForVersionsBegin, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Versions Begins", partialMatchResponseForVersionsBegin, 8, 8, Set.of("67", "68", "69", "61"));

        WorkItemsFilter partialMatchFilterForVersionsEnds = WorkItemsFilter.builder().partialMatch(Map.of("workitem_versions", Map.of("$ends", "KNOWN_"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForVersionsEnd = workItemService
                .listByFilter(COMPANY, partialMatchFilterForVersionsEnds, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Versions Ends", partialMatchResponseForVersionsEnd, 8, 8, Set.of("67", "68", "69", "61"));

        WorkItemsFilter partialMatchFilterForVersionsContains= WorkItemsFilter.builder().partialMatch(Map.of("workitem_versions", Map.of("$contains", "_UNKNO"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForVersionsContains = workItemService
                .listByFilter(COMPANY, partialMatchFilterForVersionsContains, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Versions Contains", partialMatchResponseForVersionsContains, 8, 8, Set.of("67", "68", "69", "61"));
    }

    @Test
    public void testPartialMatchForCustomFields() throws SQLException {
        WorkItemsFilter partialMatchFilterForCustomFieldsBegins = WorkItemsFilter.builder().partialMatch(Map.of("Custom.TestCustomField1", Map.of("$begins", "1"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForCustomFieldsBegins = workItemService.listByFilter(COMPANY,
                partialMatchFilterForCustomFieldsBegins, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for CustomFields Begins", partialMatchResponseForCustomFieldsBegins, 2, 2, Set.of("69"));

        WorkItemsFilter partialMatchFilterForCustomFieldsEnds = WorkItemsFilter.builder().partialMatch(Map.of("Custom.TestCustomField1", Map.of("$ends", "2"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForCustomFieldsEnds = workItemService
                .listByFilter(COMPANY, partialMatchFilterForCustomFieldsEnds, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for CustomFields Ends", partialMatchResponseForCustomFieldsEnds, 2, 2, Set.of("61"));

        WorkItemsFilter partialMatchFilterForCustomFieldsContains= WorkItemsFilter.builder().partialMatch(Map.of("Custom.TestCustomField1", Map.of("$contains", "1"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForCustomFieldsContains = workItemService
                .listByFilter(COMPANY, partialMatchFilterForCustomFieldsContains, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for CustomFields Contains", partialMatchResponseForCustomFieldsContains, 2, 2, Set.of("69"));
    }

    @Test
    public void testHygieneFilters() throws SQLException {
        DbListResponse<DbWorkItem> listResponse = workItemService
                .listByFilter(COMPANY, WorkItemsFilter.builder().integrationIds(List.of("1"))
                                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)).build(),
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(listResponse.getTotalCount()).isEqualTo(8);
        listResponse = workItemService
                .listByFilter(COMPANY, WorkItemsFilter.builder().integrationIds(List.of("1"))
                                .workItemIds(List.of("67"))
                                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time)).build(),
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(listResponse.getTotalCount()).isEqualTo(2);
        listResponse = workItemService
                .listByFilter(COMPANY, WorkItemsFilter.builder().integrationIds(List.of("1"))
                                .workItemIds(List.of("67"))
                                .workItemTypes(List.of("Bug")).build(),
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(listResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testHygieneFiltersWithSlaColumns() throws SQLException {
        DbListResponse<DbWorkItem> listResponse = workItemService
                .listByFilter(COMPANY, WorkItemsFilter.builder().integrationIds(List.of("1"))
                                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                                .ticketCategorizationFilters(List.of(
                                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                                .index(0)
                                                .name("cat1")
                                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                                .build(),
                                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                                .index(1)
                                                .name("cat2")
                                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                                .build()
                                ))
                                .ticketCategories(List.of("cat1", "cat2")).build(),
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(listResponse.getTotalCount()).isEqualTo(6);
        listResponse = workItemService
                .listByFilter(COMPANY, WorkItemsFilter.builder().integrationIds(List.of("1"))
                                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time))
                                .ticketCategorizationFilters(List.of(
                                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                                .index(0)
                                                .name("cat1")
                                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                                .build(),
                                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                                .index(1)
                                                .name("cat2")
                                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                                .build()
                                ))
                                .ticketCategories(List.of("cat1", "cat2")).build(),
                        WorkItemsMilestoneFilter.builder().states(List.of("Open")).build(), null, 0, 100);
        assertThat(listResponse.getTotalCount()).isEqualTo(0);
        listResponse = workItemService
                .listByFilter(COMPANY, WorkItemsFilter.builder().integrationIds(List.of("1"))
                                .workItemIds(List.of("67"))
                                .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time))
                                .ticketCategorizationFilters(List.of(
                                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                                .index(0)
                                                .name("cat1")
                                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                                .build(),
                                        WorkItemsFilter.TicketCategorizationFilter.builder()
                                                .index(1)
                                                .name("cat2")
                                                .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                                .build()
                                ))
                                .ticketCategories(List.of("cat1")).build(),
                        WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(listResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testPartialMatchForAttributes() throws SQLException {
        WorkItemsFilter partialMatchFilterForAttributesBegins = WorkItemsFilter.builder().partialMatch(Map.of("code_area", Map.of("$begins", "Agil"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForAttributesBegins = workItemService.listByFilter(COMPANY,
                partialMatchFilterForAttributesBegins, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Attributes Begins", partialMatchResponseForAttributesBegins, 8, 8, Set.of("67", "68", "69", "61"));

        WorkItemsFilter partialMatchFilterForAttributesEnds = WorkItemsFilter.builder().partialMatch(Map.of("code_area", Map.of("$ends", "agile-team-2"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForAttributesEnds = workItemService
                .listByFilter(COMPANY, partialMatchFilterForAttributesEnds, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Attributes Ends", partialMatchResponseForAttributesEnds, 6, 6, Set.of("67", "68", "69"));

        WorkItemsFilter partialMatchFilterForAttributesContains= WorkItemsFilter.builder().partialMatch(Map.of("code_area", Map.of("$contains", "Project"))).build();
        DbListResponse<DbWorkItem> partialMatchResponseForAttributesContains = workItemService
                .listByFilter(COMPANY, partialMatchFilterForAttributesContains, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
        assertListResponse("Partial Match for Attributes Contains", partialMatchResponseForAttributesContains, 8, 8, Set.of("67", "68", "69", "61"));

        String setClause = " SET attributes = jsonb_set(attributes,'{\"teams\"}', :teams::jsonb) ";
        String projectName = "cgn-test/Agile-Project";
        String area = "Agile-Project-2\\agile-team-3";

        Map<String, Object> params = new HashMap<>();
        params.put("teams", "[" + String.join(",", List.of("\"cgn-test/Agile-Project/team1\"", "\"cgn-test/Agile-Project/team2\"")) + "]");

        List<String> conditions = getWhereClause("1", projectName, area, params);
        boolean updateResult = workItemService.updateWorkItems(COMPANY, setClause, conditions, params);
        if (updateResult) {
            WorkItemsFilter partialMatchFilterForMultiValuedAttributesContains= WorkItemsFilter.builder().partialMatch(Map.of("teams", Map.of("$contains", "team1"))).build();
            DbListResponse<DbWorkItem> partialMatchResponseForMultiValuedAttributesContains = workItemService
                    .listByFilter(COMPANY, partialMatchFilterForMultiValuedAttributesContains, WorkItemsMilestoneFilter.builder().build(), null,0, 100);
            assertListResponse("Partial Match for Attributes Contains", partialMatchResponseForMultiValuedAttributesContains, 2, 2, Set.of("61"));
        }
    }

    @Test
    public void testFilterEpic() throws SQLException {
        WorkItemsFilter inclEpicFilter = WorkItemsFilter.builder()
                .epics(List.of("_UNKNOWN_"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclEpicResp = workItemService.listByFilter(COMPANY,
                inclEpicFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl epics", inclEpicResp, 4, 4, Set.of("61", "67", "68", "69"));

        WorkItemsFilter exclEpicFilter = WorkItemsFilter.builder()
                .excludeEpics(List.of("_UNKNOWN_"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclEpicResp = workItemService
                .listByFilter(COMPANY, exclEpicFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl epics", exclEpicResp, 0, 0, Set.of());

        WorkItemsFilter inclExclEpicFilter = WorkItemsFilter.builder()
                .epics(List.of("_UNKNOWN_")).excludeEpics(List.of("EPIC-1"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclEpicResp = workItemService
                .listByFilter(COMPANY, inclExclEpicFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl epics", inclExclEpicResp, 4, 4, Set.of("61", "67", "68", "69"));
    }

    @Test
    public void testFilterVersions() throws SQLException {
        WorkItemsFilter inclVersionFilter = WorkItemsFilter.builder()
                .versions(List.of(DbWorkItem.UNKNOWN))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclVersionResp = workItemService.listByFilter(COMPANY,
                inclVersionFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl version", inclVersionResp, 4, 4, Set.of("61", "67", "68", "69"));

        WorkItemsFilter exclVersionFilter = WorkItemsFilter.builder()
                .excludeVersions(List.of(DbWorkItem.UNKNOWN))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclVersionResp = workItemService.listByFilter(COMPANY,
                exclVersionFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl version", exclVersionResp, 0, 0, Set.of());
    }

    @Test
    public void testFilterStatus() throws SQLException {
        WorkItemsFilter inclStatusesFilter = WorkItemsFilter.builder()
                .statuses(List.of("Closed"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclStatusesResp = workItemService.listByFilter(COMPANY,
                inclStatusesFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl status", inclStatusesResp, 2, 2, Set.of("61", "67"));

        WorkItemsFilter exclStatusesFilter = WorkItemsFilter.builder()
                .excludeStatuses(List.of("Closed"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclStatusesResp = workItemService
                .listByFilter(COMPANY, exclStatusesFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl status", exclStatusesResp, 2, 2, Set.of("69", "68"));

        WorkItemsFilter inclExclStatusesFilter = WorkItemsFilter.builder()
                .statuses(List.of("Open"))
                .excludeStatuses(List.of("Closed"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclStatusesResp = workItemService
                .listByFilter(COMPANY, inclExclStatusesFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl status", inclExclStatusesResp, 1, 1, Set.of("69"));
    }

    @Test
    public void testFilterIssueTypes() throws SQLException {
        WorkItemsFilter inclIssueTypeFilter = WorkItemsFilter.builder()
                .workItemTypes(List.of("User Story", "Bug"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclIssueTypeResp = workItemService.listByFilter(COMPANY,
                inclIssueTypeFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl issueType", inclIssueTypeResp, 1, 1, Set.of("69"));

        WorkItemsFilter exclIssueTypeFilter = WorkItemsFilter.builder()
                .excludeWorkItemTypes(List.of("Task"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclIssueTypeResp = workItemService.listByFilter(COMPANY,
                exclIssueTypeFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl issueType", exclIssueTypeResp, 2, 2, Set.of("69", "61"));

        WorkItemsFilter inclExclIssueTypeFilter = WorkItemsFilter.builder()
                .workItemTypes(List.of("User Story", "Bug"))
                .excludeWorkItemTypes(List.of("Task"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclIssueTypeResp = workItemService.listByFilter(COMPANY,
                inclExclIssueTypeFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl issueType", inclExclIssueTypeResp, 1, 1, Set.of("69"));
    }

    @Test
    public void testFilterLabels() throws SQLException {
        WorkItemsFilter inclLabelFilter = WorkItemsFilter.builder()
                .labels(List.of("sampleTag"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclLabelResp = workItemService.listByFilter(COMPANY,
                inclLabelFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl label", inclLabelResp, 1, 1, Set.of("69"));

        WorkItemsFilter exclLabelFilter = WorkItemsFilter.builder()
                .excludeLabels(List.of("sampleTag"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclLabelResp = workItemService.listByFilter(COMPANY,
                exclLabelFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl label", exclLabelResp, 3, 3, Set.of("61", "67", "68"));

        WorkItemsFilter inclExclLabelFilter = WorkItemsFilter.builder()
                .labels(List.of("tag"))
                .excludeLabels(List.of("sampleTag"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclLabelResp = workItemService.listByFilter(COMPANY,
                inclExclLabelFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl label", inclExclLabelResp, 1, 1, Set.of("68"));
    }

    @Test
    public void testFilterFixVersions() throws SQLException {
        WorkItemsFilter inclFixVersionFilter = WorkItemsFilter.builder()
                .fixVersions(List.of(DbWorkItem.UNKNOWN))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclFixVersionResp = workItemService.listByFilter(COMPANY,
                inclFixVersionFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl fixVersion", inclFixVersionResp, 4, 4, Set.of("61", "67", "68", "69"));

        WorkItemsFilter exclFixVersionFilter = WorkItemsFilter.builder()
                .excludeFixVersions(List.of(DbWorkItem.UNKNOWN))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclFixVersionResp = workItemService.listByFilter(COMPANY,
                exclFixVersionFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl fixVersion", exclFixVersionResp, 0, 0, Set.of());
    }

    @Test
    public void testFilterSprintIds() throws SQLException {
        String sprintId = UUID.randomUUID().toString();
        WorkItemsFilter inclSprintIdFilter = WorkItemsFilter.builder()
                .sprintIds(List.of(sprintId))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclSprintIdResp = workItemService.listByFilter(COMPANY,
                inclSprintIdFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl sprintId", inclSprintIdResp, 4, 4, Set.of("67", "68", "69", "61"));

        WorkItemsFilter exclSprintIdFilter = WorkItemsFilter.builder()
                .excludeSprintIds(List.of(sprintId))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclSprintIdResp = workItemService.listByFilter(COMPANY,
                exclSprintIdFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl sprintId", exclSprintIdResp, 4, 4, Set.of("67", "68", "69", "61"));

        WorkItemsFilter inclExclSprintIdFilter = WorkItemsFilter.builder()
                .sprintIds(List.of(sprintId))
                .excludeSprintIds(List.of(sprintId))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclSprintIdResp = workItemService.listByFilter(COMPANY,
                inclExclSprintIdFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl sprintId", inclExclSprintIdResp, 4, 4, Set.of("67", "68", "69", "61"));
    }

    @Test
    public void testFilterParentKey() throws SQLException {
        WorkItemsFilter inclParentKeyFilter = WorkItemsFilter.builder()
                .parentWorkItemIds(List.of("61"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclParentKeyResp = workItemService.listByFilter(COMPANY,
                inclParentKeyFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl parentKey", inclParentKeyResp, 3, 3, Set.of("67", "68", "69"));

        WorkItemsFilter exclParentKeyFilter = WorkItemsFilter.builder()
                .excludeParentWorkItemIds(List.of("4"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclParentKeyResp = workItemService.listByFilter(COMPANY,
                exclParentKeyFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl parentKey", exclParentKeyResp, 3, 3, Set.of("67", "68", "69"));

        WorkItemsFilter inclExclParentKeyFilter = WorkItemsFilter.builder()
                .parentWorkItemIds(List.of("61"))
                .excludeParentWorkItemIds(List.of("4"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclParentKeyResp = workItemService.listByFilter(COMPANY,
                inclExclParentKeyFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl parentKey", inclExclParentKeyResp, 3, 3, Set.of("67", "68", "69"));
    }

    @Test
    public void testFilterWorkitemIds() throws SQLException {
        WorkItemsFilter workItemsFilter = WorkItemsFilter.builder()
                .workItemIds(List.of("67", "61"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> dbListResponse = workItemService.listByFilter(COMPANY, workItemsFilter,
                WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbListResponse.getCount()).isEqualTo(2);
        DbWorkItem dbWorkItem = dbListResponse.getRecords().stream()
                .filter(dbWorkItem1 -> dbWorkItem1.getWorkItemId().equals("67")).findFirst().get();
        Assert.assertEquals("67", dbWorkItem.getWorkItemId());
    }

    @Test
    public void testFilterIngestedAt() throws SQLException {
        Long ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        WorkItemsFilter correctIngestedAt = WorkItemsFilter.builder().ingestedAt(ingestedAt).build();
        DbListResponse<DbWorkItem> dbListResponse = workItemService.listByFilter(COMPANY,
                correctIngestedAt, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbListResponse.getCount()).isEqualTo(4);
        Long yesterday = DateUtils.truncate(org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1), Calendar.DATE);
        WorkItemsFilter oldIngestedAt = WorkItemsFilter.builder().ingestedAt(yesterday).build();
        DbListResponse<DbWorkItem> dbListResponse1 = workItemService.listByFilter(COMPANY,
                oldIngestedAt, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbListResponse1.getCount()).isEqualTo(4);
    }

    @Test
    public void testFilterAssignee() throws SQLException {
        var assignee1 = userIdentityService.getUserByDisplayName(COMPANY, integrationId, "srinath.chandrashekhar");
        WorkItemsFilter inclAssigneeFilter = WorkItemsFilter.builder()
                .assignees(List.of(Objects.requireNonNull(assignee1.orElse(null))))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclAssigneeResp = workItemService.listByFilter(COMPANY,
                inclAssigneeFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl assignee", inclAssigneeResp, 3, 3, Set.of("67", "68", "69"));
    }

    @Test
    public void testFilterProject() throws SQLException {
        WorkItemsFilter inclProjectFilter = WorkItemsFilter.builder()
                .projects(List.of("cgn-test/Agile-Project"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclProjectResp = workItemService.listByFilter(COMPANY,
                inclProjectFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl project", inclProjectResp, 4, 4, Set.of("67", "68", "69", "61"));

        WorkItemsFilter exclProjectFilter = WorkItemsFilter.builder()
                .excludeProjects(List.of("cgn-test/Agile-Project2"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclProjectResp = workItemService.listByFilter(COMPANY,
                exclProjectFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl project", exclProjectResp, 4, 4, Set.of("67", "68", "69", "61"));

        WorkItemsFilter inclExclProjectFilter = WorkItemsFilter.builder()
                .projects(List.of("cgn-test/Agile-Project"))
                .excludeProjects(List.of("Agile-Project2"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclProjectResp = workItemService.listByFilter(COMPANY,
                inclExclProjectFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl project", inclExclProjectResp, 4, 4, Set.of("67", "68", "69", "61"));
    }

    @Test
    public void testFilterStoryPoints() throws SQLException {
        WorkItemsFilter gtSPFilter = WorkItemsFilter.builder()
                .storyPointsRange(ImmutablePair.of(1.0f, null))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> gtSPResp = workItemService
                .listByFilter(COMPANY, gtSPFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("gt story points", gtSPResp, 3, 3, Set.of("67", "68", "69"));

        WorkItemsFilter ltSPFilter = WorkItemsFilter.builder()
                .storyPointsRange(ImmutablePair.of(null, 5.0f))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> ltSPResp = workItemService
                .listByFilter(COMPANY, ltSPFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("lt story points", ltSPResp, 4, 4, Set.of("61", "67", "68", "69"));

        WorkItemsFilter ltGtSPFilter = WorkItemsFilter.builder()
                .storyPointsRange(ImmutablePair.of(2.0f, 5.0f))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> ltGtSPResp = workItemService
                .listByFilter(COMPANY, ltGtSPFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("ltGt story points", ltGtSPResp, 3, 3, Set.of("67", "68", "69"));
    }

    @Test
    public void testFilterReporter() throws SQLException {
        var reporter1 = userIdentityService.getUser(COMPANY, integrationId, "srinath.chandrashekhar@levelops.io");
        var reporter2 = userIdentityService.getUser(COMPANY, integrationId, "user2@levelops.io");
        WorkItemsFilter inclReporterFilter = WorkItemsFilter.builder()
                .reporters(List.of(reporter1))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclReporterResp = workItemService.listByFilter(COMPANY,
                inclReporterFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl reporters", inclReporterResp, 3, 3, Set.of("61", "68", "69"));

        WorkItemsFilter exclReporterFilter = WorkItemsFilter.builder()
                .excludeReporters(List.of(reporter1))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> exclEpicResp = workItemService
                .listByFilter(COMPANY, exclReporterFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("excl reporters", exclEpicResp, 1, 1, Set.of("67"));

        WorkItemsFilter inclExclReporterFilter = WorkItemsFilter.builder()
                .reporters(List.of(reporter2))
                .excludeReporters(List.of(reporter1))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> inclExclReporterResp = workItemService
                .listByFilter(COMPANY, inclExclReporterFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("incl-excl epics", inclExclReporterResp, 1, 1, Set.of("67"));
    }

    @Test
    public void testFilterIssueCreatedAt() throws SQLException {
        WorkItemsFilter issueCreatedAtFilter = WorkItemsFilter.builder()
                .workItemCreatedRange(ImmutablePair.of(1526932595L, 1626932595L))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> issueCreatedAtResp = workItemService.listByFilter(COMPANY,
                issueCreatedAtFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("issueCreatedAt", issueCreatedAtResp, 4, 4, Set.of("61", "67", "68", "69"));
    }

    @Test
    public void testFilterIssueResolvedRange() throws SQLException {
        WorkItemsFilter issueResolvedRangeFilter = WorkItemsFilter.builder()
                .workItemResolvedRange(ImmutablePair.of(1526932595L, 1626932595L))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> issueResolvedRangeResp = workItemService.listByFilter(COMPANY,
                issueResolvedRangeFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("issueResolvedRange", issueResolvedRangeResp, 2, 2, Set.of("69", "61"));
    }

    @Test
    public void testFilterIntegrationIdByIssueUpdatedRange() throws SQLException {
        /*
        select id, workitem_id, workitem_updated_at, extract(epoch from workitem_updated_at) as a from test.issue_mgmt_workitems where ingested_at=1673942400 order by workitem_updated_at;
                  id                  | workitem_id |    workitem_updated_at     |       a
        --------------------------------------+-------------+----------------------------+----------------
         09f40e36-eb5b-49dc-b7f1-52084cd3f259 | 67          | 2021-05-07 06:37:12.137+00 | 1620369432.137
         2a4921ef-ba4b-41e9-be91-fe636a9ba35d | 61          | 2021-05-07 07:24:01.71+00  |  1620372241.71
         45c6311e-1152-4727-b8b9-a072290ce40d | 69          | 2021-05-07 07:24:01.71+00  |  1620372241.71
         7b5f4cc7-fd50-4738-95d5-0c08a3970774 | 68          | 2021-05-07 07:25:17.51+00  |  1620372317.51
         */
        //For single ingested_at & integration_id=1 & workitem_updated_at> 1620372240 we should get 3 work items
        WorkItemsFilter issueCreatedAtFilter = WorkItemsFilter.builder()
                .integrationIdByIssueUpdatedRange(Map.of(1, ImmutablePair.of(1620372240l, null)))
                .ingestedAt(ingestedAt).sort(Map.of("workitem_updated_at", SortingOrder.fromString("ASC")))
                .build();
        DbListResponse<DbWorkItem> issueCreatedAtResp = workItemService.listByFilter(COMPANY,
                issueCreatedAtFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("issueCreatedAt", issueCreatedAtResp, 3, 3, Set.of("61", "69", "68"));

        //For single ingested_at & integration_id=1 & workitem_updated_at> 1620372240 we should get 3 work items
        issueCreatedAtFilter = WorkItemsFilter.builder()
                .integrationIdByIssueUpdatedRange(Map.of(1, ImmutablePair.of(1620372242l, null)))
                .ingestedAt(ingestedAt).sort(Map.of("workitem_updated_at", SortingOrder.fromString("ASC")))
                .build();
        issueCreatedAtResp = workItemService.listByFilter(COMPANY,
                issueCreatedAtFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("issueCreatedAt", issueCreatedAtResp, 1, 1, Set.of("68"));

        //For single ingested_at & integration_id=1 & workitem_updated_at>1620372240 & workitem_updated_at<1620372316 we should get 2 work items
        issueCreatedAtFilter = WorkItemsFilter.builder()
                .integrationIdByIssueUpdatedRange(Map.of(1, ImmutablePair.of(1620372240l, 1620372316l)))
                .ingestedAt(ingestedAt).sort(Map.of("workitem_updated_at", SortingOrder.fromString("ASC")))
                .build();
        issueCreatedAtResp = workItemService.listByFilter(COMPANY,
                issueCreatedAtFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("issueCreatedAt", issueCreatedAtResp, 2, 2, Set.of("61", "69"));

        //For single ingested_at & integration_id=2 we should get all 4 work items (integration_id=2 does not have any records, & integration_id=1 doesn't have time filter so all 4 should be returned)
        issueCreatedAtFilter = WorkItemsFilter.builder()
                .integrationIdByIssueUpdatedRange(Map.of(2, ImmutablePair.of(1620369430l, null)))
                .ingestedAt(ingestedAt).sort(Map.of("workitem_updated_at", SortingOrder.fromString("ASC")))
                .build();
        issueCreatedAtResp = workItemService.listByFilter(COMPANY,
                issueCreatedAtFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("issueCreatedAt", issueCreatedAtResp, 4, 4, Set.of("67","61", "69", "68"));
    }

    @Test
    public void testGroupByAssignee() throws SQLException {
        WorkItemsFilter groupByAssigneeFilter = WorkItemsFilter.builder()
                .priorities(List.of("2"))
                .across(WorkItemsFilter.DISTINCT.assignee)
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> groupByAssigneeResp = workItemService.listByFilter(COMPANY,
                groupByAssigneeFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("groupByAssigneeFilter", groupByAssigneeResp, 2, 2, Set.of("67", "68"));
    }

    @Test
    public void testGroupByProject() throws SQLException {
        WorkItemsFilter groupByProjectFilter = WorkItemsFilter.builder()
                .priorities(List.of("2"))
                .across(WorkItemsFilter.DISTINCT.project)
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> groupByProjectResp = workItemService.listByFilter(COMPANY,
                groupByProjectFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertListResponse("groupByProject", groupByProjectResp, 2, 2, Set.of("67", "68"));
    }

    @Test
    public void testAttributes() throws SQLException {
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .attributes(Map.of("code_area", List.of("Agile-Project-2\\agile-team-3")))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> dbListResponse1 = workItemService.listByFilter(COMPANY,
                workItemsFilter1, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbListResponse1.getCount()).isEqualTo(1);
        assertThat(dbListResponse1.getRecords().get(0).getCustomFields()).isNotNull();
    }

    @Test
    public void testCustomFields() throws SQLException {
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .customFields(Map.of("Custom.TestCustomField1", List.of("1")))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> dbListResponse1 = workItemService.listByFilter(COMPANY,
                workItemsFilter1, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbListResponse1.getCount()).isEqualTo(1);
        assertThat(dbListResponse1.getRecords().get(0).getAttributes()).isNotNull();
    }

    @Test
    public void testGetWorkitem() throws SQLException {
        Optional<DbWorkItem> workItem = workItemService.list(COMPANY, 0, 1).getRecords().stream().findFirst();
        if (workItem.isPresent()) {
            DbWorkItem dbWorkItem = workItemService.get(COMPANY, "1", workItem.get().getWorkItemId());
            assertThat(dbWorkItem.getWorkItemId()).isEqualTo(workItem.get().getWorkItemId());

            dbWorkItem = workItemService.get(COMPANY, "1", workItem.get().getWorkItemId(), workItem.get().getIngestedAt());
            assertThat(dbWorkItem.getWorkItemId()).isEqualTo(workItem.get().getWorkItemId());
        }
    }

    @Test
    public void testSort() throws SQLException {
        WorkItemsFilter workItemsFilter1 = WorkItemsFilter.builder()
                .sort(Map.of("project", SortingOrder.fromString("DESC")))
                .across(WorkItemsFilter.DISTINCT.fromString("project"))
                .ingestedAt(ingestedAt)
                .build();
        DbListResponse<DbWorkItem> dbListResponse1 = workItemService.listByFilter(COMPANY,
                workItemsFilter1, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(dbListResponse1).isNotNull();
        assertThat(dbListResponse1.getRecords()).isSortedAccordingTo(Comparator.comparing(DbWorkItem::getProject).reversed());
    }

    @Test
    public void testIncludeSolveTime() throws SQLException {
        DbListResponse<DbWorkItem> listResponse = workItemService.listByFilter(COMPANY,
                WorkItemsFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        assertThat(listResponse.getRecords().stream().map(DbWorkItem::getSolveTime)).containsOnlyNulls();
        Assertions.assertThat(listResponse.getRecords().stream().map(DbWorkItem::getSolveTime)).containsOnlyNulls();

        listResponse = workItemService.listByFilter(COMPANY,
                WorkItemsFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(ingestedAt)
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10000);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbWorkItem::getSolveTime)).isNotNull();
    }

    private void assertListResponse(String filter, DbListResponse<DbWorkItem> actualResponse,
                                    int total, int pageCount, Set<String> ids) {
        assertThat(actualResponse.getTotalCount()).as("Total count - " + filter).isEqualTo(total);
        assertThat(actualResponse.getCount()).as("Page count - " + filter).isEqualTo(pageCount);
        assertThat(getIds(actualResponse)).as("WorkItem IDs - " + filter).isEqualTo(ids);
    }

    @NotNull
    private Set<String> getIds(DbListResponse<DbWorkItem> dbWorkItemListResponse) {
        return dbWorkItemListResponse.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toSet());
    }

    @Test
    public void testUpdate() {
        String setClause = " SET attributes = jsonb_set(attributes,'{\"teams\"}', :teams::jsonb) ";
        Map<String, Object> params = new HashMap<>();
        String team1 = "Agile-team-2";
        team1 = "\"" + team1 + "\"";
        String team2 = "Agile-team-3";
        team2 = "\"" + team2 + "\"";
        params.put("teams", "[" + String.join(",", List.of(team1, team2)) + "]");
        List<String> conditions = getWhereClause("1", "Agile-Project", "Agile-Project\\agile-team-2", params);
        boolean s = workItemService.updateWorkItems(COMPANY, setClause, conditions, params);
        assertThat(s).isTrue();
    }

    private List<String> getWhereClause(String integrationId, String project, String area, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("(attributes ->> 'code_area') = :area ");
        params.put("area", area);
        conditions.add("integration_id = :integration_id");
        params.put("integration_id", NumberUtils.toInteger(integrationId));
        conditions.add("project = :project");
        params.put("project", project);
        return conditions;
    }

    @Test
    public void testIncludeSprintFullNames() throws SQLException {
        WorkItemsFilter inclSprintIdFilter = WorkItemsFilter.builder()
                .includeSprintFullNames(true)
                .ingestedAt(ingestedAt)
                .sort(Map.of("workitem_updated_at", SortingOrder.ASC))
                .build();
        DbListResponse<DbWorkItem> response = workItemService.listByFilter(COMPANY,
                inclSprintIdFilter, WorkItemsMilestoneFilter.builder().build(), null, 0, 100);
        response.getRecords().stream().map(DbWorkItem::getSprintFullNames).forEach(System.out::println);
        assertThat(response.getRecords().stream().map(DbWorkItem::getSprintFullNames)).containsExactly(
                List.of("Agile-Project\\sprint-2"),
                List.of(),
                List.of("Agile-Project\\sprint-2"),
                List.of("Agile-Project\\sprint-2")
        );
    }
}
