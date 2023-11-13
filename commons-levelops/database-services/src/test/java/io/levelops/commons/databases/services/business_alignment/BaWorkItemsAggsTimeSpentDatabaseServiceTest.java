package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssueMgmtTestUtil;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTestUtils;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsMetadataService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@Ignore // FIXME
public class BaWorkItemsAggsTimeSpentDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    BaWorkItemsAggsDatabaseService baWorkItemsAggsDatabaseService;
    WorkItemsService workItemsService;
    WorkItemFieldsMetaService workItemFieldsMetaService;
    WorkItemTimelineService workItemTimelineService;
    IssuesMilestoneService issuesMilestoneService;
    IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    WorkItemsMetadataService workItemsMetadataService;
    UserIdentityService userIdentityService;
    WorkItemsPrioritySLAService workItemsPrioritySLAService;

    IntegrationService integrationService;
    OrgProfileDatabaseService ouProfileDbService;
    VelocityConfigsDatabaseService velocityConfigDbService;
    DevProductivityProfileDatabaseService devProductivityProfileDbService;
    CiCdJobsDatabaseService ciCdJobsDatabaseService;
    CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    String integrationId;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + COMPANY + " CASCADE").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);

        WorkItemTestUtils.TestDbs testDbs = WorkItemTestUtils.initDbServices(dataSource, COMPANY);
        integrationId = testDbs.getIntegrationService().insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azure_devops_test")
                .status("enabled")
                .build());

        workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemsService = testDbs.getWorkItemsService();
        workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemFieldsMetaService.ensureTableExistence(COMPANY);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issuesMilestoneService.ensureTableExistence(COMPANY);
        workItemsMetadataService = new WorkItemsMetadataService(dataSource);
        workItemsMetadataService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,m);
        ouProfileDbService.ensureTableExistence(COMPANY);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,m,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(COMPANY);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, m);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(COMPANY);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,m);
        devProductivityProfileDbService.ensureTableExistence(COMPANY);

        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_metadata.json");
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        List<DbIssueStatusMetadata> dbIssueStatusMetadata = new ArrayList<>();
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    dbIssueStatusMetadata.addAll(DbIssueStatusMetadata.fromAzureDevopsWorkItemMetadata(
                            String.valueOf(integrationId), project, enrichedProjectData.getMetadata()));
                });
        dbIssueStatusMetadata.forEach(
                issueStatusMetadata -> {
                    try {
                        workItemsMetadataService.insert(COMPANY, issueStatusMetadata);
                    } catch (SQLException e) {
                        log.warn("setupAzureDevopsWorkItemsMetadata: error inserting project: "
                                + issueStatusMetadata.getProjectId() + " for project id: " + issueStatusMetadata.getProjectId(), e);
                    }
                }
        );

        String historiesResourcePath = "json/databases/azure_devops_workitem_history_2.json";
        IssueMgmtTestUtil.setupHistories(COMPANY, "1", workItemTimelineService, historiesResourcePath);

        issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService = testDbs.getWorkItemsPrioritySLAService();
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        BaWorkItemsAggsQueryBuilder baWorkItemsAggsQueryBuilder = new BaWorkItemsAggsQueryBuilder(workItemFieldsMetaService);
        BaWorkItemsAggsActiveWorkQueryBuilder baWorkItemsAggsActiveWorkQueryBuilder = new BaWorkItemsAggsActiveWorkQueryBuilder(baWorkItemsAggsQueryBuilder);
        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        baWorkItemsAggsDatabaseService = new BaWorkItemsAggsDatabaseService(dataSource, baWorkItemsAggsQueryBuilder,
                baWorkItemsAggsActiveWorkQueryBuilder, null, ticketCategorizationSchemeDatabaseService);
    }

    public static DbWorkItem buildBaseWorkItem(String integrationId) {
        return DbWorkItem.builder()
                .workItemId("123")
                .integrationId(integrationId)
                .project("Agile-Project")
                .summary("summary")
                .descSize(3)
                .priority("high")
                .reporter("max")
                .status("Done")
                .statusCategory("Done")
                .workItemType("Bug")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .workItemCreatedAt(DateUtils.toTimestamp(Instant.now()))
                .workItemUpdatedAt(DateUtils.toTimestamp(Instant.now()))
                .ingestedAt(INGESTED_AT)
                .reporter("gandalf")
                .assignee("frodo")
                .versions(List.of("14"))
                .fixVersions(List.of("15"))
                .isActive(true)
                .build();
    }

    private void insertMockData() throws SQLException {
        String userId1 = userIdentityService.upsert(COMPANY, DbScmUser.builder().integrationId(integrationId).cloudId("frodo").displayName("frodo").originalDisplayName("frodo").build());
        String userId2 = userIdentityService.upsert(COMPANY, DbScmUser.builder().integrationId(integrationId).cloudId("aragorn").displayName("aragorn").originalDisplayName("aragorn").build());
        String userId3 = userIdentityService.upsert(COMPANY, DbScmUser.builder().integrationId(integrationId).cloudId("gandalf").displayName("gandalf").originalDisplayName("gandalf").build());
        // ---- ingested_at = INGESTED_AT
        DbWorkItem workItem1 = buildBaseWorkItem(integrationId).toBuilder().ingestedAt(INGESTED_AT).assigneeId(userId1).reporterId(userId3).build();
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("108").workItemType("Bug").storyPoint(20F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600000000L))).status("Resolved").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("110").workItemType("Task").storyPoint(5F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600200000L))).status("To Do").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("105").workItemType("Task").statusCategory("InProgress").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("129").workItemType("Epic")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600300000L))).status("To Do").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("107").workItemType("Task")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).status("Resolved").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("119").workItemType("Bug").assignee("aragorn").assigneeId(userId2)
                .storyPoint(100F).workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).status("New").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("120").workItemType("Task").project("Basic-Project")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600600000L))).status("New").build());

        // ---- ingested_at = INGESTED_AT_PREV
        DbWorkItem workItem2 = buildBaseWorkItem(integrationId).toBuilder().ingestedAt(INGESTED_AT_PREV).build();
        workItemsService.insert(COMPANY, workItem2.toBuilder().workItemId("108").workItemType("Bug").storyPoint(20F).build());
        workItemsService.insert(COMPANY, workItem2.toBuilder().workItemId("110").workItemType("Task").storyPoint(5F).build());
    }

    private WorkItemsFilter baseFilter() {
        return WorkItemsFilter.builder()
                .ingestedAt(INGESTED_AT)
                .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
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
                .projects(List.of("Agile-Project"))
                .build();
    }

    @Test
    public void testTicketTimeSpentFTE() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, filter, WorkItemsMilestoneFilter.builder().build(),
                null, null);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.22341174f).total(7L).effort(1L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.77658826f).total(7L).effort(5L).build()
        );

        response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY,
                filter.toBuilder().ingestedAt(INGESTED_AT_PREV).build(), WorkItemsMilestoneFilter.builder().build(),
                null, null);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.0f).total(1L).effort(1L).build()
        );
    }

    @Test
    public void testTicketTimeSpentFTEAcrossAssignees() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        // -- cat 1
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter.toBuilder()
                        .ticketCategories(List.of("cat1"))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.22341174f).total(7L).effort(1L).build()
        );
        // -- cat 2
        response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter.toBuilder()
                        .ticketCategories(List.of("cat2"))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.77658826f).total(7L).effort(5L).build()
        );
        // -- other
        response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter.toBuilder()
                        .ticketCategories(List.of("Other"))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        assertThat(response.getRecords().size()).isEqualTo(0);
    }

    @Test
    public void testTicketTimeSpentFTEAcrossWorkItemResolvedAt() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT, filter.toBuilder()
                        .ticketCategories(List.of("cat1", "Other"))
                        .assignees(List.of(userIdentityService.getUser(COMPANY, integrationId, "frodo")))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1599980400").additionalKey("13-9-2020").fte(1.0f).total(1L).effort(1L).build()
        );
    }

    @Test
    public void testTicketTimeSpentBaOptions() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, filter, WorkItemsMilestoneFilter.builder().build(),
                BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .inProgressStatuses(List.of("To Do", "New"))
                        .build(), null);
        assertThat(response.getRecords().size()).isEqualTo(2);
    }
}
