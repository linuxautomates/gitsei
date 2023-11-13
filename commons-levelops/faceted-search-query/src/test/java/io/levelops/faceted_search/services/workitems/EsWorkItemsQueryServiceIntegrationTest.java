package io.levelops.faceted_search.services.workitems;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.issue_management.DbWorkItemPrioritySLA;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsAgeReportService;
import io.levelops.commons.databases.services.WorkItemsFirstAssigneeReportService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsReportService;
import io.levelops.commons.databases.services.WorkItemsResolutionTimeReportService;
import io.levelops.commons.databases.services.WorkItemsResponseTimeReportService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.WorkItemsStageTimesReportService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

/*
* Before running this integration test, you have to run below-mentioned test class to make sure data is there into ES
* io.levelops.faceted_search.services.issue_mgmt.EsWorkItemIntegrationTest
*/
public class EsWorkItemsQueryServiceIntegrationTest {
    private static final String company = "test";
    private static final String esIp = System.getenv("ES_IP");
    private static final Integer esPort = Integer.valueOf(System.getenv("ES_PORT"));
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static EsWorkItemsQueryService esWorkItemsQueryService;
    private static String integrationId;
    private static Long ingestedAt;
    private static ESClientFactory esClientFactory;
    private static String user1;
    private static String user2;

    @BeforeClass
    public static void setUp() throws SQLException, GeneralSecurityException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        WorkItemsReportService workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsStageTimesReportService workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        IntegrationService integrationService = new IntegrationService(dataSource);
        WorkItemsAgeReportService workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResponseTimeReportService workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsService workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                null, null, workItemsFirstAssigneeReportService,
                workItemFieldsMetaService, null);
        integrationService.ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        workItemService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(company);
        workItemFieldsMetaService.ensureTableExistence(company);
        IssuesMilestoneService issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issuesMilestoneService.ensureTableExistence(company);
        IssueMgmtSprintMappingDatabaseService sprintMappingService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        sprintMappingService.ensureTableExistence(company);

        integrationId = integrationService.insert(company, Integration.builder()
                .id("1")
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());
        ingestedAt = 1647129602L;

        user1 = userIdentityService.upsert(company, DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId("sam")
                .displayName("samso")
                .originalDisplayName("samso")
                .build());
        user2 = userIdentityService.upsert(company, DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId("man")
                .displayName("manso")
                .originalDisplayName("manso")
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("1")
                .integrationId(integrationId)
                .summary("summary")
                .priority("2")
                .assignee("samso")
                .epic("epic")
                .parentWorkItemId("agile")
                .reporter("manso")
                .assigneeId(user1)
                .reporterId(user2)
                .status("complete")
                .workItemType("task")
                .storyPoint(5.5f)
                .customFields(Map.of("custom_field_456789", "key",
                        "custom_field_123456", "hello"))
                .ingestedAt(ingestedAt)
                .project("Agile-Test")
                .components(List.of("comp-1"))
                .labels(List.of("A"))
                .versions(List.of("1"))
                .fixVersions(List.of("22"))
                .resolution("2")
                .statusCategory("done")
                .ticketCategory("issue")
                .originalEstimate(10.0f)
                .descSize(50)
                .workItemCreatedAt(Timestamp.valueOf("2022-04-12 04:47:30.952"))
                .workItemResolvedAt(Timestamp.valueOf("2022-04-12 04:47:30.952"))
                .workItemDueAt(Timestamp.valueOf("2022-04-12 04:47:30.952"))
                .workItemUpdatedAt(Timestamp.valueOf("2022-04-12 04:47:30.952"))
                .firstAttachmentAt(Timestamp.valueOf("2022-03-15 19:52:59.833"))
                .firstCommentAt(Timestamp.valueOf("2022-03-15 19:52:59.833"))
                .attributes(Map.of("organization", "attributeValue", "project", "title"))
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("2")
                .integrationId(integrationId)
                .summary("summary-2")
                .priority("2")
                .assignee("samso")
                .epic("epic")
                .parentWorkItemId("agile-2")
                .reporter("manso")
                .assigneeId(user1)
                .reporterId(user2)
                .status("To Do")
                .workItemType("Issue")
                .storyPoint(5.5f)
                .customFields(Map.of("custom_field_456789", "key",
                        "custom_field_123456", "hello"))
                .ingestedAt(ingestedAt)
                .project("Agile-Test-2")
                .components(List.of("comp-1"))
                .labels(List.of("A"))
                .versions(List.of("1"))
                .fixVersions(List.of("22"))
                .resolution("2")
                .statusCategory("done")
                .ticketCategory("issue")
                .originalEstimate(10.0f)
                .descSize(50)
                .workItemCreatedAt(Timestamp.valueOf("2022-04-12 04:47:30.952"))
                .workItemResolvedAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .workItemDueAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .workItemUpdatedAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .firstAttachmentAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .firstCommentAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .attributes(Map.of("organization", "attributeValue", "project", "title"))
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("3")
                .integrationId(integrationId)
                .summary("summary-3")
                .priority("3")
                .assignee("samso")
                .epic("epic-2")
                .parentWorkItemId("agile-3")
                .reporter("manso")
                .assigneeId(user1)
                .reporterId(user2)
                .status("To Do")
                .workItemType("Issue")
                .storyPoint(5.5f)
                .customFields(Map.of("custom_field_456789", "key",
                        "custom_field_123456", "hello"))
                .ingestedAt(ingestedAt)
                .project("Agile-Test-2")
                .components(List.of("comp-2"))
                .labels(List.of("A"))
                .versions(List.of("3"))
                .fixVersions(List.of("44"))
                .resolution("5")
                .statusCategory("completed")
                .ticketCategory("issue")
                .originalEstimate(10.0f)
                .descSize(50)
                .workItemCreatedAt(Timestamp.valueOf("2019-05-16 19:52:59.833"))
                .workItemResolvedAt(Timestamp.valueOf("2019-05-17 19:52:59.833"))
                .workItemDueAt(Timestamp.valueOf("2019-05-17 19:52:59.833"))
                .workItemUpdatedAt(Timestamp.valueOf("2019-05-17 19:52:59.833"))
                .firstAttachmentAt(Timestamp.valueOf("2019-05-17 19:52:59.833"))
                .firstCommentAt(Timestamp.valueOf("2019-05-17 19:52:59.833"))
                .attributes(Map.of("organization", "attributeValue", "project", "title"))
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("4")
                .integrationId(integrationId)
                .summary("summary-4")
                .priority("3")
                .assignee("samso")
                .epic("epic")
                .parentWorkItemId("agile-2")
                .reporter("manso")
                .assigneeId(user1)
                .reporterId(user2)
                .status("To Do")
                .workItemType("task")
                .storyPoint(5.5f)
                .customFields(Map.of("custom_field_456789", "key",
                        "custom_field_123456", "hello"))
                .ingestedAt(ingestedAt)
                .project("Agile-Test-2")
                .components(List.of("comp-1"))
                .labels(List.of("A"))
                .versions(List.of("4"))
                .fixVersions(List.of("33"))
                .resolution("2")
                .statusCategory("completed")
                .ticketCategory("issue")
                .originalEstimate(10.0f)
                .descSize(50)
                .workItemCreatedAt(Timestamp.valueOf("2021-02-16 19:52:59.833"))
                .workItemResolvedAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .workItemDueAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .workItemUpdatedAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .firstAttachmentAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .firstCommentAt(Timestamp.valueOf("2021-03-17 19:52:59.833"))
                .attributes(Map.of("organization", "attributeValue", "project", "title"))
                .build());
        workItemFieldsMetaService.batchUpsert(company, List.of(DbWorkItemField.builder()
                        .name("custom_field_456789")
                        .fieldKey("custom_field_456789")
                        .integrationId(integrationId)
                        .fieldType("string")
                        .custom(true)
                        .build(),
                DbWorkItemField.builder()
                        .name("custom_field_123456")
                        .fieldKey("custom_field_123456")
                        .integrationId(integrationId)
                        .fieldType("string")
                        .custom(true)
                        .build()));
        workItemsPrioritySLAService.insert(company, DbWorkItemPrioritySLA.builder()
                .integrationId(integrationId)
                .priority("2")
                .workitemType("task")
                .project("Agile-Test")
                .respSla(86400L)
                .solveSla(86400L)
                .build());
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("assignee")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("status")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("story_points")
                .fieldValue("5.5")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("sprint")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        issuesMilestoneService.insert(company, DbIssuesMilestone.builder()
                .fieldType("sprint")
                .fieldValue("s1")
                .integrationId(Integer.valueOf(integrationId))
                .name("test")
                .projectId("1234")
                .state("done")
                .parentFieldValue("56er56")
                .completedAt(new Timestamp(0))
                .endDate(new Timestamp(10))
                .startDate(new Timestamp(0))
                .attributes(Map.of("organization", "attributeValue", "project", "title"))
                .build());
        sprintMappingService.upsert(company, DbIssueMgmtSprintMapping.builder()
                .sprintId("Agile-Project\\sprint-test-2")
                .addedAt(1631865550L)
                .workitemId("1")
                .integrationId(integrationId)
                .build());

        esClientFactory = new ESClientFactory(List.of(ESClusterInfo.builder()
                .name("CLUSTER_1")
                .ipAddresses(List.of("10.128.15.195"))
                .port(9220)
                .defaultCluster(true)
                .build()));
        OrgUsersDatabaseService orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, new ObjectMapper(), new OrgVersionsDatabaseService(dataSource), userIdentityService);
        esWorkItemsQueryService = new EsWorkItemsQueryService(esClientFactory, workItemFieldsMetaService, orgUsersDatabaseService);
    }

    @Test
    public void testGetListFromEs() throws IOException {
        DbListResponse<DbWorkItem> list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertNotNull(list);

        assertThat(list.getTotalCount()).isEqualTo(4);
        assertThat(list.getRecords().get(0).getIngestedAt()).isEqualTo(ingestedAt);
        assertThat(list.getRecords().get(0).getIntegrationId()).isEqualTo(integrationId);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2", "3", "4");
        assertThat(list.getRecords().get(0).getAssignee()).isEqualTo("samso");
        assertThat(list.getRecords().get(0).getReporter()).isEqualTo("manso");
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemType).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Issue", "task", "task", "Issue");
        assertThat(list.getRecords().get(0).getStoryPoint()).isEqualTo(5.5f);

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .versions(List.of("1"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(2);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2");
        list.getRecords().stream().map(DbWorkItem::getVersions).forEach(versions ->
                assertThat(versions).containsExactlyInAnyOrder("1"));


        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .fixVersions(List.of("22"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(2);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2");
        list.getRecords().stream().map(DbWorkItem::getFixVersions).forEach(fixVersions ->
                assertThat(fixVersions).containsExactlyInAnyOrder("22"));

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .epics(List.of("epic"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(3);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2", "4");
        assertThat(list.getRecords().stream().map(DbWorkItem::getEpic).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("epic", "epic", "epic");

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .projects(List.of("Agile-Test-2"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(3);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("3", "2", "4");
        assertThat(list.getRecords().stream().map(DbWorkItem::getProject).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Agile-Test-2", "Agile-Test-2", "Agile-Test-2");

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .projects(List.of("Agile-Test-2"))
                .statusCategories(List.of("completed"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(2);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("3", "4");
        assertThat(list.getRecords().stream().map(DbWorkItem::getProject).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Agile-Test-2", "Agile-Test-2");
        assertThat(list.getRecords().stream().map(DbWorkItem::getStatusCategory).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("completed", "completed");

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .priorities(List.of("2"))
                .statusCategories(List.of("done"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(2);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2");
        assertThat(list.getRecords().stream().map(DbWorkItem::getPriority).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2", "2");
        assertThat(list.getRecords().stream().map(DbWorkItem::getStatusCategory).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("done", "done");

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of(integrationId))
                .customFields(Map.of("custom_field_123456", List.of("hello")))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(4);
        assertThat(list.getRecords().stream().map(DbWorkItem::getWorkItemId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("1", "2", "3", "4");
        List<String> customFieldsNames = new ArrayList<>();
        list.getRecords().stream().map(DbWorkItem::getCustomFields).forEach(customMap ->
                customMap.forEach((key, value) -> customFieldsNames.add(key)));
        assertThat(customFieldsNames).containsExactlyInAnyOrder("custom_field_123456", "custom_field_456789",
                "custom_field_123456", "custom_field_456789", "custom_field_123456", "custom_field_456789",
                "custom_field_123456", "custom_field_456789");

        list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("11234"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, 0, 10);

        assertThat(list.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testAcross() throws SQLException, IOException {
        DbListResponse<DbAggregationResult> ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.project)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);
        assertNotNull(ticketReport);
        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Agile-Test", "Agile-Test-2");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2", "3");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.status_category)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("done", "completed");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("epic", "epic-2");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.assignee)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("samso");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.reporter)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("manso");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.resolution)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2", "5");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.custom_field)
                        .customAcross("custom_field_456789")
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("key");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.component)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("comp-1", "comp-2");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.attribute)
                        .attributeAcross("project")
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("attributeValue", "title");

        // timeline query
        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.workitem_created_at)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2019", "2021", "2022");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.workitem_updated_at)
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2019", "2021", "2022");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                        .aggInterval("day")
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("17-05-2019", "17-03-2021", "11-04-2022");
        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.trend)
                        .aggInterval("year")
                        .integrationIds(List.of(integrationId))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, null, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2022");
    }

    @Test
    public void testStacks() throws SQLException, IOException {
        DbListResponse<DbAggregationResult> ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.project)
                        .integrationIds(List.of(integrationId))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.assignee, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertNotNull(ticketReport);
        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Agile-Test", "Agile-Test-2");
        assertThat(ticketReport.getRecords().get(0).getStacks()
                .get(0).getAdditionalKey()).isEqualTo("samso");
        assertThat(ticketReport.getRecords().get(1).getStacks()
                .get(0).getAdditionalKey()).isEqualTo("samso");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.priority)
                        .integrationIds(List.of(integrationId))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.reporter, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("2", "3");
        assertThat(ticketReport.getRecords().get(0).getStacks()
                .get(0).getAdditionalKey()).isEqualTo("manso");

        ticketReport = esWorkItemsQueryService.getAggReport(company,
                WorkItemsFilter.builder()
                        .ingestedAt(ingestedAt)
                        .across(WorkItemsFilter.DISTINCT.assignee)
                        .integrationIds(List.of(integrationId))
                        .build(),
                WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.DISTINCT.status_category, WorkItemsFilter.CALCULATION.issue_count, null, false, null, null);

        assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        assertThat(ticketReport.getRecords().get(0).getTotalTickets()).isEqualTo(4L);
        assertThat(ticketReport.getRecords().get(0).getStacks().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("done", "completed");
    }
}
