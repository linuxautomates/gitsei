package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class BaJiraAggsDatabaseServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;

    static DataSource dataSource;

    static BaJiraAggsDatabaseService baJiraAggsDatabaseService;
    static JiraIssueService jiraIssueService;
    static String integrationId;
    private static UserIdentityService userIdentityService;
    private static OrgUnitHelper orgUnitHelper;
    private static DatabaseTestUtils.JiraTestDbs jiraTestDbs;
    private static OrgUnitsDatabaseService ouDbService;
    private static JiraFilterParser jiraFilterParser;
    private static DatabaseTestUtils.OuTestDbs ouTestDbs;

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(COMPANY, "1", displayName);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        BaJiraAggsQueryBuilder baJiraAggsQueryBuilder = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), false);
        BaJiraAggsActiveWorkQueryBuilder baJiraAggsActiveWorkQueryBuilder = new BaJiraAggsActiveWorkQueryBuilder(baJiraAggsQueryBuilder);

        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());

        baJiraAggsDatabaseService = new BaJiraAggsDatabaseService(dataSource, baJiraAggsQueryBuilder, baJiraAggsActiveWorkQueryBuilder, null, ticketCategorizationSchemeDatabaseService);

        integrationId = DatabaseTestUtils.createIntegrationId(jiraTestDbs.getIntegrationService(), COMPANY, "jira");
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        userIdentityService = jiraTestDbs.getUserIdentityService();

        String input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
        PaginatedResponse<JiraUser> jiraUsers = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(user -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(user, "1");
            jiraIssueService.insertJiraUser(COMPANY, tmp);
            if (user.getDisplayName() != null) {
                try {
                    userIdentityService.batchUpsert(COMPANY,
                            List.of(DbScmUser.builder()
                                    .integrationId("1")
                                    .cloudId(user.getAccountId())
                                    .displayName(user.getDisplayName())
                                    .originalDisplayName(user.getDisplayName())
                                    .build()));
                } catch (SQLException throwables) {
                    System.out.println("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + COMPANY + ", integration id:" + "1");
                }
            }
        });

        jiraFilterParser = jiraTestDbs.getJiraFilterParser();

        ouTestDbs = DatabaseTestUtils.setUpOuTestDbs(dataSource, COMPANY);
        orgUnitHelper = ouTestDbs.getOrgUnitHelper();
        ouDbService = ouTestDbs.getOrgUnitsDatabaseService();
    }

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("DELETE FROM " + COMPANY + ".jira_issues").execute();
    }

    public DbJiraIssue buildBaseJiraIssue(String integrationId) {
        return DbJiraIssue.builder()
                .key("LEV-123")
                .integrationId(integrationId)
                .project("LEV")
                .summary("sum")
                .descSize(3)
                .priority("high")
                .reporter("max")
                .status("Done")
                .statusCategory("Done")
                .issueType("BUG")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .issueCreatedAt(1L)
                .issueUpdatedAt(2L)
                .ingestedAt(INGESTED_AT)
                .reporter("gandalf")
                .assignee("frodo")
                .assigneeId(userIdOf("frodo").isPresent() ? userIdOf("frodo").get() : "")
                .versions(List.of("R14"))
                .fixVersions(List.of("R15"))
                .build();
    }

    private void insertMockData() throws SQLException {
        // ---- historical assignees
        List<DbJiraAssignee> lev1Assignees = List.of(
                DbJiraAssignee.builder().issueKey("LEV-1").integrationId(integrationId).assignee("gimli").startTime(1600400000L).endTime(1600500000L).build(),
                DbJiraAssignee.builder().issueKey("LEV-1").integrationId(integrationId).assignee("frodo").startTime(1600500000L).endTime(1600600000L).build());
        List<DbJiraStatus> lev1Statuses = List.of(
                DbJiraStatus.builder().issueKey("LEV-1").integrationId(integrationId).status("IN PROGRESS").startTime(1600400000L).endTime(1600450000L).build(),
                DbJiraStatus.builder().issueKey("LEV-1").integrationId(integrationId).status("IN QA").startTime(1600450000L).endTime(1600600000L).build());

        List<DbJiraAssignee> lev2Assignees = List.of(
//                DbJiraAssignee.builder().issueKey("LEV-1").integrationId(integrationId).assignee("gimli").startTime(1600400000L).endTime(1600500000L).build(),
                DbJiraAssignee.builder().issueKey("LEV-2").integrationId(integrationId).assignee("frodo").startTime(1600400000L).endTime(1600600000L).build());
        List<DbJiraStatus> lev2Statuses = List.of(
                DbJiraStatus.builder().issueKey("LEV-2").integrationId(integrationId).status("IN PROGRESS").startTime(1600400000L).endTime(160060000L).build()
//                DbJiraStatus.builder().issueKey("LEV-1").integrationId(integrationId).status("IN QA").startTime(1600450000L).endTime(1600600000L).build()
        );


        // ---- ingested_at = INGESTED_AT
        DbJiraIssue issue1 = buildBaseJiraIssue(integrationId).toBuilder().ingestedAt(INGESTED_AT).build();
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).issueResolvedAt(1600000000L).status("In QA").assigneeList(lev1Assignees).statuses(lev1Statuses).parentKey("LEV-4").parentIssueType("EPIC").parentLabels(List.of("A")).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).issueResolvedAt(1600200000L).assigneeList(lev2Assignees).statuses(lev2Statuses).parentKey("LEV-4").parentIssueType("EPIC").parentLabels(List.of("B")).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-3").issueType("TASK").statusCategory("IN PROGRESS").build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-4").issueType("EPIC").issueResolvedAt(1600300000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-5").issueType("TASK").issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-6").issueType("BUG").assignee("aragorn").storyPoints(100).issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LFE-1").issueType("TASK").project("LFE").issueResolvedAt(1600600000L).build());
        // -- ticket count:
        // frodo: total_effort = 3 tasks (ignoring 1 in progress) + 1 bug + 1 epic = 5
        // aragorn: total_effort = 1 bug

        // -- story points:
        // frodo: total_effort = 20 (bug) + 5 (task) = 25
        // aragorn: total_effort = 100 (bug)

        // ---- ingested_at = INGESTED_AT_PREV
        DbJiraIssue issue2 = buildBaseJiraIssue(integrationId).toBuilder().ingestedAt(INGESTED_AT_PREV).build();
        jiraIssueService.insert(COMPANY, issue2.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).build());
        jiraIssueService.insert(COMPANY, issue2.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).build());

    }

    private void insertMockData2() throws SQLException {

        DbJiraIssue issue1 = buildBaseJiraIssue(integrationId).toBuilder().ingestedAt(INGESTED_AT).build();
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-6").issueType("BUG").assignee("aragorn").storyPoints(100).issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-7").issueType("BUG").assignee("aragorn1").storyPoints(100).issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-8").issueType("BUG").assignee("aragorn2").storyPoints(100).issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-9").issueType("BUG").assignee("aragorn3").storyPoints(100).issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-10").issueType("BUG").assignee("aragorn4").storyPoints(100).issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-1").issueType("BUG").assignee("aragorn5").storyPoints(100).issueResolvedAt(1600400000L).build());
    }

    private JiraIssuesFilter baseFilter() {
        return JiraIssuesFilter.builder()
                .ingestedAt(INGESTED_AT)
                .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
                .ticketCategorizationFilters(List.of(
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(JiraIssuesFilter.builder().issueTypes(List.of("BUG")).build())
                                .build(),
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(1)
                                .name("cat2")
                                .filter(JiraIssuesFilter.builder().issueTypes(List.of("TASK")).build())
                                .build()
                ))
                .projects(List.of("LEV"))
                .build();
    }

    @Test
    public void testTicketCountFTE() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter();
        // -- ticket count
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Other").fte(0.2f).total(5L).effort(1L).build(), // in LEV: frodo=1 epic/5, aragorn=0/1 = 1/5 = 0.2
                DbAggregationResult.builder().key("cat1").fte(1.2f).total(6L).effort(2L).build(), // in LEV: frodo=1 bug/5, aragon=1 bug/1 = 1/5 + 1 = 1.2
                DbAggregationResult.builder().key("cat2").fte(0.4f).total(5L).effort(2L).build() // in LEV: frodo=2 tasks (LEV only)/5, aragorn=0/1 = 2/5 = 0.4
        );

        // -- story points
        response = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Other").fte(0f).total(25L).effort(0L).build(),
                DbAggregationResult.builder().key("cat1").fte(1.8f).total(125L).effort(120L).build(), // bugs: frodo=20/25 + aragorn=100/100 = 0.8+1
                DbAggregationResult.builder().key("cat2").fte(0.2f).total(25L).effort(5L).build() // tasks: frodo=5/25 = 0.2
        );

        // -- different ingested at
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter.toBuilder().ingestedAt(INGESTED_AT_PREV).build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.5f).total(2L).effort(1L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.5f).total(2L).effort(1L).build()
        );
    }

    @Test
    public void testTicketCountFTEAcrossAssignees() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter();

        // -- ticket count
        // - cat1
        filter = filter.toBuilder().ticketCategories(List.of("cat1")).build();
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").fte(1.0f).total(1L).effort(1L).build(), // in LEV: aragon=1 bug/1 = 1
                DbAggregationResult.builder().key("frodo").fte(0.2f).total(5L).effort(1L).build() // in LEV: frodo=1 bug/5 = 0.2
        );
        // - cat2
        filter = filter.toBuilder().ticketCategories(List.of("cat2")).build();
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.4f).total(5L).effort(2L).build() // in LEV: frodo=2 tasks (LEV only)/5 = 2/5 = 0.4, aragorn=0/1 = 0
        );
        // - Other
        filter = filter.toBuilder().ticketCategories(List.of("Other")).build();
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.2f).total(5L).effort(1L).build() // in LEV: frodo=1 epic/5 = 0.2, aragorn=0/1
        );

        // -- story points
        // bugs: frodo=20/25 + aragorn=100/100 = 0.8+1
        // tasks: frodo=5/25 = 0.2
        // - cat1
        filter = filter.toBuilder().ticketCategories(List.of("cat1")).build();
        response = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.ASSIGNEE, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").fte(1.0f).total(100L).effort(100L).build(),
                DbAggregationResult.builder().key("frodo").fte(0.8f).total(25L).effort(20L).build()
        );
        // - cat2
        filter = filter.toBuilder().ticketCategories(List.of("cat2")).build();
        response = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.ASSIGNEE, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.2f).total(25L).effort(5L).build()
        );
        // - Other
        filter = filter.toBuilder().ticketCategories(List.of("Other")).build();
        response = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.ASSIGNEE, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.0f).total(25L).effort(0L).build()
        );
    }

    @Test
    public void testTicketCountFTETrend() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter().toBuilder()
                .ingestedAt(123567L) // doesn't matter - will get removed
                .projects(List.of("LEV"))
//                .ticketCategories(List.of("cat1"))
                .assignees(List.of(userIdOf("frodo").isPresent() ? userIdOf("frodo").get() : ""))
                .build();

        // -- ticket count
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TREND, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1634713200").additionalKey("20-10-2021").fte(1.8f).total(16L).effort(5L).build(),
                DbAggregationResult.builder().key("1633762800").additionalKey("9-10-2021").fte(1.0f).total(4L).effort(2L).build()
        );

    }

    @Test
    public void testTicketCountFTEAcrossIssueResolvedAt() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter();

        // -- day
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ISSUE_RESOLVED_AT, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600326000").additionalKey("17-9-2020").fte(2.0f).total(2L).effort(2L).build(),
                DbAggregationResult.builder().key("1600239600").additionalKey("16-9-2020").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("1600153200").additionalKey("15-9-2020").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("1599980400").additionalKey("13-9-2020").fte(1.0f).total(1L).effort(1L).build()
        );

        // -- week
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ISSUE_RESOLVED_AT, filter.toBuilder().aggInterval("week").build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600066800").additionalKey("38-2020").fte(1.75f).total(9L).effort(4L).build(),
                DbAggregationResult.builder().key("1599462000").additionalKey("37-2020").fte(1.0f).total(1L).effort(1L).build()
        );

        // -- month
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ISSUE_RESOLVED_AT, filter.toBuilder().aggInterval("month").build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1598943600").additionalKey("9-2020").fte(1.8f).total(16L).effort(5L).build()
        );
    }

    @Test
    public void aggTimeQuery() {
//        AggTimeQueryHelper.AggTimeQuery trendAggQuery = AggTimeQueryHelper.getAggTimeQuery("ingested_at", "trend", "weeks", true);
//        DefaultObjectMapper.prettyPrint(trendAggQuery);
//        trendAggQuery = AggTimeQueryHelper.getAggTimeQuery("ingested_at", "trend", "months", true);
//        DefaultObjectMapper.prettyPrint(trendAggQuery);
//        trendAggQuery = AggTimeQueryHelper.getAggTimeQuery("ingested_at", "trend", "quarter", true);
//        DefaultObjectMapper.prettyPrint(trendAggQuery);
//        trendAggQuery = AggTimeQueryHelper.getAggTimeQuery("ingested_at", "trend", null, true);
//        DefaultObjectMapper.prettyPrint(trendAggQuery);

        AggTimeQueryHelper.AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                .columnName("issue_resolved_at")
                .across("issue_resolved_at")
                .interval("week")
                .isBigInt(true)
                .prefixWithComma(false)
                .build());
        DefaultObjectMapper.prettyPrint(aggTimeQuery);
        assertThat(aggTimeQuery.getHelperColumn().trim()).doesNotStartWith(",");
    }

    @Test
    public void testFilters() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter().toBuilder()
                .versions(List.of("R14"))
                .fixVersions(List.of("R15"))
                .build();

        // -- good filters
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Other").fte(0.2f).total(5L).effort(1L).build(), // in LEV: frodo=1 epic/5, aragorn=0/1 = 1/5 = 0.2
                DbAggregationResult.builder().key("cat1").fte(1.2f).total(6L).effort(2L).build(), // in LEV: frodo=1 bug/5, aragon=1 bug/1 = 1/5 + 1 = 1.2
                DbAggregationResult.builder().key("cat2").fte(0.4f).total(5L).effort(2L).build() // in LEV: frodo=2 tasks (LEV only)/5, aragorn=0/1 = 2/5 = 0.4
        );

        // -- bad filters
        assertThat(baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, baseFilter().toBuilder()
                .versions(List.of("R14"))
                .fixVersions(List.of("R0"))
                .build(), null, null).getRecords()).isEmpty();

        assertThat(baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, baseFilter().toBuilder()
                .versions(List.of("R0"))
                .fixVersions(List.of("R15"))
                .build(), null, null).getRecords()).isEmpty();
    }

    @Test
    public void testBaOptions() throws SQLException, BadRequestException {
        insertMockData();
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, baseFilter(), BaJiraOptions.builder()
                .completedWorkStatuses(List.of("Done")) // ignores In QA status
                .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.6f).total(5L).effort(3L).build(),
                DbAggregationResult.builder().key("Other").fte(0.2f).total(5L).effort(1L).build()
        );
    }

    @Test
    public void testTicketCountFTEUsingHistoricalAssignees() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter().toBuilder().projects(List.of()).build();
        // -- ticket count across cat
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, BaJiraOptions.builder()
                .attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.5f).total(3L).effort(2L).build(), // gimli + frodo (lev1)
                DbAggregationResult.builder().key("cat2").fte(0.5f).total(2L).effort(1L).build() // frodo (lev2)
        );

        // -- ticket count across assignee
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                BaJiraOptions.builder()
                        .attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.5f).total(2L).effort(1L).build(),
                DbAggregationResult.builder().key("gimli").fte(1.0f).total(1L).effort(1L).build()
        );

        // -- ticket count across assignee filtered by status
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                BaJiraOptions.builder()
                        .attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .historicalAssigneesStatuses(List.of("IN PROGRESS"))
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("gimli").fte(1.0f).total(1L).effort(1L).build() // only gimli did both 'in progress' and 'in qa' in cat1
        );

        // -- story points across cat
        response = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, BaJiraOptions.builder()
                .attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.8f).total(45L).effort(40L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.2f).total(25L).effort(5L).build()
        );

    }

    @Test
    public void testTicketCountFTEUsingHistoricalAssigneesFilter() throws BadRequestException, SQLException {
        insertMockData();
        String gimliId = userIdentityService.upsert(COMPANY, DbScmUser.builder()
                .integrationId("1")
                .cloudId("611b4043ee94700071830dfd")
                .displayName("gimli")
                .originalDisplayName("gimli")
                .build());

        JiraIssuesFilter filter = baseFilter().toBuilder().projects(List.of()).assignees(List.of(gimliId)).build();
        DbListResponse<DbAggregationResult> response;
        // -- without OU
        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE,
                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
                BaJiraOptions.builder()
                        .attributionMode(BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("gimli").fte(1.0f).total(1L).effort(1L).build()
        );

        // -- with OU
        // TODO
//        response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE,
//                filter.toBuilder().ticketCategories(List.of("cat1")).build(),
//                BaJiraAggsDatabaseService.BaJiraOptions.builder()
//                        .attributionMode(BaJiraAggsDatabaseService.BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES)
//                        .build(), OUConfiguration.builder()
//                                .ouId()
//                            .build());
//        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void testPagination() throws SQLException, BadRequestException, NotFoundException {
        insertMockData();
        insertMockData2();
        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, baseFilter().builder()
                        .ingestedAt(INGESTED_AT)
                        .build(),
                null, null, 0, 1);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbAggregationResultDbListResponse.getCount()).isEqualTo(1);
        dbAggregationResultDbListResponse = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, baseFilter().builder()
                        .acrossLimit(2)
                        .build(),
                null, null, 0, 1);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbAggregationResultDbListResponse.getCount()).isEqualTo(2);
        dbAggregationResultDbListResponse = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.ASSIGNEE, baseFilter().builder()
                        .ingestedAt(INGESTED_AT)
                        .acrossLimit(DefaultListRequest.DEFAULT_ACROSS_LIMIT)
                        .build(),
                null, null, 0, 2);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbAggregationResultDbListResponse.getCount()).isEqualTo(2);

        dbAggregationResultDbListResponse = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, baseFilter().builder()
                        .acrossLimit(4)
                        .build(),
                null, null, 0, 1);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbAggregationResultDbListResponse.getCount()).isEqualTo(4);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("aragorn", "aragorn1", "aragorn2", "aragorn3");

        dbAggregationResultDbListResponse = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.ASSIGNEE, baseFilter().builder()
                        .acrossLimit(10)
                        .build(),
                null, null, 4, 1);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbAggregationResultDbListResponse.getCount()).isEqualTo(7);
        DefaultObjectMapper.prettyPrint(dbAggregationResultDbListResponse);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("aragorn", "aragorn1", "aragorn2", "aragorn3", "aragorn4", "aragorn5", "frodo");
        // this is not about pagination, but doing a quick validation for SEI-2291
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getFte)
                .collect(Collectors.toList())).containsOnly(1.0f);


        dbAggregationResultDbListResponse = baJiraAggsDatabaseService.calculateStoryPointsFTE(COMPANY, JiraAcross.ASSIGNEE, baseFilter().builder()
                        .ingestedAt(INGESTED_AT)
                        .acrossLimit(DefaultListRequest.DEFAULT_ACROSS_LIMIT)
                        .build(),
                null, null, 1, 3);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbAggregationResultDbListResponse.getCount()).isEqualTo(3);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("aragorn3", "aragorn4", "aragorn5");
    }

    @Test
    public void testParentIssueTypesFilter() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = baseFilter().toBuilder()
                .parentIssueTypes(List.of("EPIC"))
                .build();
        // -- ticket count
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);

        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.2f).total(5L).effort(1L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.2f).total(5L).effort(1L).build()
        );

        /*
        LEV-1 BUG frodo parent=LEV-4 -- included
        LEV-2 TASK frodo parent=LEV-4 -- included
        LEV-4 EPIC frodo parent=null -- not included (parent issue type != epic)
        LEV-5 TASK frodo parent=null -- not included (parent issue type != epic)
        LEV-6 BUG aragorn parent=null -- not included (parent issue type != epic)
        LFE-1 TASK frodo parent=null -- not included (not in LEV and parent issue type != epic)

        cat1 bugs: frodo=1/5 arag=0/1 = 0.2
        cat2 tasks: frodo = 1/5 arg=0/1 = 0.2
         */

    }

    @Test
    public void testParentIssueTypesFilterAsCategory() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .ingestedAt(INGESTED_AT)
                .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
                .ticketCategorizationFilters(List.of(
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(JiraIssuesFilter.builder().parentIssueTypes(List.of("EPIC")).build())
                                .build()
                ))
                .projects(List.of("LEV"))
                .build();
        // -- ticket count
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);

        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.4f).total(5L).effort(2L).build(),
                DbAggregationResult.builder().key("Other").fte(1.4f).total(6L).effort(3L).build()
        );

        /*
        jiraIssueService.list(COMPANY, JiraIssuesFilter.builder()
                                .ticketCategorizationFilters(filter.getTicketCategorizationFilters())
                                .ingestedAt(filter.getIngestedAt())
                                .statusCategories(List.of("Done")).build()
                        , null, null, null, null, null, 0, 1000).getRecords()
                .forEach(r -> System.out.println(r.getKey() + " " + r.getAssignee() + " " + r.getTicketCategory()));
        */

        /*
         jiraIssueService.list(COMPANY, filter, null, null, null, null, null, 0, 1000).getRecords()
                .forEach(r -> System.out.println(r.getKey() + " " + r.getAssignee() + " " + r.getTicketCategory()));

        LEV-1 frodo cat1
        LEV-2 frodo cat1
        LEV-4 frodo cat2
        LEV-5 frodo cat2
        LEV-6 aragorn cat2
        LFE-1 frodo cat2 -- not included, not in LEV

        cat1 frodo=2/5 arag=0/1 = 0.4
        cat2 frodo = 2/5 arg=1/1 = 1.4
         */

    }

    @Test
    public void testOrFilter() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .ingestedAt(INGESTED_AT)
                .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
                .ticketCategorizationFilters(List.of(
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(JiraIssuesFilter.builder()
                                        .orFilter(JiraOrFilter.builder()
                                                .parentIssueTypes(List.of("EPIC"))
                                                .projects(List.of("LFE"))
                                                .build())

                                        .build())
                                .build()
                ))
                .build();

        Map<String, String> tickets = jiraIssueService.list(COMPANY, filter, null, null, null, null, null, 0, 1000).getRecords()
                .stream().peek(r -> System.out.println(r.getKey() + " " + r.getAssignee() + " " + r.getTicketCategory()))
                .collect(Collectors.toMap(DbJiraIssue::getKey, DbJiraIssue::getTicketCategory));
        assertThat(tickets).containsExactlyInAnyOrderEntriesOf(Map.of(
                "LEV-1", "cat1", // frodo
                "LEV-2", "cat1", // frodo
                "LEV-3", "Other", // frodo - in progress
                "LEV-4", "Other", // frodo
                "LEV-5", "Other", // frodo
                "LEV-6", "Other", // aragorn
                "LFE-1", "cat1" // frodo
        ));

        // cat1: frodo=3/5=0.6
        // other: frodo=2/5 + arag=1/1 = 1.4

        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);

        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.6f).total(5L).effort(3L).build(),
                DbAggregationResult.builder().key("Other").fte(1.4f).total(6L).effort(3L).build()
        );
    }

    @Test
    public void testPeopleBasedOuAndHistoricalAssignees() throws BadRequestException, SQLException {
        insertMockData();

        UUID ouId = ouDbService.insertForId(COMPANY, DBOrgUnit.builder()
                .name("ou1")
                .refId(1)
                .versions(Set.of(1))
                .ouCategoryId(ouTestDbs.getOrgUnitCategory().getId())
                .sections(Set.of(
                        DBOrgContentSection.builder()
                                .defaultSection(true)
                                .dynamicUsers(Map.of("custom_field_team", List.of("Exquisite Baboons")))
                                .build()))
                .build()).getLeft();
        ouDbService.update(COMPANY, ouId, true); // mark as active!

        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .ouIds(Set.of(1))
                .ouUserFilterDesignation(Map.of("jira", Set.of("assignee")))
                .filter(Map.of("ba_attribution_mode", "current_and_previous_assignees"))
                .build();

        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(COMPANY, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        assertThat(OrgUnitHelper.isOuConfigActive(ouConfig)).isTrue();

        JiraAcross across = JiraAcross.ISSUE_RESOLVED_AT;
        BaJiraOptions baOptions = BaJiraOptions.fromDefaultListRequest(request);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(COMPANY, request, null, null, null, request.getAggInterval(), false);

//        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, across, jiraFilter, baOptions, ouConfig, request.getPage(), request.getPageSize());
//        DefaultObjectMapper.prettyPrint(response);
//        jiraIssueService.list(COMPANY, jiraFilter, null, null, ouConfig, null, null, 0, 1000).getRecords()
//                .forEach(r -> System.out.println(r.getKey() + " " + r.getAssignee() + " " + r.getTicketCategory()));

        BaJiraAggsQueryBuilder queryBuilderWithoutIgnoringOU = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), false);
        BaJiraAggsQueryBuilder.Query query = queryBuilderWithoutIgnoringOU.buildIssueFTEQuery(COMPANY, jiraFilter, ouConfig, across, baOptions, Calculation.TICKET_COUNT, 0, 10);
        assertThat(query.getSql()).contains("org_user_cloud_id_mapping"); // checking that the OU users condition is present (this table is just one criterion of the condition, it could be something else)

        BaJiraAggsQueryBuilder queryBuilderWithIgnoringOU = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), true);
        BaJiraAggsQueryBuilder.Query query2 = queryBuilderWithIgnoringOU.buildIssueFTEQuery(COMPANY, jiraFilter, ouConfig, across, baOptions, Calculation.TICKET_COUNT, 0, 10);
        assertThat(query2.getSql()).doesNotContain("org_user_cloud_id_mapping"); // // checking that the OU users condition is NOT there (this table is just one criterion of the condition, it could be something else)

    }

    @Test
    public void testParentLabelsAsCategory() throws BadRequestException, SQLException {
        insertMockData();
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .ingestedAt(INGESTED_AT)
                .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
                .ticketCategorizationFilters(List.of(
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat1")
                                .filter(JiraIssuesFilter.builder().parentLabels(List.of("A")).build())
                                .build()
                ))
                .projects(List.of("LEV"))
                .build();
        // -- ticket count
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketCountFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);

        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.2f).total(5L).effort(1L).build(),
                DbAggregationResult.builder().key("Other").fte(1.6f).total(6L).effort(4L).build()
        );

    }
}