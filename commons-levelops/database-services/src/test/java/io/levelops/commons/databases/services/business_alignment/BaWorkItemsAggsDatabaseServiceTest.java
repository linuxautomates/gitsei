package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.*;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTestUtils;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.*;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

public class BaWorkItemsAggsDatabaseServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static OrgUnitHelper unitsHelper;
    private static OrgUnitsDatabaseService unitsService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;
    private static Pair<UUID, Integer> ids, ids2, ids3;
    private static UserService userService;
    static OrgUsersDatabaseService orgUsersDatabaseService;
    static BaWorkItemsAggsDatabaseService baWorkItemsAggsDatabaseService;
    static WorkItemsService workItemsService;
    static WorkItemFieldsMetaService workItemFieldsMetaService;
    static WorkItemTimelineService workItemTimelineService;
    static IssuesMilestoneService issuesMilestoneService;
    static IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    static UserIdentityService userIdentityService;
    static String integrationId;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + COMPANY + " CASCADE").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        WorkItemTestUtils.TestDbs testDbs = WorkItemTestUtils.initDbServices(dataSource, COMPANY);
        integrationId = testDbs.getIntegrationService().insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azure_devops_test")
                .status("enabled")
                .build());

        userService = new UserService(dataSource, MAPPER);
        userService.ensureTableExistence(COMPANY);
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        TagsService tagsService = new TagsService(dataSource);
        TagItemDBService tagItemService = new TagItemDBService(dataSource);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, MAPPER);
        dashboardWidgetService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, MAPPER, versionsService, userIdentityService);
        unitsService = new OrgUnitsDatabaseService(dataSource, MAPPER, tagItemService, orgUsersDatabaseService, versionsService, dashboardWidgetService);
        unitsHelper = new OrgUnitHelper(unitsService, testDbs.getIntegrationService());
        ProductsDatabaseService productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(COMPANY);
        tagsService.ensureTableExistence(COMPANY);
        tagItemService.ensureTableExistence(COMPANY);
        versionsService.ensureTableExistence(COMPANY);
        new ProductService(dataSource).ensureTableExistence(COMPANY);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, MAPPER);
        orgUnitCategoryDatabaseService.ensureTableExistence(COMPANY);
        orgUsersDatabaseService.ensureTableExistence(COMPANY);
        unitsService.ensureTableExistence(COMPANY);
        workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemsService = testDbs.getWorkItemsService();
        workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemFieldsMetaService.ensureTableExistence(COMPANY);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issuesMilestoneService.ensureTableExistence(COMPANY);
        issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService = testDbs.getWorkItemsPrioritySLAService();
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        BaWorkItemsAggsQueryBuilder baWorkItemsAggsQueryBuilder = new BaWorkItemsAggsQueryBuilder(workItemFieldsMetaService);
        BaWorkItemsAggsActiveWorkQueryBuilder baWorkItemsAggsActiveWorkQueryBuilder = new BaWorkItemsAggsActiveWorkQueryBuilder(baWorkItemsAggsQueryBuilder);
        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        baWorkItemsAggsDatabaseService = new BaWorkItemsAggsDatabaseService(dataSource, baWorkItemsAggsQueryBuilder,
                baWorkItemsAggsActiveWorkQueryBuilder, null, ticketCategorizationSchemeDatabaseService);

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,MAPPER);
        ouProfileDbService.ensureTableExistence(COMPANY);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,MAPPER,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(COMPANY);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(COMPANY);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,MAPPER);
        devProductivityProfileDbService.ensureTableExistence(COMPANY);
    }

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".questionnaires").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".users").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".tags").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".questionnaire_bpracticesitem_mappings").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + COMPANY + ".workitems").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".tagitems").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".questionnaire_templates").execute();
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

        // ---- historical assignees
        List.of(
                DbWorkItemHistory.builder().workItemId("1").integrationId(integrationId).fieldType("assignee").fieldValue("gimli")
                        .startDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).endDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600500000L))).build(),
                DbWorkItemHistory.builder().workItemId("1").integrationId(integrationId).fieldType("assignee").fieldValue("frodo")
                        .startDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600500000L))).endDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600600000L))).build(),
                DbWorkItemHistory.builder().workItemId("1").integrationId(integrationId).fieldType("status").fieldValue("InProgress")
                        .startDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).endDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600450000L))).build(),
                DbWorkItemHistory.builder().workItemId("1").integrationId(integrationId).fieldType("status").fieldValue("Resolved")
                        .startDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600450000L))).endDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600600000L))).build(),
                DbWorkItemHistory.builder().workItemId("2").integrationId(integrationId).fieldType("assignee").fieldValue("frodo")
                        .startDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).endDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600600000L))).build(),
                DbWorkItemHistory.builder().workItemId("2").integrationId(integrationId).fieldType("status").fieldValue("InProgress")
                        .startDate(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).endDate(DateUtils.toTimestamp(Instant.ofEpochSecond(160060000L))).build()
        ).forEach(x -> {
            try {
                workItemTimelineService.insert(COMPANY, x);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // ---- ingested_at = INGESTED_AT
        DbWorkItem workItem1 = buildBaseWorkItem(integrationId).toBuilder().ingestedAt(INGESTED_AT).assigneeId(userId1).reporterId(userId3).build();
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("1").workItemType("Bug").storyPoint(20F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600000000L))).status("Resolved").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("2").workItemType("Task").storyPoint(5F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600200000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("3").workItemType("Task").statusCategory("InProgress").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("4").workItemType("Epic")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600300000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("5").workItemType("Task")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("6").workItemType("Bug").assignee("aragorn").assigneeId(userId2)
                .storyPoint(100F).workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("11").workItemType("Task").project("Basic-Project")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600600000L))).build());
        // -- ticket count:
        // frodo: total_effort = 3 tasks (ignoring 1 in progress) + 1 bug + 1 epic = 5
        // aragorn: total_effort = 1 bug

        // -- story points:
        // frodo: total_effort = 20 (bug) + 5 (task) = 25
        // aragorn: total_effort = 100 (bug)

        // ---- ingested_at = INGESTED_AT_PREV
        DbWorkItem workItem2 = buildBaseWorkItem(integrationId).toBuilder().ingestedAt(INGESTED_AT_PREV).build();
        workItemsService.insert(COMPANY, workItem2.toBuilder().workItemId("1").workItemType("Bug").storyPoint(20F).build());
        workItemsService.insert(COMPANY, workItem2.toBuilder().workItemId("2").workItemType("Task").storyPoint(5F).build());
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
    public void testTicketCountFTE() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        // -- ticket count
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.2f).total(6L).effort(2L).build(), // in Agile-Project: frodo=1 bug/5, aragon=1 bug/1 = 1/5 + 1 = 1.2
                DbAggregationResult.builder().key("cat2").fte(0.4f).total(5L).effort(2L).build(), // in Agile-Project: frodo=2 tasks (Agile-Project only)/5, aragorn=0/1 = 2/5 = 0.4
                DbAggregationResult.builder().key("Other").fte(0.2f).total(5L).effort(1L).build() // in Agile-Project: frodo=1 epic/5, aragorn=0/1 = 1/5 = 0.2
        );

        // -- different ingested at
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY,
                filter.toBuilder().ingestedAt(INGESTED_AT_PREV).build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.5f).total(2L).effort(1L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.5f).total(2L).effort(1L).build()
        );
    }

    @Test
    public void testStoryPointsFTE() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        // -- story points
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.8f).total(125L).effort(120L).build(), // bugs: frodo=20/25 + aragorn=100/100 = 0.8+1 = 1.8
                DbAggregationResult.builder().key("cat2").fte(0.2f).total(25L).effort(5L).build(), // tasks: frodo=5/25 = 0.2
                DbAggregationResult.builder().key("Other").fte(0f).total(25L).effort(0L).build()
        );

        // -- different ingested at
        response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY,
                filter.toBuilder().ingestedAt(INGESTED_AT_PREV).build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.8f).total(25L).effort(20L).build(), // bugs: frodo=20/25 + aragorn=100/100 = 0.8+1 = 1.8
                DbAggregationResult.builder().key("cat2").fte(0.2f).total(25L).effort(5L).build() // tasks: frodo=5/25 = 0.2
        );
    }

    @Test
    public void testTicketCountFTEAcrossAssignees() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();

        // -- ticket count
        // - cat1
        filter = filter.toBuilder().ticketCategories(List.of("cat1")).build();
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").additionalKey(response.getRecords().get(0).getAdditionalKey()).fte(1.0f).total(1L).effort(1L).build(), // in Agile-Project: aragon=1 bug/1 = 1
                DbAggregationResult.builder().key("frodo").additionalKey(response.getRecords().get(1).getAdditionalKey()).fte(0.2f).total(5L).effort(1L).build() // in Agile-Project: frodo=1 bug/5 = 0.2
        );
        // - cat2
        filter = filter.toBuilder().ticketCategories(List.of("cat2")).build();
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter,
                WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").additionalKey(response.getRecords().get(0).getAdditionalKey()).fte(0.4f).total(5L).effort(2L).build() // in Agile-Project: frodo=2 tasks (Agile-Project only)/5 = 2/5 = 0.4, aragorn=0/1 = 0
        );
        // - Unplanned
        filter = filter.toBuilder().ticketCategories(List.of("Other")).build();
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter,
                WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").additionalKey(response.getRecords().get(0).getAdditionalKey()).fte(0.2f).total(5L).effort(1L).build() // in Agile-Project: frodo=1 epic/5 = 0.2, aragorn=0/1
        );
    }

    @Test
    public void testStoryPointsFTEAcrossAssignees() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();
        // -- story points
        // bugs: frodo=20/25 + aragorn=100/100 = 0.8+1
        // tasks: frodo=5/25 = 0.2
        // - cat1
        filter = filter.toBuilder().ticketCategories(List.of("cat1")).build();
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").additionalKey(response.getRecords().get(0).getAdditionalKey()).fte(1.0f).total(100L).effort(100L).build(),
                DbAggregationResult.builder().key("frodo").additionalKey(response.getRecords().get(1).getAdditionalKey()).fte(0.8f).total(25L).effort(20L).build()
        );
        // - cat2
        filter = filter.toBuilder().ticketCategories(List.of("cat2")).build();
        response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter,
                WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").additionalKey(response.getRecords().get(0).getAdditionalKey()).fte(0.2f).total(25L).effort(5L).build()
        );
        // - Unplanned
        filter = filter.toBuilder().ticketCategories(List.of("Other")).build();
        response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE, filter,
                WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").additionalKey(response.getRecords().get(0).getAdditionalKey()).fte(0.0f).total(25L).effort(0L).build()
        );
    }

    @Test
    public void testTicketCountFTETrend() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter().toBuilder()
                .projects(List.of("Agile-Project"))
                .assignees(List.of(userIdentityService.getUser(COMPANY, integrationId, "frodo")))
                .build();
        // -- ticket count
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TREND, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1634713200").additionalKey("20-10-2021").fte(0.8f).total(15L).effort(4L).build()
        );
    }

    @Test
    public void testStoryPointsFTETrend() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter().toBuilder()
                .projects(List.of("Agile-Project"))
                .assignees(List.of(userIdentityService.getUser(COMPANY, integrationId, "frodo")))
                .build();

        // -- story points
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TREND, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1634713200").additionalKey("20-10-2021").fte(1.0f).total(75L).effort(25L).build()
        );
    }

    @Ignore
    @Test
    public void testTicketCountFTEAcrossWorkItemResolvedAt() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();

        // -- day
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600326000").additionalKey("17-9-2020").fte(2.0f).total(2L).effort(2L).build(),
                DbAggregationResult.builder().key("1600239600").additionalKey("16-9-2020").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("1600153200").additionalKey("15-9-2020").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("1599980400").additionalKey("13-9-2020").fte(1.0f).total(1L).effort(1L).build()
        );

        // -- week
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT, filter.toBuilder().aggInterval("week").build(),
                WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600066800").additionalKey("38-2020").fte(1.75f).total(9L).effort(4L).build(),
                DbAggregationResult.builder().key("1599462000").additionalKey("37-2020").fte(1.0f).total(1L).effort(1L).build()
        );

        // -- month
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT,
                filter.toBuilder().aggInterval("month").build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1598943600").additionalKey("9-2020").fte(1.8f).total(16L).effort(5L).build()
        );
    }

    @Ignore
    @Test
    public void testStoryPointsFTEAcrossWorkItemResolvedAt() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter();

        // -- day
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600326000").additionalKey("17-9-2020").fte(1.0f).total(100L).effort(100L).build(),
                DbAggregationResult.builder().key("1600239600").additionalKey("16-9-2020").fte(0.0f).total(0L).effort(0L).build(),
                DbAggregationResult.builder().key("1600153200").additionalKey("15-9-2020").fte(1.0f).total(5L).effort(5L).build(),
                DbAggregationResult.builder().key("1599980400").additionalKey("13-9-2020").fte(1.0f).total(20L).effort(20L).build()
        );

        // -- week
        response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT,
                filter.toBuilder().aggInterval("week").build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600066800").additionalKey("38-2020").fte(2.0f).total(110L).effort(105L).build(),
                DbAggregationResult.builder().key("1599462000").additionalKey("37-2020").fte(1.0f).total(20L).effort(20L).build()
        );

        // -- month
        response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.WORKITEM_RESOLVED_AT,
                filter.toBuilder().aggInterval("month").build(), WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1598943600").additionalKey("9-2020").fte(2.0f).total(175L).effort(125L).build()
        );
    }

    @Test
    public void aggTimeQuery() {
        AggTimeQueryHelper.AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                .columnName("workitem_resolved_at")
                .across("workitem_resolved_at")
                .interval("week")
                .isBigInt(true)
                .prefixWithComma(false)
                .build());
        DefaultObjectMapper.prettyPrint(aggTimeQuery);
        assertThat(aggTimeQuery.getHelperColumn().trim()).doesNotStartWith(",");
    }

    @Test
    public void testFilters() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter().toBuilder()
                .versions(List.of("14"))
                .fixVersions(List.of("15"))
                .build();

        // -- good filters
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, filter, WorkItemsMilestoneFilter.builder().build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.2f).total(6L).effort(2L).build(), // in Agile-Project: frodo=1 bug/5, aragon=1 bug/1 = 1/5 + 1 = 1.2
                DbAggregationResult.builder().key("cat2").fte(0.4f).total(5L).effort(2L).build(), // in Agile-Project: frodo=2 tasks (Agile-Project only)/5, aragorn=0/1 = 2/5 = 0.4
                DbAggregationResult.builder().key("Other").fte(0.2f).total(5L).effort(1L).build() // in Agile-Project: frodo=1 epic/5, aragorn=0/1 = 1/5 = 0.2
        );

        // -- bad filters
        assertThat(baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, baseFilter().toBuilder()
                .versions(List.of("14"))
                .fixVersions(List.of("0"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, null).getRecords()).isEmpty();

        assertThat(baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, baseFilter().toBuilder()
                .versions(List.of("0"))
                .fixVersions(List.of("15"))
                .build(), WorkItemsMilestoneFilter.builder().build(), null, null).getRecords()).isEmpty();
    }

    @Test
    public void testBaOptions() throws SQLException {
        insertMockData();
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY,
                baseFilter(), WorkItemsMilestoneFilter.builder().build(), BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .completedWorkStatuses(List.of("Done"))
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.6f).total(5L).effort(3L).build(),
                DbAggregationResult.builder().key("Other").fte(0.2f).total(5L).effort(1L).build()
        );
    }

    @Test
    public void testTicketCountFTEUsingHistoricalAssignees() throws SQLException {
        insertMockData();
        WorkItemsFilter filter = baseFilter().toBuilder().projects(List.of()).build();
        // -- ticket count across cat
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY,
                filter, WorkItemsMilestoneFilter.builder().build(), BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .attributionMode(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.5f).effort(2L).total(3L).build(), // gimli + frodo (lev1)
                DbAggregationResult.builder().key("cat2").fte(0.5f).effort(1L).total(2L).build() // frodo (lev2)
        );

        // -- ticket count across assignee
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                WorkItemsMilestoneFilter.builder().build(),
                BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .attributionMode(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.5f).effort(1L).total(2L).build(),
                DbAggregationResult.builder().key("gimli").fte(1.0f).effort(1L).total(1L).build()
        );

        // -- ticket count across assignee filtered by status
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                WorkItemsMilestoneFilter.builder().build(),
                BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .attributionMode(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .historicalAssigneesStatuses(List.of("InProgress"))
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("gimli").fte(1.0f).effort(1L).total(1L).build() // only gimli did both 'in progress' and 'in qa' in cat1
        );

        // -- story points across cat
        response = baWorkItemsAggsDatabaseService.calculateStoryPointsFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY, filter,
                WorkItemsMilestoneFilter.builder().build(),
                BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .attributionMode(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.8f).effort(40L).total(45L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.2f).effort(5L).total(25L).build()
        );
    }

    @Test
    public void testTicketCountFTEUsingHistoricalAssigneesFilter() throws SQLException {
        insertMockData();
        String gimliId = userIdentityService.upsert(COMPANY, DbScmUser.builder()
                .integrationId("1")
                .cloudId("gimli")
                .displayName("gimli")
                .originalDisplayName("gimli")
                .build());

        WorkItemsFilter filter = baseFilter().toBuilder().projects(List.of()).assignees(List.of(gimliId)).build();
        DbListResponse<DbAggregationResult> response;
        // -- without OU
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                WorkItemsMilestoneFilter.builder().build(),
                BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .attributionMode(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("gimli").fte(1.0f).effort(1L).total(1L).build()
        );

        // -- with OU
        var orgUser = DBOrgUser.builder()
                .email("frodo")
                .fullName("frodo")
                .active(true)
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("frodo").username("frodo").integrationType("azure_devops")
                        .integrationId(Integer.parseInt(integrationId)).build()))
                .versions(Set.of(1))
                .build();
        var userId = orgUsersDatabaseService.upsert(COMPANY, orgUser);
        var manager = OrgUserId.builder().id(userId.getId()).refId(userId.getRefId()).fullName(orgUser.getFullName()).email(orgUser.getEmail()).build();
        var managers = Set.of(manager);
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(COMPANY, orgGroup1);
        DBOrgUnit unit = DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integrationId))
                        .integrationFilters(Map.of("assignees", List.of("frodo")))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .build();
        ids=unitsService.insertForId(COMPANY, unit);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());
        Optional<DBOrgUnit> dbOrgUnit = unitsService.get(COMPANY, 1,true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(1)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY,
                IntegrationType.getIssueManagementIntegrationTypes(), defaultListRequest,
                dbOrgUnit.orElseThrow(), false);
        response = baWorkItemsAggsDatabaseService.calculateTicketCountFTE(COMPANY, BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                WorkItemsMilestoneFilter.builder().build(),
                BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                        .attributionMode(BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), ouConfig);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.5f).effort(1L).total(2L).build()
        );
    }
}
