package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.ActiveWork;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.Goal;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.Goals;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.IssuesActiveWork;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.TicketCategorizationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.BaAllocation;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.models.database.TicketCategorizationScheme.Uncategorized.builder;
import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.IN_PROGRESS_STATUS_CATEGORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BaJiraAggsActiveWorkTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;
    private static final String gandalfCloudId = "c890d28e49c45d6049c4c812";
    private static final String frodoCloudId = "5d6049c417bc890d28ef70dd";
    private static final String aragornCloudId = "5d6049c4c812c40d27c01ea9";

    DataSource dataSource;

    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    BaJiraAggsDatabaseService baJiraAggsDatabaseService;
    JiraIssueService jiraIssueService;
    UserIdentityService userIdentityService;
    String integrationId;

    @Before
    public void setUp() throws Exception {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        BaJiraAggsQueryBuilder baJiraAggsQueryBuilder = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), false);
        BaJiraAggsActiveWorkQueryBuilder baJiraAggsActiveWorkQueryBuilder = new BaJiraAggsActiveWorkQueryBuilder(baJiraAggsQueryBuilder);
        ticketCategorizationSchemeDatabaseService = Mockito.mock(TicketCategorizationSchemeDatabaseService.class);
        when(ticketCategorizationSchemeDatabaseService.get(eq(COMPANY), anyString())).thenReturn(Optional.of(TicketCategorizationScheme.builder()
                .config(TicketCategorizationConfig.builder()
                        .uncategorized(builder()
                                .goals(Goals.builder()
                                        .enabled(true)
                                        .idealRange(new Goal(20, 30))
                                        .acceptableRange(new Goal(20, 30))
                                        .build())
                                .build())
                        .categories(Map.of(
                                "cat1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("cat1")
                                        .goals(Goals.builder()
                                                .enabled(true)
                                                .idealRange(new Goal(20, 30))
                                                .acceptableRange(new Goal(20, 45))
                                                .build())
                                        .build(),
                                "cat'1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("cat'1")
                                        .goals(Goals.builder()
                                                .enabled(true)
                                                .idealRange(new Goal(20, 30))
                                                .acceptableRange(new Goal(20, 45))
                                                .build())
                                        .build(),
                                "the og's", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("the og's")
                                        .goals(Goals.builder()
                                                .enabled(true)
                                                .idealRange(new Goal(20, 30))
                                                .acceptableRange(new Goal(20, 45))
                                                .build())
                                        .build(),
                                "cat2", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("cat2")
                                        .goals(Goals.builder()
                                                .enabled(true)
                                                .idealRange(new Goal(20, 30))
                                                .acceptableRange(new Goal(20, 30))
                                                .build())
                                        .build()))
                        .activeWork(ActiveWork.builder()
                                .issues(IssuesActiveWork.builder()
                                        .assigned(true)
                                        .activeSprints(true)
                                        .inProgress(true)
                                        .build())
                                .build())
                        .build())
                .build()));

        baJiraAggsDatabaseService = new BaJiraAggsDatabaseService(dataSource, baJiraAggsQueryBuilder, baJiraAggsActiveWorkQueryBuilder, null, ticketCategorizationSchemeDatabaseService);

        integrationId = DatabaseTestUtils.createIntegrationId(jiraTestDbs.getIntegrationService(), COMPANY, "jira");
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.batchUpsert(COMPANY, List.of(
                DbScmUser.builder()
                        .integrationId(integrationId)
                        .cloudId(frodoCloudId)
                        .displayName("frodo")
                        .originalDisplayName("frodo")
                        .build(),
                DbScmUser.builder()
                        .integrationId(integrationId)
                        .cloudId(gandalfCloudId)
                        .displayName("gandalf")
                        .originalDisplayName("gandalf")
                        .build(),
                DbScmUser.builder()
                        .integrationId(integrationId)
                        .cloudId(aragornCloudId)
                        .displayName("aragorn")
                        .originalDisplayName("aragorn")
                        .build()
        ));
    }

    public static DbJiraIssue buildBaseJiraIssue(String integrationId) {
        return DbJiraIssue.builder()
                .key("LEV-123")
                .integrationId(integrationId)
                .project("LEV")
                .summary("sum")
                .descSize(3)
                .priority("high")
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
                .build();
    }

    private DbJiraIssue populateDbJiraIssueUserIds(String reporter, String assignee, DbJiraIssue dbJiraIssue) {
        String reporterId = null;
        String assigneeId = null;
        String firstAssigneeId = null;
        if (StringUtils.isNotEmpty(dbJiraIssue.getReporter())) {
            if (StringUtils.isNotEmpty(reporter)) {
                reporterId = userIdentityService.getUser(COMPANY, integrationId, reporter);
            }
        }
        if (StringUtils.isNotEmpty(dbJiraIssue.getAssignee())) {
            if (StringUtils.isNotEmpty(assignee)) {
                assigneeId = userIdentityService.getUser(COMPANY, integrationId, assignee);
            }
        }
        if (StringUtils.isNotEmpty(dbJiraIssue.getFirstAssignee())) {
            Optional<String> firstAssignee = userIdentityService.getUserByDisplayName(COMPANY, integrationId,
                    dbJiraIssue.getFirstAssignee());
            if (firstAssignee.isPresent()) {
                firstAssigneeId = firstAssignee.get();
            }
        }
        return dbJiraIssue.toBuilder()
                .reporterId(reporterId)
                .assigneeId(assigneeId)
                .firstAssigneeId(firstAssigneeId)
                .build();
    }

    private void insertMockData() throws SQLException {
        jiraIssueService.insertJiraSprint(COMPANY, DbJiraSprint.builder()
                        .integrationId(Integer.valueOf(integrationId))
                        .sprintId(42)
                        .name("sprint 42")
                        .state("ACTIVE")
                        .updatedAt(10L)
                .build());

        // ---- ingested_at = INGESTED_AT
        DbJiraIssue issue1 = buildBaseJiraIssue(integrationId).toBuilder().ingestedAt(INGESTED_AT).build();
        issue1 = issue1.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).issueResolvedAt(1600000000L).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        issue1 = issue1.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).issueResolvedAt(1600200000L).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        issue1 = issue1.toBuilder().key("LEV-3").issueType("TASK").statusCategory(IN_PROGRESS_STATUS_CATEGORY).sprintIds(List.of(42)).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        issue1 = issue1.toBuilder().key("LEV-4").issueType("EPIC").issueResolvedAt(1600300000L).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        issue1 = issue1.toBuilder().key("LEV-5").issueType("TASK").issueResolvedAt(1600400000L).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        issue1 = issue1.toBuilder().key("LEV-6").issueType("BUG").assignee("aragorn").storyPoints(100).issueResolvedAt(1600400000L).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, aragornCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        issue1 = issue1.toBuilder().key("LFE-1").issueType("TASK").project("LFE").issueResolvedAt(1600600000L).build();
        issue1 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue1);
        jiraIssueService.insert(COMPANY, issue1);
        // -- ticket count:
        // frodo: total_effort = 3 tasks (ignoring 1 in progress) + 1 bug + 1 epic = 5
        // aragorn: total_effort = 1 bug

        // -- story points:
        // frodo: total_effort = 20 (bug) + 5 (task) = 25
        // aragorn: total_effort = 100 (bug)

        // ---- ingested_at = INGESTED_AT_PREV
        DbJiraIssue issue2 = buildBaseJiraIssue(integrationId).toBuilder().ingestedAt(INGESTED_AT_PREV).build();
        issue2 = issue2.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).build();
        issue2 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue2);
        jiraIssueService.insert(COMPANY, issue2);
        issue2 = issue2.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).build();
        issue2 = populateDbJiraIssueUserIds(gandalfCloudId, frodoCloudId, issue2);
        jiraIssueService.insert(COMPANY, issue2);
    }

    @Test
    public void testEscapeCharacterActiveWork() throws BadRequestException, SQLException, NotFoundException {
        insertMockData();
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .ingestedAt(INGESTED_AT)
                .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
                .ticketCategorizationFilters(List.of(
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("cat'1")
                                .filter(JiraIssuesFilter.builder().issueTypes(List.of("BUG")).build())
                                .build(),
                        JiraIssuesFilter.TicketCategorizationFilter.builder()
                                .index(0)
                                .name("the og's")
                                .filter(JiraIssuesFilter.builder().issueTypes(List.of("TASK")).build())
                                .build()
                ))
                .projects(List.of("LEV"))
                .build();
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.doCalculateActiveWork(COMPANY, JiraAcross.TICKET_CATEGORY, filter, Calculation.TICKET_COUNT, null);
        DefaultObjectMapper.prettyPrint(response);

        BaAllocation result = response.getRecords().get(0).getCategoryAllocations().get("cat'1");
        Assert.assertNotNull(result);

        result = response.getRecords().get(0).getCategoryAllocations().get("the og's");
        Assert.assertNotNull(result);
    }

    @Test
    public void testActiveWork() throws BadRequestException, SQLException, NotFoundException {
        insertMockData();
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
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

        // -- across ticket categories
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.doCalculateActiveWork(COMPANY, JiraAcross.TICKET_CATEGORY, filter, Calculation.TICKET_COUNT, null);
        DefaultObjectMapper.prettyPrint(response);

        DbAggregationResult expected = DbAggregationResult.builder()
                .alignmentScore(1)
                .percentageScore(0.17777777f)
                .categoryAllocations(Map.of(
                        "cat'1", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0f)
                                .allocation(0f)
                                .effort(0)
                                .totalEffort(6)
                                .build(),
                        "the og's", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0f)
                                .allocation(0f)
                                .effort(0)
                                .totalEffort(6)
                                .build(),
                        "cat2", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0f)
                                .allocation(0.5f)
                                .effort(3)
                                .totalEffort(6)
                                .build(),
                        "cat1", BaAllocation.builder()
                                .alignmentScore(2)
                                .percentageScore(0.88888884f)
                                .allocation(0.33333334f)
                                .effort(2)
                                .totalEffort(6)
                                .build(),
                        "Other", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.16666667f)
                                .effort(1)
                                .totalEffort(6)
                                .build()))
                .build();
        assertThat(response.getRecords())
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(expected) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(response.getRecords()))
                .containsExactlyInAnyOrder(expected);

        // -- across assignees
        response = baJiraAggsDatabaseService.doCalculateActiveWork(COMPANY, JiraAcross.ASSIGNEE, filter, Calculation.TICKET_COUNT, null);
        DefaultObjectMapper.prettyPrint(response);
        List<DbAggregationResult> expected2 = List.of(DbAggregationResult.builder()
                        .key("aragorn")
                        .alignmentScore(1)
                        .percentageScore(0f)
                        .categoryAllocations(Map.of(
                                "cat'1", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0f)
                                        .effort(0)
                                        .totalEffort(1)
                                        .build(),
                                "the og's", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0f)
                                        .effort(0)
                                        .totalEffort(1)
                                        .build(),
                                "cat2", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0.0f)
                                        .effort(0)
                                        .totalEffort(1)
                                        .build(),
                                "cat1", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(1.0f)
                                        .effort(1)
                                        .totalEffort(1)
                                        .build(),
                                "Other", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0.0f)
                                        .effort(0)
                                        .totalEffort(1)
                                        .build()))
                        .build(),
                DbAggregationResult.builder()
                        .key("frodo")
                        .alignmentScore(2)
                        .percentageScore(0.4f)
                        .categoryAllocations(Map.of(
                                "cat'1", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0f)
                                        .effort(0)
                                        .totalEffort(5)
                                        .build(),
                                "the og's", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0f)
                                        .effort(0)
                                        .totalEffort(5)
                                        .build(),
                                "cat2", BaAllocation.builder()
                                        .alignmentScore(1)
                                        .percentageScore(0f)
                                        .allocation(0.6f)
                                        .effort(3)
                                        .totalEffort(5)
                                        .build(),
                                "cat1", BaAllocation.builder()
                                        .alignmentScore(3)
                                        .percentageScore(1.f)
                                        .allocation(0.2f)
                                        .effort(1)
                                        .totalEffort(5)
                                        .build(),
                                "Other", BaAllocation.builder()
                                        .alignmentScore(3)
                                        .percentageScore(1.0f)
                                        .allocation(0.2f)
                                        .effort(1)
                                        .totalEffort(5)
                                        .build()))
                        .build());

        assertThat(response.getRecords())
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(expected2) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(response.getRecords()))
                .containsExactlyInAnyOrderElementsOf(expected2);

    }

}